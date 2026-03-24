package cn.geoair.map.dynamic.tools.grid.converter;

import cn.geoair.map.dynamic.tools.grid.dto.BoxReferencedEnvelope;
import cn.geoair.map.dynamic.tools.grid.dto.RangeApo;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Envelope;

import java.util.Objects;

/**
 * WGS84（4326）非等轴瓦片转换实现类 核心特征：经度/纬度轴独立计算瓦片跨度（经度360/2^z，纬度180/2^z）
 */
@Deprecated
public class Wgs84SeparateAxisTileUtils extends AbstractWgs84TileConverter {

	// 单例实例
	private static volatile Wgs84SeparateAxisTileUtils INSTANCE;

	/**
	 * 双重校验锁单例
	 */
	public static Wgs84SeparateAxisTileUtils getInstance() {
		if (INSTANCE == null) {
			synchronized (Wgs84SeparateAxisTileUtils.class) {
				if (INSTANCE == null) {
					INSTANCE = new Wgs84SeparateAxisTileUtils();
				}
			}
		}
		return INSTANCE;
	}

	// ========== 差异化核心方法实现（非等轴） ==========
	@Override
	protected double calculateTileLonSpan(int z) {
		return 360.0 / (1 << z); // 经度跨度：360/2^z（位运算替代Math.pow，提升性能）
	}

	@Override
	protected double calculateTileLatSpan(int z) {
		return 180.0 / (1 << z); // 纬度跨度：180/2^z（非等轴核心）
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
		lat_min = clamp(lat_min, MIN_VALID_LAT, MAX_VALID_LAT);
		lat_max = clamp(lat_max, MIN_VALID_LAT, MAX_VALID_LAT);

		// 转换为目标坐标系
		ReferencedEnvelope envelope4326 = new ReferencedEnvelope(lon_min, lon_max, lat_min, lat_max,
				sridConvertOpt.getCRS(4326));
		Envelope targetEnvelope = sridConvertOpt.convert(envelope4326, 4326, targetSrid);

		return new BoxReferencedEnvelope(targetEnvelope, targetSrid);
	}

	@Override
	public RangeApo tileRangeByBox(int z, Envelope envelope) {
		if (Objects.isNull(envelope)) {
			throw new IllegalArgumentException("地理范围Envelope不能为空");
		}

		// 处理空范围（点/线几何）
		if (envelope.isNull() || (Math.abs(envelope.getMaxX() - envelope.getMinX()) < PRECISION)
				|| (Math.abs(envelope.getMaxY() - envelope.getMinY()) < PRECISION)) {
			double centerX = (envelope.getMinX() + envelope.getMaxX()) / 2;
			double centerY = (envelope.getMinY() + envelope.getMaxY()) / 2;
			envelope = new Envelope(centerX - POINT_OFFSET, centerX + POINT_OFFSET, centerY - POINT_OFFSET,
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
		int maxTileIndex = (1 << z) - 1;

		// 逆算瓦片范围（添加精度补偿）
		double tileXmin = Math.floor((envMinX - MIN_LON - PRECISION) / tileLonSpan);
		double tileXmax = Math.ceil((envMaxX - MIN_LON + PRECISION) / tileLonSpan);
		double tileYmin = Math.floor((MAX_LAT - envMaxY - PRECISION) / tileLatSpan);
		double tileYmax = Math.ceil((MAX_LAT - envMinY + PRECISION) / tileLatSpan);

		// 索引边界修正
		tileXmin = clamp(tileXmin, 0, maxTileIndex);
		tileXmax = clamp(tileXmax, 0, maxTileIndex);
		tileYmin = clamp(tileYmin, 0, maxTileIndex);
		tileYmax = clamp(tileYmax, 0, maxTileIndex);

		return new RangeApo(tileXmin, tileXmax, tileYmin, tileYmax);
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
		// 非等轴保留球面投影公式（若需线性计算可改为：MAX_LAT - y * calculateTileLatSpan(z)）
		double n = Math.PI - (2.0 * Math.PI * y) / (1 << z);
		return Math.toDegrees(Math.atan(Math.sinh(n)));
	}

	/**
	 * 在 4326 非等轴场景下，把 “原点在左下角（TMS）” 的 Y 索引，转为 “原点在左上角（XYZ/WMS）” 的合法 Y 索引
	 * @param y Y索引
	 * @param z 缩放级别
	 * @return
	 */
	public int reverseY(int y, int z) {
		validateXyz(z, 0, y);
		// zoom-1 4326 纬度非等轴场景瓦片数减半
		int maxYIndex = (1 << z - 1) - 1;
		// -y翻转 Y 轴原点（左下→左上）
		return maxYIndex - y;
	}

}
