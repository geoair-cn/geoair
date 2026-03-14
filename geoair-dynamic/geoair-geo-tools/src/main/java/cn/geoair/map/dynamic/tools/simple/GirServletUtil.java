package cn.geoair.map.dynamic.tools.simple;

import cn.geoair.base.Gir;
import cn.geoair.base.util.GutilObject;
import cn.geoair.web.util.GirHttpServletHelper;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author ：张逢吉
 * @date ：Created in 2024/3/29 12:30 @description： TODO
 */
public class GirServletUtil extends ServletUtil {

	/** 设置严格的无缓存响应头（适用于动态内容、API 等） */
	public static void setNoCacheHeaders() {
		HttpServletResponse response = GirHttpServletHelper.getResponse();
		// HTTP 1.1
		response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, proxy-revalidate");
		// HTTP 1.0
		response.setHeader("Pragma", "no-cache");
		// 明确过期时间
		response.setDateHeader("Expires", 0);
	}

	/**
	 * 获取自己这台服务器的地址，通过httpRequest
	 * @return
	 */
	public static String getServerPathByRequest() {
		HttpServletRequest request = GirHttpServletHelper.getRequest();
		Map<String, String> headerMap = GirServletUtil.getHeaderMap(request);
		String host = headerMap.get("host");
		String property = Gir.property.getProperty("server.servlet.context-path");
		if (GutilObject.isNotEmpty(property)) {
			property = StrUtil.replaceFirst(property, "/", "");
		}
		else {
			property = "";
		}
		return "http://" + host + "/" + property;
	}

	/**
	 * 获取自己这台服务器的地址，通过httpRequest
	 * @return
	 */
	public static Map<String, String> getServerInfoByRequest() {
		HttpServletRequest request = GirHttpServletHelper.getRequest();
		Map<String, String> headerMap = GirServletUtil.getHeaderMap(request);
		String requestHost = headerMap.get("host");
		Map<String, String> infoMap = new HashMap<>();
		infoMap.put("requestHost", requestHost);
		infoMap.put("serverPathBySpring", getServerPathBySpring());
		infoMap.put("serverPathByRequest", getServerPathByRequest());
		infoMap.put("contextPath", request.getContextPath());
		infoMap.put("requestURI", request.getRequestURI());
		infoMap.put("servletPath", request.getServletPath());
		return infoMap;
	}

	/**
	 * 获取自己这台服务器的地址，通过spring配置文件
	 * @return
	 */
	public static String getServerPathBySpring() {
		String localhostStr = NetUtil.getLocalhostStr();
		String property = Gir.property.getProperty("server.servlet.context-path");
		String port = Gir.property.getProperty("server.port");
		if (GutilObject.isNotEmpty(property)) {
			property = StrUtil.replaceFirst(property, "/", "");
		}
		else {
			property = "";
		}
		return "http://" + localhostStr + ":" + port + "/" + property;
	}

	public static void toResponse(HttpServletResponse response, byte[] re, String contentType) {
		ServletOutputStream outputStream = null;
		ByteArrayInputStream byteArrayInputStream = null;
		response.setContentType(contentType);
		try {
			byteArrayInputStream = new ByteArrayInputStream(re);
			outputStream = response.getOutputStream();
			IoUtil.copy(byteArrayInputStream, outputStream);
		}
		catch (Exception e) {
		}
		finally {
			IoUtil.close(byteArrayInputStream);
			IoUtil.close(outputStream);
		}
	}

}
