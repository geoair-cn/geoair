package cn.geoair.web.util;

import cn.geoair.base.lang.invoke.GaMethodHandDefine;
import cn.geoair.base.lang.invoke.GkMethodHand;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class GirHttpServletHelper {

    private static volatile GiWebContextProvider contextProvider;

    public static void setContextProvider(GiWebContextProvider contextProvider) {
        GirHttpServletHelper.contextProvider = contextProvider;
    }

    private static GiWebContextProvider getContextProvider() {
        return contextProvider;
    }

    // static {
    // MethodHand.implFromClass( girHttpServletHelper.class);
    // }

    @GaMethodHandDefine(expectClassName = "cn.geoair.spi.web.SpringServlet4Gir")
    public static HttpServletRequest getRequest() {
        GiWebContextProvider provider = getContextProvider();
        if (provider != null) {
            return provider.getRequest();
        }
        return (HttpServletRequest) GkMethodHand.invokeSelf();
    }

    @GaMethodHandDefine(expectClassName = "cn.geoair.spi.web.SpringServlet4Gir")
    public static HttpServletResponse getResponse() {
        GiWebContextProvider provider = getContextProvider();
        if (provider != null) {
            return provider.getResponse();
        }
        return (HttpServletResponse) GkMethodHand.invokeSelf();
    }

    @GaMethodHandDefine(expectClassName = "cn.geoair.spi.web.SpringServlet4Gir")
    public static ServletContext getServletContext() {
        GiWebContextProvider provider = getContextProvider();
        if (provider != null) {
            return provider.getServletContext();
        }
        return (ServletContext) GkMethodHand.invokeSelf();
    }

    public static HttpSession getSession(boolean autoCreate) {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return null;
        }
        return request.getSession(autoCreate);
    }

    public static Object getRequestAttribute(String name) {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return null;
        }
        return request.getAttribute(name);
    }

    public static void setRequestAttribute(String name, Object value) {
        HttpServletRequest request = getRequest();
        if (request != null) {
            request.setAttribute(name, value);
        }
    }

    public static String getHeader(String name) {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return null;
        }
        return request.getHeader(name);
    }

    public static String getParameter(String name) {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return null;
        }
        return request.getParameter(name);
    }
}
