package cn.geoair.sdk.file;

import cn.geoair.base.gpa.id.GirIdGenerator;
import cn.geoair.base.text.GuStrFormatter;
import cn.geoair.sdk.GirSdkException;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * @author ：张俊
 * @date ：Created in 2023/2/28 15:44 @description： 文件输出流封装对象
 */
public class GirMultipartOutputStream extends OutputStream {

    private static final String CONTENT_DISPOSITION_TEMPLATE =
            "Content-Disposition: form-data; name=\"{}\"\r\n";

    private static final String CONTENT_DISPOSITION_FILE_TEMPLATE =
            "Content-Disposition: form-data; name=\"{}\"; filename=\"{}\"\r\n";

    // private static final String CONTENT_TYPE_FILE_TEMPLATE = "Content-Type: {}\r\n";

    String CRLF = "\r\n";

    // http输出流
    private final OutputStream out;

    // 编码
    private final Charset charset;

    // 文件边界符
    private final String boundary;

    private boolean isFinish;

    /**
     * 构造
     *
     * @param out HTTP写出流
     * @param charset 编码
     */
    public GirMultipartOutputStream(OutputStream out, Charset charset) {
        this(out, charset, "-------------------- girSDk_" + GirIdGenerator.simpleUUID());
    }

    public GirMultipartOutputStream(OutputStream out, Charset charset, String boundary) {
        this.out = out;
        this.charset = charset;
        this.boundary = boundary;
    }

    /**
     * 构造
     *
     * @param out HTTP写出流
     */
    public GirMultipartOutputStream(OutputStream out) {
        this(out, StandardCharsets.UTF_8);
    }

    /**
     * 添加Multipart表单的数据项<br>
     *
     * <pre>
     *     --分隔符(boundary)[换行]
     *     Content-Disposition: form-data; name="参数名"[换行]
     *     [换行]
     *     参数值[换行]
     * </pre>
     *
     * <p>或者：
     *
     * <pre>
     *     --分隔符(boundary)[换行]
     *     Content-Disposition: form-data; name="表单名"; filename="文件名"[换行]
     *     Content-Type: MIME类型[换行]
     *     [换行]
     *     文件的二进制内容[换行]
     * </pre>
     *
     * @param formFieldName 表单名
     * @param value 值，可以是普通值、资源（如文件等）
     * @return this
     * @throws IOException IO异常
     */
    public GirMultipartOutputStream write(String formFieldName, Object value) throws IOException {

        // --分隔符(boundary)[换行]
        beginPart();

        if (value instanceof GiSdkMultipartFile) {
            appendFileResource(formFieldName, (GiSdkMultipartFile) value);
        } else {
            appendFieldResource(formFieldName, value);
        }

        writeContent(CRLF);
        return this;
    }

    @Override
    public void write(int b) throws IOException {
        this.out.write(b);
    }

    /**
     * 上传表单结束
     *
     * @throws IOException IO异常
     */
    public void finish() {
        if (false == isFinish) {
            writeContent(GuStrFormatter.format("--{}--\r\n", boundary));
            this.isFinish = true;
        }
    }

    @Override
    public void close() {
        finish();
        close(this.out);
    }

    /**
     * 添加Multipart表单的数据项 文件
     *
     * @param formFieldName 表单名
     * @param resource 资源
     * @throws IOException IO异常
     */
    private void appendFileResource(String formFieldName, GiSdkMultipartFile resource)
            throws IOException {
        final String fileName = resource.getName();

        // Content-Disposition
        if (null == fileName) {
            // Content-Disposition: form-data; name="参数名"[换行]
            writeContent(GuStrFormatter.format(CONTENT_DISPOSITION_TEMPLATE, formFieldName));
        } else {
            // Content-Disposition: form-data; name="参数名"; filename="文件名"[换行]
            writeContent(
                    GuStrFormatter.format(
                            CONTENT_DISPOSITION_FILE_TEMPLATE, formFieldName, fileName));
        }

        // 内容
        writeContent("\r\n");
        resource.writeTo(this);
    }

    /**
     * 添加Multipart表单的数据项
     *
     * @param formFieldName 表单名
     * @param resource 资源
     * @throws IOException IO异常
     */
    private void appendFieldResource(String formFieldName, Object resource) throws IOException {

        // Content-Disposition: form-data; name="参数名"[换行]
        writeContent(GuStrFormatter.format(CONTENT_DISPOSITION_TEMPLATE, formFieldName));
        // 内容
        writeContent("\r\n");
        writeContent(resource.toString());
    }

    /**
     * part开始，写出:<br>
     *
     * <pre>
     *     --分隔符(boundary)[换行]
     * </pre>
     */
    private void beginPart() throws IOException {
        // --分隔符(boundary)[换行]
        writeContent("--", boundary, CRLF);
    }

    /**
     * 写出对象
     *
     * @param objs 写出的对象（转换为字符串）
     */
    private void writeContent(Object... objs) {
        try {
            write(this, this.charset, false, objs);
        } catch (IOException e) {
            close();
            throw new GirSdkException("SDK multipart请求体写出失败", e);
        }
    }

    public static void close(Closeable closeable) {
        if (null != closeable) {
            try {
                closeable.close();
            } catch (Exception e) {
                // 静默关闭
            }
        }
    }

    /**
     * 将多部分内容写到流中，自动转换为字符串
     *
     * @param out 输出流
     * @param charset 写出的内容的字符集
     * @param isCloseOut 写入完毕是否关闭输出流
     * @param contents 写入的内容，调用toString()方法，不包括不会自动换行
     * @throws IOException IO异常
     * @since 3.0.9
     */
    public static void write(
            OutputStream out, Charset charset, boolean isCloseOut, Object... contents)
            throws IOException {
        OutputStreamWriter osw = null;
        try {
            if (null == charset) {
                osw = new OutputStreamWriter(out);
            } else {
                osw = new OutputStreamWriter(out, charset);
            }
            for (Object content : contents) {
                if (content != null) {
                    osw.write(Optional.of(content.toString()).orElse(""));
                }
            }
            osw.flush();
        } catch (IOException e) {
            throw new IOException(e);
        } finally {
            if (isCloseOut) {
                close(osw);
            }
        }
    }
}
