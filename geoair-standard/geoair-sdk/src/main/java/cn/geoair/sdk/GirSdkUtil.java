package cn.geoair.sdk;

import cn.geoair.base.Gir;
import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.sdk.body.GiRequestBody;
import cn.geoair.sdk.body.GirMultipartBody;
import cn.geoair.sdk.body.StringRequestBody;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

public class GirSdkUtil {

    private static final GiLogger log = GirLoggerFactory.getLogger(GirSdkUtil.class);

    public static Charset CharsetName = StandardCharsets.UTF_8;

    public static <T> T postUrlEncode(
            String urlStr, Map<String, Object> data, Class<T> clazz, Type type) throws Exception {

        String dataString = null;
        if (data == null) {
            dataString = "";
        } else {
            dataString = GirSdkTransport.mapToUrl(data, true, CharsetName);
        }
        String resString =
                post(
                        urlStr,
                        new StringRequestBody(
                                dataString, "application/x-www-form-urlencoded", CharsetName));
        return parseResult(resString, clazz, type);
    }

    public static <T> T postMultipartFile(
            String urlStr, Map<String, Object> data, Class<T> clazz, Type type) throws Exception {
        GirMultipartBody girMultipartBody = GirMultipartBody.create(data, StandardCharsets.UTF_8);
        String post = post(urlStr, girMultipartBody);
        return parseResult(post, clazz, type);
    }

    public static <T> T postJson(String urlStr, Object data, Class<T> clazz, Type type)
            throws Exception {

        String dataString = null;
        if (data == null) {
            dataString = "";
        } else if (data instanceof String) {
            dataString = (String) data;
        } else {
            dataString = Gir.toJson(data).toJSONString();
        }
        String resString =
                post(urlStr, new StringRequestBody(dataString, "application/json", CharsetName));
        return parseResult(resString, clazz, type);
    }

    public static <T> T postXML(String urlStr, String data, Class<T> clazz, Type type)
            throws Exception {

        String dataString = null;
        if (data == null) {
            dataString = "";
        } else {
            dataString = data;
        }
        String resString =
                post(urlStr, new StringRequestBody(dataString, "application/xml", CharsetName));
        return parseResult(resString, clazz, type);
    }

    public static <T> T parseResult(String resString, Class<T> clazz, Type type) {
        try {
            return GirSdkResponseParser.parseResult(resString, clazz, type);
        } catch (GirSdkException e) {
            log.error(e);
            throw e;
        }
    }

    public static void parseError(String resString) {
        throw GirSdkResponseParser.buildSdkException("SDK调用出错", resString, null);
    }

    public static String post(String urlStr, String data, String contentType) throws Exception {
        return post(urlStr, new StringRequestBody(data, contentType, CharsetName));
    }

    public static String post(String urlStr, GiRequestBody giRequestBody) throws Exception {
        return GirSdkTransport.post(urlStr, giRequestBody, CharsetName);
    }

    public static <T> T getUrlEncode(
            String urlStr, Map<String, Object> data, Class<T> clazz, Type type) throws Exception {
        String resString = get(urlStr, data);
        return parseResult(resString, clazz, type);
    }

    public static String get(String urlStr, Map<String, Object> data) throws Exception {
        return GirSdkTransport.get(urlStr, data, CharsetName);
    }

    static void logTransportUrl(Long timeStamp, String method, String url) {
        log.debug("[" + timeStamp + "]SDK " + method + " URL:" + url);
    }

    static void logTransportResult(Long timeStamp, String method, String resString) {
        log.debug("[" + timeStamp + "]SDK " + method + " RESULT:" + resString);
    }

    static void logParserError(Exception ex) {
        log.error(ex);
    }

    static void logIgnoreSslWarning(String urlHostName, String peerHost) {
        log.warn("Warning: URL Host: " + urlHostName + " vs. " + peerHost);
    }

    public static void validate(HttpServletRequest request) throws Exception {
        GirSdkRequestValidator.validate(request);
    }

    public static void validate(HttpServletRequest request, GirSdkSecretProvider secretProvider)
            throws Exception {
        GirSdkRequestValidator.validate(request, secretProvider);
    }

    public static void validateClient(
            HttpServletRequest request, String clientId, String clientSecret) throws Exception {
        GirSdkRequestValidator.validateClient(request, clientId, clientSecret);
    }

    public static String getRandomFileName(Integer number) {
        return GirSdkSupport.getRandomFileName(number);
    }

    public static boolean hasText(String str) {
        return GirSdkSupport.hasText(str);
    }

    public static void ignoreSsl() {
        GirSdkSupport.ignoreSsl();
    }
}
