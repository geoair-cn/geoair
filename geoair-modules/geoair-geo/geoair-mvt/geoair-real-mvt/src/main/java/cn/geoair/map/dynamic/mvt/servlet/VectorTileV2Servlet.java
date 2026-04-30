package cn.geoair.map.dynamic.mvt.servlet;

import cn.geoair.base.api.annotation.GaApi;
import cn.geoair.map.dynamic.mvt.GirRealMvtHelper;
import cn.geoair.map.dynamic.mvt.dto.ParamCheckResult;
import cn.geoair.map.dynamic.mvt.dto.TileRequestParams;
import cn.geoair.map.dynamic.tools.GirGeoTools;
import cn.geoair.map.dynamic.tools.grid.dto.BoxReferencedEnvelope;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Geometry;
import org.springframework.stereotype.Component;


import java.io.IOException;
import java.nio.charset.StandardCharsets;


@Slf4j
@GaApi(text = "矢量瓦片服务", tags = {"矢量瓦片服务"})

public class VectorTileV2Servlet extends TileCommonServlet {

    public VectorTileV2Servlet() {
        log.info("初始化矢量瓦片 Servlet 完成");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String uri = request.getRequestURI();

        try {
            if (uri.contains("/vectorTileService/v2/real/")) {
                handleRealTile(request, response);
            } else if (uri.contains("/vectorTileService/v2/debug/")) {
                handleDebugTile(request, response);
            } else {
                response.sendError(404);
            }
        } catch (Exception e) {
            log.error("瓦片请求异常", e);
            response.sendError(500);
        }
    }

    /**
     * 处理真实 PBF 瓦片请求
     */
    private void handleRealTile(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String pathInfo = request.getPathInfo();
        String[] parts = pathInfo.split("/");

        // parts格式: [ "", layerName, z, x, y.pbf ]
        String layerName = parts[1];
        int z = Integer.parseInt(parts[2]);
        int x = Integer.parseInt(parts[3]);
        String yStr = parts[4].replace(".pbf", "");
        int y = Integer.parseInt(yStr);

        // 获取参数
        String paramTile = request.getParameter("paramTile");
        String minZoomStr = request.getParameter("minZoom");
        String isGeoStr = request.getParameter("isGeo");

        // 参数校验
        if (ObjectUtil.isEmpty(paramTile)) {
            toResponse(response, "参数错误001".getBytes(StandardCharsets.UTF_8), "text/plain; charset=utf-8");
            return;
        }

        TileRequestParams params = TileRequestParams.fromBase32(paramTile);

        if (minZoomStr != null) {
            params.setMinZoom(Integer.parseInt(minZoomStr));
        }
        if (isGeoStr != null) {
            params.setGeo(Boolean.parseBoolean(isGeoStr));
        }

        // 执行业务
        doMvt(layerName, params, z, x, y, response, request);
    }

    /**
     * 调试接口：返回瓦片信息 JSON
     */
    private void handleDebugTile(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String pathInfo = request.getPathInfo();
        String[] parts = pathInfo.split("/");

        String layerName = parts[1];
        int z = Integer.parseInt(parts[2]);
        int x = Integer.parseInt(parts[3]);
        String yStr = parts[4].replace(".pbf", "");
        int y = Integer.parseInt(yStr);

        String paramTile = request.getParameter("paramTile");
        String minZoomStr = request.getParameter("minZoom");

        if (ObjectUtil.isEmpty(paramTile)) {
            toResponse(response, "参数错误001".getBytes(StandardCharsets.UTF_8), "text/plain; charset=utf-8");
            return;
        }

        TileRequestParams params = TileRequestParams.fromBase32(paramTile);
        if (minZoomStr != null) {
            params.setMinZoom(Integer.parseInt(minZoomStr));
        }

        JSONObject re = new JSONObject();
        re.put("params", params);
        re.put("z", z);
        re.put("x", x);
        re.put("y", y);

        try {
            BoxReferencedEnvelope boxReferencedEnvelope;
            int gridSrid = params.isGeo() ? 4326 : 3857;

            if (!params.isGeo()) {
                boxReferencedEnvelope = GirGeoTools.me().getTileGrid3857Opt().xyzToTileBox(z, x, y, 3857);
            } else {
                boxReferencedEnvelope = GirGeoTools.me().getTileGrid4326Opt().xyzToTileBox(z, x, y, 4326);
            }

            Geometry geometry = GirGeoTools.me().getSridOpt().convertToGeom(boxReferencedEnvelope);
            re.put("bbox", geometry.toText());

            Geometry convert = GirGeoTools.me().getSridOpt().convert(geometry, gridSrid, 4326);
            re.put("bbox4326", convert.toText());
        } catch (Exception ignored) {
        }

        String json = JSON.toJSONString(re, JSONWriter.Feature.PrettyFormat);
        toResponse(response, json.getBytes(StandardCharsets.UTF_8), "application/json; charset=utf-8");
    }
}
