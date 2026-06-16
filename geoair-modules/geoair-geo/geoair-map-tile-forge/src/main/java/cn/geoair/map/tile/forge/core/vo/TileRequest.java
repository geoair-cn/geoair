package cn.geoair.map.tile.forge.core.vo;

import cn.geoair.map.tile.forge.core.enums.GirMapTileType;
import cn.geoair.map.tile.forge.core.enums.GirStorageType;

import lombok.Data;
import org.springframework.http.MediaType;

import java.io.Serializable;

/**
 * @author ：张俊
 * &#064;date ：Created in 2025/11/13 16:17
 * &#064;description：瓦片请求数据传输对象
 */
@Data
public class TileRequest implements Serializable {

    /**
     * 图层名称
     */
    private String layerName;



    /**
     * 瓦片格式
     */
    private GirMapTileType mapTileType;

    /**
     * 存储类型
     */
    private GirStorageType storageType;

    /**
     * 瓦片输入流
     */
    private byte [] bytes;

    /**
     * 最后修改时间戳
     */
    private long lastModified;

    /**
     * 瓦片文件大小
     */
    private long size;

    /**
     * 媒体类型，默认为PNG格式
     */
//    protected  MediaType mimeType = MediaType.IMAGE_PNG;
    protected  String  mimeType = MediaType.IMAGE_PNG_VALUE;

    /**
     * 瓦片是否存在标识
     */
    private boolean exists;

    private String httpUrl;

    public MediaType mimeTypeToType() {
        return MediaType.parseMediaType(mimeType);
    }

    public void mimeTypeByType(MediaType mimeType) {
        this.mimeType = mimeType.toString();
    }
}
