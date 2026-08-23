package cn.geoair.map.dynamic.tools.simple.response;


import jakarta.servlet.http.HttpServletResponse;

/**
 * 将 {@link TileResponse} 写入 Servlet HTTP 响应的策略接口。
 *
 * <p>本接口只负责 HTTP 输出，不负责 URI 解析或瓦片读取；URI 到响应对象的转换由
 * {@link TileResponseProvider} 完成。</p>
 *
 * @author 张俊
 * @date 2026/7/13
 */
public interface GirTileResponseOpt {

    /**
     * 将异常转换为 HTTP 错误响应。
     *
     * @param exception 处理过程中产生的异常
     * @param response  Servlet HTTP 响应
     */
    void buildFromException(Exception exception, HttpServletResponse response);

    /**
     * 将统一瓦片响应写入 Servlet HTTP 响应。
     *
     * @param tileResponse 瓦片响应元数据及内容
     * @param response     Servlet HTTP 响应
     */
    void buildFromTileResponse(TileResponse tileResponse, HttpServletResponse response);



}
