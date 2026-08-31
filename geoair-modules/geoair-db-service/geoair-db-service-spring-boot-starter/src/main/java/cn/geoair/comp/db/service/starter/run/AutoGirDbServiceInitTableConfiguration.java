package cn.geoair.comp.db.service.starter.run;

import cn.geoair.base.Gir;
import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.geoair.map.dynamic.adv.query.dialect.dm.AdvExecutorDm;
import cn.geoair.map.dynamic.adv.spring.EnableGirAdvDynamic;
import cn.hutool.core.util.StrUtil;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author ：张逢吉 @description： 自动装配模块（按数据库方言初始化表）
 */
@Configuration
@EnableGirAdvDynamic
public class AutoGirDbServiceInitTableConfiguration {

    @Bean
    @ConditionalOnBean(IAdvExecutor.class)
    public Integer initTableByGirSpringAdvExecutor(IAdvExecutor iAdvExecutor) {
        Gir.log.debug("开始初始化表...");

        if (!iAdvExecutor.dIsTableExists("dsapi_datasource")) {
            initDsapiDatasource(iAdvExecutor);
        }
        if (!iAdvExecutor.dIsTableExists("dsapi_group")) {
            initDsapiGroup(iAdvExecutor);
        }
        if (!iAdvExecutor.dIsTableExists("dsapi_config")) {
            initDsapiConfig(iAdvExecutor);
        }

        Gir.log.debug("初始化表完成...");
        return 0;
    }

    private void initDsapiDatasource(IAdvExecutor iAdvExecutor) {
        executeCreateAndComments(
                iAdvExecutor,
                "dsapi_datasource",
                buildCreateDsapiDatasourceSql(iAdvExecutor),
                buildOwnerSql(iAdvExecutor, "dsapi_datasource"),
                new String[][] {
                    {"id", "名称"},
                    {"name", "名称"},
                    {"note", "备注信息"},
                    {"type", "类型"},
                    {"url", "jdbcUrl"},
                    {"username", "用户名"},
                    {"password", "密码"},
                    {"driver", "驱动名称"},
                    {"table_sql", "创建或编辑API的时候，选择数据源，会执行此sql来获取该数据源下的所有表名称"},
                    {"create_user_id", "创建人"},
                    {"create_time", "创建时间"},
                    {"update_time", "更新时间"},
                    {"del_is", "是否删除标识"},
                    {"time_create", "创建时间"},
                    {"time_update", "更新时间"},
                    {"name_create", "创建人名称"},
                    {"name_update", "更新人名称"}
                },
                "数据源信息");
    }

    private void initDsapiGroup(IAdvExecutor iAdvExecutor) {
        executeCreateAndComments(
                iAdvExecutor,
                "dsapi_group",
                buildCreateDsapiGroupSql(iAdvExecutor),
                buildOwnerSql(iAdvExecutor, "dsapi_group"),
                new String[][] {
                    {"id", "主键"},
                    {"name", "分组名称"},
                    {"create_user_id", "创建人Id"},
                    {"create_time", "创建时间"},
                    {"update_time", "更新时间"},
                    {"time_create", "创建时间"},
                    {"time_update", "更新时间"},
                    {"name_create", "创建人名称"},
                    {"name_update", "更新人名称"}
                },
                "api分组信息");
    }

    private void initDsapiConfig(IAdvExecutor iAdvExecutor) {
        executeCreateAndComments(
                iAdvExecutor,
                "dsapi_config",
                buildCreateDsapiConfigSql(iAdvExecutor),
                buildOwnerSql(iAdvExecutor, "dsapi_config"),
                new String[][] {
                    {"id", "主键"},
                    {"name", "名称"},
                    {"note", "备注"},
                    {"path", "接口定义的路径"},
                    {"params", "参数信息"},
                    {"json_param", "入参信息"},
                    {"status", "状态，停用与否"},
                    {"access", "api类型"},
                    {"group_id", "分组Id"},
                    {"content_type", "请求头"},
                    {"task", "该任务信息"},
                    {"create_user_id", "创建人Id"},
                    {"create_time", "创建时间"},
                    {"update_time", "更新时间"},
                    {"del_is", "是否删除标识"},
                    {"time_create", "创建时间"},
                    {"time_update", "更新时间"},
                    {"name_create", "创建人名称"},
                    {"name_update", "更新人名称"}
                },
                "api配置信息");
    }

    private void executeCreateAndComments(
            IAdvExecutor iAdvExecutor,
            String tableName,
            String createSql,
            String ownerSql,
            String[][] columnComments,
            String tableComment) {
        iAdvExecutor.dExecuteDDL(createSql, tableName, "create");
        if (StrUtil.isNotEmpty(ownerSql)) {
            iAdvExecutor.dExecuteDDL(ownerSql, tableName, "owner");
        }
        for (String[] columnComment : columnComments) {
            iAdvExecutor.dExecuteDDL(
                    buildCommentOnColumnSql(tableName, columnComment[0], columnComment[1]),
                    tableName,
                    "comment column");
        }
        iAdvExecutor.dExecuteDDL(
                buildCommentOnTableSql(tableName, tableComment), tableName, "comment table");
    }

    private String buildCreateDsapiDatasourceSql(IAdvExecutor iAdvExecutor) {
        if (isDmExecutor(iAdvExecutor)) {
            return String.join(
                    "\n",
                    "CREATE TABLE \"dsapi_datasource\" (",
                    "    \"id\" VARCHAR2(255) NOT NULL,",
                    "    \"name\" VARCHAR2(255),",
                    "    \"note\" CLOB,",
                    "    \"type\" VARCHAR2(255),",
                    "    \"url\" VARCHAR2(1000),",
                    "    \"username\" VARCHAR2(255),",
                    "    \"password\" VARCHAR2(255),",
                    "    \"driver\" VARCHAR2(255),",
                    "    \"table_sql\" CLOB,",
                    "    \"create_user_id\" NUMBER(10),",
                    "    \"create_time\" VARCHAR2(255),",
                    "    \"update_time\" VARCHAR2(255),",
                    "    \"del_is\" VARCHAR2(255),",
                    "    \"time_create\" TIMESTAMP(6),",
                    "    \"time_update\" TIMESTAMP(6),",
                    "    \"name_create\" VARCHAR2(255),",
                    "    \"name_update\" VARCHAR2(255),",
                    "    CONSTRAINT \"ds_datasource_pkey\" PRIMARY KEY (\"id\")",
                    ")");
        }
        return String.join(
                "\n",
                "CREATE TABLE \"dsapi_datasource\" (",
                "    \"id\" text COLLATE \"pg_catalog\".\"default\" NOT NULL,",
                "    \"name\" text COLLATE \"pg_catalog\".\"default\",",
                "    \"note\" text COLLATE \"pg_catalog\".\"default\",",
                "    \"type\" text COLLATE \"pg_catalog\".\"default\",",
                "    \"url\" text COLLATE \"pg_catalog\".\"default\",",
                "    \"username\" text COLLATE \"pg_catalog\".\"default\",",
                "    \"password\" text COLLATE \"pg_catalog\".\"default\",",
                "    \"driver\" text COLLATE \"pg_catalog\".\"default\",",
                "    \"table_sql\" text COLLATE \"pg_catalog\".\"default\",",
                "    \"create_user_id\" int4,",
                "    \"create_time\" text COLLATE \"pg_catalog\".\"default\",",
                "    \"update_time\" text COLLATE \"pg_catalog\".\"default\",",
                "    \"del_is\" varchar(255) COLLATE \"pg_catalog\".\"default\",",
                "    \"time_create\" timestamp(6),",
                "    \"time_update\" timestamp(6),",
                "    \"name_create\" text COLLATE \"pg_catalog\".\"default\",",
                "    \"name_update\" text COLLATE \"pg_catalog\".\"default\",",
                "    CONSTRAINT \"ds_datasource_pkey\" PRIMARY KEY (\"id\")",
                ")");
    }

    private String buildCreateDsapiGroupSql(IAdvExecutor iAdvExecutor) {
        if (isDmExecutor(iAdvExecutor)) {
            return String.join(
                    "\n",
                    "CREATE TABLE \"dsapi_group\" (",
                    "    \"id\" VARCHAR2(255) NOT NULL,",
                    "    \"name\" VARCHAR2(255) NOT NULL,",
                    "    \"create_user_id\" NUMBER(10),",
                    "    \"create_time\" VARCHAR2(255),",
                    "    \"update_time\" VARCHAR2(255),",
                    "    \"time_create\" TIMESTAMP(6),",
                    "    \"time_update\" TIMESTAMP(6),",
                    "    \"name_create\" VARCHAR2(255),",
                    "    \"name_update\" VARCHAR2(255),",
                    "    CONSTRAINT \"ds_api_group_pkey\" PRIMARY KEY (\"id\")",
                    ")");
        }
        return String.join(
                "\n",
                "CREATE TABLE \"dsapi_group\" (",
                "    \"id\" text COLLATE \"pg_catalog\".\"default\" NOT NULL,",
                "    \"name\" text COLLATE \"pg_catalog\".\"default\" NOT NULL,",
                "    \"create_user_id\" int4,",
                "    \"create_time\" text COLLATE \"pg_catalog\".\"default\",",
                "    \"update_time\" text COLLATE \"pg_catalog\".\"default\",",
                "    \"time_create\" timestamp(6),",
                "    \"time_update\" timestamp(6),",
                "    \"name_create\" text COLLATE \"pg_catalog\".\"default\",",
                "    \"name_update\" text COLLATE \"pg_catalog\".\"default\",",
                "    CONSTRAINT \"ds_api_group_pkey\" PRIMARY KEY (\"id\")",
                ")");
    }

    private String buildCreateDsapiConfigSql(IAdvExecutor iAdvExecutor) {
        if (isDmExecutor(iAdvExecutor)) {
            return String.join(
                    "\n",
                    "CREATE TABLE \"dsapi_config\" (",
                    "    \"id\" VARCHAR2(255) NOT NULL,",
                    "    \"name\" VARCHAR2(255),",
                    "    \"note\" CLOB,",
                    "    \"path\" VARCHAR2(1000) NOT NULL,",
                    "    \"params\" CLOB,",
                    "    \"json_param\" CLOB,",
                    "    \"status\" NUMBER(10),",
                    "    \"access\" NUMBER(10),",
                    "    \"group_id\" VARCHAR2(255),",
                    "    \"content_type\" VARCHAR2(255),",
                    "    \"task\" CLOB,",
                    "    \"create_user_id\" VARCHAR2(255),",
                    "    \"create_time\" VARCHAR2(255),",
                    "    \"update_time\" VARCHAR2(255),",
                    "    \"del_is\" VARCHAR2(255),",
                    "    \"time_create\" TIMESTAMP(6),",
                    "    \"time_update\" TIMESTAMP(6),",
                    "    \"name_create\" VARCHAR2(255),",
                    "    \"name_update\" VARCHAR2(255),",
                    "    CONSTRAINT \"ds_api_config_pkey\" PRIMARY KEY (\"id\")",
                    ")");
        }
        return String.join(
                "\n",
                "CREATE TABLE \"dsapi_config\" (",
                "    \"id\" text COLLATE \"pg_catalog\".\"default\" NOT NULL,",
                "    \"name\" text COLLATE \"pg_catalog\".\"default\",",
                "    \"note\" text COLLATE \"pg_catalog\".\"default\",",
                "    \"path\" text COLLATE \"pg_catalog\".\"default\" NOT NULL,",
                "    \"params\" text COLLATE \"pg_catalog\".\"default\",",
                "    \"json_param\" text COLLATE \"pg_catalog\".\"default\",",
                "    \"status\" int4,",
                "    \"access\" int4,",
                "    \"group_id\" text COLLATE \"pg_catalog\".\"default\",",
                "    \"content_type\" text COLLATE \"pg_catalog\".\"default\",",
                "    \"task\" text COLLATE \"pg_catalog\".\"default\",",
                "    \"create_user_id\" text COLLATE \"pg_catalog\".\"default\",",
                "    \"create_time\" text COLLATE \"pg_catalog\".\"default\",",
                "    \"update_time\" text COLLATE \"pg_catalog\".\"default\",",
                "    \"del_is\" varchar(255) COLLATE \"pg_catalog\".\"default\",",
                "    \"time_create\" timestamp(6),",
                "    \"time_update\" timestamp(6),",
                "    \"name_create\" text COLLATE \"pg_catalog\".\"default\",",
                "    \"name_update\" text COLLATE \"pg_catalog\".\"default\",",
                "    CONSTRAINT \"ds_api_config_pkey\" PRIMARY KEY (\"id\")",
                ")");
    }

    private String buildOwnerSql(IAdvExecutor iAdvExecutor, String tableName) {
        if (isDmExecutor(iAdvExecutor)) {
            return null;
        }
        return "ALTER TABLE " + quoteIdentifier(tableName) + " OWNER TO \"postgres\"";
    }

    private String buildCommentOnColumnSql(String tableName, String columnName, String comment) {
        return "COMMENT ON COLUMN "
                + quoteIdentifier(tableName)
                + "."
                + quoteIdentifier(columnName)
                + " IS '"
                + escapeSql(comment)
                + "'";
    }

    private String buildCommentOnTableSql(String tableName, String comment) {
        return "COMMENT ON TABLE "
                + quoteIdentifier(tableName)
                + " IS '"
                + escapeSql(comment)
                + "'";
    }

    private String quoteIdentifier(String name) {
        return "\"" + name + "\"";
    }

    private String escapeSql(String value) {
        return value == null ? "" : value.replace("'", "''");
    }

    private boolean isDmExecutor(IAdvExecutor iAdvExecutor) {
        return iAdvExecutor instanceof AdvExecutorDm;
    }
}
