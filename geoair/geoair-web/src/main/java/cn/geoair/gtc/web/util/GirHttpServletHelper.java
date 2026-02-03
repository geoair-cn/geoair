package cn.geoair.gtc.web.util;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import cn.geoair.gtc.base.lang.invoke.GkMethodHand;
import cn.geoair.gtc.base.lang.invoke.GaMethodHandDefine;


public class GirHttpServletHelper {


	//static {
	//	MethodHand.implFromClass( gtcHttpServletHelper.class);
	//}


	@GaMethodHandDefine(expectClassName = "com.gtc.spi.web.SpringServlet4Gtc")
    public static HttpServletRequest getRequest() {
		return (HttpServletRequest)GkMethodHand.invokeSelf();
    }

	@GaMethodHandDefine(expectClassName = "com.gtc.spi.web.SpringServlet4Gtc")
    public static HttpServletResponse getResponse() {
		return (HttpServletResponse)GkMethodHand.invokeSelf();
    }

	@GaMethodHandDefine(expectClassName = "com.gtc.spi.web.SpringServlet4Gtc")
    public static ServletContext getServletContext() {
		return (ServletContext)GkMethodHand.invokeSelf();
    }


}
