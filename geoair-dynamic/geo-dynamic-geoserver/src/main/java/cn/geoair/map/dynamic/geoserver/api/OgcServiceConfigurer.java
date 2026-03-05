package cn.geoair.map.dynamic.geoserver.api;

import cn.geoair.map.dynamic.geoserver.enums.OgcServiceType;
import cn.geoair.map.dynamic.geoserver.exception.ServicePublishException;

import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.ServiceInfo;

import java.util.Map;

/** OGC 服务全局配置接口 */
public interface OgcServiceConfigurer {

    /**
     * 获取工作区下的 OGC 服务配置
     *
     * @param workspace 工作区（null表示全局配置）
     * @param serviceType OGC服务类型
     * @return 服务配置信息
     */
    ServiceInfo getServiceConfig(WorkspaceInfo workspace, OgcServiceType serviceType);

    /**
     * 更新 OGC 服务配置
     *
     * @param workspace 工作区（null表示全局配置）
     * @param serviceType OGC服务类型
     * @param configParams 配置参数（如标题、最大返回条数、是否启用缓存等）
     * @return 更新后的服务配置
     * @throws ServicePublishException 配置更新失败时抛出
     */
    ServiceInfo updateServiceConfig(
            WorkspaceInfo workspace, OgcServiceType serviceType, Map<String, Object> configParams);

    /**
     * 启用/禁用全局 OGC 服务
     *
     * @param serviceType OGC服务类型
     * @param enabled true=启用，false=禁用
     * @throws ServicePublishException 操作失败时抛出
     */
    void enableGlobalService(OgcServiceType serviceType, boolean enabled);
}
