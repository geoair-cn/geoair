package cn.geoair.sdk.body;

import cn.geoair.sdk.GirSdkException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author ：张俊
 * @date ：Created in 2023/2/28 16:26 @description： TODO
 */
public class StringRequestBody implements GiRequestBody {

    String bodyString;

    String contentType;

    Charset charsetName = StandardCharsets.UTF_8;

    public StringRequestBody(String bodyString, String contentType) {
        this.bodyString = bodyString;
        this.contentType = contentType;
    }

    public StringRequestBody(String bodyString, String contentType, Charset charsetName) {
        this.bodyString = bodyString;
        this.contentType = contentType;
        this.charsetName = charsetName;
    }

    @Override
    public void write(OutputStream out) {
        try {
            out.write((bodyString == null ? "" : bodyString).getBytes(charsetName));
        } catch (IOException e) {
            throw new GirSdkException("SDK请求体写出失败", e);
        }
    }

    @Override
    public String getContentType() {
        return contentType;
    }
}
