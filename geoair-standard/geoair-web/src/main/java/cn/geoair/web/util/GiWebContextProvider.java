package cn.geoair.web.util;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface GiWebContextProvider {

    HttpServletRequest getRequest();

    HttpServletResponse getResponse();

    ServletContext getServletContext();
}
