package cn.geoair.comp.dynamic.ds;

import cn.geoair.base.Gir;
import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLogger;
import cn.geoair.comp.dynamic.ds.apo.DataSourceApo;
import cn.geoair.comp.dynamic.ds.dswrapper.AdvDataSourceWrapper;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Setter;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;

/**
 * 动态数据源的存储器实现类 实现动态数据源的管理功能，包括添加、获取、移除和缓存清空等操作
 */
public class AdvDynamicDataSourceStorage implements DynamicDataSourceManager {

    private static final GiLogger log = GirLogger.getLoger();

    static DynamicDataSourceManager dataSourceManager;
    @Setter
    IAdvDataSourceHelper iAdvDataSourceHelper;

    /**
     * 全局只有一个存储器实例
     *
     * @return
     */
    public static DynamicDataSourceManager getInstance() {
        if (dataSourceManager == null) {
            dataSourceManager = new AdvDynamicDataSourceStorage();
        }
        return dataSourceManager;
    }

    public static DynamicDataSourceManager getInstance(IAdvDataSourceHelper iAdvDataSourceHelper) {
        if (dataSourceManager == null) {
            AdvDynamicDataSourceStorage advDynamicDataSourceStorage = new AdvDynamicDataSourceStorage();
            advDynamicDataSourceStorage.setIAdvDataSourceHelper(iAdvDataSourceHelper);
            dataSourceManager = advDynamicDataSourceStorage;
        }
        return dataSourceManager;
    }


    public IAdvDataSourceHelper getAdvDataSourceHelper() {
        if (iAdvDataSourceHelper == null) {
            try {
                iAdvDataSourceHelper = Gir.beans.getBean(IAdvDataSourceHelper.class);
            } catch (Exception e) {
                log.error(e, e.getMessage());
                throw new RuntimeException("无法找到 IAdvDataSourceHelper的实现类!" + e.getMessage());
            }
        }
        return iAdvDataSourceHelper;
    }

    private AdvDynamicDataSourceStorage() {

    }

    // 数据源映射
    private final Map<String, AdvDataSourceWrapper> dataSourceMap = new ConcurrentHashMap<>();

    @Override
    public void cleanCache() {
        log.info("执行清空数据源缓存并释放数据库链接操作！");
        if (ObjectUtil.isNotEmpty(dataSourceMap)) {
            Set<Map.Entry<String, AdvDataSourceWrapper>> entries = dataSourceMap.entrySet();
            entries.forEach(
                    entry -> {
                        // 关闭数据源，释放连接
                        entry.getValue().close();
                    });
            dataSourceMap.clear();
        }
    }

    @Override
    public boolean containsDataSource(String dataSourceId) {
        return dataSourceMap.containsKey(dataSourceId);
    }

    @Override
    public AdvDataSourceWrapper getDataSource(String dataSourceId) {
        if (containsDataSource(dataSourceId)) {
            return dataSourceMap.get(dataSourceId);
        } else {
            try {
                DataSourceApo dataSourceApoById = getAdvDataSourceHelper().getDataSourceApoById(dataSourceId);
                AdvDataSourceWrapper dataSourceByDataSourceApo = getDataSourceByDataSourceApo(dataSourceApoById);
                dataSourceMap.put(dataSourceId, dataSourceByDataSourceApo);
            } catch (Exception e) {
                log.error(e, e.getMessage());
                String format = StrUtil.format("无法找到数据源ID为{}的数据源 message:{}", dataSourceId, e.getMessage());
                throw new RuntimeException(format);
            }
            return dataSourceMap.get(dataSourceId);
        }
    }

    @Override
    public void addDataSource(DataSource dataSource, String dataSourceId) {
        // 只有当数据源不存在时才添加
        AdvDataSourceWrapper existingDataSource = dataSourceMap.get(dataSourceId);
        if (existingDataSource == null) {
            dataSourceMap.put(dataSourceId, AdvDataSourceWrapper.wrap(dataSource));
            log.debug("已添加数据源: {}", dataSourceId);
        } else {
            log.debug("数据源已存在，不执行添加操作: {}", dataSourceId);
        }
    }

    /**
     * 这里由于只有postgresql，故这里简化
     *
     * @param dataSource 数据源APO对象
     * @return 创建的Druid数据源
     */
    @Override
    public AdvDataSourceWrapper getDataSourceByDataSourceApo(DataSourceApo dataSource) {
        return AdvDataSourceWrapper.wrap(getAdvDataSourceHelper().getDbDataSourceByApo(dataSource));
    }

    @Override
    public boolean removeDataSource(String dataSourceId) {
        if (containsDataSource(dataSourceId)) {
            AdvDataSourceWrapper dataSource = dataSourceMap.remove(dataSourceId);
            // 关闭数据源，释放资源
            if (dataSource != null) {
                dataSource.close();
                log.info("已移除并关闭数据源: {}", dataSourceId);
                return true;
            }
        }
        log.debug("数据源不存在，移除失败: {}", dataSourceId);
        return false;
    }
}
