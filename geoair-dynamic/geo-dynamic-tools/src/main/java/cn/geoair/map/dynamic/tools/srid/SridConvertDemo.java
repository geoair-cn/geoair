package cn.geoair.map.dynamic.tools.srid;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

public class SridConvertDemo {

	public static void main(String[] args) {
		// 获取单例实例
		GirSridConvertUtils sridUtils = GirSridConvertUtils.getInstance();

		// 1. 单点转换：WGS84(4326)转Web墨卡托(3857)
		double[] mercatorCoord = sridUtils.convertPoint(116.403874, 39.914885, 4326, 3857);
		System.out.println("Web墨卡托坐标：x=" + mercatorCoord[0] + ", y=" + mercatorCoord[1]);

		// 2. 几何对象转换：Web墨卡托(3857)转WGS84(4326)
		GeometryFactory gf = new GeometryFactory();
		Point mercatorPoint = gf.createPoint(new Coordinate(12958129.39, 4810373.09));
		mercatorPoint.setSRID(3857);
		Point wgs84Point = (Point) sridUtils.convert(mercatorPoint, 3857, 4326);
		System.out.println("WGS84坐标：lng=" + wgs84Point.getX() + ", lat=" + wgs84Point.getY());

		// 4. 异常安全模式：转换失败返回null
		double[] invalidCoord = sridUtils.convertPoint(999, 999, 4326, 9999, true);
		System.out.println("无效转换结果：" + (invalidCoord == null ? "null" : invalidCoord[0]));
	}

}
