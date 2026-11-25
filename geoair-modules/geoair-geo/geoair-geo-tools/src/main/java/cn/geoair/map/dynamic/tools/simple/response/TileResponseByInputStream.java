package cn.geoair.map.dynamic.tools.simple.response;

import cn.geoair.web.mime.GiMimeType;
import cn.geoair.web.mime.GirImageMime;
import cn.hutool.core.io.IoUtil;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 以输入流承载瓦片内容的响应。
 *
 * <p>未缓存字节时，{@link #toInputStream()} 返回原始输入流，调用方负责按一次性流处理。 {@link #toByteArrays()}
 * 会读取并关闭原始流，再缓存读取到的字节；缓存存在后可重复取得流。 {@link InputStream#available()} 不能可靠表示总长度，因此仅显式指定的长度或已缓存字节会 作为
 * HTTP Content-Length 使用。
 *
 * @author 张逢吉
 */
@Data
@Accessors(chain = true)
public class TileResponseByInputStream extends TileResponse {

    /** 瓦片输入流 */
    private InputStream inputStream;

    /** 缓存字节数组（从输入流读取后缓存） */
    private byte[] bytes;

    /** @return 空的输入流瓦片响应。 */
    public static TileResponseByInputStream of() {
        return new TileResponseByInputStream();
    }

    /**
     * 获取内容流；缓存存在时返回新建内存流，否则返回原始流。
     *
     * @return 可读取的内容流；无可用内容时返回 {@code null}
     */
    public InputStream toInputStream() {
        // 优先从缓存字节创建新流。

        if (bytes != null && bytes.length > 0) {
            return new ByteArrayInputStream(bytes);
        }
        // InputStream 没有通用的“未关闭”检测方式，available() 也不能代表可读总长度。
        // 因此只要调用方提供了流就交由实际写出过程消费；写出失败由输出链路处理。
        return inputStream;
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
                this.bytes = baos.toByteArray();
                this.size = (long) this.bytes.length;
                this.exists = this.bytes.length > 0;
                return this.bytes;
            } catch (Exception e) {
                this.bytes = null;
                this.size = 0L;
                this.exists = false;
                return null;
            } finally {
                IoUtil.close(inputStream);
                // 原始流已被读取并关闭，后续必须从缓存字节创建新流。
                this.inputStream = null;
            }
        }

        return null;
    }

    /** 判断响应是否成功、存在且至少拥有可读取的内容来源。 */
    public boolean isValid() {
        // 基础验证
        if (!success || !exists) {
            return false;
        }

        return (bytes != null && bytes.length > 0) || inputStream != null;
    }

    /**
     * 获取可可靠声明的内容长度（兼容 HTTP Content-Length）。
     *
     * <p>优先级为显式 {@code size}、缓存字节长度。仅持有输入流时返回 {@code null}， 因为 {@link InputStream#available()}
     * 只表示当前非阻塞可读取字节数，不能作为内容总长度。
     */
    public Long getContentLength() {

        if (size != null) {
            return size;
        }

        if (bytes != null && bytes.length > 0) {
            return (long) bytes.length;
        }
        return null;
    }

    /** 设置瓦片字节并自动更新size */
    public TileResponse setBytesAndUpdateSize(byte[] bytes) {
        this.bytes = bytes;
        this.size = bytes != null ? (long) bytes.length : 0;
        this.exists = bytes != null && bytes.length > 0;
        return this;
    }

    /**
     * 设置输入流（同时清空之前缓存的字节和长度）。
     *
     * <p>不再根据 {@code available()} 推测长度；需要写出 Content-Length 时，请使用 {@link #success(InputStream,
     * GiMimeType, long)} 或显式设置 {@link #setSize(Long)}。
     */
    public TileResponseByInputStream setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
        this.bytes = null;
        this.size = null;
        this.exists = inputStream != null;
        return this;
    }

    /** 创建成功的响应（基于输入流） */
    public static TileResponseByInputStream success(InputStream inputStream, GiMimeType mimeType) {
        TileResponseByInputStream response = new TileResponseByInputStream();
        response.setSuccess(true);
        response.setInputStream(inputStream);
        response.setMimeType(mimeType != null ? mimeType : GirImageMime.png);
        response.setExists(inputStream != null);
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
