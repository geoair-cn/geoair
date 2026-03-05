package cn.geoair.map.dynamic.geoserver.utils;

import cn.geoair.map.dynamic.geoserver.beans.GsWorkspace;
import cn.geoair.map.dynamic.geoserver.exception.ServicePublishException;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/** GsWorkspace 与 WorkspaceInfo 的转换工具类 */
@Component
public class WorkspaceConvertUtil {

    // 注入 GeoServer 的 Catalog（核心，用于创建 WorkspaceInfo/NamespaceInfo 实例）
    @Autowired private Catalog catalog;

    /**
     * 将自定义 GsWorkspace 转换为 GeoServer 的 WorkspaceInfo（创建新实例）
     *
     * @param gsWorkspace 自定义工作区对象
     * @return 完整的 WorkspaceInfo 实例
     * @throws ServicePublishException 转换失败时抛出
     */
    public WorkspaceInfo convertToWorkspaceInfo(GsWorkspace gsWorkspace) {
        try {
            // 1. 参数校验
            Assert.notNull(gsWorkspace, "GsWorkspace 不能为空");
            Assert.hasText(gsWorkspace.getName(), "工作区名称不能为空");
            Assert.hasText(gsWorkspace.getUri(), "工作区URI不能为空");

            // 2. 创建 WorkspaceInfo 实例（通过 Catalog 工厂）
            WorkspaceInfo workspace = catalog.getFactory().createWorkspace();

            // 4. 关联 NamespaceInfo（工作区必须绑定命名空间）
            NamespaceInfo namespace = createOrGetNamespace(gsWorkspace);
            workspace.setName(namespace.getPrefix());
            workspace.setIsolated(namespace.isIsolated());
            WorkspaceInfo workspaceByName = catalog.getWorkspaceByName(workspace.getName());
            if (workspaceByName != null) {
                workspace = workspaceByName;
            } else {
                catalog.add(workspace);
            }
            return workspace;
        } catch (Exception e) {
            throw new ServicePublishException(
                    "GsWorkspace 转换为 WorkspaceInfo 失败：" + e.getMessage(), e);
        }
    }

    /**
     * 基于 GsWorkspace 更新已有 WorkspaceInfo 的属性
     *
     * @param existingWorkspace 已存在的 WorkspaceInfo
     * @param gsWorkspace 自定义工作区对象（新属性）
     * @return 更新后的 WorkspaceInfo
     */
    public WorkspaceInfo updateWorkspaceInfo(
            WorkspaceInfo existingWorkspace, GsWorkspace gsWorkspace) {
        Assert.notNull(existingWorkspace, "已有 WorkspaceInfo 不能为空");
        Assert.notNull(gsWorkspace, "GsWorkspace 不能为空");

        // 更新名称（可选：如需修改名称，需确保不重复）
        if (org.apache.commons.lang3.StringUtils.isNotBlank(gsWorkspace.getName())) {
            existingWorkspace.setName(gsWorkspace.getName());
        }

        // 更新命名空间URI
        if (org.apache.commons.lang3.StringUtils.isNotBlank(gsWorkspace.getUri())) {}

        return existingWorkspace;
    }

    /** 创建或获取与 GsWorkspace 匹配的 NamespaceInfo （工作区和命名空间是一一对应的核心关系） */
    public NamespaceInfo createOrGetNamespace(GsWorkspace gsWorkspace) {
        // 先尝试根据前缀（工作区名称）获取已有命名空间
        NamespaceInfo namespace = catalog.getNamespaceByPrefix(gsWorkspace.getName());
        if (namespace != null) {
            // 更新URI（如果不一致）
            if (!gsWorkspace.getUri().equals(namespace.getURI())) {
                namespace.setURI(gsWorkspace.getUri());
                catalog.save(namespace);
            }
            return namespace;
        }

        // 不存在则创建新命名空间
        namespace = catalog.getFactory().createNamespace();
        namespace.setPrefix(gsWorkspace.getName()); // 命名空间前缀 = 工作区名称（GeoServer 规范）
        namespace.setURI(gsWorkspace.getUri());
        catalog.add(namespace); // 存入 Catalog

        return namespace;
    }
}
