package cn.geoair.map.tile.forge.core.servlet;


import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.base.util.GutilObject;
import cn.geoair.map.dynamic.tools.grid.dto.TileZxyApo;
import cn.geoair.map.dynamic.tools.simple.GirTileResponseUtil;
import cn.geoair.map.dynamic.tools.simple.response.TileResponse;
import cn.geoair.map.tile.forge.core.GirLayerConfigContextHelper;
import cn.geoair.map.dynamic.tools.simple.response.TileParamEnums;
import cn.geoair.map.tile.forge.core.enums.GirMapTileType;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.TileRequest;

import cn.geoair.map.tile.forge.core.service.GirMapTileService;
import cn.hutool.core.util.StrUtil;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * url构建逻辑参考 TileUrlBuilder
 */


public class XYZServlet extends D3TilesServlet {
    public static GiLogger log = GirLoggerFactory.getLogger();

    public XYZServlet(GirMapTileService mapTileService) {
        super(mapTileService);
    }

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
        GirLayerConfigContext layerConfigContext = null;
        try {
            layerConfigContext
                    = getGirLayerConfigContext(type, fileId, fileName, serviceName);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            GirTileResponseUtil.buildFromException(e, response);
            return;
        }
        layerConfigContext.setFormat("png");

        GirMapTileType mapTileType = layerConfigContext.getMapTileType();
        try {
            if (mapTileType == GirMapTileType.XYZ) {
                String zxyType = request.getParameter(TileParamEnums.ZXY_TYPE.getValue());
                String gridSet = request.getParameter(TileParamEnums.GRID_SET.getValue());
                String originType = request.getParameter(TileParamEnums.ORIGIN_TYPE.getValue());
                int wmtsY = Integer.parseInt(y);

                if (GutilObject.isEmpty(gridSet)) {
                    gridSet = TileParamEnums.GRID_SET.getDefaultValue();
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
                try {
                    tileRequest = mapTileService.getLayerTile(layerConfigContext, zInt + "", wmtsY + "", xInt + "");
                    TileResponse tileResponse = tileRequest.toTileResponse();
                    if (!tileResponse.isSuccess()) {
                        String format1 = StrUtil.format("无法找到瓦片 z:{}, x:{}, y:{}", z, x, y);
                        tileResponse.setErrorMessage(format1);
                    }
                    tileResponse.setCoordinate(new TileZxyApo(zInt, xInt, wmtsY)).setGridEpsgStr(gridSet);
                    GirTileResponseUtil.buildFromTileResponse(tileResponse, response);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    TileResponse tileResponse = TileResponse.error(e.getMessage());
                    tileResponse.setCoordinate(new TileZxyApo(zInt, xInt, wmtsY)).setGridEpsgStr(gridSet);
                    GirTileResponseUtil.buildFromTileResponse(tileResponse, response);
                    return;
                }

            } else {
                try {
                    TileRequest layerTile = mapTileService.getLayerTile(layerConfigContext, z, y, x);
                    TileResponse tileResponse = layerTile.toTileResponse();
                    GirTileResponseUtil.buildFromTileResponse(tileResponse, response);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    GirTileResponseUtil.buildFromException(e, response);
                }

            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            GirTileResponseUtil.buildFromException(e, response);
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
