package cn.geoair.comp.db.service.core.basic.executor;

import cn.geoair.base.data.page.GiPageParam;
import cn.geoair.base.data.page.GiPager;
import cn.geoair.base.data.page.support.GirPager;
import cn.geoair.comp.db.service.core.basic.apo.DsDataSourceApo;
import cn.geoair.comp.db.service.core.basic.dto.ApiSqlDto;
import cn.geoair.comp.db.service.core.basic.dto.SQLTaskDto;
import cn.geoair.comp.db.service.core.basic.service.DsDataSourceService;
import cn.geoair.comp.db.service.core.basic.util.JdbcUtil;
import cn.geoair.comp.db.service.core.basic.util.PoolManager;
import cn.geoair.comp.db.service.core.basic.util.SqlEngineUtil;
import cn.geoair.comp.db.service.core.dialect.BaseDialect;
import cn.geoair.comp.dynamic.ds.tx.TxAction;
import cn.geoair.comp.dynamic.ds.tx.TxActionNp;
import cn.geoair.map.dynamic.adv.mybatis.SqlMeta;
import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamList;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
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

    @Autowired
    DsDataSourceService dsDataSourceService;

    @Override
    public Object execute(JSONObject taskJson, Map<String, Object> sqlParam) throws Exception {
        SQLTaskDto task = taskJson.toJavaObject(SQLTaskDto.class);
        DsDataSourceApo datasource = dsDataSourceService.detail(task.getDatasourceId());
        if (datasource == null) {
            throw new RuntimeException("Datasource not exists!");
        }
        List<ApiSqlDto> sqlList = task.getSqlList();
        List<Object> dataList = executeSql(task, sqlParam, datasource);
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
            SQLTaskDto task,
            Map<String, Object> sqlParam,
            DsDataSourceApo datasource) {
        try {
            IAdvExecutor iAdvExecutor = PoolManager.getIAdvExecutor(datasource);
            final List<Object>[] dataList = new List[]{new ArrayList<>()};
            if (task.transactionIs()) {
                iAdvExecutor.tx(new TxActionNp() {
                    @Override
                    public void run() {
                        dataList[0] = getObjects(task, sqlParam, iAdvExecutor);
                    }
                });
                return dataList[0];
            } else {
                return getObjects(task, sqlParam, iAdvExecutor);
            }
        } catch (Exception e) {

            throw new RuntimeException(e);
        }
    }

    private static List<Object> getObjects(SQLTaskDto task, Map<String, Object> sqlParam, IAdvExecutor iAdvExecutor) {
        List<Object> dataList = new ArrayList<>();
        List<ApiSqlDto> sqlList = task.getSqlList();
        GiPageParam giPageParam = null;
        if (task.pageIs()) {
            giPageParam = GiPageParam.of();
        }
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
                Number number = iAdvExecutor.bSelectRecordRowCount(sqlMeta.getSql(), SqlParamList.of(sqlMeta.getJdbcParamValues()));
                Long count = number.longValue();
                String pageSql = iAdvExecutor.tbBuildPageSql(sqlMeta.getSql(), giPageParam.pageNum(), pageSize, true);
                List<GirAdvOneRow> girAdvOneRows = iAdvExecutor.bSelectList(pageSql);
                if (task.humpIs()) {
                    girAdvOneRows = GirAdvOneRow.toCamelCaseList(girAdvOneRows);
                }
                GiPager<GirAdvOneRow> pager = new GirPager<>();
                giPageParam.setPageNumStartZero(true);
                pager.put(girAdvOneRows, count, giPageParam, true);
                dataList.add(pager);
            } else {
                List<GirAdvOneRow> girAdvOneRows = iAdvExecutor.bSelectList(sqlMeta.getSql(), SqlParamList.of(sqlMeta.getJdbcParamValues()));
                if (task.humpIs()) {
                    girAdvOneRows = GirAdvOneRow.toCamelCaseList(girAdvOneRows);
                }
                dataList.add(girAdvOneRows);
            }
        }

        return dataList;
    }
}
