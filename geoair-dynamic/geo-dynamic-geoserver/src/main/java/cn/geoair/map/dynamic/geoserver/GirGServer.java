package cn.geoair.map.dynamic.geoserver;

import cn.geoair.base.Gir;
import cn.geoair.map.dynamic.geoserver.api.DataStoreManager;
import cn.geoair.map.dynamic.geoserver.api.LayerPublisher;
import cn.geoair.map.dynamic.geoserver.api.OgcServiceConfigurer;
import cn.geoair.map.dynamic.geoserver.api.WorkspaceManager;
import cn.hutool.core.lang.Singleton;

/**
 * @author ：张逢吉
 * @date ：Created in 14:36
 * @description： TODO
 */
public class GirGServer {

    public static DataStoreManager getDataStoreManager() {
        return getPxyBeanC(DataStoreManager.class);
    }

    public static LayerPublisher getLayerPublisher() {
        return getPxyBeanC(LayerPublisher.class);
    }

    public static OgcServiceConfigurer getOgcServiceConfigurer() {
        return getPxyBeanC(OgcServiceConfigurer.class);
    }

    public static WorkspaceManager getWorkspaceManager() {
        return getPxyBeanC(WorkspaceManager.class);
    }

    public static <T> T getPxyBeanC(Class<T> classs) {
        if (Singleton.exists(classs)) {
            return Singleton.get(classs);
        } else {
            T bean = Gir.beans.getBean(classs);
            Singleton.put(classs.getName(), bean);
            return bean;
        }
    }
}
