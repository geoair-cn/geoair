package cn.geoair.map.dynamic.tools.simple.response;

import cn.geoair.web.mime.GiMimeType;
import cn.geoair.web.mime.GirImageMime;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * @author ：张俊
 * @date ：Created in 2025/11/13 16:17
 * @description：瓦片请求数据传输对象
 */
@Data
@Accessors(chain = true)
public class TileResponseByByte extends TileResponse {

    public static TileResponseByByte of() {
        return new TileResponseByByte();
    }


    /**
     * 瓦片输入字节
     */
    private byte[] bytes;

    public InputStream getInputStream() {
        return new ByteArrayInputStream(bytes);
    }


    public byte[] getBytes() {
        return bytes;
    }

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
        // 由子类实现
        this.bytes = bytes;
        this.size = bytes != null ? (long) bytes.length : 0;
        this.exists = bytes != null && bytes.length > 0;
        return this;
    }

    /**
     * 创建成功的响应
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
