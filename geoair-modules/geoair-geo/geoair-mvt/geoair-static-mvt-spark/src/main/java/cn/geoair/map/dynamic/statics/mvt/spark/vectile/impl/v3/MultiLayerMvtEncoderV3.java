package cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v3;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.mvt.tools.PipelineBuilder;
import cn.geoair.map.dynamic.mvt.tools.model.PbfInfo;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.MultiLayerTileSliceParameter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.MvtLayerGeometryMode;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.MvtLayerSliceParameter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.utils.TileUtils;
import cn.geoair.map.dynamic.tools.GirGeoTools;
import cn.geoair.map.dynamic.tools.grid.dto.TileZxyApo;
import no.ecc.vectortile.VectorTileEncoder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Set;

/**
 * V3 单 PBF 多 MVT 内部图层编码器。
 * <p>
 * 同一个 {@link VectorTileEncoder} 被多个图层共享，因此编码结果始终是一个完整的 MVT Tile，
 * 而不是多个 PBF 字节数组的拼接。
 * <p>
 * 本编码器同时承担 tippecanoe 的"要素编码期优化"职责，按图层依次应用
 * 字段过滤、几何简化、点聚合、特征 id 生成，并在单个 PBF 超出总大小限制时
 * 逐级提升简化级别、再按图层优先级与要素大小裁剪，尽量保留信息量大的要素。
 *
 * @author 张逢吉
 */
public final class MultiLayerMvtEncoderV3 {

    /** MVT 内部坐标的瓦片边长，与 tippecanoe 保持一致 */
    private static final int EXTENT = 4096;

    /** 未配置缓冲区时使用的默认值（tippecanoe 默认 5 像素≈8 内部坐标单位） */
    private static final int DEFAULT_BUFFER = 8;

    /** 裁剪循环的安全上限。
     * <p>每轮只削减一个图层，因此上限需要留出多图层依次削空的余量；
     * 达到上限后由 {@code encodeWithTileLimit} 的兜底清空阶段强制达标。</p>
     */
    private static final int MAX_TRIM_ROUNDS = 12;

    /** 无要素可写的瓦片编码结果：空字节数组，调用方据此跳过写出 */
    private static final byte[] EMPTY_TILE = new byte[0];

    /** 超限降级时要素数削减的下限比例：最多削到原数量的 1% */
    private static final double TRIM_FLOOR_RATIO = 0.01D;

    /** 超限降级时要素数削减的绝对下限：无论比例算出来多小，都至少保留这么多要素 */
    private static final int TRIM_FLOOR_MIN = 200;

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    private static final GiLogger LOG = GirLoggerFactory.getLogger();

    private MultiLayerMvtEncoderV3() {
    }

    /**
     * 编码一个 tileId 对应的全部内部图层。
     *
     * @param tileId Bing QuadKey
     * @param group 按内部图层归集的要素
     * @param parameter V3 任务参数
     * @return 可直接持久化的 PBF；是否 gzip 由 {@link MultiLayerTileSliceParameter#gzipPbf} 决定
     */
    public static PbfInfo encode(
            String tileId, V3TileFeatureGroup group, MultiLayerTileSliceParameter parameter) throws Exception {
        TileZxyApo zxy = GirGeoTools.defaultInstance().getTileGridBingMapOpt().quadKeyToXyz(tileId);
        Envelope envelope = TileUtils.getTileEnvelope(
                zxy.getZ(), zxy.getX(), zxy.getY(), parameter.getOutGridSrid());
        Map<String, List<GirAdvOneRow>> features = group.copyFeaturesByLayer();
        // 图层要素数限制在这里只做一次：Spark 侧每次 merge 都会触发聚合逻辑，
        // 带排序/几何序列化的密度裁剪放在那里会被放大成平方级开销
        applyFeatureLimits(features, envelope, parameter);
        // 点聚合只依赖图层配置与本瓦片范围，先做一次，后续所有编码轮次复用结果
        clusterPoints(features, envelope, parameter);
        byte[] encoded = encodeWithTileLimit(features, envelope, zxy.getZ(), parameter);
        return new PbfInfo().setData(encoded).setZoom(zxy.getZ()).setGridSrid(parameter.getOutGridSrid());
    }


    /**
     * 按图层施加单瓦片要素数限制：先按空间密度合并，仍超限再按密度丢弃，最后才按顺序截断。
     * <p>
     * 只有图层显式开启 {@code featureLimitEnabled} 时才做会改变几何的密度合并，
     * 聚合阶段的安全上限（{@code hardFeatureLimit}）只负责防止 executor OOM。
     * <p>
     * 这里用的是 V3 自己的 {@link V3FeatureUtils}，而不是 {@code AdvMvtDensityUtils}——
     * 后者被 V1/V2 与实时切片共用，V3 不应当改变它的任何行为。
     */
    private static void applyFeatureLimits(
            Map<String, List<GirAdvOneRow>> features, Envelope envelope, MultiLayerTileSliceParameter parameter) {
        boolean isGeographic = isGeographicGrid(parameter.getOutGridSrid());
        for (MvtLayerSliceParameter layer : parameter.getLayers()) {
            List<GirAdvOneRow> rows = features.get(layer.getLayerName());
            if (rows == null || rows.isEmpty() || !layer.isFeatureLimitEnabled()) {
                continue;
            }
            int limit = resolveFeatureLimit(layer);
            if (limit <= 0 || rows.size() <= limit) {
                continue;
            }
            List<GirAdvOneRow> result = rows;
            if (layer.isCoalesceDensestAsNeeded()) {
                result = V3FeatureUtils.coalesceBySpatialDensity(
                        result, limit, layer.getGeomFieldName(), envelope, isGeographic);
            }
            if (result.size() > limit && layer.isDropDensestAsNeeded()) {
                result = V3FeatureUtils.filterBySpatialDensity(
                        result, limit, layer.getGeomFieldName(), layer.getIdFieldName(),
                        envelope, isGeographic);
            }
            if (result.size() > limit) {
                result = new ArrayList<>(result.subList(0, limit));
            }
            features.put(layer.getLayerName(), result);
        }
    }

    /** 输出网格是否为经纬度坐标系（4326 / 4490） */
    private static boolean isGeographicGrid(int outGridSrid) {
        return GirGeoTools.defaultInstance().getSridOpt().isGeographicCRS(outGridSrid);
    }

    /** 有效的要素数上限：优先业务上限，其次聚合安全上限 */
    private static int resolveFeatureLimit(MvtLayerSliceParameter layer) {
        if (layer.getFeatureLimit() != null && layer.getFeatureLimit() > 0) {
            return layer.getFeatureLimit();
        }
        return layer.getHardFeatureLimit() == null ? 0 : layer.getHardFeatureLimit();
    }

    /**
     * 按 tippecanoe 的优化顺序逼近单个 PBF 的总大小限制。
     * <p>
     * 原则是<b>先降精度、再减数量；既不因为超限丢弃整格瓦片，也不因为超限改动属性</b>——
     * 地图上出现空洞、或者点击查不到属性，都比瓦片稍微大一点糟糕得多。降级阶梯：
     * <ol>
     *   <li>逐级提升几何简化级别：只降精度，要素一个不丢；</li>
     *   <li>几何降级：面/线转质心点、线抽稀到骨架；</li>
     *   <li>按图层优先级降要素数上限，直到降到该图层的下限；</li>
     *   <li>仍超限则接受当前结果并打 WARN——瓦片照样输出，不会变空。</li>
     * </ol>
     * <p>
     * <b>这里不做属性裁剪。</b>输出哪些字段完全由图层配置决定（{@code includeFields} /
     * {@code excludeFields} / {@code sysIncludeFields}），超限不是改它的理由。
     * tippecanoe 的降级手段（{@code --drop-densest-as-needed}、{@code --coalesce-*}、
     * 逐级降低 detail 等）同样只动几何与要素数，从不自动裁属性；减属性是用户用
     * {@code -x} / {@code -y} 自己做的决定，工具的职责只是执行这个决定。
     * <p>
     * 裁属性看着很划算（MVT 的属性是逐要素存 key-value，字段名要在每个要素里重复一遍，
     * 删字段名的收益远大于删值），但它会让按属性的样式过滤失效、点击查不到内容，
     * 而用户完全不知情。那属于改变用户看得见的行为，比瓦片大一点严重得多。
     * <p>只有"所有图层都没有要素"的瓦片才会返回空（天然无内容的瓦片），
     * 那是数据本身就没有覆盖到，不是因为超限。
     */
    private static byte[] encodeWithTileLimit(
            Map<String, List<GirAdvOneRow>> features,
            Envelope envelope,
            int zoom,
            MultiLayerTileSliceParameter parameter) throws Exception {
        byte[] bytes = encodeAll(features, envelope, zoom, parameter, 0);
        Long limit = parameter.isTileSizeLimitEnabled() ? parameter.getTileSizeLimitByte() : null;
        if (limit == null || limit <= 0 || bytes.length <= limit) {
            return bytes;
        }

        int maxExtraSimplify = Math.max(1, parameter.getTileSizeOptimizeRounds());
        String tileKey = "z" + zoom;

        // 第 1 级：提升简化级别。只降精度、不丢要素，代价最低。
        for (int extra = 1; extra <= maxExtraSimplify; extra++) {
            bytes = encodeAll(features, envelope, zoom, parameter, extra);
            if (bytes.length <= limit) {
                return bytes;
            }
        }

        // 第 2 级：几何降级（面/线转质心点）。仍保留每个要素，只是表达变简单。
        bytes = encodeAll(features, envelope, zoom, parameter, maxExtraSimplify, true);
        if (bytes.length <= limit) {
            LOG.info("瓦片[{}]通过简化+几何降级达标（{} 字节，限制 {} 字节）",
                    tileKey, bytes.length, limit);
            return bytes;
        }

        // 第 3 级：这才开始降要素数量。优先级低的图层先降，降到各自的下限为止。
        List<MvtLayerSliceParameter> candidates = new ArrayList<>(parameter.getLayers());
        candidates.sort(Comparator.comparingInt(MvtLayerSliceParameter::getPriority));
        for (MvtLayerSliceParameter layer : candidates) {
            if (layer.isDropSmallestAsNeeded()) {
                // 让列表按屏幕占用升序排列，后续按前缀裁剪即可丢弃最小的要素
                features.put(layer.getLayerName(),
                        sortedByScreenSize(features.get(layer.getLayerName()), layer, envelope));
            }
        }
        for (int round = 0; bytes.length > limit && round < MAX_TRIM_ROUNDS; round++) {
            if (!trimLowestPriorityLayer(features, candidates, limit, bytes.length,
                    envelope, isGeographicGrid(parameter.getOutGridSrid()))) {
                break;
            }
            bytes = encodeAll(features, envelope, zoom, parameter, maxExtraSimplify, true);
        }

        // 第 4 级：仍超限也照样输出。这里只告警，不丢格、不裁属性、不清空，保证地图上没有空洞。
        if (bytes.length > limit) {
            int encoded = countEncodedFeatures(features);
            LOG.warn("瓦片[{}]经简化、几何降级与要素裁剪后仍为 {} 字节（限制 {} 字节），"
                            + "保留 {} 个要素照常输出；如需更小请调低图层要素上限或加大简化等级",
                    tileKey, bytes.length, limit, encoded);
        }
        return bytes;
    }

    /**
     * 按优先级从低到高找到第一个仍有要素、且还没降到下限的图层，按"还差多少"等比削减它的要素数量。
     * <p>
     * <b>每层都有下限</b>（见 {@link #resolveTrimFloor}）：削减到下限即停止，绝不把图层削空。
     * 这是刻意的取舍——地图上少一片数据的观感问题，比瓦片稍微大一点严重得多。
     * <p>
     * <b>截到下限只是"不削空"，不足以保证"没空洞"</b>：削掉的是哪一批要素由
     * {@link #selectForTrim} 决定，取列表前 N 个会把整片区域一次抹掉，
     * 反倒制造出这里想避免的空洞。
     *
     * @return 是否确实削减了要素；所有图层都为空或都已到下限时返回 false
     */
    private static boolean trimLowestPriorityLayer(
            Map<String, List<GirAdvOneRow>> features,
            List<MvtLayerSliceParameter> candidates,
            long limit,
            int currentBytes,
            Envelope envelope,
            boolean isGeographic) {
        for (MvtLayerSliceParameter layer : candidates) {
            List<GirAdvOneRow> rows = features.get(layer.getLayerName());
            if (rows == null || rows.isEmpty()) {
                continue;
            }
            int floor = resolveTrimFloor(rows.size());
            if (rows.size() <= floor) {
                // 已到该图层下限，交给下一个图层
                continue;
            }
            double keepRatio = (double) limit / (double) currentBytes;
            int keep = (int) Math.floor(rows.size() * keepRatio);
            // 每轮必须真的减少要素，否则比例收敛不到限制值
            if (keep >= rows.size()) {
                keep = rows.size() - 1;
            }
            keep = Math.max(keep, floor);
            features.put(layer.getLayerName(), selectForTrim(rows, keep, layer, envelope, isGeographic));
            return true;
        }
        return false;
    }

    /**
     * 从要素列表里挑出这一轮要保留的 {@code keep} 个。
     * <p>
     * 开启 {@code dropDensestAsNeeded} 时按<b>空间密度</b>挑：低密度区域先留、高密度区域先丢，
     * 也就是 tippecanoe {@code --drop-densest-as-needed} 的语义。走的是与图层要素上限
     * 同一条实现（{@link V3FeatureUtils#filterBySpatialDensity}），两处口径一致，
     * 不会出现"按要素上限削出来是均匀的、按大小超限削出来是成片空洞的"这种自相矛盾。
     * <p>
     * 开启了 {@code dropSmallestAsNeeded} 时保持原行为：列表在进循环前已按屏幕占用升序排过，
     * 取前缀就是"先丢最小的"。两个开关都开时以它为准，避免改动既有产物。
     */
    private static List<GirAdvOneRow> selectForTrim(
            List<GirAdvOneRow> rows,
            int keep,
            MvtLayerSliceParameter layer,
            Envelope envelope,
            boolean isGeographic) {
        if (!layer.isDropSmallestAsNeeded() && layer.isDropDensestAsNeeded()) {
            return V3FeatureUtils.filterBySpatialDensity(
                    rows, keep, layer.getGeomFieldName(), layer.getIdFieldName(), envelope, isGeographic);
        }
        return new ArrayList<>(rows.subList(0, keep));
    }

    /**
     * 要素数削减的下限：超限降级时最多削到原数量的 {@value #TRIM_FLOOR_RATIO}，
     * 且不少于 {@value #TRIM_FLOOR_MIN} 个，保证图层不会被削空、地图上不会出现空洞。
     */
    private static int resolveTrimFloor(int originalSize) {
        int ratioFloor = (int) Math.ceil(originalSize * TRIM_FLOOR_RATIO);
        return Math.min(originalSize, Math.max(TRIM_FLOOR_MIN, ratioFloor));
    }

    /**
     * 按图层编码全部要素。
     *
     * @param extraSimplify 在当前图层简化级别之上额外提升的级别，用于超限时的逐级优化
     * @return 编码结果；**所有图层都没有要素可写时返回空数组**，由调用方作为"该瓦片无内容"处理
     */
    private static byte[] encodeAll(
            Map<String, List<GirAdvOneRow>> features,
            Envelope envelope,
            int zoom,
            MultiLayerTileSliceParameter parameter,
            int extraSimplify) throws Exception {
        return encodeAll(features, envelope, zoom, parameter, extraSimplify, false);
    }

    /**
     * 按图层编码全部要素，并支持超限时的几何降级。
     *
     * @param extraSimplify    在当前图层简化级别之上额外提升的级别
     * @param degradeGeometry  是否把面/线降级为质心点（要素仍在图上，只是表达变简单）
     * @return 编码结果；所有图层都没有要素可写时返回空数组
     * <p>属性始终按图层配置输出（{@link #getAttributes}），降级不改它。</p>
     */
    private static byte[] encodeAll(
            Map<String, List<GirAdvOneRow>> features,
            Envelope envelope,
            int zoom,
            MultiLayerTileSliceParameter parameter,
            int extraSimplify,
            boolean degradeGeometry) throws Exception {
        int buffer = parameter.getBuffer() == null ? DEFAULT_BUFFER : Math.max(0, parameter.getBuffer());
        VectorTileEncoder encoder = new VectorTileEncoder(EXTENT, buffer, false);
        PipelineBuilder pipeline = PipelineBuilder.newBuilder(envelope, parameter.getOutGridSrid());
        long nextFeatureId = 1L;
        int encodedFeatures = 0;
        for (MvtLayerSliceParameter layer : parameter.getLayers()) {
            List<GirAdvOneRow> rows = features.get(layer.getLayerName());
            if (rows == null || rows.isEmpty()) {
                continue;
            }
            for (GirAdvOneRow row : rows) {
                Geometry geometry = getOutputGeometry(
                        row, layer, envelope, zoom, parameter.getOutGridSrid(), extraSimplify);
                if (geometry == null || geometry.isEmpty()) {
                    continue;
                }
                if (degradeGeometry) {
                    geometry = degrade(geometry);
                    if (geometry == null || geometry.isEmpty()) {
                        continue;
                    }
                }
                Geometry screenGeometry = pipeline.transform(geometry);
                if (screenGeometry == null || screenGeometry.isEmpty()) {
                    continue;
                }
                Map<String, Object> attributes = getAttributes(row, layer);
                if (layer.isGenerateIds()) {
                    encoder.addFeature(layer.getLayerName(), attributes, screenGeometry, nextFeatureId++);
                } else {
                    encoder.addFeature(layer.getLayerName(), attributes, screenGeometry);
                }
                encodedFeatures++;
            }
        }
        // 没有任何要素真正写进 PBF 时返回空数组：
        // 这种瓦片在产物里应当表现为"不存在"，而不是一条 tile_data 为空的记录
        // （空记录会让瓦片数统计虚高，也会让下游按"存在与否"做的判断失真）
        if (encodedFeatures == 0) {
            return EMPTY_TILE;
        }
        byte[] pbf = encoder.encode();
        // 编码器仍会写出只有图层头、没有要素的字节，同样按空瓦片处理
        if (pbf.length == 0) {
            return EMPTY_TILE;
        }
        return parameter.isGzipPbf() ? V3FeatureUtils.gzip(pbf) : pbf;
    }

    /**
     * 几何降级：面取质心、线取中点，点为原样。
     * <p>
     * 这是超限时的最后手段之一——每个要素仍然会出现在图上（只是变成了一个点），
     * 而不是被丢弃，因此地图上不会出现整片空洞。
     */
    private static Geometry degrade(Geometry geometry) {
        if (geometry == null || geometry.isEmpty()) {
            return geometry;
        }
        if (geometry.getDimension() == 0) {
            return geometry;
        }
        Geometry centroid = geometry.getCentroid();
        return centroid == null || centroid.isEmpty() ? geometry : centroid;
    }

    /** 统计当前还剩多少个要素会被真正编码（几何为空的不计），仅用于日志说明 */
    private static int countEncodedFeatures(Map<String, List<GirAdvOneRow>> features) {
        int count = 0;
        for (List<GirAdvOneRow> rows : features.values()) {
            if (rows != null) {
                count += rows.size();
            }
        }
        return count;
    }

    /** 先做几何表达方式的转换，再按图层配置做几何简化。 */
    private static Geometry getOutputGeometry(
            GirAdvOneRow row,
            MvtLayerSliceParameter layer,
            Envelope envelope,
            int zoom,
            int outGridSrid,
            int extraSimplify) {
        Geometry geometry = row.getGeometry(layer.getGeomFieldName());
        if (geometry == null || geometry.isEmpty()) {
            return null;
        }
        MvtLayerGeometryMode mode = layer.getGeometryMode() == null
                ? MvtLayerGeometryMode.ORIGINAL : layer.getGeometryMode();
        if (mode == MvtLayerGeometryMode.CENTROID) {
            geometry = geometry.getCentroid();
        } else if (mode == MvtLayerGeometryMode.BOUNDARY) {
            geometry = geometry.getBoundary();
        }
        if (geometry == null || geometry.isEmpty()) {
            return null;
        }
        return simplify(geometry, layer, envelope, extraSimplify);
    }

    /**
     * 按图层配置简化几何。
     * <p>
     * 容差以瓦片宽度换算，保证不同网格和层级下简化尺度一致。面要素默认使用拓扑保持简化
     * （保留与相邻要素共享的节点），关闭共享节点保护后改用逐要素独立简化；
     * 线要素可由 {@code simplifyLines} 单独关闭简化。
     */
    private static Geometry simplify(
            Geometry geometry, MvtLayerSliceParameter layer, Envelope envelope, int extraSimplify) {
        int level = (layer.getSimplificationLevel() == null ? 0 : layer.getSimplificationLevel())
                + Math.max(0, extraSimplify);
        if (level <= 0) {
            return geometry;
        }
        double tolerance = envelope.getWidth() / EXTENT * level;
        if (tolerance <= 0) {
            return geometry;
        }
        if (isLinear(geometry)) {
            if (!layer.isSimplifyLines()) {
                return geometry;
            }
            return DouglasPeuckerSimplifier.simplify(geometry, tolerance);
        }
        if (layer.isPreserveSharedNodes()) {
            return TopologyPreservingSimplifier.simplify(geometry, tolerance);
        }
        return DouglasPeuckerSimplifier.simplify(geometry, tolerance);
    }

    private static boolean isLinear(Geometry geometry) {
        return geometry instanceof LineString || geometry instanceof MultiLineString;
    }

    /** 组装要素属性：先按白名单过滤，再扣除排除字段。 */
    private static Map<String, Object> getAttributes(GirAdvOneRow row, MvtLayerSliceParameter layer) {
        Map<String, Object> attributes = new HashMap<>();
        boolean includeAll = layer.getIncludeFields() == null || layer.getIncludeFields().isEmpty();
        List<String> excludeFields = layer.getExcludeFields();
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            String key = entry.getKey();
            if (key.equals(layer.getGeomFieldName())) {
                continue;
            }
            boolean included = includeAll || layer.getIncludeFields().contains(key)
                    || (layer.getSysIncludeFields() != null && layer.getSysIncludeFields().contains(key));
            if (included) {
                attributes.put(key, entry.getValue() == null ? "" : entry.getValue());
            }
        }
        if (excludeFields != null) {
            for (String excludeField : excludeFields) {
                attributes.remove(excludeField);
            }
        }
        if (layer.getIdFieldName() != null && !layer.getIdFieldName().trim().isEmpty()) {
            attributes.put(layer.getIdFieldName(), row.get(layer.getIdFieldName()));
        }
        return attributes;
    }

    /**
     * 按 {@code coalesceDistance} 合并本图层中相互距离过近的点要素。
     * <p>
     * 距离以屏幕单位衡量（瓦片边长按 4096 计），因此与缩放级别和输出网格无关。
     * 合并后的要素保留组内第一个要素的全部属性，几何为多点。
     */
    private static void clusterPoints(
            Map<String, List<GirAdvOneRow>> features, Envelope envelope, MultiLayerTileSliceParameter parameter) {
        double tileWidth = envelope.getWidth();
        if (tileWidth <= 0) {
            return;
        }
        for (MvtLayerSliceParameter layer : parameter.getLayers()) {
            Integer distance = layer.getCoalesceDistance();
            if (distance == null || distance <= 0) {
                continue;
            }
            List<GirAdvOneRow> rows = features.get(layer.getLayerName());
            if (rows == null || rows.size() < 2) {
                continue;
            }
            // 屏幕单位换算回数据单位
            double tolerance = tileWidth / EXTENT * distance;
            features.put(layer.getLayerName(), clusterRows(rows, layer, envelope, tolerance));
        }
    }

    /**
     * 把互相靠近的点并成多点要素。
     * <p>
     * <b>必须用空间网格，不能逐对比较。</b>原实现对每个锚点扫描它之后的全部要素、每次重新取几何；
     * 而 {@code GirAdvOneRow.getGeometry} 每次都重新解析 WKT/GeoJSON/WKB、不缓存，于是总代价是
     * {@code n²/2} 次<b>几何解析</b>——76 万点时不是"慢"，是永远跑不完。
     * <p>
     * 这里改成：几何只解析一次 → 点要素按边长 {@code = tolerance} 的网格分桶 → 每个锚点只查
     * 本格与相邻 8 格。该格边长下，同格内任意两点的 |Δx|、|Δy| 都小于 tolerance、必然同簇；
     * 而与锚点在容差内的点也必定落在锚点所在格或相邻格，所以 9 格是完备的。
     * 候选仍按下标升序合并，多点内的坐标顺序、成簇结果与原实现逐位一致。
     */
    private static List<GirAdvOneRow> clusterRows(
            List<GirAdvOneRow> rows, MvtLayerSliceParameter layer, Envelope envelope, double tolerance) {
        int size = rows.size();
        String geomField = layer.getGeomFieldName();

        // 1. 几何只解析一次，缓存坐标与"是否点要素"
        double[] xs = new double[size];
        double[] ys = new double[size];
        boolean[] pointLike = new boolean[size];
        for (int i = 0; i < size; i++) {
            Geometry geometry = rows.get(i).getGeometry(geomField);
            pointLike[i] = isPointLike(geometry);
            if (pointLike[i]) {
                Coordinate coordinate = geometry.getCoordinate();
                xs[i] = coordinate.x;
                ys[i] = coordinate.y;
            }
        }

        // 2. 点要素分桶（每桶内下标天然升序）
        Map<Long, List<Integer>> buckets = new HashMap<>();
        for (int i = 0; i < size; i++) {
            if (pointLike[i]) {
                buckets.computeIfAbsent(cellKey(xs[i], ys[i], tolerance), key -> new ArrayList<>())
                        .add(i);
            }
        }

        // 3. 按原顺序生成簇
        boolean[] consumed = new boolean[size];
        List<GirAdvOneRow> result = new ArrayList<>(size);
        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            if (consumed[i]) {
                continue;
            }
            GirAdvOneRow anchor = rows.get(i);
            if (!pointLike[i]) {
                // 非点要素不参与聚合，保持原样
                result.add(anchor);
                continue;
            }
            List<Coordinate> clusterCoordinates = new ArrayList<>();
            clusterCoordinates.add(new Coordinate(xs[i], ys[i]));
            consumed[i] = true;

            candidates.clear();
            long gx = cellIndex(xs[i], tolerance);
            long gy = cellIndex(ys[i], tolerance);
            for (long x2 = gx - 1; x2 <= gx + 1; x2++) {
                for (long y2 = gy - 1; y2 <= gy + 1; y2++) {
                    List<Integer> bucket = buckets.get(mixCell(x2, y2));
                    if (bucket == null) {
                        continue;
                    }
                    for (int j : bucket) {
                        // 与原实现一致：只并"下标更大、且尚未被合并"的点
                        if (j > i && !consumed[j]) {
                            candidates.add(j);
                        }
                    }
                }
            }
            Collections.sort(candidates);
            for (int j : candidates) {
                if (consumed[j]) {
                    continue;
                }
                if (isEnvelopeWithin(xs[i], ys[i], xs[j], ys[j], tolerance)) {
                    clusterCoordinates.add(new Coordinate(xs[j], ys[j]));
                    consumed[j] = true;
                }
            }
            if (clusterCoordinates.size() == 1) {
                result.add(anchor);
                continue;
            }
            // 用锚点要素作为属性模板；该构造函数会一并复制 TypeHandler 注册表，
            // 保证合并后的行仍能按方言正确解析几何值
            GirAdvOneRow merged = GirAdvOneRow.ofByMap(anchor);
            merged.put(geomField, toMultiPoint(clusterCoordinates));
            result.add(merged);
        }
        return result;
    }

    private static boolean isPointLike(Geometry geometry) {
        if (geometry == null || geometry.isEmpty()) {
            return false;
        }
        return geometry instanceof Point || geometry instanceof MultiPoint;
    }

    /** 网格边长取 tolerance，保证"同格必然同簇" */
    private static long cellIndex(double value, double cellSize) {
        return (long) Math.floor(value / cellSize);
    }

    private static long cellKey(double x, double y, double cellSize) {
        return mixCell(cellIndex(x, cellSize), cellIndex(y, cellSize));
    }

    /**
     * 把两个格号拼成一个 long 键。
     * <p>坐标量级下格号一定落在 int 范围（3857 最坏约 1.4e8，经纬度更小），
     * 所以高低 32 位互不影响。
     */
    private static long mixCell(long gx, long gy) {
        return (gx << 32) ^ (gy & 0xffffffffL);
    }

    private static boolean isEnvelopeWithin(double ax, double ay, double bx, double by, double tolerance) {
        return Math.abs(ax - bx) <= tolerance && Math.abs(ay - by) <= tolerance;
    }

    private static MultiPoint toMultiPoint(List<Coordinate> coordinates) {
        // 聚合后的坐标必须去重，否则 JTS 会认为多点退化。
        // 改用哈希判重并保留首次出现顺序：原来是"逐个与已收集坐标比对"，O(k²)，
        // 一个簇并进来几万个点时它自己就会先爆掉。
        // 注：±0.0 在 equals2D 下相等、但 doubleToLongBits 不同，先归一化再入集合，
        // 保证判重语义与原来的 equals2D 一致。
        Set<Coordinate> seen = new HashSet<>(Math.max(16, coordinates.size() * 2));
        List<Coordinate> distinct = new ArrayList<>(coordinates.size());
        for (Coordinate coordinate : coordinates) {
            Coordinate key = new Coordinate(
                    coordinate.x == 0D ? 0D : coordinate.x,
                    coordinate.y == 0D ? 0D : coordinate.y);
            if (seen.add(key)) {
                distinct.add(coordinate);
            }
        }
        if (distinct.size() == 1) {
            return GEOMETRY_FACTORY.createMultiPoint(new Point[]{
                    GEOMETRY_FACTORY.createPoint(distinct.get(0))});
        }
        return GEOMETRY_FACTORY.createMultiPoint(distinct.toArray(new Coordinate[0]));
    }

    /**
     * 返回按屏幕占用升序排列的要素副本。
     * <p>
     * 占用按要素外接矩形与瓦片范围的比例衡量：面取面积比，线取对角线比，点全部并列（保持原顺序）。
     * <p>
     * 关键字先整体算好再排序：{@link Location#score} 要取几何、做 JTS 计算，写在比较器里
     * 会被每个要素重复算约 log n 次。排序用稳定排序、同分保持原顺序，与逐次现算关键字的
     * 旧写法结果逐位一致（只省算力，不改顺序）。
     */
    private static List<GirAdvOneRow> sortedByScreenSize(
            List<GirAdvOneRow> rows, MvtLayerSliceParameter layer, Envelope envelope) {
        if (rows == null || rows.size() < 2) {
            return rows;
        }
        Location location = new Location(envelope);
        int size = rows.size();
        double[] scores = new double[size];
        for (int i = 0; i < size; i++) {
            scores[i] = location.score(rows.get(i), layer);
        }
        // Arrays.sort 是稳定排序，与原来 List.sort 的稳定性、比较规则（Double.compare）一致
        Integer[] order = new Integer[size];
        for (int i = 0; i < size; i++) {
            order[i] = i;
        }
        Arrays.sort(order, (left, right) -> Double.compare(scores[left], scores[right]));
        List<GirAdvOneRow> copy = new ArrayList<>(size);
        for (int index : order) {
            copy.add(rows.get(index));
        }
        return copy;
    }

    private static final class Location {

        private final double tileWidth;
        private final double tileDiagonal;

        private Location(Envelope envelope) {
            this.tileWidth = envelope.getWidth() <= 0 ? 1D : envelope.getWidth();
            this.tileDiagonal = Math.max(1e-12, Math.hypot(envelope.getWidth(), envelope.getHeight()));
        }

        private double score(GirAdvOneRow row, MvtLayerSliceParameter layer) {
            Geometry geometry = row.getGeometry(layer.getGeomFieldName());
            if (geometry == null || geometry.isEmpty()) {
                return 0D;
            }
            Envelope bounds = geometry.getEnvelopeInternal();
            if (bounds.isNull()) {
                return 0D;
            }
            if (geometry.getDimension() == 2) {
                return bounds.getArea() / (tileWidth * tileWidth);
            }
            return Math.hypot(bounds.getWidth(), bounds.getHeight()) / tileDiagonal;
        }
    }
}
