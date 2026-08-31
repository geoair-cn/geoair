package cn.geoair.map.dynamic.adv.mybatis;

/**
 * {@link DynamicSqlEngine} 的单例工具类。
 *
 * <p>全局持有一个 {@link DynamicSqlEngine} 实例，通过 {@link #getEngine()} 获取。
 *
 * @author zhangjun
 */
public class SqlEngineUtil {

    private static final DynamicSqlEngine ENGINE = new DynamicSqlEngine();

    private SqlEngineUtil() {}

    /**
     * 获取全局唯一的 DynamicSqlEngine 实例。
     *
     * @return DynamicSqlEngine 单例
     */
    public static DynamicSqlEngine getEngine() {
        return ENGINE;
    }
}
