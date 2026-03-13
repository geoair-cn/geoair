package cn.geoair.map.dynamic.dbservice.core.basic.service;

import cn.geoair.map.dynamic.dbservice.core.DsApiUserInfoHelper;
import cn.geoair.map.dynamic.dbservice.core.basic.apo.ApiConfigApo;
import cn.geoair.map.dynamic.dbservice.core.basic.apo.DataSourceApo;
import cn.geoair.map.dynamic.dbservice.core.basic.util.DESUtils;
import cn.geoair.map.dynamic.dbservice.core.basic.util.PoolManager;
import cn.geoair.map.dynamic.dbservice.core.basic.util.UUIDUtil;
import cn.geoair.map.dynamic.dbservice.core.common.ResponseDto;
import cn.geoair.map.dynamic.dbservice.core.dao.GirDsDataSourceDao;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.cache.CacheManager;
// import org.springframework.cache.annotation.CacheEvict;
// import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;

/**
 * 数据源服务类
 *
 * <p>
 * 提供数据源的增删改查操作，包括： - 数据源的新增和批量插入 - 数据源的更新（支持密码加密） - 数据源的删除（检查是否被API使用） - 数据源的详情查询和缓存 -
 * 所有数据源的列表查询
 *
 * <p>
 * 使用Spring缓存管理器进行缓存控制，支持事务管理
 *
 * @author: zhangfengji
 * @create: 2021-01-20 10:43
 */
@Service
@Slf4j
public class DsDataSourceService {

	// /** Spring缓存管理器 */
	// @Autowired
	// CacheManager cacheManager;

	/** 数据源DAO */
	@Resource
	GirDsDataSourceDao girDsDataSourceDao;

	/** API配置服务 */
	@Autowired
	DsApiConfigService dsApiConfigService;

	/** 用户信息助手 */
	@Resource
	DsApiUserInfoHelper dsApiUserInfoHelper;

	/**
	 * 新增数据源
	 *
	 * <p>
	 * 自动生成UUID作为主键，设置创建时间和更新时间， 对密码进行DES加密后保存到数据库
	 * @param dataSourceApo 待新增的数据源对象
	 */
	@Transactional
	public void add(DataSourceApo dataSourceApo) {
		dataSourceApo.setId(UUIDUtil.id());
		dataSourceApo.setUpdateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
		dataSourceApo.setCreateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
		// 新增数据源对密码加密
		try {
			dataSourceApo.setPassword(DESUtils.encrypt(dataSourceApo.getPassword()));
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		// DbApiDataSourcePo po = dataSource.toPo();
		// po.initCreateMeta();
		// po.setNameCreate(dbApiUserInfoHelper.getSubjectName());
		dataSourceApo.setCreateUserName(dsApiUserInfoHelper.getSubjectName());
		girDsDataSourceDao.accessSelective(dataSourceApo);
	}

	/**
	 * 更新数据源
	 *
	 * <p>
	 * 设置更新时间，如果密码被修改则重新加密， 更新数据库记录并清除相关缓存
	 * @param dataSourceApo 待更新的数据源对象
	 */
	// @CacheEvict(value = "datasource", key = "#dataSourceApo.id")
	@Transactional
	public void update(DataSourceApo dataSourceApo) {
		dataSourceApo.setUpdateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
		// 如果修改了密码, 需要对密码加密
		if (dataSourceApo.isEdit_password()) {
			try {
				dataSourceApo.setPassword(DESUtils.encrypt(dataSourceApo.getPassword()));
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		// DbApiDataSourcePo po = dataSource.toPo();
		// po.initUpdateMeta();
		// po.setNameUpdate(dbApiUserInfoHelper.getSubjectName());
		girDsDataSourceDao.updateSelectiveById(dataSourceApo);
		PoolManager.removeJdbcConnectionPool(dataSourceApo.getId());
		// cacheManager.getCache("datasource").evictIfPresent(dataSourceApo.getId());
	}

	/**
	 * 删除数据源
	 *
	 * <p>
	 * 删除前检查该数据源是否被任何API配置使用， 如果被使用则返回错误信息，否则执行删除操作 同时清除连接池和缓存
	 * @param id 待删除的数据源ID
	 * @return 删除结果响应，成功或失败信息
	 */
	@Transactional
	public ResponseDto delete(String id) {
		List<ApiConfigApo> list = dsApiConfigService.getAll();
		List<String> str = list.stream().filter(t -> {
			String task = t.getTask();
			JSONArray array = JSON.parseArray(task);
			for (int i = 0; i < array.size(); i++) {
				JSONObject jo = array.getJSONObject(i);
				String datasourceId = jo.getString("datasourceId");
				if (id.equals(datasourceId)) {
					return true;
				}
			}
			return false;
		}).map(item -> item.getName() + "(" + item.getId() + ")").collect(Collectors.toList());

		if (str.isEmpty()) {
			girDsDataSourceDao.deleteByPK(id);
			PoolManager.removeJdbcConnectionPool(id);
			// cacheManager.getCache("datasource").evictIfPresent(id);

			return ResponseDto.successWithMsg("Datasource delete success");
		}
		else {
			return ResponseDto.fail("Can not delete! Used by API: " + str.stream().collect(Collectors.joining(";")));
		}
	}

	/**
	 * 查询数据源详情
	 *
	 * <p>
	 * 根据ID查询数据源详情，结果会被缓存
	 * @param id 数据源ID
	 * @return 数据源对象，如果未找到则返回null
	 */
	// @Cacheable(value = "datasource", key = "#id", unless = "#result == null")
	public DataSourceApo detail(String id) {
		return girDsDataSourceDao.getById(id);
	}

	/**
	 * 获取所有数据源列表
	 *
	 * <p>
	 * 按更新时间倒序排列所有数据源
	 * @return 数据源列表，按更新时间倒序排列
	 */
	public List<DataSourceApo> getAll() {
		// List<DbApiDataSourcePo> dbApiDataSourcePos = dbApiDataSourceDao.gtcSearchAll();
		// List<DbApiDataSourcePo> collect =
		// dbApiDataSourcePos.stream()
		//
		// .sorted(Comparator.comparing(DbApiDataSourcePo::getTimeUpdate).reversed())
		// .collect(Collectors.toList());
		return girDsDataSourceDao.searchAll();
	}

	/**
	 * 批量查询数据源
	 *
	 * <p>
	 * 根据ID列表批量查询数据源
	 * @param ids 数据源ID列表
	 * @return 数据源列表
	 */
	public List<DataSourceApo> selectBatch(List<String> ids) {
		// List<DbApiDataSourcePo> dbApiDataSourcePos =
		// dbApiDataSourceDao.selectBatchIds(ids);
		// return DataSource.fromPos(dbApiDataSourcePos);
		return girDsDataSourceDao.selectBatchIds(ids);
	}

	/**
	 * 批量插入数据源
	 *
	 * <p>
	 * 批量插入数据源列表，为每个数据源设置更新时间和创建元数据
	 * @param list 待插入的数据源列表
	 */
	@Transactional
	public void insertBatch(List<DataSourceApo> list) {
		list.forEach(t -> {
			t.setUpdateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
			// DbApiDataSourcePo po = t.toPo();
			// po.initCreateMeta();
			t.setCreateUserName(dsApiUserInfoHelper.getSubjectName());
			girDsDataSourceDao.accessSelective(t);
		});
	}

}
