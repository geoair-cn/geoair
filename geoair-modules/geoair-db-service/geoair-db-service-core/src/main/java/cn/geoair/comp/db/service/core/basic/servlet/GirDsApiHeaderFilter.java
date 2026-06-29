package cn.geoair.comp.db.service.core.basic.servlet;

import cn.geoair.web.util.GirCorsFilter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
// 不能加Bean注解，否则会自动注册，只有standalone需要
public class GirDsApiHeaderFilter extends GirCorsFilter {


}
