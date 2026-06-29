package cn.geoair.map.tile.forge.core.servlet;

import cn.geoair.web.util.GirCorsFilter;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
// 不能加Bean注解，否则会自动注册
public class HeaderFilter extends GirCorsFilter {


}
