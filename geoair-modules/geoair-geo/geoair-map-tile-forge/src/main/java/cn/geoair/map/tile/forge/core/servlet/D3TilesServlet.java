package cn.geoair.map.tile.forge.core.servlet;

import cn.geoair.map.dynamic.tools.simple.GirServletUtil;
import cn.geoair.map.tile.forge.core.GirLayerConfigContextHelper;
import cn.geoair.map.tile.forge.core.enums.GirMapTileType;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.service.GirMapTileService;
import cn.geoair.map.tile.forge.core.vo.TileRequest;
import cn.hutool.core.io.IoUtil;

import jakarta.annotation.Resource;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class D3TilesServlet extends HttpServlet {
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
            GirServletUtil.toResponse(response, e.getMessage().getBytes(Charset.defaultCharset()), "text/plain; charset=utf-8");
            return;
        }
        try {
            TileRequest layerTile = gMapTileService.getLayerTile(layerConfigContext, contentAfterPrefix, "", "");
            buildTileResponse(layerTile, response);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 构建瓦片响应
     */
    protected void buildTileResponse(TileRequest tileRequest, HttpServletResponse response) throws Exception {
        // 1. 校验瓦片是否存在
        if (!tileRequest.isExists()) {
            response.setStatus(HttpStatus.NO_CONTENT.value());
            return;
        }
        response.setHeader("Cache-Control", "public, max-age=86400");
        response.setHeader("Last-Modified", tileRequest.getLastModified() + "");
        if (tileRequest.getSize() > 0) {
            response.setHeader("Content-Length", tileRequest.getSize() + "");
        }
        response.setContentType(tileRequest.getMimeType());
        response.setStatus(HttpStatus.OK.value());

        // 3. 转换输入流为字节数组并返回
        byte[] tileData = tileRequest.getBytes();
        ServletOutputStream outputStream = response.getOutputStream();
        IoUtil.copy(new ByteArrayInputStream(tileData), outputStream);
        IoUtil.close(outputStream);

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
