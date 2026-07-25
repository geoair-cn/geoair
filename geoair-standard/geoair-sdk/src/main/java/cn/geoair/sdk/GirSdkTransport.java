package cn.geoair.sdk;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;

import cn.geoair.sdk.GirSdkProfileConfig.ProfileEnum;
import cn.geoair.sdk.body.GiRequestBody;

final class GirSdkTransport {

    private GirSdkTransport() {}

    static String post(String urlStr, GiRequestBody giRequestBody, Charset charsetName)
            throws Exception {

        Long timeStamp = Long.valueOf(System.currentTimeMillis());
        String newUrl = appendUrl(urlStr, "_", timeStamp.toString(), false, charsetName);
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
        try {
            giRequestBody.write(out);
            out.flush();
        } finally {
            out.close();
        }

        if (conn.getResponseCode() != 200) {
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), charsetName));
            try {
                GirSdkUtil.parseError(readBody(br));
            } finally {
                br.close();
                conn.disconnect();
            }
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), charsetName));
        try {
            String resString = readBody(br);
            GirSdkUtil.logTransportResult(timeStamp, "POST", resString);
            return resString;
        } finally {
            br.close();
            conn.disconnect();
        }
    }

    static String get(String urlStr, Map<String, Object> data, Charset charsetName) throws Exception {

        String newUrl = urlStr;

        Long timeStamp = Long.valueOf(System.currentTimeMillis());
        if (data == null) {
            newUrl = appendUrl(newUrl, "_", timeStamp.toString(), false, charsetName);
        }
        else {
            Map<String, Object> queryData = new LinkedHashMap<>(data);
            queryData.put("_", timeStamp);
            newUrl = appendUrl(urlStr, queryData, true, charsetName);
        }

        URL url = new URL(newUrl);
        // if ("https".equalsIgnoreCase(url.getProtocol())) {
        // ignoreSsl();
        // }
        GirSdkUtil.logTransportUrl(timeStamp, "GET", newUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn = setHeaders(conn);
        conn.connect();

        if (conn.getResponseCode() != 200) {
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), charsetName));
            try {
                GirSdkUtil.parseError(readBody(br));
            } finally {
                br.close();
                conn.disconnect();
            }
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), charsetName));
        try {
            String resString = readBody(br);
            GirSdkUtil.logTransportResult(timeStamp, "GET", resString);
            return resString;
        } finally {
            br.close();
            conn.disconnect();
        }
    }

    static String appendUrl(String url, Map<String, Object> data, boolean encodeValue, Charset charsetName)
            throws Exception {
        if (url.contains("?")) {
            return url + "&" + mapToUrl(data, encodeValue, charsetName);
        }
        else {
            return url + "?" + mapToUrl(data, encodeValue, charsetName);
        }
    }

    static String appendUrl(String url, String key, String value, boolean encodeValue, Charset charsetName)
            throws Exception {
        if (value != null) {
            if (url.indexOf("?") >= 0) {
                return url + "&" + key + "="
                        + (encodeValue ? URLEncoder.encode(value, charsetName.name()) : value);
            }
            else {
                return url + "?" + key + "="
                        + (encodeValue ? URLEncoder.encode(value, charsetName.name()) : value);
            }
        }
        return url;
    }

    static String mapToUrl(Map<String, Object> data, boolean encodeValue, Charset charsetName)
            throws Exception {
        if (data.isEmpty()) {
            return null;
        }
        StringBuilder param = new StringBuilder();
        Object value = null;
        for (String key : data.keySet()) {
            value = data.get(key);
            if (value != null) {
                param.append(key).append("=");
                param.append(encodeValue ? URLEncoder.encode(data.get(key).toString(), charsetName.name())
                        : value.toString());
                param.append("&");
            }
        }
        return param.substring(0, param.length() - 1);
    }

    private static HttpURLConnection setHeaders(HttpURLConnection conn) throws Exception {
        String nonce = GirSdkSupport.getRandomFileName(Integer.valueOf(32));
        Long curTime = Long.valueOf(System.currentTimeMillis() / 1000L);
        ProfileEnum profile = GirSdkProfileResolver.getCurrentProfile();

        String clientId = GirSdkProfileResolver.getClientId(profile);
        String clientSecret = GirSdkProfileResolver.getClientSecret(profile);

        String checkSum = CheckSumBuilder.getCheckSum(clientSecret, nonce, curTime.toString());
        conn.setRequestProperty("clientId", clientId);
        conn.setRequestProperty("Nonce", nonce);
        conn.setRequestProperty("CurTime", curTime.toString());
        conn.setRequestProperty("CheckSum", checkSum);
        conn.setRequestProperty("Profile", profile.name());
        return conn;
    }

    private static String readBody(BufferedReader br) throws Exception {
        StringBuilder res = new StringBuilder();
        String lines;
        while ((lines = br.readLine()) != null) {
            res.append(lines);
        }
        return res.toString();
    }
}
