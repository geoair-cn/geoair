package cn.geoair.map.dynamic.statics.mvt.spark.vectile.utils;

import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.mvt.tools.AdvMvtDensityUtils;
import cn.geoair.map.dynamic.mvt.tools.model.PbfInfo;
import cn.geoair.map.dynamic.mvt.tools.model.PbfTileParameter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.*;
import cn.geoair.map.dynamic.tools.GirAdvTools;
import cn.geoair.map.dynamic.tools.grid.dto.TileZxyApo;
import cn.hutool.core.bean.BeanUtil;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import scala.Tuple4;

/**
 * 矢量瓦片生成通用工具类 抽离与运行环境无关的核心算法
 */
@Slf4j
public class VectorTileCommonUtils {

    /**
     * 通用空间要素转换（单条要素）
     */
    public static GirAdvOneRow transformSingleFeature(
            GirAdvOneRow feature, TileSliceParameter parameter) {
        if (feature == null) return null;
        Geometry geometry = feature.getGeometry(parameter.getGeomFieldName());
        if (geometry == null) {
            return null;
        }
        if (!geometry.isValid()) {
            log.error("=================Geometry is not valid===============");
            return null;
        }
        // 坐标系转换
        Geometry convertedGeom =
                GirAdvTools.getSridOpt()
                        .convert(
                                geometry,
                                parameter.getSourceDataSrid(),
                                parameter.getOutGridSrid());
        feature.put(parameter.getGeomFieldName(), convertedGeom);
        return feature;
    }

    /**
     * 通用要素映射到瓦片（单条要素）
     */
    public static Map<String, List<GirAdvOneRow>> mapSingleFeatureToTiles(
            GirAdvOneRow feature,
            String geomFieldName,
            Integer minZoom,
            Integer maxZoom,
            int outGridSrid) {
        Map<String, List<GirAdvOneRow>> tileMap = new HashMap<>();
        Geometry geom = null;
        try {
            geom = (Geometry) feature.get(geomFieldName);
            if (geom == null || geom.isEmpty()) return tileMap;
        } catch (Exception e) {
            return tileMap;
        }
        minZoom = Optional.ofNullable(minZoom).orElse(4);
        maxZoom = Optional.ofNullable(maxZoom).orElse(15);

        for (int zoom = minZoom; zoom <= maxZoom; zoom++) {
            Tuple4<Integer, Integer, Integer, Integer> tileRange =
                    TileUtils.rangeToIndex(zoom, geom, outGridSrid);
            int xmin = tileRange._1();
            int xmax = tileRange._2();

            int ymin = tileRange._3();
            int ymax = tileRange._4();

            for (int y = ymin; y <= ymax; y++) {
                for (int x = xmin; x <= xmax; x++) {
                    String quadKey = GirAdvTools.getTileGridBingMapOpt().xyzToQuadKey(x, y, zoom);
                    // String tileId = zoom + "#" + y + "#" + x;
                    tileMap.computeIfAbsent(quadKey, k -> new ArrayList<>()).add(feature);
                }
            }
        }
        return tileMap;
    }

    public static Map<String, GirAdvOneRow> mapSingleFeatureToTiles1(
            GirAdvOneRow feature, TileSliceParameter parameter) {
        Map<String, GirAdvOneRow> tileMap = new HashMap<>();
        Geometry geom = (Geometry) feature.get(parameter.getGeomFieldName());
        if (geom == null || geom.isEmpty()) return tileMap;

        int minZoom = Optional.ofNullable(parameter.getMinZoom()).orElse(4);
        int maxZoom = Optional.ofNullable(parameter.getMaxZoom()).orElse(15);

        for (int zoom = minZoom; zoom <= maxZoom; zoom++) {
            Tuple4<Integer, Integer, Integer, Integer> tileRange =
                    TileUtils.rangeToIndex(zoom, geom, parameter.getOutGridSrid());
            int xmin = tileRange._1();
            int xmax = tileRange._2();

            int ymin = tileRange._3();
            int ymax = tileRange._4();

            for (int y = ymin; y <= ymax; y++) {
                for (int x = xmin; x <= xmax; x++) {
                    String quadKey = GirAdvTools.getTileGridBingMapOpt().xyzToQuadKey(x, y, zoom);
                    // String tileId = zoom + "#" + y + "#" + x;
                    tileMap.put(quadKey, feature);
                }
            }
        }
        return tileMap;
    }

    /**
     * 通用瓦片要素聚合
     */
    public static List<GirAdvOneRow> aggregateTileFeatures(
            List<GirAdvOneRow> list1, List<GirAdvOneRow> list2) {
        return Stream.concat(list1.stream(), list2.stream()).collect(Collectors.toList());
    }

    public static List<GirAdvOneRow> limitTileFeatures(
            List<GirAdvOneRow> mergedList, TileSliceParameter parameter) {
        List<GirAdvOneRow> limitList = mergedList;
        // 要素数量限制逻辑
        if (parameter.isEnableFeatureLimitIs() && parameter.getFeatureLimit() != null) {
            int limit = parameter.getFeatureLimit();

            if (limitList.size() > limit) {
                // 空间密度合并
                if (parameter.isCoalesceDensestAsNeeded()) {
                    limitList =
                            AdvMvtDensityUtils.doCoalesceBySpatialDensity(
                                    limitList,
                                    limit,
                                    parameter.getGeomFieldName(),
                                    parameter.getIdFieldName(),
                                    parameter.getOutGridSrid());
                }
                // 空间密度过滤
                if (limitList.size() > limit && parameter.isDropDensestAsNeeded()) {
                    limitList =
                            AdvMvtDensityUtils.doFilterBySpatialDensity(
                                    limitList,
                                    limit,
                                    parameter.getGeomFieldName(),
                                    parameter.getIdFieldName(),
                                    parameter.getOutGridSrid());
                }
                // 兜底截断
                if (limitList.size() > limit) {
                    limitList = limitList.subList(0, limit);
                }
            }
        }
        return limitList;
    }

    /**
     * 通用PBF生成
     */
    public static PbfInfo generateSingleTilePbf(
            String tileId,
            List<GirAdvOneRow> features,
            TileSliceParameter parameter,
            PbfTargetInfo pbfTargetInfo)
            throws Exception {
        TileZxyApo tileZxyApo = GirAdvTools.getTileGridBingMapOpt().quadKeyToXyz(tileId);
        int zoom = tileZxyApo.getZ();
        int y = tileZxyApo.getY();
        int x = tileZxyApo.getX();

        // 计算瓦片范围
        Envelope envelope = TileUtils.getTileEnvelope(zoom, x, y, parameter.getOutGridSrid());

        PbfTileParameter pbfTileParameter = formToPbfTileParameter(parameter);

        pbfTileParameter.setPPbfType(pbfTargetInfo.getPPbfType());
        pbfTileParameter.setOnly(pbfTargetInfo.isOnly());
        pbfTileParameter.setSaveFeatureList(pbfTargetInfo.isSaveFeatureList());

        // 生成原始PBF
        PbfInfo rawPbf =
                AdvMvtDensityUtils.doGetPbfByteByGeotools(
                        features, envelope, zoom, pbfTileParameter);

        return AdvMvtDensityUtils.doValidateAndOptimizeTileSize(
                tileId, rawPbf, features, envelope, zoom, pbfTileParameter);
    }

    /**
     * 通用PG写入参数构建
     */
    public static Map<String, String> buildPgWriteParams(TileSliceParameter parameter) {
        PgConnectInfo pgInfo = parameter.getOutPutConnectInfo();
        Map<String, String> params = pgInfo.toParams();
        params.put("batchSize", "50");
        params.put("tableName", pgInfo.getTableName());
        return params;
    }

    public static PbfTileParameter formToPbfTileParameter(TileSliceParameter tileSliceParameter) {
        if (tileSliceParameter == null) {
            return new PbfTileParameter();
        }
        PbfTileParameter pbfParam = new PbfTileParameter();
        BeanUtil.copyProperties(tileSliceParameter, pbfParam);
        return pbfParam;
    }
}
