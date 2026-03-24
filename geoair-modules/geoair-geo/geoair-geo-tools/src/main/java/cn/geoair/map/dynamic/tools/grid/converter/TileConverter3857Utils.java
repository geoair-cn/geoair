package cn.geoair.map.dynamic.tools.grid.converter;

import java.util.Objects;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

import cn.geoair.map.dynamic.tools.GirAdvTools;
import cn.geoair.map.dynamic.tools.grid.dto.BoxReferencedEnvelope;
import cn.geoair.map.dynamic.tools.grid.dto.RangeApo;

/**
 * Web墨卡托（3857）坐标系瓦片转换实现类
 */
public class TileConverter3857Utils extends TileConverterCommon {

	// 墨卡托投影常量（地球半径）
	private static final double EARTH_RADIUS = 6378137.0;

	private static final double MAX_MERCATOR = EARTH_RADIUS * Math.PI;

	// 单例实例（volatile保证可见性，防止指令重排）
	private static volatile TileConverter3857Utils INSTANCE;

	/**
	 * 获取单例实例（双重校验锁）
	 * @return 单例对象
	 */
	public static TileConverter3857Utils getInstance() {
		if (INSTANCE == null) {
			synchronized (TileConverter3857Utils.class) {
				if (INSTANCE == null) {
					INSTANCE = new TileConverter3857Utils();
				}
			}
		}
		return INSTANCE;
	}

	public BoxReferencedEnvelope xyzToTileBox(int z, int x, int y, int targetSrid) {
		validateXyz(z, x, y);
		// 直接计算3857坐标的瓦片范围
		double minX = tileXToCoordinateX(x, z);
		double maxX = tileXToCoordinateX(x + 1, z);
		double minY = tileYToCoordinateY(y + 1, z); // 注意y轴反转：瓦片Y越大，3857Y越小
		double maxY = tileYToCoordinateY(y, z);

		Envelope envelope = new Envelope(minX, maxX, minY, maxY);
		Envelope convert = GirAdvTools.getSridOpt().convert(envelope, 3857, targetSrid);
		return new BoxReferencedEnvelope(convert, targetSrid);
	}

	@Override
	public RangeApo tileRangeByBox(int z, Envelope tileBox) {
		if (Objects.isNull(tileBox)) {
			throw new IllegalArgumentException("地理范围Envelope不能为空");
		}
		validateXyz(z, 0, 0);

		// 1. 计算当前层级瓦片尺寸（3857平面坐标，单位：米）
		double tileSize = 2 * MAX_MERCATOR / Math.pow(2, z);

		// 2. 直接基于3857坐标计算瓦片索引
		double tileXmin = Math.floor((tileBox.getMinX() + MAX_MERCATOR) / tileSize);
		double tileXmax = Math.ceil((tileBox.getMaxX() + MAX_MERCATOR) / tileSize);
		double tileYmin = Math.floor((MAX_MERCATOR - tileBox.getMaxY()) / tileSize);
		double tileYmax = Math.ceil((MAX_MERCATOR - tileBox.getMinY()) / tileSize);

		// 3. 边界修正（确保瓦片索引在合法范围）
		int maxTileIndex = (1 << z) - 1;
		tileXmin = Math.max(0, Math.min(tileXmin, maxTileIndex));
		tileXmax = Math.max(0, Math.min(tileXmax, maxTileIndex));
		tileYmin = Math.max(0, Math.min(tileYmin, maxTileIndex));
		tileYmax = Math.max(0, Math.min(tileYmax, maxTileIndex));

		// 4. 返回瓦片索引范围
		return new RangeApo(tileXmin, tileXmax, tileYmin, tileYmax);
	}

	/**
	 * 瓦片X索引转3857坐标系X坐标（米）
	 * @param x 瓦片X索引
	 * @param z 缩放级别
	 * @return 3857 X坐标（米）
	 */
	public double tileXToCoordinateX(int x, int z) {
		validateXyz(z, x, 0);
		double tileCount = Math.pow(2.0, z);
		// 核心公式：3857X = (x / 总瓦片数) * 2*MAX_MERCATOR - MAX_MERCATOR
		return (x / tileCount) * 2 * MAX_MERCATOR - MAX_MERCATOR;
	}

	/**
	 * 瓦片Y索引转3857坐标系Y坐标（米）
	 * @param y 瓦片Y索引
	 * @param z 缩放级别
	 * @return 3857 Y坐标（米）
	 */
	public double tileYToCoordinateY(int y, int z) {
		validateXyz(z, 0, y);
		double tileCount = Math.pow(2.0, z);
		// 核心公式：3857Y = MAX_MERCATOR - (y / 总瓦片数) * 2*MAX_MERCATOR（Y轴反转）
		return MAX_MERCATOR - (y / tileCount) * 2 * MAX_MERCATOR;
	}

	/**
	 * 将几何图形从源坐标系转换为WGS84(4326)坐标系
	 * @param geometry 几何图形对象
	 * @param srcSrid 源坐标系SRID代码
	 * @return 转换后的几何图形对象
	 */
	public Geometry transform(Geometry geometry, int srcSrid) {
		return sridConvertOpt.convert(geometry, srcSrid, 3857);
	}

	@Override
	public int convertSeparateAxisYToEqualAxisY(int separateAxisY, int zoom,
			AbstractWgs84TileConverter.RoundingType roundingType) {
		return separateAxisY;
	}

	@Override
	public int convertEqualAxisYToSeparateAxisY(int equalAxisY, int zoom,
			AbstractWgs84TileConverter.RoundingType roundingType) {
		return equalAxisY;
	}

}
