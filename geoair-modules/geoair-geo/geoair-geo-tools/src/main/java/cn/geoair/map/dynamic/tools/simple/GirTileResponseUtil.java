package cn.geoair.map.dynamic.tools.simple;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.dynamic.tools.simple.response.GirTileResponseDefaultOpt;
import cn.geoair.map.dynamic.tools.simple.response.GirTileResponseOpt;
import cn.geoair.map.dynamic.tools.simple.response.TileResponse;
import jakarta.servlet.http.HttpServletResponse;


/**
 * 瓦片响应构建工具
 * 基于TileResponse对象进行响应构建
 *
 * @author 张俊
 * @date 2026/7/13
 */
public class GirTileResponseUtil {

    public static GirTileResponseUtil getInstance() {
        return new GirTileResponseUtil();
    }

    private static final GiLogger log = GirLoggerFactory.getLogger();

    GirTileResponseOpt tileResponseOpt;

    public GirTileResponseOpt getTileResponseOpt() {
        if (tileResponseOpt == null) {
            tileResponseOpt = new GirTileResponseDefaultOpt();
        }
        return tileResponseOpt;
    }

    /**
     * 暴露具体的实现由客户端定义
     *
     * @param tileResponseOpt
     * @return
     */
    public GirTileResponseUtil setTileResponseOpt(GirTileResponseOpt tileResponseOpt) {
        this.tileResponseOpt = tileResponseOpt;
        return this;
    }


    public static void buildFromException(Exception exception, HttpServletResponse response) {
        GirTileResponseUtil.getInstance().getTileResponseOpt().buildFromException(exception, response);
    }


    public static void buildFromTileResponse(TileResponse tileResponse, HttpServletResponse response) {
        GirTileResponseUtil.getInstance().getTileResponseOpt().buildFromTileResponse(tileResponse, response);
    }
}
