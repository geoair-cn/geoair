package cn.geoair.map.tile.forge.core.servlet;

import cn.geoair.map.tile.forge.core.TileRequest;
import cn.hutool.core.io.IoUtil;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;


import java.io.ByteArrayInputStream;

/**
 * @author ：张俊
 * @date ：Created in 2026/7/3 14:32
 * @description： TODO
 */
public class TileResponseUtils {

    /**
     * 构建瓦片响应
     */
    public static void buildTileResponse(TileRequest tileRequest, HttpServletResponse response) throws Exception {
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
        response.setContentType(tileRequest.getMimeType().getFormat());
        response.setStatus(HttpStatus.OK.value());

        // 3. 转换输入流为字节数组并返回
        byte[] tileData = tileRequest.getBytes();
        ServletOutputStream outputStream = response.getOutputStream();
        IoUtil.copy(new ByteArrayInputStream(tileData), outputStream);
        IoUtil.close(outputStream);

    }

}
