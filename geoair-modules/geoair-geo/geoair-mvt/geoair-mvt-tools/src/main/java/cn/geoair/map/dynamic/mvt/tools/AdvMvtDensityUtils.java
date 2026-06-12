package cn.geoair.map.dynamic.mvt.tools;

import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.mvt.tools.model.*;
import cn.geoair.map.dynamic.tools.GirGeoTools;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.zip.GZIPOutputStream;
import lombok.extern.slf4j.Slf4j;
import no.ecc.vectortile.VectorTileEncoder;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.union.UnaryUnionOp;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/12/29 13:56 @description： 处理瓦片丢弃的相关处理策略，与spark无关
 */
@Slf4j
public class AdvMvtDensityUtils {

    // MVT默认扩展尺寸（像素）
    private static final int MVT_EXTENT_SIZE = 4096;

    // 3857坐标系（米）：基础简化容差（单位：米）
    private static final double BASE_TOLERANCE_3857 = 0.5;

    // 4326坐标系（度）：基础简化容差（单位：度，对应赤道0.5米）
    private static final double BASE_TOLERANCE_4326 = 0.0000045;

    // 赤道上1度对应的米数
    private static final double METERS_PER_DEGREE_EQUATOR = 111319.9;

    // 面积过滤阈值基础值（米）
    private static final double areaFilteringThreshold = 10.0;

    // 默认聚合距离（像素）
    private static final int DEFAULT_COALESCE_DISTANCE = 0;

    // 默认简化级别（0=关闭）
    private static final int DEFAULT_SIMPLIFICATION_LEVEL = 0;

    /**
     * 按密度合并高密度区域的要素（适配coalesceDensestAsNeeded参数）
     *
     * @param featureList 待合并的要素列表
     * @param limit 单瓦片要素数上限（合并后不超过该值）
     * @param geomFieldName 瓦片参数
     * @param idFieldName 瓦片参数
     * @param idFieldName 网格坐标系
     * @return 合并后的要素列表
     */
    public static List<GirAdvOneRow> doCoalesceBySpatialDensity(
            List<GirAdvOneRow> featureList,
            int limit,
            String geomFieldName,
            String idFieldName,
            int gridSrid) {
        // 1. 前置判断：若要素数未超限，直接返回
        if (featureList.size() <= limit) {
            return featureList;
        }
        double GRID_SCALE = 100.0;
        if (GirGeoTools.defaultInstance().getSridOpt().isGeographicCRS(gridSrid)) {
            GRID_SCALE = 10.0;
        }

        Map<String, Integer> gridDensityMap = new HashMap<>();
        Map<String, List<GirAdvOneRow>> gridFeatureMap = new HashMap<>(); // 网格→要素列表映射

        // 遍历要素，构建网格-要素映射 + 统计网格密度
        for (GirAdvOneRow feature : featureList) {
            try {
                Geometry geom = (Geometry) feature.get(geomFieldName);
                if (geom == null || geom.isEmpty()) continue;

                // 计算要素中心点 + 网格ID
                Coordinate center = geom.getCentroid().getCoordinate();
                Envelope env = geom.getEnvelopeInternal();
                double tileWidth = env.getMaxX() - env.getMinX();
                double tileHeight = env.getMaxY() - env.getMinY();
                double gridCellWidth = tileWidth / GRID_SCALE;
                double gridCellHeight = tileHeight / GRID_SCALE;
                int xGrid = (int) Math.floor((center.x - env.getMinX()) / gridCellWidth);
                int yGrid = (int) Math.floor((center.y - env.getMinY()) / gridCellHeight);
                String gridId = xGrid + "#" + yGrid;

                // 更新网格密度
                gridDensityMap.put(gridId, gridDensityMap.getOrDefault(gridId, 0) + 1);
                // 更新网格-要素映射
                gridFeatureMap.computeIfAbsent(gridId, k -> new ArrayList<>()).add(feature);
            } catch (Exception e) {
                // 几何计算失败：单独放入error网格，不参与合并
                gridFeatureMap.computeIfAbsent("error#error", k -> new ArrayList<>()).add(feature);
                gridDensityMap.put(
                        "error#error", gridDensityMap.getOrDefault("error#error", 0) + 1);
            }
        }

        // 3. 计算密度阈值（仅合并密度超过阈值的网格）
        // 阈值 = 平均密度 * 1.5
        double avgDensity =
                gridDensityMap.values().stream().mapToInt(Integer::intValue).average().orElse(1.0);
        double densityThreshold = avgDensity * 1.5;

        // 4. 遍历网格，合并高密度网格内的要素
        List<GirAdvOneRow> mergedResult = new ArrayList<>();
        for (Map.Entry<String, List<GirAdvOneRow>> entry : gridFeatureMap.entrySet()) {
            String gridId = entry.getKey();
            List<GirAdvOneRow> gridFeatures = entry.getValue();
            int gridDensity = gridDensityMap.getOrDefault(gridId, 0);

            // 4.1 低密度网格：直接保留所有要素
            if (gridDensity <= densityThreshold) {
                mergedResult.addAll(gridFeatures);
                continue;
            }

            // 4.2 高密度网格：合并要素（按几何类型分类合并）
            Map<String, Geometry> mergedGeomMap = new HashMap<>(); // 合并后的几何→属性映射（取第一个要素的属性）
            for (GirAdvOneRow feature : gridFeatures) {
                try {
                    Geometry geom = (Geometry) feature.get(geomFieldName);
                    if (geom == null || geom.isEmpty()) continue;

                    String geomType = geom.getGeometryType().toLowerCase();
                    // 按几何类型合并
                    switch (geomType) {
                        case "point":
                            mergePointGeoms(mergedGeomMap, geom, feature);
                            break;
                        case "multipoint":
                            mergeMultiPointGeoms(mergedGeomMap, geom, feature);
                            break;
                        case "linestring":
                            mergeLineStringGeoms(mergedGeomMap, geom, feature);
                            break;
                        case "multilinestring":
                            mergeMultiLineStringGeoms(mergedGeomMap, geom, feature);
                            break;
                        case "polygon":
                        case "multipolygon":
                            mergePolygonGeoms(mergedGeomMap, geom, feature);
                            break;
                        default:
                            // 不支持的几何类型：直接保留
                            mergedResult.add(feature);
                    }
                } catch (Exception e) {

                    mergedResult.add(feature);
                }
            }

            for (Map.Entry<String, Geometry> geomEntry : mergedGeomMap.entrySet()) {
                Geometry mergedGeom = geomEntry.getValue();
                // 从gridFeatures中取第一个要素的属性（也可自定义属性合并规则）
                GirAdvOneRow templateFeature = gridFeatures.get(0);
                GirAdvOneRow mergedFeature = copyAdvOneRow(templateFeature);
                mergedFeature.put(geomFieldName, mergedGeom);
                mergedResult.add(mergedFeature);
            }
        }

        // // 若合并后仍超限，降级为按密度丢弃（保证不超过limit）
        // if (mergedResult.size() > limit) {
        // mergedResult = doFilterBySpatialDensity(mergedResult, limit, geomFieldName,
        // idFieldName, gridSrid);
        // }

        return mergedResult;
    }

    /**
     * 聚类逻辑
     *
     * @param featureList
     * @param coalescePixel
     * @param coalesceGeo
     * @param z
     * @param parameter
     * @return
     */
    public static List<GirAdvOneRow> coalescePointFeatures(
            List<GirAdvOneRow> featureList,
            int coalescePixel,
            double coalesceGeo,
            int z,
            PbfTileParameter parameter) {

        if (featureList == null
                || featureList.isEmpty()
                || coalescePixel <= 0
                || coalesceGeo <= 0) {
            return new ArrayList<>(featureList); // 返回新列表，避免原列表被修改
        }

        Map<String, CoalesceDistanceStat> aggKeyStatMap = new HashMap<>();
        Map<String, GirAdvOneRow> aggKeyFirstRowMap = new HashMap<>(); // 存储每个聚合格网的第一个点
        List<GirAdvOneRow> nonPointFeatures = new ArrayList<>(); // 存储非点要素（直接保留）

        for (GirAdvOneRow row : featureList) {
            Geometry geom = row.getGeometry(parameter.getGeomFieldName());

            // 非点要素/空几何：直接加入非点列表
            if (geom == null || !geom.getGeometryType().equalsIgnoreCase("Point")) {
                nonPointFeatures.add(row);
                continue;
            }

            Coordinate coord = geom.getCoordinate();
            if (coord == null || Double.isNaN(coord.x) || Double.isNaN(coord.y)) {
                nonPointFeatures.add(row);
                continue;
            }

            // 计算聚合格网中心（ 将点归到格网中心）
            double aggX = Math.round(coord.x / coalesceGeo) * coalesceGeo;
            double aggY = Math.round(coord.y / coalesceGeo) * coalesceGeo;
            String aggKey = aggX + "_" + aggY;

            // 统计该格网的总点数
            CoalesceDistanceStat stat =
                    aggKeyStatMap.computeIfAbsent(aggKey, k -> new CoalesceDistanceStat());
            stat.countTotal();

            // 仅保留第一个点，并更新其坐标为格网中心
            if (stat.isFirstPoint()) {
                // // 复制行（避免修改原数据）
                // AdvOneRow aggRow = AdvOneRow.ofByMap(row); // 假设AdvOneRow有拷贝构造函数
                // // 更新几何坐标为聚合格网中心
                // GeometryFactory gf = new GeometryFactory();
                // Point aggPoint = gf.createPoint(new Coordinate(aggX, aggY));
                // aggRow.put(geomFieldName, aggPoint);
                // // 存储聚合后的第一个点
                aggKeyFirstRowMap.put(aggKey, row);
            }
        }

        // 第二步：组装最终结果（聚合点 + 非点要素）
        List<GirAdvOneRow> resultList = new ArrayList<>();
        // 处理聚合点：赋值统计字段
        for (Map.Entry<String, GirAdvOneRow> entry : aggKeyFirstRowMap.entrySet()) {
            String aggKey = entry.getKey();
            GirAdvOneRow aggRow = entry.getValue();
            CoalesceDistanceStat stat = aggKeyStatMap.get(aggKey);

            // 赋值统计字段（兼容原有代码的字段名）
            aggRow.put(VecConstant.CoalesceDistancePointCount, stat.point_count);
            aggRow.put(VecConstant.CoalesceDistanceSqrtPointCount, stat.sqrt_point_count);
            aggRow.put(VecConstant.CoalesceDistanceClustered, Boolean.TRUE);

            resultList.add(aggRow);
        }
        // 添加非点要素
        resultList.addAll(nonPointFeatures);

        if (resultList.size() != featureList.size()) {
            Set<String> sysIncludeFields = parameter.getSysIncludeFields();
            sysIncludeFields.add(VecConstant.CoalesceDistanceClustered);
            sysIncludeFields.add(VecConstant.CoalesceDistanceSqrtPointCount);
            sysIncludeFields.add(VecConstant.CoalesceDistancePointCount);
        }

        return resultList;
    }

    /**
     * 按空间密度过滤要素
     *
     * @param featureList 待合并的要素列表
     * @param limit 单瓦片要素数上限
     * @param geomFieldName 瓦片参数
     * @param idFieldName 瓦片参数
     * @return 合并后的要素列表
     */
    public static List<GirAdvOneRow> doFilterBySpatialDensity(
            List<GirAdvOneRow> featureList,
            int limit,
            String geomFieldName,
            String idFieldName,
            int gridSrid) {
        // 1. 网格大小（像素转地理坐标，适配瓦片分辨率）
        // 网格越小，密度计算越精细；此处取瓦片范围的1/100作为网格单元
        double GRID_SCALE = 100.0;
        if (GirGeoTools.defaultInstance().getSridOpt().isGeographicCRS(gridSrid)) {
            GRID_SCALE = 10.0;
        }

        // 2. 第一步：计算所有要素的中心点，并映射到网格
        // key: 网格ID（x_grid#y_grid）, value: 该网格内的要素数量（密度）
        Map<String, Integer> gridDensityMap = new HashMap<>();
        // 存储要素+对应的网格ID+中心点
        List<FeatureWithGrid> featureWithGridList = new ArrayList<>();

        for (GirAdvOneRow feature : featureList) {
            try {
                // 获取要素几何对象
                Geometry geom = (Geometry) feature.get(geomFieldName);
                if (geom == null || geom.isEmpty()) {
                    continue;
                }
                // 计算几何中心点
                Coordinate center = geom.getCentroid().getCoordinate();

                // 计算网格ID（将瓦片范围划分为GRID_SCALE*GRID_SCALE的网格）
                // 先获取瓦片范围（此处通过要素的最大/最小坐标估算瓦片边界）
                Envelope env = geom.getEnvelopeInternal();
                double tileWidth = env.getMaxX() - env.getMinX();
                double tileHeight = env.getMaxY() - env.getMinY();

                // 网格单元大小
                double gridCellWidth = tileWidth / GRID_SCALE;
                double gridCellHeight = tileHeight / GRID_SCALE;

                // 计算中心点所在网格
                int xGrid = (int) Math.floor((center.x - env.getMinX()) / gridCellWidth);
                int yGrid = (int) Math.floor((center.y - env.getMinY()) / gridCellHeight);
                String gridId = xGrid + "#" + yGrid;

                // 更新网格密度
                gridDensityMap.put(gridId, gridDensityMap.getOrDefault(gridId, 0) + 1);
                // 存储要素+网格ID
                featureWithGridList.add(new FeatureWithGrid(feature, gridId, center));
            } catch (Exception e) {
                // 几何计算失败：保留要素（避免数据丢失）
                featureWithGridList.add(new FeatureWithGrid(feature, "error#error", null));
                continue;
            }
        }

        // 3. 第二步：按网格密度升序排序（低密度要素优先保留）
        Collections.sort(
                featureWithGridList,
                new Comparator<FeatureWithGrid>() {
                    @Override
                    public int compare(FeatureWithGrid f1, FeatureWithGrid f2) {
                        // 获取两个要素所在网格的密度
                        int density1 = gridDensityMap.getOrDefault(f1.getGridId(), 1);
                        int density2 = gridDensityMap.getOrDefault(f2.getGridId(), 1);

                        // 密度升序：低密度在前
                        if (density1 != density2) {
                            return Integer.compare(density1, density2);
                        } else {
                            // 密度相同：按ID排序（保证结果可重复）
                            String id1 = getFeatureId(f1.getFeature(), geomFieldName, idFieldName);
                            String id2 = getFeatureId(f2.getFeature(), geomFieldName, idFieldName);
                            return id1.compareTo(id2);
                        }
                    }
                });

        // 4. 第三步：截取前limit个要素（保留低密度要素）
        List<GirAdvOneRow> filteredList = new ArrayList<>();
        // for (int i = 0; i < Math.min(limit, featureWithGridList.size()); i++) {
        for (int i = 0; i < Math.min(limit, featureWithGridList.size()); i++) {
            filteredList.add(featureWithGridList.get(i).getFeature());
        }

        return filteredList;
    }

    /**
     * 校验瓦片大小并执行 （适配tileSizeLimit参数）
     *
     * @param tileId 瓦片ID（zoom#row#col）
     * @param pbfData 原始PBF二进制数据
     * @param featureList 该瓦片的要素列表
     * @param envelope 瓦片地理范围
     * @param zoom 瓦片级别
     * @param parameter 瓦片参数
     * @return 优化后的PBF数据（null表示超限且无法优化）
     */
    public static PbfInfo doValidateAndOptimizeTileSize(
            String tileId,
            PbfInfo pbfData,
            List<GirAdvOneRow> featureList,
            Envelope envelope,
            int zoom,
            PbfTileParameter parameter)
            throws Exception {

        if (!parameter.isEnableFeatureSizeLimit()) {
            return pbfData;
        }

        // 获取瓦片大小限制（默认tippecanoe内置值：500KB ）
        Long tileSizeLimit =
                Optional.ofNullable(parameter.getTileSizeLimitByte()).orElse(512000L); // 500KB

        // 若未超限，直接返回
        if (pbfData == null || pbfData.getData().length <= tileSizeLimit) {
            return pbfData;
        }
        PbfTileParameter copyParameter = parameter.copy();
        // 执行二次优化（逐步提升简化级别+聚合距离）
        log.info(
                "瓦片["
                        + tileId
                        + "]原始大小："
                        + pbfData.getData().length
                        + "B，超过限制："
                        + tileSizeLimit
                        + "B，执行二次优化");
        PbfInfo optimizedPbf = pbfData;
        List<GirAdvOneRow> optimizedFeatureList = new ArrayList<>(featureList);

        // 4.1 第一步：提升简化级别（每次+1，最多提升3级）
        int originSimplifyLevel =
                Optional.ofNullable(copyParameter.getSimplificationLevel()).orElse(0);
        int maxSimplifyLevel = originSimplifyLevel + 3;
        for (int level = originSimplifyLevel + 1; level <= maxSimplifyLevel; level++) {
            copyParameter.setSimplificationLevel(level);
            optimizedPbf =
                    doGetPbfByteByGeotools(optimizedFeatureList, envelope, zoom, copyParameter);
            if (optimizedPbf != null && optimizedPbf.getData().length <= tileSizeLimit) {
                log.info(
                        "瓦片["
                                + tileId
                                + "]优化后大小："
                                + optimizedPbf.getData().length
                                + "B（提升简化级别至"
                                + level
                                + "）");
                // 恢复原始简化级别
                copyParameter.setSimplificationLevel(originSimplifyLevel);
                return optimizedPbf;
            }
        }

        // 降级为按密度丢弃要素
        if (copyParameter.isDropDensestAsNeeded() && copyParameter.getFeatureLimit() != null) {
            int originFeatureLimit = copyParameter.getFeatureLimit();
            // 逐步降低要素数限制（每次减少20%）
            for (int i = 0; i < 3; i++) {
                int newLimit = (int) (originFeatureLimit * (0.8 - i * 0.2));
                if (newLimit <= 0) break;
                copyParameter.setFeatureLimit(newLimit);
                // 重新聚合要素并生成瓦片
                List<GirAdvOneRow> filteredList =
                        doFilterBySpatialDensity(
                                optimizedFeatureList,
                                newLimit,
                                copyParameter.getGeomFieldName(),
                                copyParameter.getIdFieldName(),
                                parameter.getOutGridSrid());
                optimizedPbf = doGetPbfByteByGeotools(filteredList, envelope, zoom, copyParameter);
                if (optimizedPbf != null && optimizedPbf.getData().length <= tileSizeLimit) {
                    log.info(
                            "瓦片["
                                    + tileId
                                    + "]优化后大小："
                                    + optimizedPbf.getData().length
                                    + "B（要素数限制降至"
                                    + newLimit
                                    + "）");
                    // 恢复原始要素数限制
                    copyParameter.setFeatureLimit(originFeatureLimit);
                    return optimizedPbf;
                }
            }
        }

        // 所有优化策略失效：返回null（丢弃该瓦片）
        // if (optimizedPbf != null) {
        log.info(
                "瓦片["
                        + tileId
                        + "]优化失败，大小仍为"
                        + optimizedPbf.getData().length
                        + "B，超过限制"
                        + tileSizeLimit
                        + "B ");
        // }
        // 恢复原始参数
        parameter.setSimplificationLevel(originSimplifyLevel);
        return optimizedPbf;
    }

    /**
     * 入参替换为 PbfTileParameter，解耦 TileSliceParameter 主要可以用于后期实时矢量瓦片的优化
     *
     * @param rs 要素列表
     * @param envelope 瓦片范围
     * @param zoom 瓦片层级
     * @param pbfParam 提取后的PBF专用参数
     * @return PbfInfo
     * @throws Exception
     */
    public static PbfInfo doGetPbfByteByGeotools(
            List<GirAdvOneRow> rs, Envelope envelope, int zoom, PbfTileParameter pbfParam)
            throws Exception {
        int simplifyLevel =
                Optional.ofNullable(pbfParam.getSimplificationLevel())
                        .orElse(DEFAULT_SIMPLIFICATION_LEVEL);
        int coalescePixel =
                Optional.ofNullable(pbfParam.getCoalesceDistance())
                        .orElse(DEFAULT_COALESCE_DISTANCE);
        double pixelToGeo = (envelope.getMaxX() - envelope.getMinX()) / MVT_EXTENT_SIZE; // 聚合像素阈值
        int srid = Optional.of(pbfParam.getOutGridSrid()).orElse(3857);

        // 地理坐标系（4326）纬度修正
        if (GirGeoTools.defaultInstance().getSridOpt().isGeographicCRS(srid)) {
            double centerLat = (envelope.getMinY() + envelope.getMaxY()) / 2;
            centerLat = Math.max(-89.9, Math.min(89.9, centerLat));
            double latCorrection = Math.cos(Math.toRadians(centerLat));
            pixelToGeo = pixelToGeo * latCorrection;
        }

        double coalesceGeo = coalescePixel * pixelToGeo; // 像素值转换为地理坐标单位
        VectorTileEncoder encoder = new VectorTileEncoder(4096, 8, false);
        VectorTileEncoder encoderLabel = null;
        VectorTileEncoder encoderBoundary = null;

        // 初始化标签/边界编码器
        if (pbfParam.isCreateLabel()) {
            encoderLabel = new VectorTileEncoder(4096, 8, false);
        }
        if (pbfParam.isCreateBoundary()) {
            encoderBoundary = new VectorTileEncoder(4096, 8, false);
        }

        // ===================== 2. 处理聚类 =====================
        rs = coalescePointFeatures(rs, coalescePixel, coalesceGeo, zoom, pbfParam);

        for (Map<String, Object> feature : rs) {
            // 获取几何对象
            Geometry geom = (Geometry) feature.get(pbfParam.getGeomFieldName());
            if (geom == null || geom.isEmpty()) continue;

            Object featureRowIDValue = feature.get(VecConstant.FeatureRowID);
            feature.remove(VecConstant.FeatureRowID); // 手动生成的Id不放到pbf里面

            // 过滤需要保留的字段（includeFields + sysIncludeFields）
            Map<String, Object> featureCopy = new HashMap<>();
            Set<Map.Entry<String, Object>> entries = feature.entrySet();
            for (Map.Entry<String, Object> entry : entries) {
                Object value = entry.getValue();
                if (value == null) {
                    value = "";
                }
                // 保留用户指定字段
                if (!pbfParam.getIncludeFields().isEmpty()
                        && pbfParam.getIncludeFields().contains(entry.getKey())) {
                    featureCopy.put(entry.getKey(), value);
                }
                // 保留系统指定字段
                if (!pbfParam.getSysIncludeFields().isEmpty()
                        && pbfParam.getSysIncludeFields().contains(entry.getKey())) {
                    featureCopy.put(entry.getKey(), value);
                }
            }
            // 强制保留ID字段和几何字段（核心字段）
            featureCopy.put(pbfParam.getIdFieldName(), feature.get(pbfParam.getIdFieldName()));
            featureCopy.put(pbfParam.getGeomFieldName(), geom);

            // 几何简化
            if (simplifyLevel > 0) {
                double tolerance = calculateDynamicTolerance(srid, zoom, envelope, simplifyLevel);
                geom = TopologyPreservingSimplifier.simplify(geom, tolerance);
                if (geom.isEmpty()) continue;
            }

            // 低层级面积过滤
            if (zoom < 5) {
                double area = geom.getArea();
                double areaThreshold = calculateAreaThreshold(srid, zoom, envelope); // 动态计算
                if (area <= areaThreshold) continue;
            }

            // 移除几何字段（避免重复）
            featureCopy.remove(pbfParam.getGeomFieldName());
            if (pbfParam.isOnly()) {
                PPbfType pPbfType = pbfParam.getPPbfType();

                PipelineBuilder pipelineBuilder =
                        PipelineBuilder.newBuilder(envelope, pbfParam.getOutGridSrid());

                if (PPbfType.rootPbf.equals(pPbfType)) {
                    // 主图层逻辑
                    Geometry transform = pipelineBuilder.transform(geom);
                    if (transform != null && StrUtil.isNotBlank(pbfParam.getLayerName())) {
                        encoder.addFeature(pbfParam.getLayerName(), featureCopy, transform);
                    }
                } else if (PPbfType.Label.equals(pPbfType)) {
                    // Label图层逻辑（增加前置校验）
                    if (pbfParam.isCreateLabel()
                            && StrUtil.isNotBlank(pbfParam.getLayerNameLabel())) {
                        Point centroid = geom.getCentroid();
                        if (centroid != null) { // 校验几何对象非空
                            Geometry transformLabel = pipelineBuilder.transform(centroid);
                            encoderLabel.addFeature(
                                    pbfParam.getLayerNameLabel(), featureCopy, transformLabel);
                        } else {
                            log.warn("几何对象的中心点为空，跳过Label图层生成：{}", geom);
                        }
                    }
                } else if (PPbfType.Boundary.equals(pPbfType)) {
                    // 边界图层逻辑（增加前置校验）
                    if (pbfParam.isCreateBoundary()
                            && StrUtil.isNotBlank(pbfParam.getLayerNameBoundary())) {
                        Geometry boundary = geom.getBoundary();
                        if (boundary != null && !boundary.isEmpty()) { // 校验边界非空且非空几何
                            Geometry transformBoundary = pipelineBuilder.transform(boundary);
                            encoderBoundary.addFeature(
                                    pbfParam.getLayerNameBoundary(),
                                    featureCopy,
                                    transformBoundary);
                        } else {
                            log.warn("几何对象的边界为空，跳过分界图层生成：{}", geom);
                        }
                    }
                } else {
                    log.warn("不支持的PPbfType类型，跳过Only模式处理：{}", pPbfType);
                }
            } else {
                PipelineBuilder pipelineBuilder =
                        PipelineBuilder.newBuilder(envelope, pbfParam.getOutGridSrid());
                if (StrUtil.isNotBlank(pbfParam.getLayerName())) {
                    Geometry transform = pipelineBuilder.transform(geom);
                    if (transform != null) {
                        encoder.addFeature(pbfParam.getLayerName(), featureCopy, transform);
                    }
                }
                if (pbfParam.isCreateLabel() && StrUtil.isNotBlank(pbfParam.getLayerNameLabel())) {
                    Point centroid = geom.getCentroid();
                    if (centroid != null) {
                        Geometry transformLabel = pipelineBuilder.transform(centroid);
                        encoderLabel.addFeature(
                                pbfParam.getLayerNameLabel(), featureCopy, transformLabel);
                    } else {
                        log.warn("几何对象的中心点为空，跳过Label图层生成：{}", geom);
                    }
                }
                if (pbfParam.isCreateBoundary()
                        && StrUtil.isNotBlank(pbfParam.getLayerNameBoundary())) {
                    Geometry boundary = geom.getBoundary();
                    if (boundary != null && !boundary.isEmpty()) {
                        Geometry transformBoundary = pipelineBuilder.transform(boundary);
                        encoderBoundary.addFeature(
                                pbfParam.getLayerNameBoundary(), featureCopy, transformBoundary);
                    } else {
                        log.warn("几何对象的边界为空，跳过分界图层生成：{}", geom);
                    }
                }
            }
            // 恢复FeatureRowID
            feature.put(VecConstant.FeatureRowID, featureRowIDValue);
        }

        PbfInfo pbfInfo =
                new PbfInfo()
                        .setData(gZip(encoder.encode()))
                        .setZoom(zoom)
                        .setGridSrid(pbfParam.getOutGridSrid());
        if (pbfParam.isCreateLabel()) {
            byte[] labelBytes = gZip(encoderLabel.encode());
            pbfInfo.setDataLabel(labelBytes);
        }
        if (pbfParam.isCreateBoundary()) {
            byte[] boundaryBytes = gZip(encoderBoundary.encode());
            pbfInfo.setDataBoundary(boundaryBytes);
        }
        if (pbfParam.isSaveFeatureList()) {
            pbfInfo.setThisPbfFeatureList(rs); // 不统计的话，这里就不保存，降低内存占用
        }

        return pbfInfo;
    }

    /** 复制AdvOneRow对象 */
    private static GirAdvOneRow copyAdvOneRow(GirAdvOneRow source) {
        return GirAdvOneRow.ofByMap(source);
    }

    /**
     * 合并点几何（Point → MultiPoint）
     *
     * @param mergedGeomMap 已合并的几何映射
     * @param geom 待合并的点几何
     * @param feature 关联要素（暂未用，预留属性合并）
     * @return 更新后的合并几何映射
     */
    private static Map<String, Geometry> mergePointGeoms(
            Map<String, Geometry> mergedGeomMap, Geometry geom, GirAdvOneRow feature) {
        try {
            Point point = (Point) geom;
            // 固定键：确保同一网格内所有点合并为一个MultiPoint
            String key = "merged_point";

            if (mergedGeomMap.containsKey(key)) {
                // 已有合并点：追加新点
                MultiPoint existingMultiPoint = (MultiPoint) mergedGeomMap.get(key);
                List<Point> pointList = new ArrayList<>();
                // 提取已有所有点
                for (int i = 0; i < existingMultiPoint.getNumGeometries(); i++) {
                    pointList.add((Point) existingMultiPoint.getGeometryN(i));
                }
                // 添加新点
                pointList.add(point);
                // 构建新的MultiPoint
                GeometryFactory factory = new GeometryFactory();
                MultiPoint newMultiPoint =
                        factory.createMultiPoint(pointList.toArray(new Point[0]));
                mergedGeomMap.put(key, newMultiPoint);
            } else {
                // 首次合并：创建MultiPoint
                GeometryFactory factory = new GeometryFactory();
                MultiPoint newMultiPoint = factory.createMultiPoint(new Point[] {point});
                mergedGeomMap.put(key, newMultiPoint);
            }
        } catch (Exception e) {
            // 合并失败：保留原几何（用WKT作为唯一键）
            String key = geom.toText();
            mergedGeomMap.put(key, geom);
        }
        return mergedGeomMap;
    }

    /**
     * 合并多点几何（MultiPoint → 更大的MultiPoint）
     *
     * @param mergedGeomMap 已合并的几何映射
     * @param geom 待合并的多点几何
     * @param feature 关联要素（暂未用，预留属性合并）
     * @return 更新后的合并几何映射
     */
    private static Map<String, Geometry> mergeMultiPointGeoms(
            Map<String, Geometry> mergedGeomMap, Geometry geom, GirAdvOneRow feature) {
        try {
            MultiPoint multiPoint = (MultiPoint) geom;
            String key = "merged_multipoint";

            if (mergedGeomMap.containsKey(key)) {
                // 已有合并多点：合并所有点
                MultiPoint existingMultiPoint = (MultiPoint) mergedGeomMap.get(key);
                List<Point> pointList = new ArrayList<>();

                // 提取已有所有点
                for (int i = 0; i < existingMultiPoint.getNumGeometries(); i++) {
                    pointList.add((Point) existingMultiPoint.getGeometryN(i));
                }
                // 提取新多点的所有点
                for (int i = 0; i < multiPoint.getNumGeometries(); i++) {
                    pointList.add((Point) multiPoint.getGeometryN(i));
                }
                // 构建新的MultiPoint
                GeometryFactory factory = new GeometryFactory();
                MultiPoint newMultiPoint =
                        factory.createMultiPoint(pointList.toArray(new Point[0]));
                mergedGeomMap.put(key, newMultiPoint);
            } else {
                // 首次合并：直接放入
                mergedGeomMap.put(key, multiPoint);
            }
        } catch (Exception e) {
            // 合并失败：保留原几何
            String key = geom.toText();
            mergedGeomMap.put(key, geom);
        }
        return mergedGeomMap;
    }

    /**
     * 合并线串几何（LineString → MultiLineString，优先尝试合并为单LineString）
     *
     * @param mergedGeomMap 已合并的几何映射
     * @param geom 待合并的线串几何
     * @param feature 关联要素（暂未用，预留属性合并）
     * @return 更新后的合并几何映射
     */
    private static Map<String, Geometry> mergeLineStringGeoms(
            Map<String, Geometry> mergedGeomMap, Geometry geom, GirAdvOneRow feature) {
        try {
            LineString lineString = (LineString) geom;
            String key = "merged_linestring";

            if (mergedGeomMap.containsKey(key)) {
                // 已有合并线：尝试合并（先Union，再转MultiLineString）
                Geometry existingGeom = mergedGeomMap.get(key);
                // 执行几何合并（Union）
                List<Geometry> geometries = ListUtil.of(existingGeom, lineString);
                Geometry unionGeom = UnaryUnionOp.union(geometries);
                // 统一转为MultiLineString（兼容单LineString/多LineString）
                MultiLineString multiLineString;
                if (unionGeom instanceof LineString) {
                    GeometryFactory factory = new GeometryFactory();
                    multiLineString =
                            factory.createMultiLineString(
                                    new LineString[] {(LineString) unionGeom});
                } else {
                    multiLineString = (MultiLineString) unionGeom;
                }
                mergedGeomMap.put(key, multiLineString);
            } else {
                // 首次合并：转为MultiLineString
                GeometryFactory factory = new GeometryFactory();
                MultiLineString multiLineString =
                        factory.createMultiLineString(new LineString[] {lineString});
                mergedGeomMap.put(key, multiLineString);
            }
        } catch (Exception e) {
            // 合并失败：保留原几何
            String key = geom.toText();
            mergedGeomMap.put(key, geom);
        }
        return mergedGeomMap;
    }

    /**
     * 合并多线串几何（MultiLineString → 更大的MultiLineString）
     *
     * @param mergedGeomMap 已合并的几何映射
     * @param geom 待合并的多线串几何
     * @param feature 关联要素（暂未用，预留属性合并）
     * @return 更新后的合并几何映射
     */
    private static Map<String, Geometry> mergeMultiLineStringGeoms(
            Map<String, Geometry> mergedGeomMap, Geometry geom, GirAdvOneRow feature) {
        try {
            MultiLineString multiLineString = (MultiLineString) geom;
            String key = "merged_multilinestring";

            if (mergedGeomMap.containsKey(key)) {
                // 已有合并多线：Union合并
                Geometry existingGeom = mergedGeomMap.get(key);
                List<Geometry> geometries = ListUtil.of(existingGeom, multiLineString);
                Geometry unionGeom = UnaryUnionOp.union(geometries);

                // 确保结果为MultiLineString
                MultiLineString newMultiLineString;
                if (unionGeom instanceof LineString) {
                    GeometryFactory factory = new GeometryFactory();
                    newMultiLineString =
                            factory.createMultiLineString(
                                    new LineString[] {(LineString) unionGeom});
                } else {
                    newMultiLineString = (MultiLineString) unionGeom;
                }
                mergedGeomMap.put(key, newMultiLineString);
            } else {
                // 首次合并：直接放入
                mergedGeomMap.put(key, multiLineString);
            }
        } catch (Exception e) {
            // 合并失败：保留原几何
            String key = geom.toText();
            mergedGeomMap.put(key, geom);
        }
        return mergedGeomMap;
    }

    /**
     * 合并面/多面几何（Polygon/MultiPolygon → 合并为MultiPolygon，优先拓扑合并）
     *
     * @param mergedGeomMap 已合并的几何映射
     * @param geom 待合并的面/多面几何
     * @param feature 关联要素（暂未用，预留属性合并）
     * @return 更新后的合并几何映射
     */
    private static Map<String, Geometry> mergePolygonGeoms(
            Map<String, Geometry> mergedGeomMap, Geometry geom, GirAdvOneRow feature) {
        try {
            String key = "merged_polygon";

            if (mergedGeomMap.containsKey(key)) {
                // 已有合并面：Union拓扑合并（解决重叠/相邻问题）
                Geometry existingGeom = mergedGeomMap.get(key);
                List<Geometry> geometries = ListUtil.of(existingGeom, geom);
                Geometry unionGeom = UnaryUnionOp.union(geometries);

                // 确保结果为MultiPolygon
                MultiPolygon multiPolygon;
                if (unionGeom instanceof Polygon) {
                    GeometryFactory factory = new GeometryFactory();
                    multiPolygon = factory.createMultiPolygon(new Polygon[] {(Polygon) unionGeom});
                } else {
                    multiPolygon = (MultiPolygon) unionGeom;
                }
                mergedGeomMap.put(key, multiPolygon);
            } else {
                // 首次合并：转为MultiPolygon
                GeometryFactory factory = new GeometryFactory();
                MultiPolygon multiPolygon;
                if (geom instanceof Polygon) {
                    multiPolygon = factory.createMultiPolygon(new Polygon[] {(Polygon) geom});
                } else {
                    multiPolygon = (MultiPolygon) geom;
                }
                mergedGeomMap.put(key, multiPolygon);
            }
        } catch (Exception e) {
            // 合并失败（如拓扑错误）：保留原几何
            String key = geom.toText();
            mergedGeomMap.put(key, geom);
        }
        return mergedGeomMap;
    }

    // 获取要素ID（用于排序）
    private static String getFeatureId(
            GirAdvOneRow feature, String geomFieldName, String idFieldName) {
        try {
            Object idObj = feature.get(idFieldName);
            if (idObj != null) {
                return idObj.toString();
            }
            // 无ID字段：用几何WKT作为唯一标识
            Geometry geom = (Geometry) feature.get(geomFieldName);
            return geom != null ? geom.toText() : UUID.randomUUID().toString();
        } catch (Exception e) {
            // 兜底：随机ID
            return UUID.randomUUID().toString();
        }
    }

    private static double calculateDynamicTolerance(
            int srid, int zoom, Envelope extent, int simplifyLevel) {

        double centerLat = (extent.getMinY() + extent.getMaxY()) / 2;

        double baseToleranceMeter = 0.5;

        double baseTolerance;
        if (GirGeoTools.defaultInstance().getSridOpt().isGeographicCRS(srid)) {
            // 地理坐标系（度）：米→度
            double meterPerDegree = getUnitConversionFactor(srid, centerLat);
            baseTolerance = baseToleranceMeter / meterPerDegree;
        } else {
            // 投影坐标系（米）：直接使用米级基准
            baseTolerance = baseToleranceMeter;
        }

        double levelScale = Math.pow(2, 18 - zoom);
        double levelTolerance = baseTolerance * levelScale;

        double finalTolerance = levelTolerance * simplifyLevel;

        if (GirGeoTools.defaultInstance().getSridOpt().isGeographicCRS(srid)) {
            // 4326（度）：最小≈0.1米，最大≈50米
            double minTolerance = 0.1 / getUnitConversionFactor(srid, centerLat);
            double maxTolerance = 50.0 / getUnitConversionFactor(srid, centerLat);
            finalTolerance = Math.max(finalTolerance, minTolerance);
            finalTolerance = Math.min(finalTolerance, maxTolerance);
        } else {
            // 3857（米）：最小0.1米，最大50米
            finalTolerance = Math.max(finalTolerance, 0.1);
            finalTolerance = Math.min(finalTolerance, 50.0);
        }

        return finalTolerance;
    }

    private static double calculateAreaThreshold(int srid, int zoom, Envelope extent) {
        double baseAreaMeter = Math.pow(areaFilteringThreshold * Math.pow(2, (4 - zoom)), 2);
        double centerLat = (extent.getMinY() + extent.getMaxY()) / 2;
        double centerLon = (extent.getMinX() + extent.getMaxX()) / 2;
        if (GirGeoTools.defaultInstance().getSridOpt().isGeographicCRS(srid)) {
            // 米² → 平方度
            return convertSquareMeterToSquareDegree(baseAreaMeter, centerLat);
        } else {
            return adjustMercatorArea(baseAreaMeter, centerLat);
        }
    }

    private static double convertSquareMeterToSquareDegree(double areaMeter, double lat) {
        if (areaMeter <= 0) return 0;
        double latRad = Math.toRadians(lat);
        double meterPerDegree = METERS_PER_DEGREE_EQUATOR * Math.cos(latRad); // 1度=多少米
        double squareDegreeToSquareMeter = Math.pow(meterPerDegree, 2); // 1平方度=多少平方米
        return areaMeter / squareDegreeToSquareMeter;
    }

    private static double adjustMercatorArea(double areaMeter, double lat) {
        if (areaMeter <= 0) return 0;
        double latRad = Math.toRadians(lat);
        double stretchFactor = 1 / Math.pow(Math.cos(latRad), 2);
        return areaMeter / stretchFactor;
    }

    public static double getUnitConversionFactor(int srid, double centerLat) {
        if (GirGeoTools.defaultInstance().getSridOpt().isGeographicCRS(srid)) {
            // 4326（度）：计算1度对应的米数（按纬度修正）
            double latRad = Math.toRadians(centerLat);
            return 111319.9 * Math.cos(latRad);
        } else {
            // 3857/UTM等（米）：系数=1
            return 1.0;
        }
    }

    public static byte[] gZip(byte[] data) {
        byte[] compressedData = null;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
            // 写入数据并完成压缩
            gzip.write(data);
            gzip.finish();
            // 获取压缩后的数据
            compressedData = bos.toByteArray();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return compressedData;
    }
}
