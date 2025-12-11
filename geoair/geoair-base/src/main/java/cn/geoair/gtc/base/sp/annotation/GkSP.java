package cn.geoair.gtc.base.sp.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.geoair.gtc.base.sp.support.GtcJdkSpLoader;
import cn.geoair.gtc.base.sp.GkSpLoader;
import cn.geoair.gtc.base.sp.support.GtcBeanFactorySpLoader;


/**
 * Service Provider 实现提供者配置
 * @author Ray
 *
 */

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface GkSP {


	/**
	 * 实例名，在spring容器或者dubbo spi实现在多个接口实现中通过id或name确定唯一实例
	 * @return
	 */
    public String name() default "";


    /**
     * Provider 加载器配置，按顺序
     * @return
     */
    public Class<? extends GkSpLoader>[] loader() default { GtcBeanFactorySpLoader.class, GtcJdkSpLoader.class};

    /**
     * 设置默认实现类,需要有无参构造函数,不带泛型的接口默认选择第一个，优先级大于placeHolders
     * @return
     */
    public Class<?>[] placeHolderClass() default {};

    /**
     * 设置默认实现类类名，,需要有无参构造函数,不带泛型的接口默认选择第一个，优先级小于placeHolderClasses
     * @return
     */
    public String[] placeHolder() default {};


    /**
     * 是否为单态(设置非单态将不缓存)
     * @return
     */
    public boolean singleton() default true;

}
