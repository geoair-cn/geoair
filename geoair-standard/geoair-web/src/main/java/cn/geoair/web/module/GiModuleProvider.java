package cn.geoair.web.module;

import cn.geoair.base.bean.GirBeanHelper;

import java.lang.reflect.Type;
import java.util.List;

public interface GiModuleProvider<T extends GiModule> {

    @SuppressWarnings("unchecked")
    public static <T extends GiModule> GiModuleProvider<T> getModuleProvider(Class<T> moduleClass) {
        return GirBeanHelper.getProvider()
                .getBean(GiModuleProvider.class, new Type[] {moduleClass});
    }

    /**
     * 注册一个模块（添加一个模块）
     *
     * @param module 模块的相关数据
     */
    void regModule(T module) throws Exception;

    /** 根据所属获取模块 */
    public T getModule(String moduleId, int deep) throws Exception;

    /** 根据所属获取模块 */
    public List<? extends T> getModuleList(String moduleId, int deep) throws Exception;

    /**
     * 获取模块树
     *
     * @param moduleId 模块编号
     * @param deep 深度（0 自己， >0 级数 , < 0 所有）
     * @param ownerIds 模块所属
     * @return
     * @throws Exception
     */
    public T getModuleTreeByOwner(String moduleId, int deep, String[] ownerIds) throws Exception;

    /**
     * 获取模块列表
     *
     * @param moduleId 模块编号
     * @param deep 深度（0 自己， >0 级数 , < 0 所有）
     * @param ownerIds 模块所属
     * @return
     * @throws Exception
     */
    public List<? extends T> getModuleListByOwner(String moduleId, int deep, String[] ownerIds)
            throws Exception;
}
