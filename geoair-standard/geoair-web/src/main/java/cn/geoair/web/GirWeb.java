package cn.geoair.web;

import cn.geoair.web.util.GirHttpServletHelper;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class GirWeb {

    public static HttpServletRequest getRequest() {
        return GirHttpServletHelper.getRequest();
    }

    public static HttpServletResponse getResponse() {
        return GirHttpServletHelper.getResponse();
    }

    public static ServletContext getServletContext() {
        return GirHttpServletHelper.getServletContext();
    }
}
