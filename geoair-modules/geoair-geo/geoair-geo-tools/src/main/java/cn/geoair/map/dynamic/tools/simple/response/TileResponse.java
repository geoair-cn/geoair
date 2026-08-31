package cn.geoair.map.dynamic.tools.simple.response;

import cn.geoair.map.dynamic.tools.grid.dto.TileZxyApo;
import cn.geoair.map.dynamic.tools.simple.collection.map.GirFastStrObjMap;
import cn.geoair.web.mime.GiMimeType;
import cn.geoair.web.mime.GirImageMime;
import java.io.InputStream;
import java.io.Serializable;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 瓦片请求的统一响应元数据。
 *
 * <p>本类只承载状态和元数据，默认不携带内容；实际内容由 {@link TileResponseByByte} 或 {@link TileResponseByInputStream} 提供。
 * {@link #success} 表示处理是否成功，{@link #exists} 表示目标瓦片是否存在， {@link #isValid()} 同时要求两者为 {@code true}。
 *
 * @author 张逢吉
 */
@Data
@Accessors(chain = true)
public class TileResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** http状态码 */
    Integer httpCode;

    /** 最后修改时间戳 */
    protected long lastModified;

    /** 瓦片文件大小 */
    protected Long size;

    /** 媒体类型，默认为PNG格式 */
    protected GiMimeType mimeType = GirImageMime.png;

    /** 瓦片是否存在标识 */
    protected boolean exists;

    /** 瓦片请求是否成功 */
    protected boolean success = true;

    /** 错误码（当success为false时使用） */
    protected String errorCode;

    /** 错误信息（当success为false时使用） */
    protected String errorMessage;

    /** 瓦片坐标信息 */
    protected TileZxyApo coordinate;

    /** 请求网格坐标系 */
    protected String gridEpsgStr;

    /** 瓦片数据来源 */
    protected String dataSource;

    /** 响应生成耗时（毫秒） */
    protected long elapsedTime;

    /** 缓存控制头（Expires、Cache-Control等） */
    protected GirFastStrObjMap<String> cacheHeaders;

    /** 瓦片格式版本 */
    protected String version = "1.0";

    /** 瓦片ETag */
    protected String eTag;
    /** 扩展数据（用于传递额外信息） */
    protected GirFastStrObjMap<String> extrasHeaders;

    /**
     * 创建无具体错误码的失败响应。
     *
     * @param errorMessage 失败说明
     * @return 不存在且处理失败的响应
     */
    public static TileResponse error(String errorMessage) {
        TileResponse response = new TileResponse();
        response.setSuccess(false);
        response.setErrorMessage(errorMessage);
        response.setExists(false);
        return response;
    }

    /**
     * 创建“瓦片不存在”的失败响应。
     *
     * @return 错误码为 {@code TILE_NOT_FOUND} 的响应
     */
    public static TileResponse notFound() {
        TileResponse response = new TileResponse();
        response.setSuccess(false);
        response.setExists(false);
        response.setErrorCode("TILE_NOT_FOUND");
        response.setErrorMessage("Tile not found");
        return response;
    }

    /**
     * 判断响应是否表示一个有效瓦片。
     *
     * <p>子类会额外校验其内容是否可读。
     *
     * @return 是否处理成功且瓦片存在
     */
    public boolean isValid() {
        return success && exists;
    }

    /** 获取内容长度（兼容HTTP Content-Length） */
    public Long getContentLength() {
        return size;
    }

    /** 获取缓存ETag（如果未设置则自动生成） */
    public String getETag() {
        if (eTag == null && size != null) {
            // 简单实现：使用大小+修改时间生成ETag
            eTag = String.format("\"%d-%d\"", size, lastModified);
        }
        return eTag;
    }

    /**
     * 获取瓦片内容流。
     *
     * @return 内容流；基础实现不保存内容，固定返回 {@code null}
     */
    public InputStream toInputStream() {
        return null;
    }

    /**
     * 获取瓦片内容字节。
     *
     * @return 内容字节；基础实现固定返回空数组
     */
    public byte[] toByteArrays() {
        return new byte[0];
    }

    /**
     * 设置瓦片字节并同步内容长度。
     *
     * <p>基础实现不保存内容，子类覆盖此方法以提供实际行为。
     *
     * @param bytes 瓦片字节
     * @return 当前响应对象
     */
    public TileResponse setBytesAndUpdateSize(byte[] bytes) {
        // 由子类实现
        return this;
    }
}
