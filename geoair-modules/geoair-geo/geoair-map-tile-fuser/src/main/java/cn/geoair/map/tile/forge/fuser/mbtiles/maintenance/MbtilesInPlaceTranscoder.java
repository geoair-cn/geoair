package cn.geoair.map.tile.forge.fuser.mbtiles.maintenance;

import cn.geoair.map.tile.forge.fuser.encoding.EncodedTile;
import cn.geoair.map.tile.forge.fuser.encoding.TileImageCodec;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationContext;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationException;
import cn.geoair.map.tile.forge.fuser.mbtiles.MbtilesAccessCoordinator;
import cn.geoair.map.tile.forge.fuser.mbtiles.MbtilesAccessLease;
import cn.hutool.json.JSONUtil;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** 同文件坐标索引分页转码；逐片条件更新与恢复点同事务，应用自己保存任务及授权。 */
public class MbtilesInPlaceTranscoder {
    private final MbtilesMaintenanceDao dao;

    public MbtilesInPlaceTranscoder() {
        this(new MbtilesMaintenanceDao());
    }

    public MbtilesInPlaceTranscoder(MbtilesMaintenanceDao dao) {
        if (dao == null) throw new IllegalArgumentException("转码DAO不能为空");
        this.dao = dao;
    }

    public MbtilesTranscodeProgress execute(
            Path file,
            MbtilesTranscodeOptions input,
            Predicate<MbtilesTileKey> filter,
            BooleanSupplier cancelled,
            Consumer<MbtilesTranscodeProgress> listener)
            throws Exception {
        if (input == null || cancelled == null) throw new IllegalArgumentException("参数和取消信号不能为空");
        MbtilesTranscodeOptions options = input.copy();
        if (filter != null
                && (options.getScopeId().isEmpty() || "all".equals(options.getScopeId())))
            throw new IllegalArgumentException("自定义范围过滤必须提供稳定的范围版本标识");
        try (MbtilesAccessLease task = MbtilesAccessCoordinator.task(file, cancelled)) {
            check(cancelled);
            dao.validateTranscode(file);
            String identity = identity(file), fingerprint = fingerprint(options);
            if (options.getExpectedFileIdentity() != null
                    && !options.getExpectedFileIdentity().equals(identity))
                throw new IllegalArgumentException("缓存文件已被替换，不能继续原任务");
            String saved = dao.readCheckpoint(file, options.getCheckpointId());
            MbtilesTranscodeProgress state =
                    saved == null
                            ? new MbtilesTranscodeProgress()
                            : JSONUtil.toBean(saved, MbtilesTranscodeProgress.class);
            if (saved != null
                    && (!identity.equals(state.getSourceIdentity())
                            || !fingerprint.equals(state.getOptionsFingerprint())))
                throw new IllegalArgumentException("文件或编码/范围参数已变化，不能复用旧恢复点");
            state.setSourceIdentity(identity);
            state.setOptionsFingerprint(fingerprint);
            if (state.isComplete()) {
                publish(listener, state);
                return copy(state);
            }
            MbtilesTileKey end =
                    state.getEndZoom() == null
                            ? dao.upperBound(file, options.getMinZoom(), options.getMaxZoom())
                            : new MbtilesTileKey(
                                    state.getEndZoom(), state.getEndX(), state.getEndRow(), 0);
            if (end != null) {
                state.setEndZoom(end.getZoom());
                state.setEndX(end.getX());
                state.setEndRow(end.getRow());
            }
            if (end != null)
                while (true) {
                    check(cancelled);
                    List<MbtilesTileKey> keys =
                            dao.keys(
                                    file,
                                    options.getMinZoom(),
                                    options.getMaxZoom(),
                                    state.getCursorZoom(),
                                    state.getCursorX(),
                                    state.getCursorRow(),
                                    end,
                                    cancelled);
                    if (keys.isEmpty()) break;
                    for (MbtilesTileKey key : keys) {
                        check(cancelled);
                        state = convert(file, state, options, filter, key, cancelled);
                        publish(listener, state);
                    }
                }
            check(cancelled);
            state.setComplete(true);
            state.setTotal(state.getProcessed());
            // 完成标识也持久化；停止/进程退出后重读同一恢复点，不重复有损处理。
            dao.commit(
                    file,
                    new MbtilesTileKey(
                            state.getCursorZoom(), state.getCursorX(), state.getCursorRow(), 0),
                    null,
                    null,
                    options.getCheckpointId(),
                    JSONUtil.toJsonStr(state),
                    cancelled);
            publish(listener, state);
            return copy(state);
        } finally {
            TileOperationContext.checkCurrent();
        }
    }

    private MbtilesTranscodeProgress convert(
            Path file,
            MbtilesTranscodeProgress current,
            MbtilesTranscodeOptions options,
            Predicate<MbtilesTileKey> filter,
            MbtilesTileKey key,
            BooleanSupplier cancelled)
            throws Exception {
        MbtilesTranscodeProgress next = copy(current);
        next.setCursorZoom(key.getZoom());
        next.setCursorX(key.getX());
        next.setCursorRow(key.getRow());
        next.setProcessed(next.getProcessed() + 1);
        MbtilesTranscodeLevel level =
                next.getLevels()
                        .computeIfAbsent(
                                key.getZoom(),
                                z -> {
                                    MbtilesTranscodeLevel value = new MbtilesTranscodeLevel();
                                    value.setZoom(z);
                                    return value;
                                });
        level.setProcessed(level.getProcessed() + 1);
        byte[] original = null, replacement = null;
        // 范围计算故障是范围/调用方故障，不能吞成一批成功跳过的数据。
        if (filter != null && !filter.test(key)) {
            next.setSkipped(next.getSkipped() + 1);
            level.setSkipped(level.getSkipped() + 1);
        } else {
            original = dao.readTile(file, key, cancelled);
            check(cancelled);
            next.setChecked(next.getChecked() + 1);
            try {
                if (original == null || original.length == 0)
                    throw new IllegalArgumentException("源瓦片缺失或超过限制");
                next.setInputBytes(next.getInputBytes() + original.length);
                next.setOutputBytes(next.getOutputBytes() + original.length);
                level.setInputBytes(level.getInputBytes() + original.length);
                level.setOutputBytes(level.getOutputBytes() + original.length);
                String actual = TileImageCodec.detect(original),
                        target = TileImageCodec.canonical(options.getEncoding().getMode());
                if (actual.equals(target)
                        || options.getSourceFormat() != MbtilesSourceFormat.ANY
                                && !actual.equalsIgnoreCase(options.getSourceFormat().name())) {
                    next.setMatching(next.getMatching() + 1);
                    skip(next, level);
                } else {
                    EncodedTile encoded = TileImageCodec.encode(original, options.getEncoding());
                    if (!encoded.getFormat().equals(target)) {
                        next.setTransparent(next.getTransparent() + 1);
                        skip(next, level);
                    } else if (options.isOnlyIfSmaller()
                            && encoded.getBytes().length >= original.length) {
                        next.setNoSaving(next.getNoSaving() + 1);
                        skip(next, level);
                    } else {
                        replacement = encoded.getBytes();
                        next.setConverted(next.getConverted() + 1);
                        level.setConverted(level.getConverted() + 1);
                        next.setOutputBytes(
                                next.getOutputBytes() + replacement.length - original.length);
                        level.setOutputBytes(
                                level.getOutputBytes() + replacement.length - original.length);
                    }
                }
            } catch (TileOperationException stopped) {
                throw stopped;
            } catch (Exception malformed) {
                check(cancelled);
                next.setFailed(next.getFailed() + 1);
                level.setFailed(level.getFailed() + 1);
                next.setDetail("单张图片解码/编码失败，已保留原内容");
            }
        }
        check(cancelled);
        next.setLastProgressAt(System.currentTimeMillis());
        try {
            dao.commit(
                    file,
                    key,
                    original,
                    replacement,
                    options.getCheckpointId(),
                    JSONUtil.toJsonStr(next),
                    cancelled);
        } catch (MbtilesTileConflictException conflict) {
            if (replacement != null) {
                next.setConverted(next.getConverted() - 1);
                level.setConverted(level.getConverted() - 1);
                next.setOutputBytes(next.getOutputBytes() + original.length - replacement.length);
                level.setOutputBytes(level.getOutputBytes() + original.length - replacement.length);
            }
            next.setConflicts(next.getConflicts() + 1);
            skip(next, level);
            dao.commit(
                    file,
                    key,
                    null,
                    null,
                    options.getCheckpointId(),
                    JSONUtil.toJsonStr(next),
                    cancelled);
        }
        return next;
    }

    private void skip(MbtilesTranscodeProgress next, MbtilesTranscodeLevel level) {
        next.setSkipped(next.getSkipped() + 1);
        level.setSkipped(level.getSkipped() + 1);
    }

    private void check(BooleanSupplier cancelled) throws InterruptedException {
        TileOperationContext.checkCurrent();
        if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted())
            throw new InterruptedException("转码已停止");
    }

    private String identity(Path file) throws Exception {
        return fileIdentity(file);
    }
    /** 接入方可在提交业务任务时固定该值，用于发现没有旧恢复点的新替换文件。 */
    public static String fileIdentity(Path file) throws Exception {
        BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
        return file.toRealPath() + "|" + attrs.fileKey() + "|" + attrs.creationTime();
    }

    private String fingerprint(MbtilesTranscodeOptions options) throws Exception {
        byte[] digest =
                MessageDigest.getInstance("SHA-256")
                        .digest(JSONUtil.toJsonStr(options).getBytes(StandardCharsets.UTF_8));
        StringBuilder value = new StringBuilder();
        for (byte b : digest) value.append(String.format("%02x", b & 255));
        return value.toString();
    }

    private MbtilesTranscodeProgress copy(MbtilesTranscodeProgress value) {
        return JSONUtil.toBean(JSONUtil.toJsonStr(value), MbtilesTranscodeProgress.class);
    }

    private void publish(
            Consumer<MbtilesTranscodeProgress> listener, MbtilesTranscodeProgress value) {
        if (listener != null) listener.accept(copy(value));
    }
}
