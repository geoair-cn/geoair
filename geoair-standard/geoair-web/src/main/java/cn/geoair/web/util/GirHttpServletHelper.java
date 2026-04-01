package cn.geoair.web.util;

import cn.geoair.base.lang.invoke.GaMethodHandDefine;
import cn.geoair.base.lang.invoke.GkMethodHand;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
