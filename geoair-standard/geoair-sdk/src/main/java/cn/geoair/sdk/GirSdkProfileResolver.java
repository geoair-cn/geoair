package cn.geoair.sdk;

import cn.geoair.sdk.GirSdkProfileConfig.ProfileEnum;

final class GirSdkProfileResolver {

    private GirSdkProfileResolver() {}

    static ProfileEnum getCurrentProfile() {
        return GirSdkProfileConfig.ProfileLocal.get();
    }

    static String getClientId(ProfileEnum profile) {
        return GirSdkProfileConfig.getConfig(profile, GirSdkProfileConfig.BusType_clientId);
    }

    static String getClientSecret(ProfileEnum profile) {
        return GirSdkProfileConfig.getConfig(profile, GirSdkProfileConfig.BusType_clientSecret);
    }

    static String getAlertMessageKey(ProfileEnum profile) {
        return GirSdkProfileConfig.getConfig(
                profile, GirSdkProfileConfig.BusType_reslut_msgkey, "alertMsg");
    }

    static ProfileEnum resolveRequestProfile(String reqProfile) {
        ProfileEnum pe = getCurrentProfile();
        if (GirSdkUtil.hasText(reqProfile)) {
            pe = ProfileEnum.valueOf(reqProfile.toUpperCase());
            if (pe == null) {
                throw new GirSdkException("Sdk请求发送了错误的profile参数:" + reqProfile);
            }
        }
        return pe;
    }
}
