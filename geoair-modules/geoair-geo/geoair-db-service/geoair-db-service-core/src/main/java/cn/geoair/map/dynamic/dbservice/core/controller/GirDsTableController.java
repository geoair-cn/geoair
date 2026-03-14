package cn.geoair.map.dynamic.dbservice.core.controller;

import cn.geoair.base.api.annotation.GaApi;
import cn.geoair.base.api.annotation.GaApiAction;
import cn.geoair.map.dynamic.dbservice.core.basic.apo.DataSourceApo;
import cn.geoair.map.dynamic.dbservice.core.basic.util.JdbcUtil;
import cn.geoair.map.dynamic.dbservice.core.basic.util.PoolManager;
import cn.geoair.map.dynamic.dbservice.core.dao.GirDsDataSourceDao;
import cn.geoair.map.dynamic.dbservice.core.utils.TokenManager;

import com.alibaba.druid.pool.DruidPooledConnection;
import com.alibaba.fastjson.JSONObject;

import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;

/**
 * @program: dbApi
 * @description:
 * @author: 武汉刘德华
 * @create: 2021-04-01 15:11
 */
@Slf4j
@RestController
@RequestMapping("/ds_api/table")
@GaApi(tags = "GirDs表相关的接口")
public class GirDsTableController {

	@Resource
	GirDsDataSourceDao girDsDataSourceDao;

	@RequestMapping("/getAllTables")
	@GaApiAction(text = "获取所有的表")
	public List<JSONObject> getAllTables(String sourceId) throws SQLException {
		TokenManager.validateToken();
		DataSourceApo dataSourceApo = girDsDataSourceDao.getById(sourceId);
		DruidPooledConnection connection = PoolManager.getPooledConnection(dataSourceApo);
		List<String> tables = JdbcUtil.getAllTables(connection, dataSourceApo.getTableSql());
		List<JSONObject> list = tables.stream().map(t -> {
			JSONObject jo = new JSONObject();
			jo.put("label", t);
			try {
				DruidPooledConnection conn = PoolManager.getPooledConnection(dataSourceApo);
				jo.put("columns", JdbcUtil.getRDBMSColumnProperties(conn, dataSourceApo.getType(), t));
			}
			catch (SQLException e) {
				e.printStackTrace();
			}
			// jo.put("columns",);
			jo.put("showColumns", false);
			return jo;
		}).collect(Collectors.toList());
		return list;
	}

	@RequestMapping("/getAllColumns")
	@GaApiAction(text = "获取表的所有列")
	public List<JSONObject> getAllTables(String sourceId, String table) throws SQLException {
		TokenManager.validateToken();
		DataSourceApo dataSourceApo = girDsDataSourceDao.getById(sourceId);
		DruidPooledConnection connection = PoolManager.getPooledConnection(dataSourceApo);
		List<JSONObject> columns = JdbcUtil.getRDBMSColumnProperties(connection, dataSourceApo.getType(), table);
		return columns;
	}

}
