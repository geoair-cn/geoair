package cn.geoair.comp.db.service.core.basic.servlet;

import java.io.IOException;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
// 不能加Bean注解，否则会自动注册，只有standalone需要
public class GirDsApiHeaderFilter implements Filter {

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	/**
	 * listScenePageByRoleId standalone 模式跨域设置
	 * @param servletRequest
	 * @param servletResponse
	 * @param filterChain
	 * @throws IOException
	 * @throws ServletException
	 */
	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
			throws IOException, ServletException {
		log.debug("ApiHeaderFilter filter execute");
		HttpServletResponse response = (HttpServletResponse) servletResponse;

		response.setCharacterEncoding("UTF-8");
		response.setContentType("application/json; charset=utf-8");
		// 跨域设置
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Credentials", "true");
		response.setHeader("Access-Control-Allow-Headers", "*"); // 这里很重要，要不然js
		// header不能跨域携带
		// Authorization属性
		response.setHeader("Access-Control-Allow-Methods", "POST, GET, PUT, OPTIONS, DELETE");

		filterChain.doFilter(servletRequest, servletResponse);
	}

	@Override
	public void destroy() {
	}

}
