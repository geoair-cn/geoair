package cn.geoair.comp.db.service.core.basic.executor;

import cn.geoair.base.data.page.GiPageParam;
import cn.geoair.base.data.page.GiPager;
import cn.geoair.base.data.page.support.GirPager;
import cn.geoair.comp.db.service.core.basic.apo.DataSourceApo;
import cn.geoair.comp.db.service.core.basic.dto.ApiSqlDto;
import cn.geoair.comp.db.service.core.basic.dto.SQLTaskDto;
import cn.geoair.comp.db.service.core.basic.service.DsDataSourceService;
import cn.geoair.comp.db.service.core.basic.util.JdbcUtil;
import cn.geoair.comp.db.service.core.basic.util.PoolManager;
import cn.geoair.comp.db.service.core.basic.util.SqlEngineUtil;
import cn.geoair.comp.db.service.core.dialect.BaseDialect;
import cn.geoair.map.dynamic.adv.mybatis.SqlMeta;
import com.alibaba.druid.pool.DruidPooledConnection;
import com.alibaba.fastjson2.JSONObject;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GirDsSQLExecutor implements Executor {

    @Autowired DsDataSourceService dsDataSourceService;

    @Override
    public Object execute(JSONObject taskJson, Map<String, Object> sqlParam) throws Exception {

        SQLTaskDto task = taskJson.toJavaObject(SQLTaskDto.class);

        DataSourceApo datasource = dsDataSourceService.detail(task.getDatasourceId());
        if (datasource == null) {
            throw new RuntimeException("Datasource not exists!");
        }
        List<ApiSqlDto> sqlList = task.getSqlList();
        DruidPooledConnection connection = PoolManager.getPooledConnection(datasource);

        List<Object> dataList = executeSql(connection, task, sqlParam, datasource);

        // 执行数据转换
        for (int i = 0; i < sqlList.size(); i++) {
            ApiSqlDto apiSql = sqlList.get(i);
            Object data = dataList.get(i);
            // 如果此单条sql是查询类sql，并且配置了数据转换插件
            if (data instanceof Iterable && StringUtils.isNotBlank(apiSql.getTransformPlugin())) {
                log.debug("transform plugin execute");
                List<JSONObject> sourceData = (List<JSONObject>) (data);
                Object resData = sourceData;
                dataList.set(i, resData);
            }
        }
        // 如果只有单条sql,返回结果不是数组格式
        return dataList.size() == 1 ? dataList.get(0) : dataList;
    }

    public List<Object> executeSql(
            Connection connection,
            List<ApiSqlDto> sqlList,
            Map<String, Object> sqlParam,
            boolean flag) {
        List<Object> dataList = new ArrayList<>();
        try {
            if (flag) connection.setAutoCommit(false);
            else connection.setAutoCommit(true);
            for (ApiSqlDto apiSql : sqlList) {
                SqlMeta sqlMeta = SqlEngineUtil.getEngine().parse(apiSql.getSqlText(), sqlParam);
                Object data =
                        JdbcUtil.executeSql(
                                connection, sqlMeta.getSql(), sqlMeta.getJdbcParamValues(), flag);
                dataList.add(data);
            }
            if (flag) connection.commit();
            return dataList;
        } catch (Exception e) {
            try {
                if (flag) connection.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            throw new RuntimeException(e);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public List<Object> executeSql(
            Connection connection,
            SQLTaskDto task,
            Map<String, Object> sqlParam,
            DataSourceApo datasource) {
        List<Object> dataList = new ArrayList<>();
        List<ApiSqlDto> sqlList = task.getSqlList();
        GiPageParam giPageParam = null;
        if (task.pageIs()) {
            giPageParam = GiPageParam.of();
        }
        try {
            if (task.transactionIs()) connection.setAutoCommit(false);
            else connection.setAutoCommit(true);
            for (ApiSqlDto apiSql : sqlList) {
                SqlMeta sqlMeta = null;
                try {
                    sqlMeta = SqlEngineUtil.getEngine().parse(apiSql.getSqlText(), sqlParam);
                } catch (RuntimeException runtimeException) {
                    String message = runtimeException.getMessage();
                    if (message.contains("could not found value")) {
                        String prefix = "could not found value : "; // 固定前缀
                        // 计算前缀长度，从前缀结束的位置开始截取
                        int prefixLength = prefix.length();
                        // 确保原字符串包含前缀，避免索引越界
                        String result = message.substring(prefixLength);
                        throw new RuntimeException("无法找到必填参数！" + result);
                    } else {
                        throw runtimeException;
                    }
                }

                if (task.pageIs()) {
                    int pageSize = giPageParam.pageSize();
                    long startRow = giPageParam.startRow();
                    BaseDialect instance = BaseDialect.getInstance(datasource.getDriver());
                    String countSql = instance.getCountSql(sqlMeta.getSql());
                    Long count =
                            JdbcUtil.executeSqlCount(
                                    connection, countSql, sqlMeta.getJdbcParamValues());
                    String sql =
                            instance.getPageSql(sqlMeta.getSql(), giPageParam.pageNum(), pageSize);
                    List data =
                            (List)
                                    JdbcUtil.executeSql(
                                            connection,
                                            sql,
                                            sqlMeta.getJdbcParamValues(),
                                            task.humpIs());
                    GiPager<List> pager = new GirPager<>();
                    pager.put(data, count, giPageParam);
                    dataList.add(pager);
                } else {
                    Object data =
                            JdbcUtil.executeSql(
                                    connection,
                                    sqlMeta.getSql(),
                                    sqlMeta.getJdbcParamValues(),
                                    task.humpIs());
                    dataList.add(data);
                }
            }
            if (task.transactionIs()) connection.commit();
            return dataList;
        } catch (Exception e) {
            try {
                if (task.transactionIs()) connection.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            throw new RuntimeException(e);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
