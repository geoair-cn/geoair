package cn.geoair.base.sp;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Service Provider loader 接口
 *
 * @author Ray
 */
public interface GkSpLoader {

    /**
     * 根据接口类型加载其实现类实例
     *
     * @param <T> 泛型类型
     * @param cls 接口类型
     * @param types 泛型类型数组
     * @return 接口实现类实例，如果找不到则返回null
     */
    public <T> T load(Class<T> cls, Type[] types);

    /**
     * 根据接口类型和实例名加载实现类实例
     *
     * <p>在多个实现类中通过name确定唯一实例：
     *
     * <ul>
     *   <li>Spring容器：通过bean name获取
     *   <li>JDK SPI：通过实现类的Class简单名称匹配
     *   <li>PlaceHolder：通过配置的Class简单名称匹配
     * </ul>
     *
     * @param <T> 泛型类型
     * @param cls 接口类型
     * @param name 实例名
     * @param types 泛型类型数组
     * @return 接口实现类实例，如果找不到则返回null
     */
    public <T> T load(Class<T> cls, String name, Type[] types);

    /**
     * 根据接口类型加载所有实现类实例
     *
     * <p>聚合所有加载策略中的实现类，适用于插件/扩展发现场景。
     *
     * @param <T> 泛型类型
     * @param cls 接口类型
     * @param types 泛型类型数组
     * @return 所有匹配的实现类实例列表
     */
    public <T> List<T> loadAll(Class<T> cls, Type[] types);
}
