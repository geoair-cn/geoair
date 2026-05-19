package cn.geoair.map.dynamic.statics.mvt.spark.vectile.utils;

import cn.geoair.map.dynamic.tools.GirGeoTools;
import cn.geoair.map.dynamic.tools.grid.dto.RangeApo;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import scala.Tuple4;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/12/26 14:34 @description： 只负责计算瓦片的相关参数
 */
public class TileUtils {

    /**
     * 根据空间范围（Envelope）计算对应瓦片级别下的行列号范围（WGS84坐标系）
     *
     * @param level 瓦片级别（Zoom Level，如8、9、10）
     * @param geometry 空间要素的外包矩形（Envelope）
     * @return Tuple4<colMin, colMax, rowMin, rowMax> 列最小值、列最大值、行最小值、行最大值
     */
    public static Tuple4<Integer, Integer, Integer, Integer> rangeToIndex(
            long level, Geometry geometry, int outGridSrid) {
        RangeApo rangeApo = null;
        if (outGridSrid == 3857) {
            rangeApo =
                    GirGeoTools.me().getTileGrid3857Opt().tileRangeByGeom(((int) level), geometry);
        } else {
            rangeApo =
                    GirGeoTools.me().getTileGrid4326Opt().tileRangeByGeom(((int) level), geometry);
        }
        return new Tuple4<>(
                rangeApo.getMinX(), rangeApo.getMaxX(), rangeApo.getMinY(), rangeApo.getMaxY());
    }

    public static Envelope getTileEnvelope(int level, int x, int y, int sourceGrid) {
        ReferencedEnvelope referencedEnvelope = null;
        if (sourceGrid == 3857) {
            referencedEnvelope =
                    GirGeoTools.me().getTileGrid3857Opt().xyzToTileBox(level, x, y, 3857);
        } else {
            referencedEnvelope =
                    GirGeoTools.me().getTileGrid4326Opt().xyzToTileBox(level, x, y, 4326);
        }
        return referencedEnvelope;
    }
}
