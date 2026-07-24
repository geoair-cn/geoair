package cn.geoair.comp.db.service.core.basic.servlet;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.web.util.GirCorsFilter;

// 不能加Bean注解，否则会自动注册，只有standalone需要
public class GirDsApiHeaderFilter extends GirCorsFilter {
    public static GiLogger log = GirLoggerFactory.getLogger();
}
