package cn.geoair.map.dynamic.dbservice;

import cn.geoair.base.bean.GirBeanHelper;

/**
 * @author ：zhangjun
 * @date ：Created in 2025/8/5 16:07
 * @description： TODO
 */
public interface DbApiUserInfoHelper {

    static DbApiUserInfoHelper getInstance() {
        return GirBeanHelper.getProvider().getBean(DbApiUserInfoHelper.class);
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
