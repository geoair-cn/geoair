package cn.geoair.map.dynamic.mvt.tools;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

import cn.geoair.map.dynamic.mvt.tools.param.TileExecParams;
import cn.geoair.map.dynamic.tools.GirAdvTools;

// import geotrellis.vector.Extent;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;

public class AdvMvtTileUtils {

	public static void main(String[] args) {
	}

	public static Envelope getTileRect(int level, int x, int y, int sourceGrid) {
		ReferencedEnvelope referencedEnvelope = null;
		if (sourceGrid == 3857) {
			referencedEnvelope = GirAdvTools.getTileGrid3857Opt().xyzToTileBox(level, x, y, 3857);
		}
		else {
			referencedEnvelope = GirAdvTools.getTileGrid4326Opt().xyzToTileBox(level, x, y, 4326);
		}
		return referencedEnvelope;
	}

	public static TileExecParams getTileExecParamsNotHasSql(int zoom, int x, int y, int sourceGrid,
			int sourceDataSrid) {
		TileExecParams tileExecParams = new TileExecParams();
		tileExecParams.setZoom(zoom).setX(x).setY(y);
		AtomicReference<Envelope> dataExtentBufferEnvelope = new AtomicReference<>();
		AtomicReference<Geometry> dataExtentBufferBoxGeom = new AtomicReference<>();
		Envelope dataExtentBox;
		Envelope gridExtentBox;
		AtomicReference<Envelope> gridExtentBufferEnvelope = new AtomicReference<>();
		AtomicReference<Geometry> gridExtentBufferBoxGeom = new AtomicReference<>();
		Envelope gridExtent;
		Envelope dataExtent;
		gridExtent = AdvMvtTileUtils.getTileRect(zoom, x, y, sourceGrid);
		tileExecParams.setGridSrid(sourceGrid).setGridExtent(gridExtent).setSourceDataSrid(sourceDataSrid);
		// 3. 处理坐标系转换
		if (!ObjectUtil.equals(sourceGrid, sourceDataSrid)) {
			double xmin = gridExtent.getMinX();
			double ymin = gridExtent.getMinY();
			double xmax = gridExtent.getMaxX();
			double ymax = gridExtent.getMaxY();
			gridExtentBox = new Envelope(xmin, xmax, ymin, ymax);
			dataExtentBox = GirAdvTools.getSridOpt().convert(gridExtentBox, sourceGrid, sourceDataSrid, false);
			if (dataExtentBox == null) {
				Geometry geometry = GirAdvTools.getSridOpt().convertToGeom(gridExtentBox);
				throw new RuntimeException(
						StrUtil.format("网格计算异常：z:{}  x :{} y:{} geometry:{}  gridSrid :{} sourceDataSrid:{} ", zoom, x,
								y, geometry.toText(), sourceGrid, sourceDataSrid));
			}
			xmin = dataExtentBox.getMinX();
			ymin = dataExtentBox.getMinY();
			xmax = dataExtentBox.getMaxX();
			ymax = dataExtentBox.getMaxY();
			dataExtent = new Envelope(xmin, ymin, xmax, ymax);
		}
		else {
			dataExtent = gridExtent;
			double xmin = gridExtent.getMinX();
			double ymin = gridExtent.getMinY();
			double xmax = gridExtent.getMaxX();
			double ymax = gridExtent.getMaxY();
			gridExtentBox = new Envelope(xmin, xmax, ymin, ymax);
			dataExtentBox = new Envelope(xmin, xmax, ymin, ymax);
		}
		tileExecParams.setDataExtentBox(dataExtentBox).setGridExtentBox(gridExtentBox).setDataExtent(dataExtent);
		initExtentBuffer(gridExtentBox, gridExtentBufferEnvelope::set, gridExtentBufferBoxGeom::set);
		initExtentBuffer(dataExtentBox, dataExtentBufferEnvelope::set, dataExtentBufferBoxGeom::set);
		tileExecParams.setDataExtentBufferBoxGeom(dataExtentBufferBoxGeom.get())
				.setDataExtentBufferEnvelope(dataExtentBufferEnvelope.get())
				.setGridExtentBufferBoxGeom(gridExtentBufferBoxGeom.get())
				.setGridExtentBufferEnvelope(gridExtentBufferEnvelope.get());
		return tileExecParams.copy();
	}

	/**
	 * 通用的范围缓冲初始化方法
	 * @param originalExtentBox 原始范围（源/目标）
	 * @param setBufferEnvelope 缓冲后的Envelope赋值回调
	 * @param setBufferGeom 缓冲后的Geometry赋值回调
	 */
	public static void initExtentBuffer(Envelope originalExtentBox, Consumer<Envelope> setBufferEnvelope,
			Consumer<Geometry> setBufferGeom) {
		// 公共缓冲计算逻辑
		double xmin = originalExtentBox.getMinX();
		double ymin = originalExtentBox.getMinY();
		double xmax = originalExtentBox.getMaxX();
		double ymax = originalExtentBox.getMaxY();

		// 取对应长度的1/256进行缓冲
		double widthX = (xmax - xmin) / 256.0;
		double heightY = (ymax - ymin) / 256.0;
		xmin -= widthX;
		ymin -= heightY;
		xmax += widthX;
		ymax += heightY;

		// 构建缓冲后的对象
		Envelope bufferEnvelope = new Envelope(xmin, xmax, ymin, ymax);
		Geometry bufferGeom = JTS.toGeometry(bufferEnvelope);

		// 通过回调赋值到对应成员变量
		setBufferEnvelope.accept(bufferEnvelope);
		setBufferGeom.accept(bufferGeom);
	}

	// public static Envelope extentToEnvelope(Extent extent) {
	// return new Envelope(extent.xmax(), extent.xmin(), extent.ymax(), extent.ymin());
	// }
	//
	// public static Extent envelopeToExtent(Envelope envelope) {
	// return new Extent(envelope.getMinX(), envelope.getMinY(), envelope.getMaxX(),
	// envelope.getMaxY());
	// }

}
