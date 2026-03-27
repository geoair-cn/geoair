package cn.geoair.sdk.body;

import cn.geoair.base.gpa.id.GirIdGenerator;
import cn.geoair.base.util.GutilCollection;
import cn.geoair.sdk.file.GirMultipartOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * @author ：张俊
 * @date ：Created in 2023/2/28 16:13 @description： 多文件上传的时候 ，文件对象的body封装
 */
public class GirMultipartBody implements GiRequestBody {

    private static final String CONTENT_TYPE_MULTIPART_PREFIX =
            "multipart/form-data" + "; boundary=";

    /** 存储表单数据 */
    private final Map<String, Object> form;

    /** 编码 */
    private final Charset charset;

    /** 边界 */
    private final String boundary = "-------------------- girSDk_" + GirIdGenerator.simpleUUID();

    /**
     * 根据已有表单内容，构建MultipartBody
     *
     * @param form 表单
     * @param charset 编码
     * @return MultipartBody
     */
    public static GirMultipartBody create(Map<String, Object> form, Charset charset) {
        return new GirMultipartBody(form, charset);
    }

    /**
     * 获取Multipart的Content-Type类型
     *
     * @return Multipart的Content-Type类型
     */
    public String getContentType() {
        return CONTENT_TYPE_MULTIPART_PREFIX + boundary;
    }

    /**
     * 构造
     *
     * @param form 表单
     * @param charset 编码
     */
    public GirMultipartBody(Map<String, Object> form, Charset charset) {
        this.form = form;
        this.charset = charset;
    }

    /**
     * 写出Multiparty数据，不关闭流
     *
     * @param out out流
     */
    public void write(OutputStream out) {
        final GirMultipartOutputStream stream = new GirMultipartOutputStream(out, this.charset);
        if (!GutilCollection.isEmpty(this.form)) {
            this.form.forEach(
                    (formFieldName, value) -> {
                        try {
                            stream.write(formFieldName, value);
                        } catch (IOException e) {
                            // todo 异常处理
                        }
                    });
        }
        stream.finish();
    }
}
