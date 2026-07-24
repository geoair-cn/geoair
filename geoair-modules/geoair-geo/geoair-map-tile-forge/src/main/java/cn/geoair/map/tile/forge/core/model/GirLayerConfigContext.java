package cn.geoair.map.tile.forge.core.model;

import cn.geoair.map.tile.forge.core.enums.GirMapTileType;
import cn.geoair.map.tile.forge.core.enums.GirStorageType;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class GirLayerConfigContext {

    /** 图层对应的数据唯一标识，可以是图层名称，也可以是图层对应的数据ID */
    private String dataId;

    /**
     * ZIP文件在存储系统中的唯一标识符/路径 如果在S3存储中，则使用S3的Object Key作为值 如果在非S3存储中，则使用本地文件系统路径作为值 例如
     * "s3://my-bucket/my-zip.zip" 或 "C:\\my-zip.zip" 如果是非解压模式，则该值应该直接指向_alllayers路径 例如
     * "C:\\arcgisTest\\arcgis_compact_tile_test_v1\\_alllayers"
     */
    private String objectKey;

    /** 存储类型枚举，表示文件的存储方式（如本地、云存储等） */
    private GirStorageType storageType;

    /** 瓦片格式类型枚举，表示地图瓦片的ArcGIS格式类型 */
    private GirMapTileType mapTileType;

    /**
     * 瓦片路径前缀 如果是压缩包的话，可能存在压缩包中的子目录，这里应该直接从压缩包的根指向到_alllayers l例如
     * "arcgis_compact_tile_test_v1/_alllayers" 如果是本地文件的话，这里直接为空
     */
    private String tilePathPrefix;
    /** 瓦片格式 */
    private String format;

    /** X方向上的最大瓦片编号 */
    private Integer maxX;

    /** Y方向上的最大瓦片编号 */
    private Integer maxY;

    /** Z方向（缩放级别）上的最大值 */
    private Integer maxZ; // 默认最大缩放级别18

    /** Z方向（缩放级别）上的最小值 */
    private Integer minZ; // 默认最小缩放级别0

    public String getLayerName() {
        return dataId;
    }
}
