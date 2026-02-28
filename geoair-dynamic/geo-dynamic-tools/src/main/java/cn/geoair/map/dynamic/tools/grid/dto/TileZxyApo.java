package cn.geoair.map.dynamic.tools.grid.dto;

import cn.geoair.map.dynamic.tools.GirAdvTools;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Geometry;

import java.util.Objects;

public class TileZxyApo {

	private int z; // 层级

	private int x; // 列号

	private int y; // 行号

	public TileZxyApo(int z, int x, int y) {
		this.z = z;
		this.x = x;
		this.y = y;
	}

	public int getZ() {
		return z;
	}

	public void setZ(int z) {
		this.z = z;
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		TileZxyApo tileZxy = (TileZxyApo) o;
		return z == tileZxy.z && x == tileZxy.x && y == tileZxy.y;
	}

	@Override
	public int hashCode() {
		return Objects.hash(z, x, y);
	}

	@Override
	public String toString() {
		return "z=" + z + ", x=" + x + ", y=" + y;
	}

	public String getZxyString() {
		return z + "/" + x + "/" + y;
	}

	public String toBox4326WktString() {
		ReferencedEnvelope referencedEnvelope = GirAdvTools.getTileGrid4326Opt().xyzToTileBox(z, x, y, 4326);
		Geometry geometry = GirAdvTools.getSridOpt().convertToGeom(referencedEnvelope, 4326, 4326);
		return GirAdvTools.getFormatOpt().jtsGeometryToWktString(geometry, true);
	}

	public String toBox3857WktString() {
		ReferencedEnvelope referencedEnvelope = GirAdvTools.getTileGrid3857Opt().xyzToTileBox(z, x, y, 3857);
		Geometry geometry = GirAdvTools.getSridOpt().convertToGeom(referencedEnvelope, 3857, 3857);
		return GirAdvTools.getFormatOpt().jtsGeometryToWktString(geometry, true);
	}

}
