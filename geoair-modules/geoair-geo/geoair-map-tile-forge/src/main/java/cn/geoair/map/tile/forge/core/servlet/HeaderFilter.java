package cn.geoair.map.tile.forge.core.servlet;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.web.util.GirCorsFilter;
 

 
// 不能加Bean注解，否则会自动注册
public class HeaderFilter extends GirCorsFilter {
    public static GiLogger log = GirLoggerFactory.getLogger();
//    @Override
//    public void init(FilterConfig filterConfig) throws ServletException {
//
//    }
//
//    /**
//     * listScenePageByRoleId
//     * standalone 模式跨域设置
//     *
//     * @param servletRequest
//     * @param servletResponse
//     * @param filterChain
//     * @throws IOException
//     * @throws ServletException
//     */
//    @Override
//    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
//
//        HttpServletResponse response = (HttpServletResponse) servletResponse;
//
//        response.setCharacterEncoding("UTF-8");
//        // 跨域设置
//        response.setHeader("Access-Control-Allow-Origin", "*");
//        response.setHeader("Access-Control-Allow-Credentials", "true");
//        response.setHeader("Access-Control-Allow-Headers", "*");//这里很重要，要不然js header不能跨域携带  Authorization属性
//        response.setHeader("Access-Control-Allow-Methods", "POST, GET, PUT, OPTIONS, DELETE");
//
//        filterChain.doFilter(servletRequest, servletResponse);
//    }
//
//    @Override
//    public void destroy() {
//
//    }
}
