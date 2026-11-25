package cn.geoair.map.dynamic.adv.dm;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.query.IAdvBaseOpt;
import cn.geoair.map.dynamic.adv.query.apo.GirSqlParam;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamMap;
import cn.geoair.map.dynamic.adv.query.dialect.dm.DmAdvDDLOpt;
import cn.geoair.map.dynamic.adv.query.dialect.dm.DmAdvSimplePageOpt;
import cn.geoair.map.dynamic.adv.query.dialect.dm.base.DmAdvBaseUpdateOpt;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandlerRegistry;
import cn.geoair.map.dynamic.adv.spring.AdvExecutorFactory;
import cn.geoair.map.dynamic.adv.spring.GirSpringDmAdvExecutor;
import cn.hutool.core.lang.Pair;
import cn.hutool.db.dialect.DialectName;
import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.junit.Assert;
import org.junit.Test;

public class DmAdaptationTest {

    @Test
    public void shouldRouteDamengDataSourceToDmExecutor() {
        for (String productName : Arrays.asList("DM", "DM DBMS", "Dameng")) {
            DataSource dataSource = buildDataSource(productName);
            Assert.assertTrue(
                    "未正确路由达梦产品名: " + productName,
                    AdvExecutorFactory.getAdvExecutorByDataSource(dataSource)
                            instanceof GirSpringDmAdvExecutor);
        }
    }

    @Test
    public void shouldBuildMergeUpsertSqlForDameng() {
        DmAdvBaseUpdateOpt updateOpt =
                new DmAdvBaseUpdateOpt(
                        AdvQueryGlobalConfig::of,
                        AdvTypeHandlerRegistry.create(DialectName.DM, null));
        updateOpt.setDataSourceGetter(buildDataSourceGetter());

        LinkedHashMap<String, Object> rowData = new LinkedHashMap<>();
        rowData.put("id", 1L);
        rowData.put("code", "A001");
        rowData.put("name", "达梦");

        Pair<String, List<Object>> upsertSql =
                updateOpt.getUpsertSql("demo_table", rowData, Arrays.asList("id", "code"));
        String sql = upsertSql.getKey();

        Assert.assertTrue(sql.contains("MERGE INTO \"demo_table\" target"));
        Assert.assertTrue(
                sql.contains(
                        "USING (SELECT ? AS \"id\", ? AS \"code\", ? AS \"name\" FROM DUAL) source"));
        Assert.assertTrue(
                sql.contains(
                        "target.\"id\" = source.\"id\" AND target.\"code\" = source.\"code\""));
        Assert.assertTrue(sql.contains("UPDATE SET target.\"name\" = source.\"name\""));
        Assert.assertTrue(
                sql.contains(
                        "WHEN NOT MATCHED THEN INSERT (\"id\",\"code\",\"name\") VALUES (source.\"id\", source.\"code\", source.\"name\")"));
        Assert.assertFalse(sql.contains("VALUES(\"name\")"));
        Assert.assertEquals(new ArrayList<Object>(rowData.values()), upsertSql.getValue());
    }

    @Test
    public void shouldBuildValidUpsertWhenAllFieldsAreConflictKeys() {
        DmAdvBaseUpdateOpt updateOpt =
                new DmAdvBaseUpdateOpt(
                        AdvQueryGlobalConfig::of,
                        AdvTypeHandlerRegistry.create(DialectName.DM, null));
        updateOpt.setDataSourceGetter(buildDataSourceGetter());

        LinkedHashMap<String, Object> rowData = new LinkedHashMap<>();
        rowData.put("id", 1L);
        rowData.put("code", "A001");

        Pair<String, List<Object>> upsertSql =
                updateOpt.getUpsertSql("demo_table", rowData, Arrays.asList("id", "code"));
        String sql = upsertSql.getKey();

        Assert.assertTrue(
                sql.contains("WHEN MATCHED THEN UPDATE SET target.\"id\" = source.\"id\""));
        Assert.assertEquals(new ArrayList<Object>(rowData.values()), upsertSql.getValue());
    }

    @Test
    public void shouldUseParentPagingCountAndRnConventions() {
        AtomicReference<String> lastCountSql = new AtomicReference<>();
        IAdvBaseOpt baseOpt =
                buildBaseOpt(
                        (sql, sqlParam) -> {
                            lastCountSql.set(sql);
                            return row("count", 12L);
                        });

        TestableDmAdvSimplePageOpt pageOpt =
                new TestableDmAdvSimplePageOpt(buildDataSourceGetter(), baseOpt, null, null);

        Long count =
                pageOpt.executeCountSqlWithParamForTest(
                        "SELECT COUNT(*) AS count FROM demo", SqlParamMap.of());
        Assert.assertEquals(Long.valueOf(12L), count);
        Assert.assertEquals("SELECT COUNT(*) AS count FROM demo", lastCountSql.get());

        GirAdvOneRow record = row("rn", 3L, "name", "dm");
        List<GirAdvOneRow> records = new ArrayList<>();
        records.add(record);
        pageOpt.convertPageOriginalResults(records);
        Assert.assertFalse(records.get(0).containsKey("rn"));
        Assert.assertEquals("dm", records.get(0).getStr("name"));
    }

    @Test
    public void shouldFallbackCurrentDatabaseToSchemaWhenDbNameMissing() {
        IAdvBaseOpt baseOpt =
                buildBaseOpt(
                        (sql, sqlParam) -> {
                            if (sql.contains("SYS_CONTEXT('USERENV', 'DB_NAME')")) {
                                return row("database_name", "");
                            }
                            if (sql.contains("SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA')")) {
                                return row("schema_name", "SYSDBA");
                            }
                            return null;
                        });

        DmAdvDDLOpt ddlOpt = new DmAdvDDLOpt(buildDataSourceGetter(), baseOpt);
        Assert.assertEquals("SYSDBA", ddlOpt.dGetCurrentDataBase());
    }

    private static IDataSourceGetter buildDataSourceGetter() {
        return proxy(
                IDataSourceGetter.class,
                (method, args) -> {
                    if ("getSchemaName".equals(method.getName())) {
                        return null;
                    }
                    if ("getDatabaseName".equals(method.getName())) {
                        return null;
                    }
                    if ("getDataSourceId".equals(method.getName())) {
                        return "test-ds";
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private static IAdvBaseOpt buildBaseOpt(BaseOptHandler handler) {
        return proxy(
                IAdvBaseOpt.class,
                (method, args) -> {
                    if ("getConfig".equals(method.getName())) {
                        return AdvQueryGlobalConfig.of();
                    }
                    if ("setDataSourceGetter".equals(method.getName())) {
                        return null;
                    }
                    if ("bSelectOne".equals(method.getName())) {
                        String sql = (String) args[0];
                        GirSqlParam sqlParam =
                                args.length > 1 && args[1] instanceof GirSqlParam
                                        ? (GirSqlParam) args[1]
                                        : null;
                        return handler.handle(sql, sqlParam);
                    }
                    throw new UnsupportedOperationException(
                            "Not needed in DM adaptation tests: " + method.getName());
                });
    }

    private static DataSource buildDataSource(String productName) {
        DatabaseMetaData metaData =
                proxy(
                        DatabaseMetaData.class,
                        (method, args) -> {
                            if ("getDatabaseProductName".equals(method.getName())) {
                                return productName;
                            }
                            return defaultValue(method.getReturnType());
                        });

        Connection connection =
                proxy(
                        Connection.class,
                        (method, args) -> {
                            if ("getMetaData".equals(method.getName())) {
                                return metaData;
                            }
                            if ("close".equals(method.getName())) {
                                return null;
                            }
                            if ("isClosed".equals(method.getName())) {
                                return false;
                            }
                            return defaultValue(method.getReturnType());
                        });

        return new DataSource() {
            @Override
            public Connection getConnection() {
                return connection;
            }

            @Override
            public Connection getConnection(String username, String password) {
                return connection;
            }

            @Override
            public <T> T unwrap(Class<T> iface) throws SQLException {
                throw new SQLException("Not a wrapper");
            }

            @Override
            public boolean isWrapperFor(Class<?> iface) {
                return false;
            }

            @Override
            public PrintWriter getLogWriter() {
                return null;
            }

            @Override
            public void setLogWriter(PrintWriter out) {}

            @Override
            public void setLoginTimeout(int seconds) {}

            @Override
            public int getLoginTimeout() {
                return 0;
            }

            @Override
            public Logger getParentLogger() {
                return Logger.getGlobal();
            }
        };
    }

    private static GirAdvOneRow row(Object... keyValues) {
        Map<String, Object> data = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            data.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return GirAdvOneRow.ofByMap(data);
    }

    private static <T> T proxy(Class<T> type, InvocationHandlerAdapter handler) {
        InvocationHandler invocationHandler =
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        if ("toString".equals(method.getName())) {
                            return type.getSimpleName() + "Proxy";
                        }
                        if ("hashCode".equals(method.getName())) {
                            return System.identityHashCode(proxy);
                        }
                        if ("equals".equals(method.getName())) {
                            return proxy == args[0];
                        }
                    }
                    return handler.invoke(method, args);
                };
        return type.cast(
                Proxy.newProxyInstance(
                        type.getClassLoader(), new Class<?>[] {type}, invocationHandler));
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType == null || !returnType.isPrimitive()) {
            return null;
        }
        if (boolean.class.equals(returnType)) {
            return false;
        }
        if (char.class.equals(returnType)) {
            return '\0';
        }
        if (byte.class.equals(returnType)) {
            return (byte) 0;
        }
        if (short.class.equals(returnType)) {
            return (short) 0;
        }
        if (int.class.equals(returnType)) {
            return 0;
        }
        if (long.class.equals(returnType)) {
            return 0L;
        }
        if (float.class.equals(returnType)) {
            return 0F;
        }
        if (double.class.equals(returnType)) {
            return 0D;
        }
        return null;
    }

    @FunctionalInterface
    private interface InvocationHandlerAdapter {
        Object invoke(Method method, Object[] args) throws Throwable;
    }

    @FunctionalInterface
    private interface BaseOptHandler {
        GirAdvOneRow handle(String sql, GirSqlParam sqlParam) throws Throwable;
    }

    private static class TestableDmAdvSimplePageOpt extends DmAdvSimplePageOpt {
        TestableDmAdvSimplePageOpt(
                IDataSourceGetter dataSourceGetter,
                IAdvBaseOpt baseOpt,
                cn.geoair.map.dynamic.adv.query.IAdvGeoPreOpt advGeoPreOpt,
                cn.geoair.map.dynamic.adv.query.IAdvDDLOpt advDDLOpt) {
            super(dataSourceGetter, baseOpt, advGeoPreOpt, advDDLOpt);
        }

        Long executeCountSqlWithParamForTest(String countSql, GirSqlParam sqlParam) {
            return super.executeCountSqlWithParam(countSql, sqlParam);
        }
    }
}
