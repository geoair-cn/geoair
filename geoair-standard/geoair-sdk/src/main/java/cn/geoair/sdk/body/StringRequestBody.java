package cn.geoair.sdk.body;

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
            out.write(bodyString.getBytes(charsetName));
        } catch (IOException e) {
            // todo 异常处理
        }
    }

    @Override
    public String getContentType() {
        return contentType;
    }
}
