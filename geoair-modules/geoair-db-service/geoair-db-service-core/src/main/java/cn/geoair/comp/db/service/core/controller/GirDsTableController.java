package cn.geoair.comp.db.service.core.controller;

import cn.geoair.base.api.annotation.GaApi;
import cn.geoair.base.api.annotation.GaApiAction;
import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.comp.db.service.core.basic.apo.DsDataSourceApo;
import cn.geoair.comp.db.service.core.basic.util.JdbcUtil;
import cn.geoair.comp.db.service.core.basic.util.PoolManager;
import cn.geoair.comp.db.service.core.dao.GirDsDataSourceDao;
import cn.geoair.comp.db.service.core.utils.TokenManager;
import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.geoair.map.dynamic.adv.query.apo.DataFieldsApo;
import cn.geoair.map.dynamic.adv.query.apo.SchemaTableApo;
import cn.geoair.map.dynamic.adv.query.enums.AdvSchemaTableTypeOpt;
import com.alibaba.fastjson2.JSONObject;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @program: dbApi
 * @description:
 * @author: 武汉刘德华
 * @create: 2021-04-01 15:11
 */
@RestController
@RequestMapping("/ds_api/table")
@GaApi(tags = "GirDs表相关的接口")
public class GirDsTableController {
    public static GiLogger log = GirLoggerFactory.getLogger();
    @Resource GirDsDataSourceDao girDsDataSourceDao;

    @RequestMapping("/getAllTables")
    @GaApiAction(text = "获取所有的表")
    public List<String> getAllTables(String sourceId) throws SQLException {
        TokenManager.validateToken();
        DsDataSourceApo dsDataSourceApo = girDsDataSourceDao.getById(sourceId);
        IAdvExecutor iAdvExecutor = PoolManager.getIAdvExecutor(dsDataSourceApo);
        List<SchemaTableApo> schemaTableApos = iAdvExecutor.dGetTableAndViewBySchema();
        List<String> tablesBySchema =
                schemaTableApos
                        .stream()
                        .filter(
                                s ->
                                        s.getType().equals(AdvSchemaTableTypeOpt.表)
                                                || s.getType().equals(AdvSchemaTableTypeOpt.视图))
                        .map(SchemaTableApo::getName)
                        .collect(Collectors.toList());
        return tablesBySchema;
    }

    @RequestMapping("/getAllColumns")
    @GaApiAction(text = "获取表的所有列")
    public List<JSONObject> getAllTables(String sourceId, String table) throws SQLException {
        TokenManager.validateToken();
        DsDataSourceApo dsDataSourceApo = girDsDataSourceDao.getById(sourceId);
        DataFieldsApo dataFieldsApo =
                PoolManager.getIAdvExecutor(dsDataSourceApo).dGetColumnsBySQLOrTable(table);
        List<JSONObject> columns = JdbcUtil.getRDBMSColumnProperties(dataFieldsApo);
        return columns;
    }

    @RequestMapping("/getAllColumnsLabels")
    @GaApiAction(text = "获取表的所有列")
    public List<String> getAllColumnsLabels(String sourceId, String table) throws SQLException {
        TokenManager.validateToken();
        DsDataSourceApo dsDataSourceApo = girDsDataSourceDao.getById(sourceId);
        DataFieldsApo dataFieldsApo =
                PoolManager.getIAdvExecutor(dsDataSourceApo).dGetColumnsBySQLOrTable(table);
        List<JSONObject> columns = JdbcUtil.getRDBMSColumnProperties(dataFieldsApo);
        List<String> labels = null;
        if (columns != null) {
            labels = columns.stream().map(c -> c.getString("label")).collect(Collectors.toList());
        }
        return labels;
    }
}
