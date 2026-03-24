package cn.geoair.map.dynamic.mvt.controller;

import java.io.ByteArrayInputStream;
import java.net.URLDecoder;
import java.util.Objects;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import cn.geoair.map.dynamic.mvt.dto.TileRequestParams;
import cn.geoair.map.dynamic.mvt.exec.ITileExecutor;
import cn.geoair.map.dynamic.mvt.exec.TileExecutorFactory;
import cn.geoair.map.dynamic.mvt.exec.dto.TileRequest;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/10/17 13:42 @description： TODO
 */
@Slf4j
public class TileCommon {

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

	public void doMvt(String layerName, TileRequestParams params, int zoom, int col, int row,
			HttpServletResponse response, HttpServletRequest request, int version) throws Exception {
		byte[] data = new byte[0];

		String filter = params.getFilter();
		if (ObjectUtil.isNotEmpty(filter)) {
			filter = URLDecoder.decode(filter.replaceAll("%(?![0-9a-fA-F]{2})", "%25"), "UTF-8");
			params.setFilter(filter);
		}
		String schemaName = params.getSchemaName();
		ITileExecutor executor = TileExecutorFactory.getInstance(version, params, layerName);
		TileRequest tileData = executor.getTileData(zoom, col, row);
		data = tileData.getPbfInfo().getData();
		HttpSession session = request.getSession(); // 别看这个代码啥也没干，但是如果删除了，就会导致 MqLogAspect
													// 这个类里面的日志采集报异常
		boolean successIs = tileData.isSuccessIs();
		if (successIs) {
			String httpUrl = tileData.getHttpUrl();
			if (httpUrl != null) {
				response.sendRedirect(httpUrl);
				return;
			}
			if (Objects.isNull(data) || data.length == 0) {
				log.info("未读取瓦片：layerName: {},schemaName: {}, z: {}, x: {}, y: {}", layerName, schemaName, zoom, col,
						row);
				response.setStatus(HttpServletResponse.SC_NO_CONTENT); // 204状态码
				return;
			}
			// 2. 设置响应头
			response.setContentType("application/octet-stream");
		}
		else {
			response.setContentType("text/plain; charset=utf-8");
			response.setStatus(500);
		}
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE, PUT");
		response.setHeader("Content-Disposition", "inline"); // 直接在浏览器打开
		// 3. 输出二进制流
		try (ServletOutputStream out = response.getOutputStream()) {
			out.write(data);
			out.flush();
		}
		catch (Exception e) {
			// 处理异常
			log.error(String.format("查询失败！%s", e.getMessage()));
			if (!response.isCommitted()) {
				response.reset();
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
						String.format("查询失败！%s", e.getMessage()));
			}
		}

	}

}
