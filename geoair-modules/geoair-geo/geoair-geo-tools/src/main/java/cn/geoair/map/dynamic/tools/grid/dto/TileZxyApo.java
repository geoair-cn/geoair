package cn.geoair.map.dynamic.tools.grid.dto;


import java.util.Objects;

import cn.geoair.map.dynamic.tools.GirGeoTools;
import lombok.Data;
import lombok.experimental.Accessors;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Geometry;

@Data
@Accessors(chain = true)
public class TileZxyApo {

    private int z; // 层级

    private int x; // 列号

    private int y; // 行号

    public static TileZxyApo of() {
        return new TileZxyApo();
    }

    public TileZxyApo() {
    }

    public TileZxyApo(int z, int x, int y) {
        this.z = z;
        this.x = x;
        this.y = y;
    }


    public TileZxyApo(String z, String x, String y) {
        try {
            this.z = Integer.parseInt(z);
            this.x = Integer.parseInt(x);
            this.y = Integer.parseInt(y);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid tile coordinates: z=" + z + ", x=" + x + ", y=" + y, e);
        }
    }

    /**
     * Long类型构造函数（支持从Long类型转换）
     */
    public TileZxyApo(Long z, Long x, Long y) {
        if (z == null || x == null || y == null) {
            throw new IllegalArgumentException("Tile coordinates cannot be null");
        }
        this.z = z.intValue();
        this.x = x.intValue();
        this.y = y.intValue();
    }

    /**
     * 从单个字符串解析（格式：z/x/y）
     */
    public TileZxyApo(String zxyString) {
        if (zxyString == null || zxyString.isEmpty()) {
            throw new IllegalArgumentException("ZXY string cannot be null or empty");
        }
        String[] parts = zxyString.split("/");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid ZXY format, expected 'z/x/y', got: " + zxyString);
        }
        try {
            this.z = Integer.parseInt(parts[0]);
            this.x = Integer.parseInt(parts[1]);
            this.y = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid numeric format in ZXY string: " + zxyString, e);
        }
    }

    /**
     * 从数组构造（格式：[z, x, y]）
     */
    public TileZxyApo(int[] zxyArray) {
        if (zxyArray == null || zxyArray.length < 3) {
            throw new IllegalArgumentException("ZXY array must have at least 3 elements");
        }
        this.z = zxyArray[0];
        this.x = zxyArray[1];
        this.y = zxyArray[2];
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
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
        ReferencedEnvelope referencedEnvelope =
                GirGeoTools.defaultInstance().getTileGrid4326Opt().xyzToTileBox(z, x, y, TileYAxis.XYZ, 4326);
        Geometry geometry = GirGeoTools.defaultInstance().getSridOpt().convertToGeom(referencedEnvelope, 4326, 4326);
        return GirGeoTools.defaultInstance().getFormatOpt().jtsGeometryToWktString(geometry, true);
    }

    public String toBox3857WktString() {
        ReferencedEnvelope referencedEnvelope =
                GirGeoTools.defaultInstance().getTileGrid3857Opt().xyzToTileBox(z, x, y, TileYAxis.XYZ, 3857);
        Geometry geometry = GirGeoTools.defaultInstance().getSridOpt().convertToGeom(referencedEnvelope, 3857, 3857);
        return GirGeoTools.defaultInstance().getFormatOpt().jtsGeometryToWktString(geometry, true);
    }
}
