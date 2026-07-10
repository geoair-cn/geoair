package cn.geoair.map.tile.forge.core.servlet;


import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.dynamic.tools.simple.GirServletUtil;
import cn.geoair.map.tile.forge.core.GirLayerConfigContextHelper;
import cn.geoair.map.tile.forge.core.TileRequest;
import cn.geoair.map.tile.forge.core.enums.GirMapTileType;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;


import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * url构建逻辑参考 TileUrlBuilder
 */

@Component
public class XYZServlet extends D3TilesServlet {
    public static GiLogger log = GirLoggerFactory.getLogger();

    Pattern pattern = Pattern.compile("/xyzTileService/rest/([^/]+)/([^/]+)/([^/]+)/([^/]+)/([^/]+)/([^/]+)/([^/]+)");

    //    /rest/xyz
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String requestURI = request.getRequestURI();
        Matcher matcher = pattern.matcher(requestURI);   ///xyzTileService/rest/v2/1993285204737159169/0-10/preview/3/6/2
        String fileId = null;
        String type = null;
        String fileName = null;
        String serviceName = null;
        String z = null;
        String x = null;
        String y = null;
        String format = null;
        if (matcher.find()) {
            type = matcher.group(1);          // 提取类型
            fileId = matcher.group(2);          // 提取FileId（aaa）
            fileName = matcher.group(3);      // 提取文件名称
            serviceName = matcher.group(4);      // 提取服务名称
            z = matcher.group(5); // 提取Z
            x = matcher.group(6); // 提取X
            y = matcher.group(7); // 提取Y
            String[] split = y.split("\\.");
            if (split.length == 2) {
                y = split[0];
                format = split[1];
            }
        }
        GirLayerConfigContext arcGisGirLayerConfigContext = null;
        try {
            arcGisGirLayerConfigContext
                    = getGirLayerConfigContext(type, fileId, fileName, serviceName);
        } catch (Exception e) {
            GirServletUtil.toResponse(response, e.getMessage().getBytes(Charset.defaultCharset()), "text/plain; charset=utf-8");
            return;
        }
        arcGisGirLayerConfigContext.setFormat("png");

        GirMapTileType mapTileType = arcGisGirLayerConfigContext.getMapTileType();
        try {
            if (mapTileType == GirMapTileType.XYZ) {
                String zxyType = request.getParameter("zxyType");
                String gridSet = request.getParameter("gridSet");
                String originType = request.getParameter("originType");
                int wmtsY = Integer.parseInt(y);

                if (gridSet == null) {
                    gridSet = "EPSG:3857";
                }

                int xInt = Integer.parseInt(x);
                int zInt = Integer.parseInt(z);

                if (Objects.equals(zxyType, "zyx")) {
                    int temp = xInt;
                    xInt = wmtsY;
                    wmtsY = temp;
                }

                if (StringUtils.equals(originType, "tms")) {
                    if (Objects.equals(gridSet, "EPSG:4326") || Objects.equals(gridSet, "EPSG:4490")) {
                        zInt = zInt - 1;
                        wmtsY = (int) (Math.pow(2, zInt) - wmtsY - 1);
                    } else {
                        wmtsY = (int) (Math.pow(2, zInt) - wmtsY - 1);
                    }
                }
                TileRequest tileRequest = null;
                tileRequest = gMapTileService.getLayerTile(arcGisGirLayerConfigContext, zInt + "", wmtsY + "", xInt + "");
                TileResponseUtils.buildTileResponse(tileRequest, response);
            } else {
                TileRequest layerTile = gMapTileService.getLayerTile(arcGisGirLayerConfigContext, z, y, x);
                TileResponseUtils.buildTileResponse(layerTile, response);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

    }


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }

    public GirLayerConfigContext getGirLayerConfigContext(String type, String fileId, String fileName, String serviceName) {
        GirMapTileType mapTileType = getGirMapTileType(type);
        GirLayerConfigContext config = GirLayerConfigContextHelper.getInstance().getGirLayerConfigContext(
                        mapTileType, serviceName, fileId, fileName
                )
                .orElseThrow(() -> new RuntimeException("图层[" + fileId + "]配置不存在"));
        return config;
    }

    private static GirMapTileType getGirMapTileType(String type) {
        GirMapTileType mapTileType = null;
        if (Objects.equals(type, "xyz")) {
            mapTileType = GirMapTileType.XYZ;
        }
        if (Objects.equals(type, "compact_v2")) {
            mapTileType = GirMapTileType.COMPACT_V2;
        }
        if (Objects.equals(type, "compact_v1")) {
            mapTileType = GirMapTileType.COMPACT_V1;
        }
        return mapTileType;
    }

}
