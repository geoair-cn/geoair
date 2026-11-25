package cn.geoair.map.dynamic.tools.simple;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.dynamic.tools.simple.response.GirTileResponseDefaultOpt;
import cn.geoair.map.dynamic.tools.simple.response.GirTileResponseOpt;
import cn.geoair.map.dynamic.tools.simple.response.TileResponse;
import javax.servlet.http.HttpServletResponse;

/**
 * {@link TileResponse} 到 Servlet HTTP 响应的便捷入口。
 *
 * <p>无参静态方法每次创建独立实例并使用默认 {@link GirTileResponseDefaultOpt}；如需自定义 输出策略，应保留本类实例并通过 {@link
 * #setTileResponseOpt(GirTileResponseOpt)} 设置。
 *
 * @author 张俊
 * @date 2026/7/13
 */
public class GirTileResponseUtil {

    /** @return 新建的响应输出工具实例。 */
    public static GirTileResponseUtil getInstance() {
        return new GirTileResponseUtil();
    }

    private static final GiLogger log = GirLoggerFactory.getLogger();

    /** 当前 HTTP 输出策略；首次使用时延迟创建默认实现。 */
    private GirTileResponseOpt tileResponseOpt;

    /**
     * 获取当前响应输出策略；未设置时创建默认策略。
     *
     * @return 当前响应输出策略
     */
    public GirTileResponseOpt getTileResponseOpt() {
        if (tileResponseOpt == null) {
            tileResponseOpt = new GirTileResponseDefaultOpt();
        }
        return tileResponseOpt;
    }

    /**
     * 设置调用方自定义的响应输出策略。
     *
     * @param tileResponseOpt 自定义输出策略
     * @return 当前工具实例
     */
    public GirTileResponseUtil setTileResponseOpt(GirTileResponseOpt tileResponseOpt) {
        this.tileResponseOpt = tileResponseOpt;
        return this;
    }

    /**
     * 使用默认输出策略将异常写入 HTTP 响应。
     *
     * @param exception 异常
     * @param response Servlet HTTP 响应
     */
    public static void buildFromException(Exception exception, HttpServletResponse response) {
        GirTileResponseUtil.getInstance()
                .getTileResponseOpt()
                .buildFromException(exception, response);
    }
    /**
     * 使用默认输出策略将瓦片响应写入 HTTP 响应。
     *
     * @param tileResponse 瓦片响应
     * @param response Servlet HTTP 响应
     */
    public static void buildFromTileResponse(
            TileResponse tileResponse, HttpServletResponse response) {
        GirTileResponseUtil.getInstance()
                .getTileResponseOpt()
                .buildFromTileResponse(tileResponse, response);
    }
}
