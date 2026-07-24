package cn.geoair.map.dynamic.tools.simple.response;

import cn.geoair.web.mime.GiMimeType;
import cn.geoair.web.mime.GirImageMime;
import cn.hutool.core.io.IoUtil;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author ：张俊
 * @date ：Created in 2026/7/17 11:14
 * @description：基于输入流的瓦片响应
 */
@Data
@Accessors(chain = true)
public class TileResponseByInputStream extends TileResponse {

    /** 瓦片输入流 */
    private InputStream inputStream;

    /** 缓存字节数组（从输入流读取后缓存） */
    private byte[] bytes;

    public static TileResponseByInputStream of() {
        return new TileResponseByInputStream();
    }

    public InputStream toInputStream() {
        // 2. 如果原始输入流不可用，从缓存的字节数组创建

        if (bytes != null && bytes.length > 0) {
            return new ByteArrayInputStream(bytes);
        }
        // 1. 检查原始输入流是否可用
        if (isInputStreamAvailable(inputStream)) {
            return inputStream;
        }

        // 3. 都没有则返回null
        return null;
    }

    /** 检查输入流是否可用（未关闭且可读） */
    private boolean isInputStreamAvailable(InputStream is) {
        if (is == null) {
            return false;
        }

        try {
            // 检查流是否已关闭：调用available()方法
            // 如果流已关闭，会抛出IOException
            int available = is.available();
            // available() 返回 -1 表示流已结束或关闭
            return available > 0;
        } catch (IOException e) {
            // 抛出IOException通常表示流已关闭或不可读
            return false;
        }
    }

    /** 获取字节数组 如果已有缓存则直接返回，否则从InputStream读取 */
    public byte[] toByteArrays() {
        // 如果已有缓存则直接返回
        if (bytes != null && bytes.length > 0) {
            return bytes;
        }

        // 如果存在inputStream，转换为byte[]
        if (inputStream != null) {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                IoUtil.copy(inputStream, baos);
                IoUtil.close(inputStream);
                this.bytes = baos.toByteArray();
                this.size = (long) this.bytes.length;
                this.exists = this.bytes.length > 0;
                return this.bytes;
            } catch (Exception e) {
                this.bytes = null;
                this.size = 0L;
                this.exists = false;
                return null;
            }
        }

        return null;
    }

    /** 判断响应是否有效 */
    /** 判断响应是否有效 */
    public boolean isValid() {
        // 基础验证
        if (!success || !exists) {
            return false;
        }

        boolean hasBytes = bytes != null && bytes.length > 0;
        boolean hasInputStream = inputStream != null;
        if (hasInputStream) {
            try {
                if (inputStream.available() < 0) {
                    return false;
                }
            } catch (IOException e) {
                return false;
            }
        }

        return hasBytes || hasInputStream;
    }

    /** 获取内容长度（兼容HTTP Content-Length） 优先级：bytes > inputStream.available() > size */
    public Long getContentLength() {
        if (bytes != null && bytes.length > 0) {
            return (long) bytes.length;
        }
        if (inputStream != null) {
            try {
                // 注意：available() 不一定准确，但作为参考值
                int available = inputStream.available();
                if (available > 0) {
                    return (long) available;
                }
            } catch (Exception e) {
                // 忽略异常，继续使用size
            }
        }
        return size;
    }

    /** 设置瓦片字节并自动更新size */
    public TileResponse setBytesAndUpdateSize(byte[] bytes) {
        this.bytes = bytes;
        this.size = bytes != null ? (long) bytes.length : 0;
        this.exists = bytes != null && bytes.length > 0;
        return this;
    }

    /** 设置输入流（同时清空之前缓存的字节） */
    public TileResponseByInputStream setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
        this.bytes = null;
        // 如果有输入流且支持available，尝试获取大小
        if (inputStream != null) {
            try {
                long available = inputStream.available();
                if (available > 0) {
                    this.size = available;
                    this.setSuccess(true);
                    this.setExists(true);
                }
            } catch (Exception e) {
                // 忽略异常
            }
        }
        return this;
    }

    /** 创建成功的响应（基于输入流） */
    public static TileResponseByInputStream success(InputStream inputStream, GiMimeType mimeType) {
        TileResponseByInputStream response = new TileResponseByInputStream();
        response.setSuccess(true);
        response.setInputStream(inputStream);
        response.setMimeType(mimeType != null ? mimeType : GirImageMime.png);
        response.setExists(true);
        response.setLastModified(System.currentTimeMillis());
        return response;
    }

    /** 创建成功的响应（基于输入流，并指定大小） */
    public static TileResponseByInputStream success(
            InputStream inputStream, GiMimeType mimeType, long size) {
        TileResponseByInputStream response = new TileResponseByInputStream();
        response.setSuccess(true);
        response.setInputStream(inputStream);
        response.setSize(size);
        response.setMimeType(mimeType != null ? mimeType : GirImageMime.png);
        response.setExists(size > 0);
        response.setLastModified(System.currentTimeMillis());
        return response;
    }
}
