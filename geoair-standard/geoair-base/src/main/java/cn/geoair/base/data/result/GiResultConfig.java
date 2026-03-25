package cn.geoair.base.data.result;

import cn.geoair.base.data.result.support.GirResultConfig;
import cn.geoair.base.sp.annotation.GkSP;

/**
 * 结果集配置
 *
 * @author Ray
 */
@GkSP(placeHolderClass = GirResultConfig.class)
public interface GiResultConfig {

    /**
     * 获取结果集对象
     *
     * @param <T>
     * @param clazz
     * @return
     */
    public <T> GiResult<T> getResult(Class<T> clazz);
}
