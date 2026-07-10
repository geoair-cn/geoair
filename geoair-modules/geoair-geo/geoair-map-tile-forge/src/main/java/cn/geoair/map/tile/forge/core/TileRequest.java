package cn.geoair.map.tile.forge.core;

import cn.geoair.map.tile.forge.core.enums.GirMapTileType;
import cn.geoair.map.tile.forge.core.enums.GirStorageType;

import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.web.mime.GiMimeType;
import cn.geoair.web.mime.GirImageMime;
import cn.geoair.web.util.GutilMimeType;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.http.MediaType;


import java.io.Serializable;

/**
 * @author ：张俊
 * &#064;date ：Created in 2025/11/13 16:17
 * &#064;description：瓦片请求数据传输对象
 */
@Data
@Accessors(chain = true)
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
    private byte[] bytes;

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
    protected GiMimeType mimeType = GirImageMime.png;

    /**
     * 瓦片是否存在标识
     */
    private boolean exists;

    private String httpUrl;


    public void mimeTypeBySpring(MediaType mimeType) {
        String type = mimeType.getSubtype();
        this.mimeType = GutilMimeType.createFromExtension(type);
    }


    public static TileRequest emptyByContext(GirLayerConfigContext layerConfigContext) {
        TileRequest tileRequest = new TileRequest();
        tileRequest.setStorageType(layerConfigContext.getStorageType());
        tileRequest.setMapTileType(layerConfigContext.getMapTileType());
        tileRequest.setLayerName(layerConfigContext.getLayerName());
        tileRequest.setExists(false);
        tileRequest.setLastModified(0);
        tileRequest.setSize(0);
        tileRequest.setBytes(new byte[0]);
        return tileRequest;
    }

}
