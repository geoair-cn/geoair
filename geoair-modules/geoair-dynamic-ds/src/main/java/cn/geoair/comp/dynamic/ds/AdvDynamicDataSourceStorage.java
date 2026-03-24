package cn.geoair.comp.dynamic.ds;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import cn.geoair.base.Gir;
import cn.geoair.comp.dynamic.ds.apo.DataSourceApo;
import cn.geoair.comp.dynamic.ds.datasource.AdvDataSourceWrapper;

import cn.hutool.core.util.ObjectUtil;

/**
 * 动态数据源的存储器实现类 实现动态数据源的管理功能，包括添加、获取、移除和缓存清空等操作
 */

public class AdvDynamicDataSourceStorage implements DynamicDataSourceManager {

	static DynamicDataSourceManager dataSourceManager;

	IAdvDataSourceHelper iAdvDataSourceHelper;

	/**
	 * 全局只有一个存储器实例
	 * @return
	 */
	public static DynamicDataSourceManager getInstance() {
		if (dataSourceManager == null) {
			dataSourceManager = new AdvDynamicDataSourceStorage();
		}
		return dataSourceManager;
	}

	private AdvDynamicDataSourceStorage() {
		iAdvDataSourceHelper = Gir.beans.getBean(IAdvDataSourceHelper.class);
	}

	// 数据源映射
	private final Map<String, AdvDataSourceWrapper> dataSourceMap = new ConcurrentHashMap<>();

	@Override
	public void cleanCache() {
		Gir.log.info("执行清空数据源缓存并释放数据库链接操作！");
		if (ObjectUtil.isNotEmpty(dataSourceMap)) {
			Set<Map.Entry<String, AdvDataSourceWrapper>> entries = dataSourceMap.entrySet();
			entries.forEach(entry -> {
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
		}
		else {
			throw new RuntimeException("数据源不存在: " + dataSourceId);
		}
	}

	@Override
	public void addDataSource(DataSource dataSource, String dataSourceId) {
		// 只有当数据源不存在时才添加
		AdvDataSourceWrapper existingDataSource = dataSourceMap.get(dataSourceId);
		if (existingDataSource == null) {
			dataSourceMap.put(dataSourceId, AdvDataSourceWrapper.wrap(dataSource));
			Gir.log.debug("已添加数据源: {}", dataSourceId);
		}
		else {
			Gir.log.debug("数据源已存在，不执行添加操作: {}", dataSourceId);
		}
	}

	/**
	 * 这里由于只有postgresql，故这里简化
	 * @param dataSource 数据源APO对象
	 * @return 创建的Druid数据源
	 */
	@Override
	public AdvDataSourceWrapper getDataSourceByDataSourceApo(DataSourceApo dataSource) {
		return AdvDataSourceWrapper.wrap(iAdvDataSourceHelper.getDbDataSourceByApo(dataSource));
	}

	@Override
	public boolean removeDataSource(String dataSourceId) {
		if (containsDataSource(dataSourceId)) {
			AdvDataSourceWrapper dataSource = dataSourceMap.remove(dataSourceId);
			// 关闭数据源，释放资源
			if (dataSource != null) {
				dataSource.close();
				Gir.log.info("已移除并关闭数据源: {}", dataSourceId);
				return true;
			}
		}
		Gir.log.debug("数据源不存在，移除失败: {}", dataSourceId);
		return false;
	}

}
