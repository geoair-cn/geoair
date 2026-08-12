package cn.geoair.map.dynamic.adv.spring;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.hutool.core.io.IoUtil;
import cn.hutool.db.dialect.DialectName;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import javax.sql.DataSource;


/** 高级查询执行器工厂，根据数据源类型自动创建对应执行器 */

public class AdvExecutorFactory {
    public static GiLogger log = GirLoggerFactory.getLogger();


    public static IAdvExecutor getAdvExecutorByDataSource(DataSource dataSource) {
        return getAdvExecutorByDataSource(dataSource, null);
    }

    public static IAdvExecutor getAdvExecutorByDataSource(
            DataSource dataSource, String dataSourceName) {

        DialectName dbType = getDbTypeFromDataSource(dataSource);
        log.trace("检测到{}数据源，创建对应Executor执行器", dbType);
        return createByDialect(dbType, dataSource, dataSourceName);
    }

    /**
     * 根据方言名称直接创建执行器（跳过 JDBC 连接探测，性能更高）
     *
     * <p>适用于调用方已经明确知道数据库类型的场景，避免 {@link #getAdvExecutorByDataSource(DataSource)}
     * 中通过 {@code DatabaseMetaData.getDatabaseProductName()} 探测数据库类型带来的额外连接开销。</p>
     *
     * @param dialectName    数据库方言
     * @param dataSource     数据源对象
     * @param dataSourceName 数据源名称（可为 null）
     * @return 匹配的 IAdvExecutor 实现类
     */
    public static IAdvExecutor getAdvExecutorByDialect(
            DialectName dialectName, DataSource dataSource, String dataSourceName) {

        if (dialectName == null) {
            throw new IllegalArgumentException("dialectName 不能为空，请指定数据库方言");
        }
        return createByDialect(dialectName, dataSource, dataSourceName);
    }

    /**
     * 根据方言创建对应的 Executor 实例（所有创建路径的统一出口）
     */
    private static IAdvExecutor createByDialect(
            DialectName dialect, DataSource dataSource, String dataSourceName) {
        switch (dialect) {
            case MYSQL:
                return GirSpringMysqlAdvExecutor.newInstance(dataSource, dataSourceName);
            case POSTGRESQL:
                return GirSpringPGAdvExecutor.newInstance(dataSource, dataSourceName);
            case ORACLE:
                return GirSpringOracleAdvExecutor.newInstance(dataSource, dataSourceName);
            case DM:
                return GirSpringDmAdvExecutor.newInstance(dataSource, dataSourceName);
            default:
                throw new UnsupportedOperationException("不支持的数据库方言：" + dialect);
        }
    }

    /**
     * 从数据源中解析数据库类型（通过JDBC元数据）
     *
     * @param dataSource Spring容器中的数据源
     * @return 数据库类型枚举
     */
    private static DialectName getDbTypeFromDataSource(DataSource dataSource) {
        Connection conn = null;
        try {
            // 获取数据库连接（Spring工具类，自动处理事务）
            conn = dataSource.getConnection();
            DatabaseMetaData metaData = conn.getMetaData();
            // 获取数据库产品名称（标准化）
            String dbProductName = metaData.getDatabaseProductName().toUpperCase();

            // 匹配数据库类型
            if (dbProductName.contains("MYSQL")) {
                return DialectName.MYSQL;
            } else if (dbProductName.contains("POSTGRESQL") || dbProductName.contains("PG")) {
                return DialectName.POSTGRESQL;
            } else if (dbProductName.contains("ORACLE")) {
                return DialectName.ORACLE;
            } else if (dbProductName.contains("DAMENG") || dbProductName.equals("DM") || dbProductName.contains("DM DBMS")) {
                return DialectName.DM;
            } else {
                throw new UnsupportedOperationException("无法识别的数据库类型：" + dbProductName);
            }
        } catch (SQLException e) {
            log.error("解析数据源类型失败", e);
            throw new RuntimeException("获取数据库类型失败", e);
        } finally {
            // 释放连接（Spring工具类，避免连接泄露）
            if (conn != null) {
                IoUtil.close(conn);
            }
        }
    }
}
