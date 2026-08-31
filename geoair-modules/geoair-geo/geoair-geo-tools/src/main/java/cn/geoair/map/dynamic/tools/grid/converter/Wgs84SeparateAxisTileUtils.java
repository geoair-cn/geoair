package cn.geoair.map.dynamic.tools.grid.converter;

import cn.geoair.map.dynamic.tools.ToolsConfig;
import cn.geoair.map.dynamic.tools.grid.dto.BoxReferencedEnvelope;
import cn.geoair.map.dynamic.tools.grid.dto.RangeApo;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Envelope;

import java.util.Objects;

/**
 * WGS84（4326）非等轴瓦片转换实现类。
 *
 * <p>网格在同一层级的经纬度行列数不同：经度方向为 {@code 2^z} 列，纬度方向为 {@code max(1, 2^(z-1))} 行。例如 z=3 时为 8 列 × 4
 * 行。纬度跨度始终按实际行数 计算，确保瓦片完整覆盖 [-90°, 90°]。
 */
public class Wgs84SeparateAxisTileUtils extends AbstractWgs84TileConverter {

    // 单例实例
    private static volatile Wgs84SeparateAxisTileUtils INSTANCE;

    public Wgs84SeparateAxisTileUtils(ToolsConfig advToolsConfig) {
        super(advToolsConfig);
    }

    @Deprecated
    public static Wgs84SeparateAxisTileUtils getInstance() {
        if (INSTANCE == null) {
            synchronized (Wgs84SeparateAxisTileUtils.class) {
                if (INSTANCE == null) {
                    INSTANCE = new Wgs84SeparateAxisTileUtils(new ToolsConfig());
                }
            }
        }
        return INSTANCE;
    }

    public static Wgs84SeparateAxisTileUtils getInstance(ToolsConfig advToolsConfig) {
        return new Wgs84SeparateAxisTileUtils(advToolsConfig);
    }

    // ========== 差异化核心方法实现（非等轴） ==========
    @Override
    protected double calculateTileLonSpan(int z) {
        return 360.0 / (1 << z); // 经度跨度：360/2^z（位运算替代Math.pow，提升性能）
    }

    @Override
    protected double calculateTileLatSpan(int z) {
        // 纬度总行数为 2^(z-1)，故跨度为 180 / 2^(z-1) = 360 / 2^z。
        // 写成后者可自然覆盖 z=0 的单行全球瓦片。
        return 360.0 / (1 << z);
    }

    // ========== 核心转换方法（复用父类公共逻辑） ==========
    @Override
    public BoxReferencedEnvelope xyzToTileBox(int z, int x, int y, int targetSrid) {
        validateXyz(z, x, y);

        // 调用子类实现的跨度计算方法
        double tileLonSpan = calculateTileLonSpan(z);
        double tileLatSpan = calculateTileLatSpan(z);

        // 计算4326瓦片边界
        double lon_min = x * tileLonSpan + MIN_LON;
        double lon_max = (x + 1) * tileLonSpan + MIN_LON;
        double lat_max = MAX_LAT - y * tileLatSpan;
        double lat_min = MAX_LAT - (y + 1) * tileLatSpan;

        // 边界修正（复用父类工具方法）
        lon_min = clamp(lon_min, MIN_LON, MAX_LON);
        lon_max = clamp(lon_max, MIN_LON, MAX_LON);
        // Separate 是线性 EPSG:4326 网格，允许到达南北极；不能套用 Web Mercator 的有效纬度。
        lat_min = clamp(lat_min, MIN_LAT, MAX_LAT);
        lat_max = clamp(lat_max, MIN_LAT, MAX_LAT);

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

        // 处理空范围（点/线几何）
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
        double envMinY = clamp(envelope.getMinY(), MIN_LAT, MAX_LAT);
        double envMaxY = clamp(envelope.getMaxY(), MIN_LAT, MAX_LAT);

        // 调用子类实现的跨度计算方法
        double tileLonSpan = calculateTileLonSpan(z);
        double tileLatSpan = calculateTileLatSpan(z);
        int maxTileXIndex = (1 << z) - 1;
        int maxTileYIndex = getTileLevelMetadata(z).getNumTilesHigh() - 1;

        // 逆算瓦片范围（添加精度补偿）
        double tileXmin = Math.floor((envMinX - MIN_LON - PRECISION) / tileLonSpan);
        double tileXmax = Math.ceil((envMaxX - MIN_LON + PRECISION) / tileLonSpan);
        double tileYmin = Math.floor((MAX_LAT - envMaxY - PRECISION) / tileLatSpan);
        double tileYmax = Math.ceil((MAX_LAT - envMinY + PRECISION) / tileLatSpan);

        // 索引边界修正
        tileXmin = clamp(tileXmin, 0, maxTileXIndex);
        tileXmax = clamp(tileXmax, 0, maxTileXIndex);
        tileYmin = clamp(tileYmin, 0, maxTileYIndex);
        tileYmax = clamp(tileYmax, 0, maxTileYIndex);

        return new RangeApo(tileXmin, tileXmax, tileYmin, tileYmax, z);
    }

    // ========== 瓦片坐标转换（非等轴逻辑） ==========
    @Override
    public double tileXToCoordinateX(int x, int z) {
        validateXyz(z, x, 0);
        return x * calculateTileLonSpan(z) + MIN_LON;
    }

    @Override
    public double tileYToCoordinateY(int y, int z) {
        validateXyz(z, 0, y);
        // 4326 Separate 为线性经纬度网格，必须与 xyzToTileBox 的纬度边界计算一致。
        return clamp(MAX_LAT - y * calculateTileLatSpan(z), MIN_LAT, MAX_LAT);
    }
}
