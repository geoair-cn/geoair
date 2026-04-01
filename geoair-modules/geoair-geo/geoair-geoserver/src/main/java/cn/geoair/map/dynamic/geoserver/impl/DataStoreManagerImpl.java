package cn.geoair.map.dynamic.geoserver.impl;

import cn.geoair.map.dynamic.geoserver.api.DataStoreManager;
import cn.geoair.map.dynamic.geoserver.beans.BaseDatastore;
import cn.geoair.map.dynamic.geoserver.exception.ServicePublishException;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/** DataStoreManager 实现类 支持 PostGIS + SHP 数据源，复用抽象父类逻辑 */
@Component
public class DataStoreManagerImpl implements DataStoreManager {

    @Autowired private Catalog catalog;

    /** 创建或获取数据源（核心实现） */
    @Override
    public DataStoreInfo getOrCreateDataStore(
            WorkspaceInfo workspace, BaseDatastore baseDatastore) {
        try {
            // 1. 参数校验
            Assert.notNull(workspace, "工作区不能为空");
            Assert.notNull(baseDatastore, "数据源配置不能为空");
            Assert.hasText(baseDatastore.getName(), "数据源名称不能为空");
            Assert.notNull(baseDatastore.getDataSourceType(), "数据源类型不能为空");

            // 2. 检查数据源是否已存在
            DataStoreInfo existingStore =
                    catalog.getDataStoreByName(workspace, baseDatastore.getName());
            if (existingStore != null) {
                // 可选：更新已有数据源的连接参数
                existingStore.getConnectionParameters().putAll(baseDatastore.toConnectionParams());
                catalog.save(existingStore);
                return existingStore;
            }

            // 3. 创建新数据源
            DataStoreInfo newStore = catalog.getFactory().createDataStore();
            newStore.setName(baseDatastore.getName());
            newStore.setWorkspace(workspace);
            // 设置连接参数（子类实现的 toConnectionParams 方法）
            newStore.getConnectionParameters().putAll(baseDatastore.toConnectionParams());
            // 保存到 Catalog
            catalog.add(newStore);

            return newStore;
        } catch (Exception e) {
            throw new ServicePublishException(
                    "创建/获取数据源 [" + baseDatastore.getName() + "] 失败：" + e.getMessage(), e);
        }
    }

    /** 根据名称获取数据源 */
    @Override
    public DataStoreInfo getDataStore(WorkspaceInfo workspace, String dataStoreName) {
        if (workspace == null || StringUtils.isBlank(dataStoreName)) {
            return null;
        }
        return catalog.getDataStoreByName(workspace, dataStoreName);
    }

    /** 删除数据源（先检查关联图层） */
    @Override
    public void deleteDataStore(WorkspaceInfo workspace, String dataStoreName) {
        try {
            // 1. 参数校验
            Assert.notNull(workspace, "工作区不能为空");
            Assert.hasText(dataStoreName, "数据源名称不能为空");

            // 2. 获取数据源
            DataStoreInfo dataStore = getDataStore(workspace, dataStoreName);
            if (dataStore == null) {
                return;
            }

            // 3. 检查是否有关联图层（必须先删除图层）
            if (!catalog.getFeatureTypesByDataStore(dataStore).isEmpty()) {
                throw new ServicePublishException(
                        "数据源 [" + dataStoreName + "] 下存在图层，无法删除，请先删除关联图层");
            }

            // 4. 删除数据源
            catalog.remove(dataStore);
        } catch (ServicePublishException e) {
            throw e;
        } catch (Exception e) {
            throw new ServicePublishException(
                    "删除数据源 [" + dataStoreName + "] 失败：" + e.getMessage(), e);
        }
    }

    /** 验证数据源连接是否有效 */
    @Override
    public boolean validateConnection(BaseDatastore baseDatastore) {
        Assert.notNull(baseDatastore, "数据源配置不能为空");
        // 调用 GeoTools DataStoreFinder 验证连接
        Map<String, String> params = baseDatastore.toConnectionParams();
        try {
            DataStore dataStore = DataStoreFinder.getDataStore(params);
            return dataStore != null;
        } catch (Exception e) {
            // 连接失败返回 false，不抛出异常（便于上层判断）
            return false;
        }
    }
}
