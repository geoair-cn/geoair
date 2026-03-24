package cn.geoair.comp.db.service.core.controller;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.druid.pool.DruidPooledConnection;
import com.alibaba.fastjson2.JSONObject;

import cn.geoair.base.api.annotation.GaApi;
import cn.geoair.base.api.annotation.GaApiAction;
import cn.geoair.comp.db.service.core.basic.apo.DataSourceApo;
import cn.geoair.comp.db.service.core.basic.util.JdbcUtil;
import cn.geoair.comp.db.service.core.basic.util.PoolManager;
import cn.geoair.comp.db.service.core.dao.GirDsDataSourceDao;
import cn.geoair.comp.db.service.core.utils.TokenManager;
import cn.geoair.map.dynamic.adv.GirAdvQuery;
import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.geoair.map.dynamic.adv.query.apo.SchemaTableApo;
import cn.geoair.map.dynamic.adv.query.enums.AdvSchemaTableTypeOpt;

import lombok.extern.slf4j.Slf4j;

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
	public List<String> getAllTables(String sourceId) throws SQLException {
		TokenManager.validateToken();
		DataSourceApo dataSourceApo = girDsDataSourceDao.getById(sourceId);
		IAdvExecutor iAdvExecutor = GirAdvQuery.getIAdvExecutor(PoolManager.getJdbcConnectionPool(dataSourceApo),
				dataSourceApo.getUrl());
		List<SchemaTableApo> schemaTableApos = iAdvExecutor.dGetTableAndViewBySchema();
		List<String> tablesBySchema = schemaTableApos.stream().filter(
				s -> s.getType().equals(AdvSchemaTableTypeOpt.表) || s.getType().equals(AdvSchemaTableTypeOpt.视图))
				.map(SchemaTableApo::getName).collect(Collectors.toList());
		return tablesBySchema;
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

	@RequestMapping("/getAllColumnsLabels")
	@GaApiAction(text = "获取表的所有列")
	public List<String> getAllColumnsLabels(String sourceId, String table) throws SQLException {
		TokenManager.validateToken();
		DataSourceApo dataSourceApo = girDsDataSourceDao.getById(sourceId);
		DruidPooledConnection connection = PoolManager.getPooledConnection(dataSourceApo);
		List<JSONObject> columns = JdbcUtil.getRDBMSColumnProperties(connection, dataSourceApo.getType(), table);
		List<String> labels = null;
		if (columns != null) {
			labels = columns.stream().map(c -> c.getString("label")).collect(Collectors.toList());
		}
		return labels;
	}

	// @RequestMapping("/getAllTables")
	// @GaApiAction(text = "获取所有的表")
	// public List<JSONObject> getAllTables(String sourceId) throws SQLException {
	// TokenManager.validateToken();
	// DataSourceApo dataSourceApo = girDsDataSourceDao.getById(sourceId);
	// DruidPooledConnection connection = PoolManager.getPooledConnection(dataSourceApo);
	// List<String> tables = JdbcUtil.getAllTables(connection,
	// dataSourceApo.getTableSql());
	// List<JSONObject> list = tables.stream().map(t -> {
	// JSONObject jo = new JSONObject();
	// jo.put("label", t);
	// try {
	// DruidPooledConnection conn = PoolManager.getPooledConnection(dataSourceApo);
	// jo.put("columns", JdbcUtil.getRDBMSColumnProperties(conn, dataSourceApo.getType(),
	// t));
	// }
	// catch (SQLException e) {
	// e.printStackTrace();
	// }
	// // jo.put("columns",);
	// jo.put("showColumns", false);
	// return jo;
	// }).collect(Collectors.toList());
	// return list;
	// }
	//
	// @RequestMapping("/getAllColumns")
	// @GaApiAction(text = "获取表的所有列")
	// public List<JSONObject> getAllTables(String sourceId, String table) throws
	// SQLException {
	// TokenManager.validateToken();
	// DataSourceApo dataSourceApo = girDsDataSourceDao.getById(sourceId);
	// DruidPooledConnection connection = PoolManager.getPooledConnection(dataSourceApo);
	// List<JSONObject> columns = JdbcUtil.getRDBMSColumnProperties(connection,
	// dataSourceApo.getType(), table);
	// return columns;
	// }

}
