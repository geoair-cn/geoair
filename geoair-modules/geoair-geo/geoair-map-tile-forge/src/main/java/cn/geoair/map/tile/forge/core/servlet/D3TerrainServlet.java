package cn.geoair.map.tile.forge.core.servlet;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.dynamic.tools.simple.GirTileResponseUtil;
import cn.geoair.map.dynamic.tools.simple.response.TileResponse;
import cn.geoair.map.tile.forge.core.TileRequest;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.service.GirMapTileService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class D3TerrainServlet extends D3TilesServlet {

    public static GiLogger log = GirLoggerFactory.getLogger();

    public D3TerrainServlet(GirMapTileService mapTileService) {
        super(mapTileService);
    }

    Pattern pattern =
            Pattern.compile(
                    "/3dTerrainService/([^/]+)/([^/]+)/([^/]+)/([^/]+(?:/[^/]+/[^/]+)?\\.\\w+)");

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String requestURI =
                request
                        .getRequestURI(); // 示例：/geospatial-api/3dTilesService/12345/myPrefix/tileset.json
        Matcher matcher = pattern.matcher(requestURI);
        String fileId = null;
        String fileName = null;
        String serviceName = null;
        String z = null;
        String x = null;
        String y = null;
        String format = null;
        if (matcher.find()) {
            fileId = matcher.group(1); // 提取FileId
            fileName = matcher.group(2); // 提取文件名称
            serviceName = matcher.group(3); // 提取服务名称
            String pathPart = matcher.group(4); // 提取第3段及以后的部分
            if (pathPart.contains("/")) {
                String[] zxyParts = pathPart.split("/");
                if (zxyParts.length >= 3) {
                    z = zxyParts[0]; // 提取z（1）
                    x = zxyParts[1]; // 提取x（2）
                    y = zxyParts[2].split("\\.")[0]; // 提取y（3）
                    format = zxyParts[2].split("\\.")[1]; // 提取y（3）
                }
            } else {
                z = pathPart; // 提取文件名（layer.json）
            }
        }
        GirLayerConfigContext layerConfigContext = null;
        try {
            layerConfigContext = getGirLayerConfigContext(fileId, fileName, serviceName);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            GirTileResponseUtil.buildFromException(e, response);
            return;
        }

        layerConfigContext.setFormat(format);
        try {
            TileRequest layerTile = mapTileService.getLayerTile(layerConfigContext, z, y, x);
            TileResponse tileResponse = layerTile.toTileResponse();
            GirTileResponseUtil.buildFromTileResponse(tileResponse, response);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            GirTileResponseUtil.buildFromException(e, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        doGet(req, resp);
    }
}
