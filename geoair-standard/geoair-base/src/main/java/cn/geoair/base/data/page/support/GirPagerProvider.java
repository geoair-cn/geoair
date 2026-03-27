package cn.geoair.base.data.page.support;

import cn.geoair.base.data.page.GiPager;
import cn.geoair.base.data.page.GiPagerProvider;

/**
 * 默认的Pager对象提供者 返回 gtcPager 类型
 *
 * @author Ray
 */
public class GirPagerProvider implements GiPagerProvider {

    @Override
    public <T> GiPager<T> getPager(Class<T> clazz) {
        return new GirPager<T>(clazz);
    }
}
