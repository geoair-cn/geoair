package cn.geoair.map.dynamic.mvt.servlet;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.dynamic.mvt.GirRealMvtHelper;
import cn.geoair.map.dynamic.mvt.dto.ParamCheckResult;
import cn.geoair.map.dynamic.mvt.dto.TileRequestParams;
import cn.geoair.map.dynamic.mvt.exec.ITileExecutor;
import cn.geoair.map.dynamic.mvt.exec.TileExecutorFactory;
import cn.geoair.map.dynamic.mvt.exec.dto.TileRequest;
import cn.geoair.map.dynamic.tools.simple.GirServletUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ObjectUtil;
 

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.Objects;

 
public class TileCommonServlet extends HttpServlet {
    public static GiLogger log = GirLoggerFactory.getLogger();
    /**
     * 输出响应内容
     */
    public static void toResponse(HttpServletResponse response, byte[] re, String contentType) {
        ServletOutputStream outputStream = null;
        ByteArrayInputStream in = null;
        response.setContentType(contentType);

        try {
            in = new ByteArrayInputStream(re);
            outputStream = response.getOutputStream();
            IoUtil.copy(in, outputStream);
        } catch (Exception e) {
            log.error("响应输出失败", e);
        } finally {
            IoUtil.close(in);
            IoUtil.close(outputStream);
        }
    }

    /**
     * 核心 MVT 瓦片生成逻辑
     */
    public void doMvt(
            String layerName,
            TileRequestParams params,
            int zoom,
            int col,
            int row,
            HttpServletResponse response,
            HttpServletRequest request
    ) throws Exception {
        byte[] data = new byte[0];
        ParamCheckResult result = GirRealMvtHelper.getInstance().checkTileRequestParams(params, layerName);

        if (!result.isSuccess()) {
            String msg = result.getMessage();
            GirServletUtil.toResponse(response, msg.getBytes(Charset.defaultCharset()), "text/plain; charset=utf-8");
            return;
        }

        ITileExecutor executor = TileExecutorFactory.getInstance(params, layerName);
        TileRequest tileData = executor.getTileData(zoom, col, row);
        data = tileData.getPbfInfo().getData();

        // 必须保留，业务依赖
        request.getSession();

        boolean success = tileData.isSuccessIs();
        if (success) {
            String httpUrl = tileData.getHttpUrl();
            if (httpUrl != null) {
                response.sendRedirect(httpUrl);
                return;
            }

            if (Objects.isNull(data) || data.length == 0) {
                log.info("未读取瓦片：layer={}, z={}, x={}, y={}", layerName, zoom, col, row);
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                return;
            }

            response.setContentType("application/octet-stream");
        } else {
            response.setContentType("text/plain; charset=utf-8");
            response.setStatus(500);
        }

        // 跨域头
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE, PUT");
        response.setHeader("Content-Disposition", "inline");

        // 输出 PBF
        try (ServletOutputStream out = response.getOutputStream()) {
            out.write(data);
            out.flush();
        } catch (Exception e) {
            log.error("瓦片输出异常", e);
            if (!response.isCommitted()) {
                response.reset();
                response.sendError(500, "查询失败：" + e.getMessage());
            }
        }
    }
}
