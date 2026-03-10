package cn.geoair.map.dynamic.dbservice.core;

import cn.geoair.base.bean.GirBeanHelper;

/**
 * @author ：zhangjun
 * @date ：Created in 2025/8/5 16:07
 */
public interface DsApiUserInfoHelper {

    static DsApiUserInfoHelper getInstance() {
        return GirBeanHelper.getProvider().getBean(DsApiUserInfoHelper.class);
    }

    /**
     * 获取用户名
     *
     * @return
     */
    String getSubjectName();

    /**
     * 获取用户id
     *
     * @return
     */
    String getSubjectId();
}
