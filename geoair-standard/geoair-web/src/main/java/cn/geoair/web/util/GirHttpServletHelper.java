package cn.geoair.web.util;

import cn.geoair.base.lang.invoke.GaMethodHandDefine;
import cn.geoair.base.lang.invoke.GkMethodHand;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
