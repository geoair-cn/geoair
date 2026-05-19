package cn.geoair.map.dynamic.adv.spring;

import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.hutool.core.io.IoUtil;
import cn.hutool.db.dialect.DialectName;
import cn.hutool.extra.spring.SpringUtil;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;

/** 高级查询执行器工厂，根据数据源类型自动创建对应执行器 */
@Slf4j
public class AdvExecutorFactory {

    /**
     * 根据数据源类型获取对应的执行器实例
     *
     * @return 匹配的IAdvExecutor实现类
     */
    public static IAdvExecutor getAdvExecutorByDataSource() {
        DataSource dataSource = SpringUtil.getBean(DataSource.class);
        return getAdvExecutorByDataSource(dataSource);
    }

    public static IAdvExecutor getAdvExecutorByDataSource(DataSource dataSource) {
        return getAdvExecutorByDataSource(dataSource, null);
    }

    public static IAdvExecutor getAdvExecutorByDataSource(
            DataSource dataSource, String dataSourceName) {

        DialectName dbType = getDbTypeFromDataSource(dataSource);

        switch (dbType) {
            case MYSQL:
                log.trace("检测到MySQL数据源，创建GirSpringMysqlAdvExecutor执行器");
                return GirSpringMysqlAdvExecutor.newInstance(dataSource, dataSourceName);
            case POSTGRESQL:
                log.trace("检测到PostgreSQL数据源，创建GirSpringPGAdvExecutor执行器");
                return GirSpringPGAdvExecutor.newInstance(dataSource, dataSourceName);
            case ORACLE:
                log.trace("检测到ORACLE数据源，创建GirSpringOracleAdvExecutor执行器");
                return GirSpringOracleAdvExecutor.newInstance(dataSource, dataSourceName);
            default:
                throw new UnsupportedOperationException("不支持的数据库类型：" + dbType);
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
