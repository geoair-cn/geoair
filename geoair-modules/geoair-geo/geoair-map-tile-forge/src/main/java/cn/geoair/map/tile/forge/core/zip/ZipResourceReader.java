package cn.geoair.map.tile.forge.core.zip;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.tile.forge.core.TileRequest;
import cn.geoair.map.tile.forge.core.config.TileTempPathConfig;
import cn.geoair.map.tile.forge.core.config.ZipReadOptions;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.utils.ExtractLockTimeoutException;
import cn.geoair.map.tile.forge.core.utils.ExtractLockUtils;
import cn.geoair.map.tile.forge.core.utils.LocalWriteUtils;
import cn.geoair.map.tile.forge.core.zip.cache.LayerPerFileDao;
import cn.geoair.map.tile.forge.core.zip.cache.ZipDirectoryGetter;
import cn.geoair.map.tile.forge.core.zip.model.CentralDirectoryModel;
import cn.geoair.map.tile.forge.core.zip.model.RootPathInfo;
import cn.geoair.web.util.GutilMimeType;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/** 索引只是加速层。缓存均有容量上限，按源版本隔离，不缓存归档正文。 */
public final class ZipResourceReader {
    private static final GiLogger LOG = GirLoggerFactory.getLogger(ZipResourceReader.class);
    private final ZipDirectoryGetter owner;
    private final ZipReadOptions options;
    private final Cache<String, RootPathInfo> roots;
    private final Cache<String, CentralDirectoryModel> entries;
    private final Cache<String, Boolean> missing;
    private final Cache<String, Boolean> unavailableIndexes;

    public ZipResourceReader(ZipDirectoryGetter owner) {
        this.owner = owner;
        options = ZipReadOptions.getInstance();
        options.validate();
        roots = CacheBuilder.newBuilder().maximumSize(options.getRootCacheEntries()).build();
        entries = CacheBuilder.newBuilder().maximumSize(options.getEntryCacheEntries()).build();
        missing =
                CacheBuilder.newBuilder()
                        .maximumSize(options.getEntryCacheEntries())
                        .expireAfterWrite(options.getMissingCacheSeconds(), TimeUnit.SECONDS)
                        .build();
        unavailableIndexes =
                CacheBuilder.newBuilder()
                        .maximumSize(options.getRootCacheEntries())
                        .expireAfterWrite(options.getMissingCacheSeconds(), TimeUnit.SECONDS)
                        .build();
    }

    public RootPathInfo root(GirLayerConfigContext context) {
        return root(context, identity(context));
    }

    private RootPathInfo root(GirLayerConfigContext context, String identity) {
        try {
            return roots.get(
                    identity,
                    () -> owner.preCheckZipAndGetRoot(context, owner.getICompressionHandler()));
        } catch (ExecutionException e) {
            throw failure(ZipReadFailure.INVALID_ARCHIVE, "无法识别ZIP数据根目录", e.getCause());
        } catch (UncheckedExecutionException e) {
            throw failure(ZipReadFailure.INVALID_ARCHIVE, "无法识别ZIP数据根目录", e.getCause());
        }
    }

    public Path directory(GirLayerConfigContext context) {
        return directory(context, identity(context));
    }

    /** ArcGIS 元数据常位于 _alllayers 的父目录，候选必须是当前数据目录的祖先。 */
    public String readArcgisMetadata(GirLayerConfigContext context, String fileName) {
        ZipPaths.relative(fileName);
        String identity = identity(context);
        String directory = root(context, identity).getRootPath();
        ICompressionHandler handler = owner.getICompressionHandler();
        try {
            CentralDirectoryModel entry =
                    entries.get(
                            identity + ":@metadata:" + fileName,
                            () -> {
                                CentralDirectoryModel[] selected = {null};
                                handler.scanAllEntries(
                                        context.getObjectKey(),
                                        (candidate, total, current) -> {
                                            String name = candidate.getName();
                                            int split = name.lastIndexOf('/') + 1;
                                            if (!candidate.isDirectoryIs()
                                                    && name.substring(split)
                                                            .equalsIgnoreCase(fileName)
                                                    && directory.startsWith(
                                                            name.substring(0, split))
                                                    && (selected[0] == null
                                                            || name.length()
                                                                    > selected[0]
                                                                            .getName()
                                                                            .length()))
                                                selected[0] = candidate;
                                            return true;
                                        });
                                if (selected[0] == null)
                                    throw new ZipReadException(
                                            ZipReadFailure.RESOURCE_NOT_FOUND,
                                            "ZIP中缺少ArcGIS配置文件",
                                            null);
                                return selected[0];
                            });
            byte[] bytes = handler.readAndDecompressEntry(entry, context.getObjectKey());
            if (!identity.equals(identity(context)))
                throw new ZipReadException(ZipReadFailure.SOURCE_CHANGED, "ZIP源已变化，请重试", null);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (ExecutionException | UncheckedExecutionException e) {
            throw failure(ZipReadFailure.INVALID_ARCHIVE, "ZIP配置文件定位失败", e.getCause());
        } catch (IOException e) {
            throw failure(ZipReadFailure.READ_FAILED, "ZIP配置文件读取失败", e);
        }
    }

    private Path directory(GirLayerConfigContext context, String identity) {
        // dataId 不参与目录拼接，防止错误配置越出缓存根；身份哈希已经包含图层、格式和源版本。
        Path root =
                Paths.get(TileTempPathConfig.getInstance().getTempPath())
                        .toAbsolutePath()
                        .normalize()
                        .resolve("zip_resources")
                        .resolve(identity);
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw failure(ZipReadFailure.EXTRACTION_FAILED, "无法创建ZIP资源缓存目录", e);
        }
        return root;
    }

    public TileRequest readTile(GirLayerConfigContext context, String relative) {
        TileRequest response = TileRequest.emptyByContext(context);
        Path path = extract(context, relative);
        if (path == null) return response;
        try {
            response.setBytes(Files.readAllBytes(path));
            response.setSize(Files.size(path));
            response.setLastModified(Files.getLastModifiedTime(path).toMillis());
            response.setMimeType(GutilMimeType.fromExtension(extension(relative)));
            response.setExists(true);
            return response;
        } catch (IOException e) {
            throw failure(ZipReadFailure.READ_FAILED, "读取ZIP提取文件失败", e);
        }
    }

    public Path extract(GirLayerConfigContext context, String relative) {
        ZipPaths.relative(relative);
        String identity = identity(context);
        Path root = directory(context, identity);
        Path target = ZipPaths.inside(root, relative);
        try {
            return ExtractLockUtils.withLock(
                    target.toString(),
                    options.getLockTimeoutMillis(),
                    () -> {
                        if (Files.isRegularFile(target)) return target;
                        CentralDirectoryModel entry = find(context, identity, relative);
                        if (entry == null) return null;
                        Files.createDirectories(target.getParent());
                        ZipPaths.inside(root, relative); // 建目录后复核，拒绝已有符号链接跳出缓存范围。
                        Path temporary = Files.createTempFile(target.getParent(), ".zip-", ".part");
                        try {
                            owner.getICompressionHandler()
                                    .readAndDecompressEntryToLocal(
                                            entry, context.getObjectKey(), temporary.toString());
                            if (!identity.equals(identity(context)))
                                throw new ZipReadException(
                                        ZipReadFailure.SOURCE_CHANGED, "ZIP源在读取期间发生变化，请重试", null);
                            LocalWriteUtils.replaceAtomically(temporary, target);
                            return target;
                        } catch (IOException e) {
                            throw failure(ZipReadFailure.EXTRACTION_FAILED, "ZIP资源提取失败", e);
                        } finally {
                            LocalWriteUtils.deleteIfExistsQuietly(temporary);
                        }
                    });
        } catch (Exception e) {
            ZipReadFailure stage =
                    Thread.currentThread().isInterrupted()
                            ? ZipReadFailure.INTERRUPTED
                            : e instanceof ExtractLockTimeoutException
                                    ? ZipReadFailure.WAIT_TIMEOUT
                                    : ZipReadFailure.EXTRACTION_FAILED;
            throw failure(stage, "ZIP资源提取未完成", e);
        }
    }

    private CentralDirectoryModel find(
            GirLayerConfigContext context, String identity, String relative) {
        String key = identity + ":" + relative;
        if (missing.getIfPresent(key) != null) return null;
        try {
            return entries.get(
                    key,
                    () -> {
                        CentralDirectoryModel indexed = indexed(context, identity, relative);
                        if (indexed != null) return indexed;
                        RootPathInfo root = root(context, identity);
                        String path =
                                relative.equals(root.getRootFileStandardName())
                                        ? root.getRootFilePath()
                                        : root.getRootPath() + relative;
                        ICompressionHandler reader = owner.getICompressionHandler();
                        CentralDirectoryModel entry =
                                reader.findEntryInCentralDir(
                                        reader.parseEocd(
                                                reader.getFileSize(context.getObjectKey()),
                                                context.getObjectKey()),
                                        path,
                                        context.getObjectKey());
                        if (entry == null || entry.isDirectoryIs()) {
                            missing.put(key, true);
                            throw new ZipReadException(
                                    ZipReadFailure.RESOURCE_NOT_FOUND, "ZIP资源不存在", null);
                        }
                        return entry;
                    });
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ZipReadException
                    && ((ZipReadException) e.getCause()).getFailure()
                            == ZipReadFailure.RESOURCE_NOT_FOUND) return null;
            throw failure(ZipReadFailure.INVALID_ARCHIVE, "ZIP条目定位失败", e.getCause());
        } catch (UncheckedExecutionException e) {
            if (e.getCause() instanceof ZipReadException
                    && ((ZipReadException) e.getCause()).getFailure()
                            == ZipReadFailure.RESOURCE_NOT_FOUND) return null;
            throw failure(ZipReadFailure.INVALID_ARCHIVE, "ZIP条目定位失败", e.getCause());
        }
    }

    private CentralDirectoryModel indexed(
            GirLayerConfigContext context, String identity, String relative) {
        if (unavailableIndexes.getIfPresent(identity) != null) return null;
        try (LayerPerFileDao index = owner.getContextHelper().getLayerPerFileDao(context)) {
            // 未标记版本的旧索引不可信，也不在请求线程下载或初始化索引文件。
            if (!identity.equals(index.getArchiveVersion())) {
                unavailableIndexes.put(identity, true);
                return null;
            }
            return index.findReadyByFileName(identity, relative);
        } catch (Exception e) {
            unavailableIndexes.put(identity, true);
            LOG.warn(
                    "ZIP索引不可用，本次从源归档读取，数据={}，原因={}",
                    context.getDataId(),
                    e.getClass().getSimpleName());
            return null;
        }
    }

    private String identity(GirLayerConfigContext context) {
        try {
            return ZipSourceIdentity.of(context, owner.getICompressionHandler());
        } catch (Exception e) {
            throw failure(ZipReadFailure.READ_FAILED, "无法读取ZIP源版本", e);
        }
    }

    private static String extension(String path) {
        int dot = path.lastIndexOf('.');
        return dot < 0 ? "bin" : path.substring(dot + 1);
    }

    private static ZipReadException failure(ZipReadFailure stage, String message, Throwable cause) {
        if (cause instanceof ZipReadException) return (ZipReadException) cause;
        if (cause instanceof SocketTimeoutException) stage = ZipReadFailure.READ_TIMEOUT;
        else if (cause instanceof InterruptedException
                || cause instanceof InterruptedIOException
                || Thread.currentThread().isInterrupted()) stage = ZipReadFailure.INTERRUPTED;
        return new ZipReadException(stage, message, cause);
    }
}
