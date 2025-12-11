package cn.geoair.gtc.sdk;

public interface  gtcSdkSecretProvider {


	String getSecret(String clientId) throws Exception;

}
