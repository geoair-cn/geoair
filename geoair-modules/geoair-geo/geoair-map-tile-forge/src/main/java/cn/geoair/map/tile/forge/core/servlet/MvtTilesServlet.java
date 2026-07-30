package cn.geoair.map.tile.forge.core.servlet;


import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.dynamic.tools.simple.GirServletUtil;
import cn.geoair.map.dynamic.tools.simple.GirTileResponseUtil;
import cn.geoair.map.dynamic.tools.simple.response.TileResponse;
import cn.geoair.map.tile.forge.core.GirLayerConfigContextHelper;
import cn.geoair.map.tile.forge.core.TileRequest;
import cn.geoair.map.tile.forge.core.enums.GirMapTileType;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.service.GirMapTileService;
import jakarta.servlet.http.HttpServletResponse;
import cn.geoair.web.GirWeb;


import java.util.regex.Pattern;


public class MvtTilesServlet extends D3TilesServlet {

    public static GiLogger log = GirLoggerFactory.getLogger();

    public MvtTilesServlet(GirMapTileService mapTileService) {
        super(mapTileService);
    }

    public Pattern getPattern() {
        return Pattern.compile("/mvtTilesService/([^/]+)/([^/]+)/([^/]+)(/.*)?");
    }


    public GirLayerConfigContext getGirLayerConfigContext(String fileId, String fileName, String layerName) {
        GirLayerConfigContext config = GirLayerConfigContextHelper.getInstance().getGirLayerConfigContext(
                        GirMapTileType.MVT_TILES, layerName, fileId, fileName
                )
                .orElseThrow(() -> new RuntimeException("图层[" + layerName + "]配置不存在"));
        return config;
    }

    public void toHttpResponse(TileRequest tileRequest, HttpServletResponse response, TileParseResult tileParseResult) {
        String requestURI = tileParseResult.getRequestURI();

        if (requestURI.contains("style.json")) {
            byte[] bytes = tileRequest.getBytes();
            String jsonContent = new String(bytes);
            String requestURL = GirServletUtil.getHttpPathByRequest() + GirWeb.getRequest().getRequestURI();
            String replace = requestURL.replace("/style.json", "");
            jsonContent = jsonContent.replace("{BASE_URL}", replace);
            tileRequest.setBytes(jsonContent.getBytes());
        }
        TileResponse tileResponse = tileRequest.toTileResponse();
        GirTileResponseUtil.buildFromTileResponse(tileResponse, response);
    }

}
