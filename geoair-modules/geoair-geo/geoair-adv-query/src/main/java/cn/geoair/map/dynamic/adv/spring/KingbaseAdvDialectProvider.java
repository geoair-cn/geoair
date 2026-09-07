package cn.geoair.map.dynamic.adv.spring;

/**
 * KingbaseES 的 PostgreSQL/PostGIS 兼容方言提供者。
 * <p>仅在目标实例启用了 PostgreSQL/PostGIS 兼容语法时使用；不将其伪装为原生 SQL Server 或 MySQL 方言。
 */
public class KingbaseAdvDialectProvider extends PostgresqlAdvDialectProvider {

    public KingbaseAdvDialectProvider() {
        super("kingbase", "KINGBASE", "KINGBASEES");
    }
}
