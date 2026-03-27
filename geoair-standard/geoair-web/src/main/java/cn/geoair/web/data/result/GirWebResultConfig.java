package cn.geoair.web.data.result;

import cn.geoair.base.data.result.GiResult;
import cn.geoair.base.data.result.GiResultConfig;

/**
 * Web结果集默认配置
 *
 * @author Ray
 */
public class GirWebResultConfig implements GiResultConfig {

    @Override
    public <T> GiResult<T> getResult(Class<T> clazz) {
        return new GirWebResult<T>(clazz);
    }
}
