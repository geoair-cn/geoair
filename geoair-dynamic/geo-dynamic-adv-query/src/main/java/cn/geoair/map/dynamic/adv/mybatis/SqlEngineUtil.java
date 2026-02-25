package cn.geoair.map.dynamic.adv.mybatis;


/**
 * @program: dbApi
 * @description:
 * @author: 武汉刘德华
 * @create: 2021-02-24 10:02
 **/
public class SqlEngineUtil {

    static DynamicSqlEngine engine = new DynamicSqlEngine();

    public static DynamicSqlEngine getEngine() {
        return engine;
    }
}
