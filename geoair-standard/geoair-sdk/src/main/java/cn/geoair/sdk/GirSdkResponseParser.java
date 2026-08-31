package cn.geoair.sdk;

import cn.geoair.base.Gir;
import cn.geoair.sdk.GirSdkProfileConfig.ProfileEnum;

import java.lang.reflect.Type;

final class GirSdkResponseParser {

    private GirSdkResponseParser() {}

    static <T> T parseResult(String resString, Class<T> clazz, Type type) {
        try {
            if (clazz != null) {
                return Gir.toJson(resString).toBean(clazz);
            }
            if (type != null) {
                return Gir.toJson(resString).toBean(type);
            }
        } catch (Exception e) {
            throw buildSdkException("SDK结果异常", resString, e);
        }
        return null;
    }

    static void parseError(String resString) {
        throw buildSdkException("SDK调用出错", resString, null);
    }

    static GirSdkException buildSdkException(
            String defaultMessage, String resString, Throwable cause) {
        String alertMsg = resolveAlertMessage(defaultMessage, resString);
        if (cause == null) {
            return new GirSdkException(alertMsg);
        }
        return new GirSdkException(alertMsg, cause);
    }

    private static String resolveAlertMessage(String defaultMessage, String resString) {
        String alertMsg = defaultMessage;
        try {
            ProfileEnum profile = GirSdkProfileResolver.getCurrentProfile();
            String alertMsgKey = GirSdkProfileResolver.getAlertMessageKey(profile);
            String aMsg = Gir.toJson(resString).getByPath(alertMsgKey, String.class);
            if (GirSdkUtil.hasText(aMsg)) {
                alertMsg = aMsg;
            }
        } catch (Exception ex) {
            GirSdkUtil.logParserError(ex);
        }
        return alertMsg;
    }
}
