package cn.geoair.map.tile.forge.core.servlet;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.dynamic.tools.simple.GirServletUtil;
import cn.geoair.map.dynamic.tools.simple.GirTileResponseUtil;
import cn.geoair.map.dynamic.tools.simple.response.TileResponse;
import cn.geoair.map.tile.forge.core.GirLayerConfigContextHelper;
import cn.geoair.map.tile.forge.core.enums.GirMapTileType;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.service.GirMapTileService;
import cn.geoair.map.tile.forge.core.TileRequest;
import cn.hutool.core.io.IoUtil;


import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Component
public class D3TilesServlet extends HttpServlet {
    public static GiLogger log = GirLoggerFactory.getLogger();
    @Resource
    GirMapTileService gMapTileService;

    Pattern pattern = Pattern.compile("/3dTilesService/([^/]+)/([^/]+)/([^/]+)(/.*)?");

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String requestURI = request.getRequestURI(); // 示例：/geospatial-api/3dTilesService/12345/myPrefix/tileset.json
        Matcher matcher = pattern.matcher(requestURI);
        String fileId = null;
        String fileName = null;
        String contentAfterPrefix = null;
        String serviceName = null;
        if (matcher.find()) {
            fileId = matcher.group(1);          // 提取FileId
            fileName = matcher.group(2);      // 提取文件名称
            serviceName = matcher.group(3);      // 提取服务名称
            contentAfterPrefix = matcher.group(4); // 提取prefixName后的内容

            if (contentAfterPrefix != null && !contentAfterPrefix.isEmpty()) {
                contentAfterPrefix = contentAfterPrefix.substring(1);
            }
        }
        GirLayerConfigContext layerConfigContext = null;
        try {
            layerConfigContext
                    = getGirLayerConfigContext(fileId, fileName, serviceName);
        } catch (Exception e) {
            GirTileResponseUtil.buildFromException(e, response);
            return;
        }
        try {
            TileRequest layerTile = gMapTileService.getLayerTile(layerConfigContext, contentAfterPrefix, "", "");
            TileResponse tileResponse = layerTile.toTileResponse();
            GirTileResponseUtil.buildFromTileResponse(tileResponse, response);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            GirTileResponseUtil.buildFromException(e, response);
        }
    }


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }

    public GirLayerConfigContext getGirLayerConfigContext(String fileId, String fileName, String layerName) {
        GirLayerConfigContext config = GirLayerConfigContextHelper.getInstance().getGirLayerConfigContext(
                        GirMapTileType.TILE_3D, layerName, fileId, fileName
                )
                .orElseThrow(() -> new RuntimeException("图层[" + layerName + "]配置不存在"));
        return config;
    }

}
