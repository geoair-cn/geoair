package cn.geoair.map.tile.forge.core.servlet;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.web.util.GirCorsFilter;



// 不能加Bean注解，否则会自动注册
public class HeaderFilter extends GirCorsFilter {
    public static GiLogger log = GirLoggerFactory.getLogger();

}
