
package cn.geoair.map.tile.forge.core.bygwc.config;



import java.util.List;

/**
 * 表示ArcGIS缓存配置文件中的{@code TileCacheInfo}元素。
 *
 * <p>XML结构:
 *
 * <pre>
 * <code>
 *   &lt;TileCacheInfo xsi:type='typens:TileCacheInfo'&gt;
 *     &lt;SpatialReference xsi:type='typens:ProjectedCoordinateSystem'&gt;
 *       ....
 *     &lt;/SpatialReference&gt;
 *     &lt;TileOrigin xsi:type='typens:PointN'&gt;
 *       &lt;X&gt;-4020900&lt;/X&gt;
 *       &lt;Y&gt;19998100&lt;/Y&gt;
 *     &lt;/TileOrigin&gt;
 *     &lt;TileCols&gt;512&lt;/TileCols&gt;
 *     &lt;TileRows&gt;512&lt;/TileRows&gt;
 *     &lt;DPI&gt;96&lt;/DPI&gt;
 *     &lt;PreciseDPI&gt;96&lt;/PreciseDPI&gt;
 *     &lt;LODInfos xsi:type='typens:ArrayOfLODInfo'&gt;
 *       &lt;LODInfo xsi:type='typens:LODInfo'&gt;
 *         &lt;LevelID&gt;0&lt;/LevelID&gt;
 *         &lt;Scale&gt;8000000&lt;/Scale&gt;
 *         &lt;Resolution&gt;2116.670900008467&lt;/Resolution&gt;
 *       &lt;/LODInfo&gt;
 *       .....
 *     &lt;/LODInfos&gt;
 *   &lt;/TileCacheInfo&gt;
 * </code>
 * </pre>
 *
 * @author Gabriel Roldan
 */
public class TileCacheInfo  implements   java.io.Serializable {

    /**
     * 空间参考系统
     */
    private SpatialReference spatialReference;

    /**
     * 瓦片原点坐标
     */
    private TileOrigin tileOrigin;

    /**
     * 瓦片列数
     */
    private int tileCols;

    /**
     * 瓦片行数
     */
    private int tileRows;

    /**
     * 每英寸点数(Dots Per Inch)
     */
    private int DPI;

    /**
     * 精确每英寸点数(Dots Per Inch)
     */
    private int PreciseDPI;

    /**
     * 详细层次(LOD)信息列表
     */
    private List<LODInfo> lodInfos;

    /**
     * 获取空间参考系统
     * @return 空间参考系统
     */
    public SpatialReference getSpatialReference() {
        return spatialReference;
    }

    /**
     * 获取瓦片原点坐标
     * @return 瓦片原点坐标
     */
    public TileOrigin getTileOrigin() {
        return tileOrigin;
    }

    /**
     * 获取瓦片列数
     * @return 瓦片列数
     */
    public int getTileCols() {
        return tileCols;
    }

    /**
     * 获取瓦片行数
     * @return 瓦片行数
     */
    public int getTileRows() {
        return tileRows;
    }

    /**
     * 获取每英寸点数
     * @return 每英寸点数
     */
    public int getDPI() {
        return DPI;
    }

    /** ArcGIS 10.1版本新增 */
    public int getPreciseDPI() {
        return PreciseDPI;
    }

    /**
     * 获取详细层次信息列表
     * @return 详细层次信息列表
     */
    public List<LODInfo> getLodInfos() {
        return lodInfos;
    }


    public void setSpatialReference(SpatialReference spatialReference) {
        this.spatialReference = spatialReference;
    }

    public void setTileOrigin(TileOrigin tileOrigin) {
        this.tileOrigin = tileOrigin;
    }

    public void setTileCols(int tileCols) {
        this.tileCols = tileCols;
    }

    public void setTileRows(int tileRows) {
        this.tileRows = tileRows;
    }

    public void setDPI(int DPI) {
        this.DPI = DPI;
    }

    public void setPreciseDPI(int preciseDPI) {
        PreciseDPI = preciseDPI;
    }

    public void setLodInfos(List<LODInfo> lodInfos) {
        this.lodInfos = lodInfos;
    }
}
