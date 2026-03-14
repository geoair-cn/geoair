package cn.geoair.sdk;

public interface GirSdkSecretProvider {

	String getSecret(String clientId) throws Exception;

}
