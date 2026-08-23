package cn.geoair.map.dynamic.tools.grid.dto;


import java.util.Objects;

import cn.geoair.map.dynamic.tools.GirGeoTools;
import lombok.Data;
import lombok.experimental.Accessors;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Geometry;

/**
 * 瓦片坐标值对象，按 {@code z/x/y} 保存层级、列号和行号。
 *
 * <p>该对象本身不携带 Y 轴原点约定；使用 TMS 行号时，需在调用转换 API 时显式传入
 * {@link TileYAxis}。无 Y 轴参数的本类边界方法固定按 Google/XYZ 顶部原点解释。</p>
 *
 * @author 张逢吉
 */
@Data
@Accessors(chain = true)
public class TileZxyApo {

    /** 缩放级别。 */
    private int z;

    /** 瓦片列号。 */
    private int x;

    /** 瓦片行号；其原点由调用上下文决定。 */
    private int y;

    /**
     * 创建空的瓦片坐标对象，适用于 Lombok 链式赋值。
     *
     * @return 空坐标对象
     */
    public static TileZxyApo of() {
        return new TileZxyApo();
    }

    /** 创建空的瓦片坐标对象。 */
    public TileZxyApo() {
    }

    /**
     * 使用数值坐标创建对象，不校验层级和行列号范围。
     *
     * @param z 缩放级别
     * @param x 瓦片列号
     * @param y 瓦片行号
     */
    public TileZxyApo(int z, int x, int y) {
        this.z = z;
        this.x = x;
        this.y = y;
    }

    /**
     * 从三个数值字符串创建对象。
     *
     * @throws IllegalArgumentException 任一字符串不是整数时抛出
     */
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
     * 从 {@code z/x/y} 格式的字符串解析瓦片坐标。
     *
     * @param zxyString 以斜杠分隔的层级、列号和行号
     * @throws IllegalArgumentException 字符串为空、格式不符或包含非整数时抛出
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
     * 从数组的前三项构造（格式：{@code [z, x, y]}）。
     *
     * @param zxyArray 至少包含三个元素的数组
     * @throws IllegalArgumentException 数组为空或长度不足三时抛出
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

    /**
     * 返回 {@code z/x/y} 格式的瓦片坐标文本。
     *
     * @return 以斜杠分隔的层级、列号与行号
     */
    public String getZxyString() {
        return z + "/" + x + "/" + y;
    }

    /**
     * 按 Google/XYZ 顶部原点解释当前行号，返回 EPSG:4326 瓦片边界的 WKT。
     *
     * @return 瓦片边界多边形的 WKT 文本
     */
    public String toBox4326WktString() {
        ReferencedEnvelope referencedEnvelope =
                GirGeoTools.defaultInstance().getTileGrid4326Opt().xyzToTileBox(z, x, y, TileYAxis.XYZ, 4326);
        Geometry geometry = GirGeoTools.defaultInstance().getSridOpt().convertToGeom(referencedEnvelope, 4326, 4326);
        return GirGeoTools.defaultInstance().getFormatOpt().jtsGeometryToWktString(geometry, true);
    }

    /**
     * 按 Google/XYZ 顶部原点解释当前行号，返回 EPSG:3857 瓦片边界的 WKT。
     *
     * @return 瓦片边界多边形的 WKT 文本
     */
    public String toBox3857WktString() {
        ReferencedEnvelope referencedEnvelope =
                GirGeoTools.defaultInstance().getTileGrid3857Opt().xyzToTileBox(z, x, y, TileYAxis.XYZ, 3857);
        Geometry geometry = GirGeoTools.defaultInstance().getSridOpt().convertToGeom(referencedEnvelope, 3857, 3857);
        return GirGeoTools.defaultInstance().getFormatOpt().jtsGeometryToWktString(geometry, true);
    }
}
