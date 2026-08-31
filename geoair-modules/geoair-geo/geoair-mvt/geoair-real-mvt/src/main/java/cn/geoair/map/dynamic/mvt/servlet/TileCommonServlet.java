package cn.geoair.map.dynamic.mvt.servlet;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.base.tool.GkSystemClock;
import cn.geoair.map.dynamic.mvt.GirRealMvtHelper;
import cn.geoair.map.dynamic.mvt.dto.ParamCheckResult;
import cn.geoair.map.dynamic.mvt.dto.TileRequestParams;
import cn.geoair.map.dynamic.mvt.exec.ITileExecutor;
import cn.geoair.map.dynamic.mvt.exec.TileExecutorFactory;
import cn.geoair.map.dynamic.mvt.exec.dto.TileRequest;
import cn.geoair.map.dynamic.tools.grid.dto.TileZxyApo;
import cn.geoair.map.dynamic.tools.simple.GirTileResponseUtil;
import cn.geoair.map.dynamic.tools.simple.collection.map.GirFastStrObjMap;
import cn.geoair.map.dynamic.tools.simple.response.TileResponse;
import cn.geoair.map.dynamic.tools.simple.response.TileResponseByByte;
import cn.geoair.map.dynamic.tools.simple.response.TileResponseProvider;
import cn.geoair.web.mime.GirApplicationMime;
import cn.hutool.core.io.IoUtil;
import java.io.ByteArrayInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TileCommonServlet extends HttpServlet implements TileResponseProvider {
    public static GiLogger log = GirLoggerFactory.getLogger();

    /** 输出响应内容 */
    public static void toResponse(HttpServletResponse response, byte[] re, String contentType) {
        ServletOutputStream outputStream = null;
        ByteArrayInputStream in = null;
        response.setContentType(contentType);
        response.setContentLength(re.length);
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

    @Override
    public TileResponse getTileResponse(String requestUri) {
        return getTileResponse(requestUri, null);
    }

    @Override
    public TileResponse getTileResponse(String requestUri, String requestHost) {
        return TileResponse.error("Unsupported vector tile URI: " + requestUri);
    }

    /** 核心 MVT 瓦片生成逻辑；此方法不依赖 HttpServletRequest。 */
    protected TileResponse buildMvtTileResponse(
            String layerName, TileRequestParams params, int zoom, int col, int row)
            throws Exception {
        ParamCheckResult result =
                GirRealMvtHelper.getInstance().checkTileRequestParams(params, layerName);
        if (!result.isSuccess()) {
            return TileResponse.error(result.getMessage())
                    .setHttpCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        ITileExecutor executor = TileExecutorFactory.getInstance(params, layerName);
        TileRequest tileData = executor.getTileData(zoom, col, row);
        if (tileData == null
                || tileData.getPbfInfo() == null
                || tileData.getPbfInfo().getData() == null) {
            return TileResponse.error("MVT tile data is empty")
                    .setHttpCode(HttpServletResponse.SC_NO_CONTENT);
        }

        String httpUrl = tileData.getHttpUrl();
        if (httpUrl != null) {
            return new TileResponse()
                    .setHttpCode(HttpServletResponse.SC_FOUND)
                    .setExtrasHeaders(GirFastStrObjMap.<String>of().addOne("Location", httpUrl))
                    .setDataSource("real-mvt-redirect");
        }
        TileResponse tileResponse =
                TileResponseByByte.of()
                        .setBytesAndUpdateSize(tileData.getPbfInfo().getData())
                        .setMimeType(GirApplicationMime.mapboxVector)
                        .setSuccess(tileData.isSuccessIs())
                        .setLastModified(GkSystemClock.now())
                        .setDataSource("real-mvt")
                        .setCoordinate(TileZxyApo.of().setZ(zoom).setX(col).setY(row))
                        .setGridEpsgStr(params.isGeoIs() ? "EPSG:4490" : "EPSG:3857");

        if (!tileResponse.isValid()) {
            tileResponse.setHttpCode(HttpServletResponse.SC_NO_CONTENT);
        }

        return tileResponse;
    }

    /**
     * @deprecated 请使用 {@link #buildMvtTileResponse(String, TileRequestParams, int, int, int)} 与
     *     {@link #writeTileResponse(TileResponse, HttpServletResponse)}。
     */
    @Deprecated
    public void doMvt(
            String layerName,
            TileRequestParams params,
            int zoom,
            int col,
            int row,
            HttpServletResponse response,
            HttpServletRequest request)
            throws Exception {
        if (request != null) {
            request.getSession();
        }
        writeTileResponse(buildMvtTileResponse(layerName, params, zoom, col, row), response);
    }

    protected void writeTileResponse(TileResponse tileResponse, HttpServletResponse response)
            throws java.io.IOException {
        if (tileResponse != null
                && Integer.valueOf(HttpServletResponse.SC_FOUND).equals(tileResponse.getHttpCode())
                && tileResponse.getExtrasHeaders() != null
                && tileResponse.getExtrasHeaders().get("Location") != null) {
            response.sendRedirect(tileResponse.getExtrasHeaders().get("Location"));
            return;
        }
        GirTileResponseUtil.buildFromTileResponse(tileResponse, response);
    }
}
