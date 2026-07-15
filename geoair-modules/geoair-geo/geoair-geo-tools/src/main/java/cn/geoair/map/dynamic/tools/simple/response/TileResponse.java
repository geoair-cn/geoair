package cn.geoair.map.dynamic.tools.simple.response;

import cn.geoair.map.dynamic.tools.grid.dto.TileZxyApo;
import cn.geoair.web.mime.GiMimeType;
import cn.geoair.web.mime.GirImageMime;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Map;

/**
 * @author ：张俊
 * @date ：Created in 2025/11/13 16:17
 * @description：瓦片请求数据传输对象
 */
@Data
@Accessors(chain = true)
public class TileResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    public static TileResponse of() {
        return new TileResponse();
    }

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


    /**
     * 瓦片请求是否成功
     */
    private boolean success = true;

    /**
     * 错误码（当success为false时使用）
     */
    private String errorCode;

    /**
     * 错误信息（当success为false时使用）
     */
    private String errorMessage;

    /**
     * 瓦片坐标信息
     */
    private TileZxyApo coordinate;

    /**
     * 请求网格坐标系
     */
    private String gridEpsgStr;

    /**
     * 瓦片数据来源
     */
    private String dataSource;

    /**
     * 响应生成耗时（毫秒）
     */
    private long elapsedTime;

    /**
     * 缓存控制头（Expires、Cache-Control等）
     */
    private Map<String, String> cacheHeaders;


    /**
     * 瓦片格式版本
     */
    private String version = "1.0";

    /**
     * 瓦片ETag
     */
    private String eTag;
    /**
     * 扩展数据（用于传递额外信息）
     */
    private Map<String, Object> extras;


    // ========== 便捷方法 ==========

    /**
     * 创建成功的响应
     */
    public static TileResponse success(byte[] bytes, GiMimeType mimeType) {
        TileResponse response = new TileResponse();
        response.setSuccess(true);
        response.setBytes(bytes);
        response.setSize(bytes != null ? bytes.length : 0);
        response.setMimeType(mimeType != null ? mimeType : GirImageMime.png);
        response.setExists(bytes != null && bytes.length > 0);
        response.setLastModified(System.currentTimeMillis());
        return response;
    }

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
        return success && exists && bytes != null && bytes.length > 0;
    }

    /**
     * 获取内容长度（兼容HTTP Content-Length）
     */
    public int getContentLength() {
        return bytes != null ? bytes.length : 0;
    }

    /**
     * 获取缓存ETag（如果未设置则自动生成）
     */
    public String getETag() {
        if (eTag == null && bytes != null) {
            // 简单实现：使用大小+修改时间生成ETag
            eTag = String.format("\"%d-%d\"", size, lastModified);
        }
        return eTag;
    }


    /**
     * 设置瓦片字节并自动更新size
     */
    public TileResponse setBytesAndUpdateSize(byte[] bytes) {
        this.bytes = bytes;
        this.size = bytes != null ? bytes.length : 0;
        this.exists = bytes != null && bytes.length > 0;
        return this;
    }
}
