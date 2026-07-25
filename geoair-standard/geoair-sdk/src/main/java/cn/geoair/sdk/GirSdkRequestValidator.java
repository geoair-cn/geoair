package cn.geoair.sdk;

import java.util.Objects;

import javax.servlet.http.HttpServletRequest;

import cn.geoair.base.cache.support.GirMemoryCache;
import cn.geoair.sdk.GirSdkProfileConfig.ProfileEnum;

final class GirSdkRequestValidator {

    private static final GirMemoryCache memoryCache = new GirMemoryCache();

    private GirSdkRequestValidator() {}

    static void validate(HttpServletRequest request) throws Exception {
        ProfileEnum pe = GirSdkProfileResolver.resolveRequestProfile(request.getHeader("Profile"));

        validateClient(request,
                GirSdkProfileResolver.getClientId(pe),
                GirSdkProfileResolver.getClientSecret(pe));
    }

    static void validate(HttpServletRequest request, GirSdkSecretProvider secretProvider)
            throws Exception {
        String clientId = request.getHeader("clientId");
        String clientSecret = memoryCache.getString("client_secret_" + clientId);
        if (!GirSdkUtil.hasText(clientSecret)) {
            clientSecret = secretProvider.getSecret(clientId);
            if (clientSecret == null) {
                throw new GirSdkException("商户号密钥信息不正确");
            }
            memoryCache.put("client_secret_" + clientId, clientSecret, 10 * 60 * 1000);
        }
        validateClient(request, clientId, clientSecret);
    }

    static void validateClient(HttpServletRequest request, String clientId, String clientSecret)
            throws Exception {
        String nonce = request.getHeader("Nonce");
        String curTime = request.getHeader("CurTime");
        String checkSum = request.getHeader("CheckSum");
        if (GirSdkUtil.hasText(clientId)
                && GirSdkUtil.hasText(nonce)
                && GirSdkUtil.hasText(curTime)
                && GirSdkUtil.hasText(checkSum)) {
            if (GirSdkUtil.hasText(clientSecret)) {
                String myCheckSum = CheckSumBuilder.getCheckSum(clientSecret, nonce, curTime.toString());
                if (Objects.equals(checkSum, myCheckSum)) {
                    return;
                }
                else {
                    throw new GirSdkException("非法访问,clientId:" + clientId + "验证密钥不正确");
                }
            }
        }
        throw new GirSdkException("非法访问");
    }
}
