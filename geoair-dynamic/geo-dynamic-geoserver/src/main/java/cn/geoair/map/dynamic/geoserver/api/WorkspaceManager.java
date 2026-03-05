package cn.geoair.map.dynamic.geoserver.api;

import cn.geoair.map.dynamic.geoserver.exception.ServicePublishException;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;

/** GeoServer 工作区/命名空间管理接口 */
public interface WorkspaceManager {

  /**
   * 创建或获取工作区
   *
   * @param workspaceName 工作区名称
   * @param namespaceUri 命名空间URI（如 http://geoair.cn/workspace）
   * @return 工作区信息
   * @throws ServicePublishException 创建失败时抛出
   */
  WorkspaceInfo getOrCreateWorkspace(String workspaceName, String namespaceUri);

  /**
   * 根据名称获取工作区
   *
   * @param workspaceName 工作区名称
   * @return 工作区信息（不存在返回null）
   */
  WorkspaceInfo getWorkspace(String workspaceName);

  /**
   * 删除工作区（需先删除关联的数据源/图层）
   *
   * @param workspaceName 工作区名称
   * @throws ServicePublishException 删除失败时抛出
   */
  void deleteWorkspace(String workspaceName);

  /**
   * 创建命名空间（关联工作区）
   *
   * @param workspace 工作区
   * @param namespacePrefix 命名空间前缀
   * @param namespaceUri 命名空间URI
   * @return 命名空间信息
   */
  NamespaceInfo createNamespace(
      WorkspaceInfo workspace, String namespacePrefix, String namespaceUri);
}
