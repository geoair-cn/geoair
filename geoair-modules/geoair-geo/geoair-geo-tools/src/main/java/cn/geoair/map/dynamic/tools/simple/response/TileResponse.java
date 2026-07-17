package cn.geoair.map.dynamic.tools.simple.response;

import cn.geoair.map.dynamic.tools.grid.dto.TileZxyApo;
import cn.geoair.map.dynamic.tools.simple.collection.map.GirFastStrObjMap;
import cn.geoair.web.mime.GiMimeType;
import cn.geoair.web.mime.GirImageMime;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.InputStream;
import java.io.Serializable;

/**
 * @author ：张俊
 * @date ：Created in 2025/11/13 16:17
 * @description：瓦片请求数据传输对象
 */
@Data
@Accessors(chain = true)
public class TileResponse implements Serializable {

    private static final long serialVersionUID = 1L;


    /**
     * 最后修改时间戳
     */
    protected long lastModified;

    /**
     * 瓦片文件大小
     */
    protected Long size;

    /**
     * 媒体类型，默认为PNG格式
     */
    protected GiMimeType mimeType = GirImageMime.png;

    /**
     * 瓦片是否存在标识
     */
    protected boolean exists;


    /**
     * 瓦片请求是否成功
     */
    protected boolean success = true;

    /**
     * 错误码（当success为false时使用）
     */
    protected String errorCode;

    /**
     * 错误信息（当success为false时使用）
     */
    protected String errorMessage;

    /**
     * 瓦片坐标信息
     */
    protected TileZxyApo coordinate;

    /**
     * 请求网格坐标系
     */
    protected String gridEpsgStr;

    /**
     * 瓦片数据来源
     */
    protected String dataSource;

    /**
     * 响应生成耗时（毫秒）
     */
    protected long elapsedTime;

    /**
     * 缓存控制头（Expires、Cache-Control等）
     */
    protected GirFastStrObjMap<String> cacheHeaders;


    /**
     * 瓦片格式版本
     */
    protected String version = "1.0";

    /**
     * 瓦片ETag
     */
    protected String eTag;
    /**
     * 扩展数据（用于传递额外信息）
     */
    protected GirFastStrObjMap<String> extrasHeaders;




    /**
     * 创建失败的响应
     */
    public static TileResponse error(String errorMessage) {
        TileResponse response = new TileResponse();
        response.setSuccess(false);
        response.setErrorMessage(errorMessage);
        response.setExists(false);
        return response;
    }

    /**
     * 创建空瓦片响应（瓦片不存在）
     */
    public static TileResponse notFound() {
        TileResponse response = new TileResponse();
        response.setSuccess(false);
        response.setExists(false);
        response.setErrorCode("TILE_NOT_FOUND");
        response.setErrorMessage("Tile not found");
        return response;
    }


    public boolean isValid() {
        return success && exists;
    }

    /**
     * 获取内容长度（兼容HTTP Content-Length）
     */
    public Long getContentLength() {
        return size;
    }

    /**
     * 获取缓存ETag（如果未设置则自动生成）
     */
    public String getETag() {
        if (eTag == null && size != null) {
            // 简单实现：使用大小+修改时间生成ETag
            eTag = String.format("\"%d-%d\"", size, lastModified);
        }
        return eTag;
    }
    public InputStream toInputStream() {
        return null;
    }


    public byte[] toByteArrays() {
        return new byte[0];
    }

    /**
     * 设置瓦片字节并自动更新size
     */
    public TileResponse setBytesAndUpdateSize(byte[] bytes) {
        // 由子类实现
        return this;
    }
}
