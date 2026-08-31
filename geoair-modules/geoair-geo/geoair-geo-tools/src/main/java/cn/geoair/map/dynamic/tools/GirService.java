package cn.geoair.map.dynamic.tools;

import cn.geoair.base.Gir;
import cn.hutool.core.lang.Singleton;

/**
 * Spring Bean 的静态获取与 Hutool 单例缓存适配器。
 *
 * <p>首次获取时从 {@link Gir#beans} 取得 Bean 并按类型写入 Hutool {@link Singleton}；
 * 后续调用直接返回缓存。仅适用于应用上下文已初始化的运行环境。
 *
 * @author 张逢吉
 */
public class GirService {

    /**
     * 按类型获取 Spring 托管 Bean，并在本进程中缓存。
     *
     * @param classs Bean 类型
     * @param <T> Bean 类型
     * @return Spring 容器中的 Bean 实例
     */
    public static <T> T getPxyBeanC(Class<T> classs) {
        if (Singleton.exists(classs)) {
            return Singleton.get(classs);
        } else {
            T bean = Gir.beans.getBean(classs);
            Singleton.put(classs.getName(), bean);
            return bean;
        }
    }
}
