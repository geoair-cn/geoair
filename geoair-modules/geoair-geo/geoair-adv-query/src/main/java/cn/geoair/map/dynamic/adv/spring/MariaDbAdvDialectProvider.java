package cn.geoair.map.dynamic.adv.spring;

/**
 * MariaDB 的 MySQL 兼容方言提供者。
 * <p>基础 SQL 与 MySQL 方言复用；空间函数与索引能力由运行中的 MariaDB 版本决定。
 */
public class MariaDbAdvDialectProvider extends MysqlAdvDialectProvider {

    public MariaDbAdvDialectProvider() {
        super("mariadb", "MARIADB");
    }
}
