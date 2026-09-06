package cn.geoair.map.dynamic.adv.spring;

import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.hutool.db.dialect.DialectName;

import javax.sql.DataSource;

/**
 * AdvQuery 数据库方言提供者。
 * <p>
 * 一个提供者负责描述可识别的 JDBC 产品名，并创建与该数据库语法、DDL 与空间能力匹配的
 * {@link IAdvExecutor}。应用或扩展模块可通过
 * {@link AdvExecutorFactory#registerProvider(AdvDialectProvider)} 注册新的原生方言，
 * 无需修改工厂的核心代码。
 *
 * @author 张逢吉
 */
public interface AdvDialectProvider {

    /** 返回全局唯一且稳定的方言标识，例如 {@code postgresql}、{@code kingbase}。 */
    String getDialectId();

    /** 返回与当前方言对应的 Hutool 方言；Hutool 未定义时可返回 {@code null}。 */
    DialectName getDialectName();

    /** 根据 JDBC {@code DatabaseMetaData#getDatabaseProductName()} 的值判断是否适用。 */
    boolean supportsProductName(String databaseProductName);

    /** 创建已初始化的数据源执行器。 */
    IAdvExecutor create(DataSource dataSource, String dataSourceName);
}
