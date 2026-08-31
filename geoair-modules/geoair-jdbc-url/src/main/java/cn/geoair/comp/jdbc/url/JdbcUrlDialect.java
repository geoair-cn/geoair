package cn.geoair.comp.jdbc.url;

import cn.geoair.comp.jdbc.url.beans.JdbcUrl;
import cn.geoair.comp.jdbc.url.enums.DatabaseType;

/**
 * 各 JDBC 驱动 URL 语法的方言接口。
 *
 * <p>每个数据库驱动使用独立实现，禁止由一个通用正则猜测所有 URL 格式。
 *
 * @author 张逢吉
 */
public interface JdbcUrlDialect {

    /**
     * 获取此方言服务的统一数据库类型。
     *
     * <p>编解码器使用该类型在创建 URL、读取 schema 参数和选择方言时建立关联。
     *
     * @return 方言对应的数据库类型，不能为 null
     */
    DatabaseType getDatabaseType();

    /**
     * 判断此方言能否解析给定 JDBC URL。
     *
     * <p>实现应只依据稳定的协议前缀或明确特征判断，不应通过宽泛正则猜测其它驱动的 URL。
     *
     * @param jdbcUrl 已通过 {@code jdbc:} 前缀校验的 JDBC URL
     * @return 能够安全解析时返回 true，否则返回 false
     */
    boolean supports(String jdbcUrl);

    /**
     * 将符合当前方言语法的 JDBC URL 解析为结构化对象。
     *
     * <p>解析结果应保留参数顺序及参数分隔风格；对驱动私有且无法安全拆分的片段，应保留在连接主体中。
     *
     * @param jdbcUrl 当前方言支持的 JDBC URL
     * @return 结构化 JDBC URL
     * @throws IllegalArgumentException 当 URL 具有当前驱动前缀但语法不完整或非法时抛出
     */
    JdbcUrl parse(String jdbcUrl);

    /**
     * 使用当前方言的默认 URL 格式构建连接主体。
     *
     * <p>例如 Oracle 默认构建 Thin/SID 格式，SQL Server 将数据库名写入分号参数。 额外驱动参数由 {@link
     * JdbcUrlCodec#withProperty(JdbcUrl, String, String)} 追加。
     *
     * @param host 数据库主机；本地或文件型数据库允许为 null
     * @param port 数据库端口；驱动允许省略端口时可为 null
     * @param databaseName 数据库名、服务名、SID 或本地文件路径
     * @return 新创建的结构化 JDBC URL
     * @throws IllegalArgumentException 当网络型 URL 缺少 host 等必要信息时抛出
     */
    JdbcUrl create(String host, Integer port, String databaseName);

    /**
     * 获取该数据库在 JDBC URL 中表示当前 schema 的参数名称。
     *
     * <p>典型值为 PostgreSQL 的 {@code currentSchema}、Oracle 的 {@code defaultSchema}、 SQL Server 的
     * {@code schemaName} 和 H2 的 {@code schema}。
     *
     * @return schema 参数名；该数据库无此 URL 语义时返回 null
     */
    String getSchemaPropertyName();
}
