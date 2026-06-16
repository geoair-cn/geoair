package cn.geoair.map.tile.forge.core.model;

import cn.geoair.map.tile.forge.core.enums.GirStorageType;
import cn.geoair.map.tile.forge.core.enums.GirMapTileType;
import lombok.Getter;

@Getter
public class GirLayerConfigContext {

    /**
     * 图层对应的数据唯一标识，可以是图层名称，也可以是图层对应的数据ID
     */
    private String dataId;


    /**
     * ZIP文件在存储系统中的唯一标识符/路径
     * 如果在S3存储中，则使用S3的Object Key作为值
     * 如果在非S3存储中，则使用本地文件系统路径作为值
     * 例如 "s3://my-bucket/my-zip.zip" 或 "C:\\my-zip.zip"
     * 如果是非解压模式，则该值应该直接指向_alllayers路径
     * 例如 "C:\\arcgisTest\\arcgis_compact_tile_test_v1\\_alllayers"
     */
    private String objectKey;

    /**
     * 存储类型枚举，表示文件的存储方式（如本地、云存储等）
     */
    private GirStorageType storageType;

    /**
     * 瓦片格式类型枚举，表示地图瓦片的ArcGIS格式类型
     */
    private GirMapTileType mapTileType;

    /**
     * 瓦片路径前缀
     * 如果是压缩包的话，可能存在压缩包中的子目录，这里应该直接从压缩包的根指向到_alllayers
     * l例如 "arcgis_compact_tile_test_v1/_alllayers"
     * 如果是本地文件的话，这里直接为空
     */
    private String tilePathPrefix;
    /**
     * 瓦片格式
     */
    private String format;


    /**
     * X方向上的最大瓦片编号
     */
    private Integer maxX;

    /**
     * Y方向上的最大瓦片编号
     */
    private Integer maxY;

    /**
     * Z方向（缩放级别）上的最大值
     */
    private Integer maxZ; // 默认最大缩放级别18

    /**
     * Z方向（缩放级别）上的最小值
     */
    private Integer minZ; // 默认最小缩放级别0


    /**
     * 设置X方向上的最大瓦片编号
     *
     * @param maxX X方向上的最大瓦片编号
     * @return 当前对象实例，支持链式调用
     */
    public GirLayerConfigContext setMaxX(Integer maxX) {
        this.maxX = maxX;
        return this;
    }

    /**
     * 设置Y方向上的最大瓦片编号
     *
     * @param maxY Y方向上的最大瓦片编号
     * @return 当前对象实例，支持链式调用
     */
    public GirLayerConfigContext setMaxY(Integer maxY) {
        this.maxY = maxY;
        return this;
    }

    /**
     * 设置Z方向（缩放级别）上的最大值
     *
     * @param maxZ Z方向上的最大缩放级别
     * @return 当前对象实例，支持链式调用
     */
    public GirLayerConfigContext setMaxZ(Integer maxZ) {
        this.maxZ = maxZ;
        return this;
    }

    /**
     * 设置Z方向（缩放级别）上的最小值
     *
     * @param minZ Z方向上的最小缩放级别
     * @return 当前对象实例，支持链式调用
     */
    public GirLayerConfigContext setMinZ(Integer minZ) {
        this.minZ = minZ;
        return this;
    }


    /**
     * 设置ZIP文件对象键
     *
     * @param objectKey 存储系统中的唯一标识符/路径
     * @return 当前对象实例，支持链式调用
     */
    public GirLayerConfigContext setObjectKey(String objectKey) {
        this.objectKey = objectKey;
        return this;
    }


    /**
     * 设置存储类型
     *
     * @param storageType 存储类型枚举
     * @return 当前对象实例，支持链式调用
     */
    public GirLayerConfigContext setStorageType(GirStorageType storageType) {
        this.storageType = storageType;
        return this;
    }

    /**
     * 设置瓦片ArcGIS类型
     *
     * @param mapTileType 瓦片格式类型枚举
     * @return 当前对象实例，支持链式调用
     */
    public GirLayerConfigContext setMapTileType(GirMapTileType mapTileType) {
        this.mapTileType = mapTileType;
        return this;
    }

    /**
     * 设置瓦片路径前缀
     *
     * @param tilePathPrefix 瓦片路径前缀
     * @return 当前对象实例，支持链式调用
     */
    public GirLayerConfigContext setTilePathPrefix(String tilePathPrefix) {
        this.tilePathPrefix = tilePathPrefix;
        return this;
    }

    public GirLayerConfigContext setDataId(String dataId) {
        this.dataId = dataId;
        return this;
    }

    public String getLayerName() {
        return dataId;
    }

    /**
     * 设置瓦片格式
     *
     * @param format png/jpg
     */
    public void setFormat(String format) {
        this.format = format;
    }
}
