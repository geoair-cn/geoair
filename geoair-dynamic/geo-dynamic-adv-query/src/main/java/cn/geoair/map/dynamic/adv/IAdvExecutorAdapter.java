package cn.geoair.map.dynamic.adv;


import cn.geoair.map.dynamic.adv.query.IAdvExecutor;

/**
 * @author ：zhangjun
 * @date ：Created in 2025/10/9 15:03
 * @description： IAdvExecutor的获取器，可以调用方自行实现
 */
public interface IAdvExecutorAdapter {


    <T extends IAdvExecutor> T getIAdvExecutor(String dataSourceId, String schema);

    <T extends IAdvExecutor> T getIAdvExecutor(String dataSourceId, String schema, Class<T> clazz);
}
