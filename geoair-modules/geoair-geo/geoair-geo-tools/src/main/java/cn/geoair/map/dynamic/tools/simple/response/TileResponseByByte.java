package cn.geoair.map.dynamic.tools.simple.response;

import cn.geoair.web.mime.GiMimeType;
import cn.geoair.web.mime.GirImageMime;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * 以字节数组承载瓦片内容的响应。
 *
 * <p>每次 {@link #toInputStream()} 都创建新的内存流，可重复读取。</p>
 *
 * @author 张逢吉
 */
@Data
@Accessors(chain = true)
public class TileResponseByByte extends TileResponse {

    /** @return 空的字节瓦片响应。 */
    public static TileResponseByByte of() {
        return new TileResponseByByte();
    }


    /**
     * 瓦片输入字节
     */
    private byte[] bytes;

    /** @return 基于当前字节数组新建的输入流。 */
    public InputStream toInputStream() {
        return new ByteArrayInputStream(bytes);
    }


    /** @return 当前瓦片字节数组本身，不创建副本。 */
    public byte[] toByteArrays() {
        return bytes;
    }

    /** @return 是否成功、存在且包含非空字节数据。 */
    public boolean isValid() {
        return success && exists && bytes != null && bytes.length > 0;
    }

    /**
     * 获取内容长度（兼容HTTP Content-Length）
     */
    public Long getContentLength() {
        return bytes != null ? (long) bytes.length : size;
    }

    /**
     * 设置瓦片字节并自动更新size
     */
    public TileResponse setBytesAndUpdateSize(byte[] bytes) {
        this.bytes = bytes;
        this.size = bytes != null ? (long) bytes.length : 0;
        this.exists = bytes != null && bytes.length > 0;
        return this;
    }

    /**
     * 创建成功的字节瓦片响应。
     *
     * @param bytes    瓦片内容
     * @param mimeType 内容类型；为 {@code null} 时使用 PNG
     * @return 字节瓦片响应
     */
    public static TileResponse success(byte[] bytes, GiMimeType mimeType) {
        TileResponseByByte response = new TileResponseByByte();
        response.setSuccess(true);
        response.setBytes(bytes);
        response.setSize((long) (bytes != null ? bytes.length : 0));
        response.setMimeType(mimeType != null ? mimeType : GirImageMime.png);
        response.setExists(bytes != null && bytes.length > 0);
        response.setLastModified(System.currentTimeMillis());
        return response;
    }
}
