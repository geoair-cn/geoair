package cn.geoair.sdk;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.http.HttpServletRequest;

import cn.geoair.base.Gir;
// import com.alibaba.fastjson.JSON;
// import com.alibaba.fastjson.JSONObject;
import cn.geoair.base.cache.support.GirMemoryCache;
import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLogger;
import cn.geoair.sdk.GirSdkProfileConfig.ProfileEnum;
import cn.geoair.sdk.body.GiRequestBody;
import cn.geoair.sdk.body.GirMultipartBody;

public class GirSdkUtil {

	private static final GiLogger log = GirLogger.getLoger(GirSdkUtil.class);

	public static String CharsetName = "UTF-8";

	private static HttpURLConnection setHeaders(HttpURLConnection conn) throws Exception {
		String nonce = getRandomFileName(Integer.valueOf(32));
		Long curTime = Long.valueOf(System.currentTimeMillis() / 1000L);

		String clientId = GirSdkProfileConfig.getConfig(GirSdkProfileConfig.ProfileLocal.get(),
				GirSdkProfileConfig.BusType_clientId);
		String clientSecret = GirSdkProfileConfig.getConfig(GirSdkProfileConfig.ProfileLocal.get(),
				GirSdkProfileConfig.BusType_clientSecret);

		String checkSum = CheckSumBuilder.getCheckSum(clientSecret, nonce, curTime.toString());
		conn.setRequestProperty("clientId", clientId);
		conn.setRequestProperty("Nonce", nonce);
		conn.setRequestProperty("CurTime", curTime.toString());
		conn.setRequestProperty("CheckSum", checkSum);
		conn.setRequestProperty("Profile", GirSdkProfileConfig.ProfileLocal.get().name());
		return conn;
	}

	public static <T> T postUrlEncode(String urlStr, Map<String, Object> data, Class<T> clazz, Type type)
			throws Exception {

		String dataString = null;
		if (data == null) {
			dataString = "";
		}
		else {
			dataString = mapToUrl(data, true);
		}
		String resString = post(urlStr, dataString, "application/x-www-form-urlencoded");
		return parseResult(resString, clazz, type);
	}

	public static <T> T postMultipartFile(String urlStr, Map<String, Object> data, Class<T> clazz, Type type)
			throws Exception {
		GirMultipartBody girMultipartBody = GirMultipartBody.create(data, StandardCharsets.UTF_8);
		String post = post(urlStr, girMultipartBody);
		return parseResult(post, clazz, type);
	}

	public static <T> T postJson(String urlStr, Object data, Class<T> clazz, Type type) throws Exception {

		String dataString = null;
		if (data == null) {
			dataString = "";
		}
		else if (data instanceof String) {
			dataString = (String) data;
		}
		else {
			dataString = Gir.toJson(data).toJSONString(); // JSON.parseObject(data);
		}
		String resString = post(urlStr, dataString, "application/json");
		return parseResult(resString, clazz, type);
	}

	public static <T> T postXML(String urlStr, String data, Class<T> clazz, Type type) throws Exception {

		String dataString = null;
		if (data == null) {
			dataString = "";
		}
		String resString = post(urlStr, dataString, "application/xml");
		return parseResult(resString, clazz, type);
	}

	public static <T> T parseResult(String resString, Class<T> clazz, Type type) {
		try {
			if (clazz != null) {
				return Gir.toJson(resString).toBean(clazz); // JSONObject.parseObject(resString,clazz);
			}
			if (type != null) {
				return Gir.toJson(resString).toBean(type); // JSONObject.parseObject(resString,type);
			}
		}
		catch (Exception e) {
			log.error(e);

			String alertMsg = "SDK结果异常";
			try {

				String alertMsgKey = GirSdkProfileConfig.getConfig(GirSdkProfileConfig.ProfileLocal.get(),
						GirSdkProfileConfig.BusType_reslut_msgkey, "alertMsg");
				String aMsg = Gir.toJson(resString).getByPath(alertMsgKey, String.class);
				if (hasText(aMsg)) {
					alertMsg = aMsg;
				}
				/*
				 * JSONObject serverResponse = JSONObject.parseObject(resString);
				 * if(serverResponse != null) { String aMsg = serverResponse.getObject(
				 * gtcSdkProfileConfig.getConfig( gtcSdkProfileConfig.ProfileLocal.get(),
				 * gtcSdkProfileConfig.BusType_reslut_msgkey,"alertMsg"),String.class);
				 * if(hasText(aMsg)) { alertMsg = aMsg; } }
				 */

			}
			catch (Exception ex) {
				log.error(ex);
			}
			throw new GirSdkException(alertMsg, e);
		}
		return null;
	}

	public static void parseError(String resString) {

		String alertMsg = "SDK调用出错";
		try {

			String alertMsgKey = GirSdkProfileConfig.getConfig(GirSdkProfileConfig.ProfileLocal.get(),
					GirSdkProfileConfig.BusType_reslut_msgkey, "alertMsg");
			String aMsg = Gir.toJson(resString).getByPath(alertMsgKey, String.class);
			if (hasText(aMsg)) {
				alertMsg = aMsg;
			}
			/*
			 * JSONObject serverResponse = JSONObject.parseObject(resString);
			 * if(serverResponse != null) { String aMsg = serverResponse.getObject(
			 * gtcSdkProfileConfig.getConfig( gtcSdkProfileConfig.ProfileLocal.get(),
			 * gtcSdkProfileConfig.BusType_reslut_msgkey,"alertMsg"),String.class);
			 * if(hasText(aMsg)) { alertMsg = aMsg; } }
			 */
		}
		catch (Exception e) {
			log.error(e);
		}
		throw new GirSdkException(alertMsg);
	}

	public static String post(String urlStr, String data, String contentType) throws Exception {

		Long timeStamp = Long.valueOf(System.currentTimeMillis());
		String newUrl = appendUrl(urlStr, "_", timeStamp.toString(), false);
		URL url = new URL(newUrl);
		// if ("https".equalsIgnoreCase(url.getProtocol())) {
		// ignoreSsl();
		// }
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setDoOutput(true);
		conn.setDoInput(true);
		conn.setRequestMethod("POST");
		conn.setUseCaches(false);
		conn.setConnectTimeout(30000);
		conn.setReadTimeout(30000);
		conn.setInstanceFollowRedirects(true);
		conn.setRequestProperty("Content-Type", contentType);
		conn = setHeaders(conn);
		DataOutputStream out = new DataOutputStream(conn.getOutputStream());
		if (data == null) {
			data = "";
		}
		log.debug("[" + timeStamp + "]SDK URL:" + newUrl + ",DATA:" + data);
		out.write(data.getBytes(CharsetName));
		out.flush();
		out.close();

		if (conn.getResponseCode() != 200) {
			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), CharsetName));

			StringBuffer res = new StringBuffer();
			String lines;
			while ((lines = br.readLine()) != null) {
				res.append(lines);
			}
			br.close();
			conn.disconnect();

			parseError(res.toString());
		}

		BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), CharsetName));

		StringBuffer res = new StringBuffer();
		String lines;
		while ((lines = br.readLine()) != null) {
			res.append(lines);
		}
		br.close();
		conn.disconnect();

		String resString = res.toString();
		log.debug("[" + timeStamp + "]SDK POST RESULT:" + resString);

		return resString;
	}

	public static String post(String urlStr, GiRequestBody giRequestBody) throws Exception {

		Long timeStamp = Long.valueOf(System.currentTimeMillis());
		String newUrl = appendUrl(urlStr, "_", timeStamp.toString(), false);
		URL url = new URL(newUrl);
		// if ("https".equalsIgnoreCase(url.getProtocol())) {
		// ignoreSsl();
		// }
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setDoOutput(true);
		conn.setDoInput(true);
		conn.setRequestMethod("POST");
		conn.setUseCaches(false);
		conn.setConnectTimeout(30000);
		conn.setReadTimeout(30000);
		conn.setInstanceFollowRedirects(true);
		conn.setRequestProperty("Content-Type", giRequestBody.getContentType());
		conn = setHeaders(conn);
		DataOutputStream out = new DataOutputStream(conn.getOutputStream());
		giRequestBody.write(out);
		out.flush();
		out.close();

		if (conn.getResponseCode() != 200) {
			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), CharsetName));

			StringBuffer res = new StringBuffer();
			String lines;
			while ((lines = br.readLine()) != null) {
				res.append(lines);
			}
			br.close();
			conn.disconnect();

			parseError(res.toString());
		}

		BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), CharsetName));

		StringBuffer res = new StringBuffer();
		String lines;
		while ((lines = br.readLine()) != null) {
			res.append(lines);
		}
		br.close();
		conn.disconnect();

		String resString = res.toString();
		log.debug("[" + timeStamp + "]SDK POST RESULT:" + resString);

		return resString;
	}

	public static <T> T getUrlEncode(String urlStr, Map<String, Object> data, Class<T> clazz, Type type)
			throws Exception {
		String resString = get(urlStr, data);
		return parseResult(resString, clazz, type);
	}

	public static String get(String urlStr, Map<String, Object> data) throws Exception {

		String newUrl = urlStr;

		Long timeStamp = Long.valueOf(System.currentTimeMillis());
		if (data == null) {
			newUrl = appendUrl(newUrl, "_", timeStamp.toString(), false);
		}
		else {
			data.put("_", timeStamp);
			newUrl = appendUrl(urlStr, data, true);
		}

		URL url = new URL(newUrl);
		// if ("https".equalsIgnoreCase(url.getProtocol())) {
		// ignoreSsl();
		// }
		log.debug("[" + timeStamp + "]SDK GET URL:" + newUrl);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn = setHeaders(conn);
		conn.connect();

		if (conn.getResponseCode() != 200) {
			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), CharsetName));

			StringBuffer res = new StringBuffer();
			String lines;
			while ((lines = br.readLine()) != null) {
				res.append(lines);
			}
			br.close();
			conn.disconnect();
			parseError(res.toString());
		}

		BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), CharsetName));

		StringBuffer res = new StringBuffer();
		String lines;
		while ((lines = br.readLine()) != null) {
			res.append(lines);
		}
		br.close();
		conn.disconnect();

		String resString = res.toString();
		log.debug("[" + timeStamp + "]SDK GET RESULT:" + resString);
		return resString;
	}

	public static String appendUrl(String url, Map<String, Object> data, boolean encodeValue) throws Exception {
		if (url.contains("?")) {
			return url + "&" + mapToUrl(data, true);
		}
		else {
			return url + "?" + mapToUrl(data, true);
		}
	}

	public static String appendUrl(String url, String key, String value, boolean encodeValue) throws Exception {
		if (value != null) {
			if (url.indexOf("?") >= 0) {
				return url + "&" + key + "=" + (encodeValue ? URLEncoder.encode(value, CharsetName) : value);
			}
			else {
				return url + "?" + key + "=" + (encodeValue ? URLEncoder.encode(value, CharsetName) : value);
			}
		}
		return url;
	}

	public static String mapToUrl(Map<String, Object> data, boolean encodeValue) throws Exception {
		if (data.isEmpty()) {
			return null;
		}
		StringBuffer param = new StringBuffer();
		Object value = null;
		for (String key : data.keySet()) {
			value = data.get(key);
			if (value != null) {
				param.append(key).append("=");
				param.append(encodeValue ? URLEncoder.encode(data.get(key).toString(), CharsetName) : value.toString());
				param.append("&");
			}
		}
		return param.substring(0, param.length() - 1);
	}

	public static void validate(HttpServletRequest request) throws Exception {
		ProfileEnum pe = GirSdkProfileConfig.ProfileLocal.get();
		String reqProfile = request.getHeader("Profile");
		if (hasText(reqProfile)) {
			pe = ProfileEnum.valueOf(reqProfile.toUpperCase());
			if (pe == null) {
				throw new GirSdkException("Sdk请求发送了错误的profile参数:" + reqProfile);
			}
		}

		validateClient(request, GirSdkProfileConfig.getConfig(pe, GirSdkProfileConfig.BusType_clientId),
				GirSdkProfileConfig.getConfig(pe, GirSdkProfileConfig.BusType_clientSecret));
	}

	private static GirMemoryCache memoryCache = new GirMemoryCache();

	public static void validate(HttpServletRequest request, GirSdkSecretProvider secretProvider) throws Exception {
		String clientId = request.getHeader("clientId");
		String clientSecret = memoryCache.getString("client_secret_" + clientId);
		if (!hasText(clientSecret)) {
			clientSecret = secretProvider.getSecret(clientId);
			if (clientSecret == null) {
				throw new GirSdkException("商户号密钥信息不正确");
			}
			memoryCache.put("client_secret_" + clientId, clientSecret, 10 * 60 * 1000);
		}
		validateClient(request, clientId, clientSecret);
	}

	public static void validateClient(HttpServletRequest request, String clientId, String clientSecret)
			throws Exception {
		String nonce = request.getHeader("Nonce");
		String curTime = request.getHeader("CurTime");
		String checkSum = request.getHeader("CheckSum");
		if (hasText(clientId) && hasText(nonce) && hasText(curTime) && hasText(checkSum)) {
			if (hasText(clientSecret)) {
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

	public static String getRandomFileName(Integer number) {
		String uuid = UUID.randomUUID().toString().replaceAll("-", "");
		Random random = new Random();
		Integer i = Integer.valueOf(0);
		if (number.intValue() < 32) {
			i = Integer.valueOf(random.nextInt(32 - number.intValue()));
		}
		return uuid.substring(i.intValue(), i.intValue() + number.intValue());
	}

	public static boolean hasText(String str) {
		return (str != null && !str.isEmpty() && containsText(str));
	}

	private static boolean containsText(CharSequence str) {
		int strLen = str.length();
		for (int i = 0; i < strLen; i++) {
			if (!Character.isWhitespace(str.charAt(i))) {
				return true;
			}
		}
		return false;
	}

	static class miTM implements TrustManager, X509TrustManager {

		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}

		public boolean isServerTrusted(X509Certificate[] certs) {
			return true;
		}

		public boolean isClientTrusted(X509Certificate[] certs) {
			return true;
		}

		public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
		}

		public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {
		}

	}

	private static void trustAllHttpsCertificates() {
		TrustManager[] trustAllCerts = new TrustManager[1];
		TrustManager tm = new miTM();
		trustAllCerts[0] = tm;
		try {
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, null);
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void ignoreSsl() {
		HostnameVerifier hv = new HostnameVerifier() {
			public boolean verify(String urlHostName, SSLSession session) {
				log.warn("Warning: URL Host: " + urlHostName + " vs. " + session.getPeerHost());
				return true;
			}
		};
		trustAllHttpsCertificates();
		HttpsURLConnection.setDefaultHostnameVerifier(hv);
	}

}
