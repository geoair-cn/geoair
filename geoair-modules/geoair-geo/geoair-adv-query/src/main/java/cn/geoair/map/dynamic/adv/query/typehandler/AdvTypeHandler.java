package cn.geoair.map.dynamic.adv.query.typehandler;

import cn.geoair.base.sp.annotation.GkSP;
import cn.geoair.base.sp.support.GirJdkSpLoader;

/**
 * @author ：张逢吉
 * @date ：Created in 2026/7/22
 * @description： 高级查询器的类型处理器
 */
@GkSP(loader = GirJdkSpLoader.class)
public interface AdvTypeHandler<T> {



    boolean supports(Class<?> javaType, Object value);

    T convertForRead(Object value, Class<?> javaType, AdvTypeHandlerContext context);

    Object convertForWrite(T value, Class<?> javaType, AdvTypeHandlerContext context);
}
