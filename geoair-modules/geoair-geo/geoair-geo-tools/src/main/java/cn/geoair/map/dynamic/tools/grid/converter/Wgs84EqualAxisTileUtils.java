package cn.geoair.map.dynamic.tools.grid.converter;

import cn.geoair.map.dynamic.tools.ToolsConfig;
import cn.geoair.map.dynamic.tools.grid.dto.BoxReferencedEnvelope;
import cn.geoair.map.dynamic.tools.grid.dto.RangeApo;

import java.util.Objects;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Envelope;

/**
 * WGS84（4326）等轴瓦片转换实现类 核心特征：经度/纬度轴使用相同瓦片跨度（均为360/2^z），兼容Mapbox4490逻辑
 */
public class Wgs84EqualAxisTileUtils extends AbstractWgs84TileConverter {

    // 单例实例
    private static volatile Wgs84EqualAxisTileUtils INSTANCE;

    public Wgs84EqualAxisTileUtils(ToolsConfig advToolsConfig) {
        super(advToolsConfig);
    }

    /**
     * 双重校验锁单例
     */
    @Deprecated
    public static Wgs84EqualAxisTileUtils getInstance() {
        if (INSTANCE == null) {
            synchronized (Wgs84EqualAxisTileUtils.class) {
                if (INSTANCE == null) {
                    INSTANCE = new Wgs84EqualAxisTileUtils(new ToolsConfig());
                }
            }
        }
        return INSTANCE;
    }

    public static Wgs84EqualAxisTileUtils getInstance(ToolsConfig advToolsConfig) {
        return new Wgs84EqualAxisTileUtils(advToolsConfig);
    }

    // ========== 差异化核心方法实现（等轴） ==========
    @Override
    protected double calculateTileLonSpan(int z) {
        return 360.0 / (1 << z); // 经度跨度：360/2^z
    }

    @Override
    protected double calculateTileLatSpan(int z) {
        return 360.0 / (1 << z); // 纬度跨度：360/2^z（等轴核心）
    }

    @Override
    public BoxReferencedEnvelope xyzToTileBox(int z, int x, int y, int targetSrid) {
        validateXyz(z, x, y);

        double tileLonSpan = calculateTileLonSpan(z);
        double tileLatSpan = calculateTileLatSpan(z);

        // 等轴瓦片边界计算
        double lon_min = x * tileLonSpan + MIN_LON;
        double lon_max = (x + 1) * tileLonSpan + MIN_LON;
        double lat_max = MAX_LAT - y * tileLatSpan;
        double lat_min = MAX_LAT - (y + 1) * tileLatSpan;

        // 边界修正
        lon_min = clamp(lon_min, MIN_LON, MAX_LON);
        lon_max = clamp(lon_max, MIN_LON, MAX_LON);
        lat_min = clamp(lat_min, MIN_VALID_LAT, MAX_VALID_LAT);
        lat_max = clamp(lat_max, MIN_VALID_LAT, MAX_VALID_LAT);

        // 转换为目标坐标系
        ReferencedEnvelope envelope4326 =
                new ReferencedEnvelope(
                        lon_min, lon_max, lat_min, lat_max, sridConvertOpt.getCRS(4326));
        Envelope targetEnvelope = sridConvertOpt.convert(envelope4326, 4326, targetSrid);

        return new BoxReferencedEnvelope(targetEnvelope, targetSrid);
    }

    @Override
    public RangeApo tileRangeByBox(int z, Envelope envelope) {
        if (Objects.isNull(envelope)) {
            throw new IllegalArgumentException("地理范围Envelope不能为空");
        }

        // 处理空范围
        if (envelope.isNull()
                || (Math.abs(envelope.getMaxX() - envelope.getMinX()) < PRECISION)
                || (Math.abs(envelope.getMaxY() - envelope.getMinY()) < PRECISION)) {
            double centerX = (envelope.getMinX() + envelope.getMaxX()) / 2;
            double centerY = (envelope.getMinY() + envelope.getMaxY()) / 2;
            envelope =
                    new Envelope(
                            centerX - POINT_OFFSET,
                            centerX + POINT_OFFSET,
                            centerY - POINT_OFFSET,
                            centerY + POINT_OFFSET);
        }

        // 边界修正
        double envMinX = clamp(envelope.getMinX(), MIN_LON, MAX_LON);
        double envMaxX = clamp(envelope.getMaxX(), MIN_LON, MAX_LON);
        double envMinY = clamp(envelope.getMinY(), MIN_VALID_LAT, MAX_VALID_LAT);
        double envMaxY = clamp(envelope.getMaxY(), MIN_VALID_LAT, MAX_VALID_LAT);

        double tileLonSpan = calculateTileLonSpan(z);
        double tileLatSpan = calculateTileLatSpan(z);
        int maxTileIndex = (1 << z) - 1;

        // 逆算瓦片范围
        double tileXmin = Math.floor((envMinX - MIN_LON - PRECISION) / tileLonSpan);
        double tileXmax = Math.ceil((envMaxX - MIN_LON + PRECISION) / tileLonSpan);
        double tileYmin = Math.floor((MAX_LAT - envMaxY - PRECISION) / tileLatSpan);
        double tileYmax = Math.ceil((MAX_LAT - envMinY + PRECISION) / tileLatSpan);

        // 索引修正
        tileXmin = clamp(tileXmin, 0, maxTileIndex);
        tileXmax = clamp(tileXmax, 0, maxTileIndex);
        tileYmin = clamp(tileYmin, 0, maxTileIndex);
        tileYmax = clamp(tileYmax, 0, maxTileIndex);

        return new RangeApo(tileXmin, tileXmax, tileYmin, tileYmax);
    }

    // ========== 瓦片坐标转换（等轴线性逻辑） ==========
    @Override
    public double tileXToCoordinateX(int x, int z) {
        validateXyz(z, x, 0);
        return x * calculateTileLonSpan(z) + MIN_LON;
    }

    @Override
    public double tileYToCoordinateY(int y, int z) {
        validateXyz(z, 0, y);
        // 等轴使用线性计算（与xyzToTileBox互逆）
        double lat = MAX_LAT - y * calculateTileLatSpan(z);
        return clamp(lat, MIN_VALID_LAT, MAX_VALID_LAT);
    }
}
