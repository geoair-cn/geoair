package cn.geoair.map.dynamic.geoserver.impl;

import cn.geoair.map.dynamic.geoserver.api.WorkspaceManager;
import cn.geoair.map.dynamic.geoserver.beans.GsWorkspace;
import cn.geoair.map.dynamic.geoserver.exception.ServicePublishException;
import cn.geoair.map.dynamic.geoserver.utils.WorkspaceConvertUtil;

import org.apache.commons.lang3.StringUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/** WorkspaceManager 接口的实现类 基于改造后的 WorkspaceConvertUtil 实现工作区/命名空间管理逻辑 */
@Component
public class WorkspaceManagerImpl implements WorkspaceManager {

	@Autowired
	private Catalog catalog;

	@Autowired
	private WorkspaceConvertUtil workspaceConvertUtil;

	/** 创建或获取工作区（核心方法） 适配改造后的 WorkspaceConvertUtil 逻辑 */
	@Override
	public WorkspaceInfo getOrCreateWorkspace(String workspaceName, String namespaceUri) {
		try {
			// 1. 参数校验
			Assert.hasText(workspaceName, "工作区名称不能为空");
			Assert.hasText(namespaceUri, "命名空间URI不能为空");

			// 2. 封装为自定义 GsWorkspace 对象
			GsWorkspace gsWorkspace = new GsWorkspace();
			gsWorkspace.setName(workspaceName);
			gsWorkspace.setUri(namespaceUri);

			// 3. 调用改造后的工具类转换（内部已处理“存在则返回、不存在则创建”逻辑）
			WorkspaceInfo workspaceInfo = workspaceConvertUtil.convertToWorkspaceInfo(gsWorkspace);

			// 4. 确保命名空间与工作区关联（补充工具类未覆盖的关联逻辑）
			NamespaceInfo namespace = catalog.getNamespaceByPrefix(workspaceName);
			if (namespace != null) {
				catalog.save(workspaceInfo);
			}

			return workspaceInfo;
		}
		catch (Exception e) {
			throw new ServicePublishException("创建/获取工作区 [" + workspaceName + "] 失败：" + e.getMessage(), e);
		}
	}

	/** 根据名称获取工作区 */
	@Override
	public WorkspaceInfo getWorkspace(String workspaceName) {
		if (StringUtils.isBlank(workspaceName)) {
			return null;
		}
		// 直接通过 Catalog 获取，GeoServer 内置缓存，性能最优
		return catalog.getWorkspaceByName(workspaceName);
	}

	/** 删除工作区（需先清理关联资源） */
	@Override
	public void deleteWorkspace(String workspaceName) {
		try {
			// 1. 参数校验
			Assert.hasText(workspaceName, "工作区名称不能为空");

			// 2. 获取工作区
			WorkspaceInfo workspace = getWorkspace(workspaceName);
			if (workspace == null) {
				return; // 工作区不存在，直接返回
			}

			// 3. 检查是否有关联的数据源/图层（必须先删除，否则 GeoServer 会拒绝删除）
			if (!catalog.getDataStoresByWorkspace(workspace).isEmpty()) {
				throw new ServicePublishException("工作区 [" + workspaceName + "] 下存在数据源，无法删除，请先删除关联数据源");
			}

			// 4. 删除工作区（先删除命名空间，再删除工作区）
			NamespaceInfo namespace = catalog.getNamespace(workspaceName);
			if (namespace != null) {
				catalog.remove(namespace);
			}
			catalog.remove(workspace);

		}
		catch (ServicePublishException e) {
			throw e; // 抛出业务异常
		}
		catch (Exception e) {
			throw new ServicePublishException("删除工作区 [" + workspaceName + "] 失败：" + e.getMessage(), e);
		}
	}

	/** 创建命名空间（关联工作区） */
	@Override
	public NamespaceInfo createNamespace(WorkspaceInfo workspace, String namespacePrefix, String namespaceUri) {
		try {
			// 1. 参数校验
			Assert.notNull(workspace, "工作区不能为空");
			Assert.hasText(namespacePrefix, "命名空间前缀不能为空");
			Assert.hasText(namespaceUri, "命名空间URI不能为空");

			// 2. 封装为 GsWorkspace，复用工具类的创建/获取逻辑
			GsWorkspace gsWorkspace = new GsWorkspace();
			gsWorkspace.setName(namespacePrefix);
			gsWorkspace.setUri(namespaceUri);

			return workspaceConvertUtil.createOrGetNamespace(gsWorkspace);
		}
		catch (Exception e) {
			throw new ServicePublishException("创建命名空间 [" + namespacePrefix + "] 失败：" + e.getMessage(), e);
		}
	}

}
