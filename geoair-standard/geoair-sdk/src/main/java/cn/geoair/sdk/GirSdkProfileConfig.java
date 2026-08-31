package cn.geoair.sdk;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GirSdkProfileConfig {

    private static final Map<ProfileEnum, Map<String, String>> configMap =
            new ConcurrentHashMap<ProfileEnum, Map<String, String>>();

    public static String getConfig(ProfileEnum profile, String busType) {
        return getConfig(profile, busType, null);
    }

    public static String getConfig(ProfileEnum profile, String busType, String defaultValue) {
        Map<String, String> map = configMap.get(profile);
        if (map != null) {
            String res = map.get(busType);
            if (res == null) {
                return defaultValue;
            }
            return res;
        }
        return defaultValue;
    }

    public static void setConfig(ProfileEnum profile, String busType, String value) {
        Map<String, String> map =
                configMap.computeIfAbsent(profile, key -> new ConcurrentHashMap<String, String>());
        String oldValue = map.putIfAbsent(busType, value);
        if (oldValue != null) {
            throw new GirSdkException(
                    "重复的属性注册:profile="
                            + profile.name()
                            + ",busType="
                            + busType
                            + ",value="
                            + value);
        }
    }

    public static final String BusType_clientId = "_clientId";

    public static final String BusType_clientSecret = "_clientSecret";

    public static final String BusType_host = "_host";

    public static final String BusType_reslut_msgkey = "_result_msgkey";

    public static void initialize(String clientId, String clientSecret, ProfileEnum profile)
            throws GirSdkException {

        if (clientId == null) {
            throw new GirSdkException("clientId不能为空");
        }
        if (clientSecret == null) {
            throw new GirSdkException("clientSecret不能为空");
        }
        if (profile == null) {
            throw new GirSdkException("profile不能为空");
        }

        ProfileLocal.setDefault(profile);

        setConfig(profile, BusType_clientId, clientId);

        setConfig(profile, BusType_clientSecret, clientSecret);
    }

    public static enum ProfileEnum {
        DEV,
        TEST,
        PRO
    }

    public static class ProfileLocal {

        /** 获取当前切面 */
        private static final ThreadLocal<ProfileEnum> PROFILE_THREAD_LOCAL = new ThreadLocal<>();

        private static volatile ProfileEnum profile = ProfileEnum.DEV;

        public static void set(ProfileEnum profile) {

            PROFILE_THREAD_LOCAL.set(profile);
        }

        public static void setDefault(ProfileEnum profile) {

            PROFILE_THREAD_LOCAL.set(profile);
            ProfileLocal.profile = profile;
        }

        public static ProfileEnum get() {
            ProfileEnum pf = PROFILE_THREAD_LOCAL.get();
            if (pf == null) {
                pf = ProfileLocal.profile;
            }
            return pf;
        }

        public static void remove() {

            PROFILE_THREAD_LOCAL.remove();
        }

        private ProfileLocal() {}
    }
}
