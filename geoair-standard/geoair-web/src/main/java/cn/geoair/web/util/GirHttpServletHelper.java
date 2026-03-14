package cn.geoair.web.util;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import cn.geoair.base.lang.invoke.GkMethodHand;
import cn.geoair.base.lang.invoke.GaMethodHandDefine;

public class GirHttpServletHelper {

	// static {
	// MethodHand.implFromClass( girHttpServletHelper.class);
	// }

	@GaMethodHandDefine(expectClassName = "cn.geoair.spi.web.SpringServlet4Gir")
	public static HttpServletRequest getRequest() {
		return (HttpServletRequest) GkMethodHand.invokeSelf();
	}

	@GaMethodHandDefine(expectClassName = "cn.geoair.spi.web.SpringServlet4Gir")
	public static HttpServletResponse getResponse() {
		return (HttpServletResponse) GkMethodHand.invokeSelf();
	}

	@GaMethodHandDefine(expectClassName = "cn.geoair.spi.web.SpringServlet4Gir")
	public static ServletContext getServletContext() {
		return (ServletContext) GkMethodHand.invokeSelf();
	}

}
