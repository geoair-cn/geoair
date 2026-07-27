package cn.geoair.web.util;


import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface GiWebContextProvider {

    HttpServletRequest getRequest();

    HttpServletResponse getResponse();

    ServletContext getServletContext();
}
