package cn.geoair.web;

import cn.geoair.web.util.GirHttpServletHelper;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
//import jakarta.servlet.ServletContext;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;

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
