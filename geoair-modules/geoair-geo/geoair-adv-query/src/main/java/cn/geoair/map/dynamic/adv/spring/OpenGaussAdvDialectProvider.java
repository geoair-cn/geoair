package cn.geoair.map.dynamic.adv.spring;

/**
 * openGauss 的 PostgreSQL 兼容方言提供者。
 * <p>空间能力取决于部署是否启用兼容扩展；基础 SQL、分页和 PostgreSQL 兼容 DDL 可复用 PG 执行器。
 */
public class OpenGaussAdvDialectProvider extends PostgresqlAdvDialectProvider {

    public OpenGaussAdvDialectProvider() {
        // GaussDB 既可能指 openGauss 兼容实例，也可能是其他产品线；不能仅凭宽泛名称自动套用 PG 方言。
        super("opengauss", "OPENGAUSS");
    }
}
