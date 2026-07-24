package cn.geoair.map.dynamic.tools.simple.response;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 瓦片响应构建工具 基于TileResponse对象进行响应构建
 *
 * @author 张俊
 * @date 2026/7/13
 */
public interface GirTileResponseOpt {

    void buildFromException(Exception exception, HttpServletResponse response);

    /** 从TileResponse对象构建响应 */
    void buildFromTileResponse(TileResponse tileResponse, HttpServletResponse response);
}
