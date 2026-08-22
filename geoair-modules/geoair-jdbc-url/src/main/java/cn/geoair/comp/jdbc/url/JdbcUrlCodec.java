package cn.geoair.comp.jdbc.url;

import cn.geoair.comp.jdbc.url.beans.JdbcUrl;
import cn.geoair.comp.jdbc.url.enums.DatabaseType;

/**
 * JDBC URL 的统一解析、构造及语义化重写入口。
 *
 * <p>调用方通过本接口处理 JDBC URL，不应自行按 {@code ?}、{@code ;} 或字符串下标拆分连接串。
 * 具体语法由 {@link JdbcUrlDialect} 实现；无法识别的驱动会返回 {@link DatabaseType#UNKNOWN}
 * 的不透明 URL，以避免重写操作破坏驱动私有格式。</p>
 *
 * @author 张逢吉
 */
public interface JdbcUrlCodec {

    /**
     * 将 JDBC URL 解析为结构化对象。
     *
     * <p>已注册方言会提取端点、数据库名与参数；未知驱动保留原始 URL，不会猜测其内部结构。</p>
     *
     * @param jdbcUrl 原始 JDBC URL，必须以 {@code jdbc:} 开头
     * @return 解析后的不可变 JDBC URL 对象
     * @throws IllegalArgumentException 当 URL 为空或不以 {@code jdbc:} 开头时抛出
     */
    JdbcUrl parse(String jdbcUrl);

    /**
     * 按指定数据库类型构建一个不含业务参数的 JDBC URL。
     *
     * <p>数据库类型决定 URL 主体格式，例如 PostgreSQL 使用路径数据库名，SQL Server 使用
     * {@code databaseName} 分号参数。需要追加普通驱动参数时请使用 {@link #withProperty}。</p>
     *
     * @param databaseType 数据库类型，必须已注册对应方言
     * @param host 数据库主机；本地文件型数据库可为 null
     * @param port 数据库端口；驱动允许省略端口时可为 null
     * @param databaseName 数据库名、服务名、SID 或本地文件路径
     * @return 新构造的结构化 JDBC URL 对象
     * @throws IllegalArgumentException 当类型不支持构建，或网络型数据库缺少 host 时抛出
     */
    JdbcUrl create(DatabaseType databaseType, String host, Integer port, String databaseName);

    /**
     * 将结构化 JDBC URL 渲染为连接字符串。
     *
     * <p>参数按照原有顺序输出，并使用该 URL 的参数风格：查询参数使用 {@code ?/&}，
     * SQL Server 和 H2 等使用 {@code ;}。</p>
     *
     * @param jdbcUrl 需要渲染的 JDBC URL 对象
     * @return JDBC 连接字符串
     * @throws IllegalArgumentException 当 URL 含参数但未声明参数分隔风格时抛出
     */
    String format(JdbcUrl jdbcUrl);

    /**
     * 新增或替换一个普通驱动参数。
     *
     * <p>该方法不修改传入对象，而是返回新对象；同名参数会合并为一个，保留第一次出现的位置。</p>
     *
     * @param jdbcUrl 原始结构化 JDBC URL
     * @param name 参数名
     * @param value 参数值；可为 null 以表达无值开关参数
     * @return 参数更新后的 JDBC URL 对象
     * @throws IllegalArgumentException 当参数名为空时抛出
     */
    JdbcUrl withProperty(JdbcUrl jdbcUrl, String name, String value);

    /**
     * 删除 JDBC URL 的全部连接参数，仅保留连接主体。
     *
     * @param jdbcUrl 原始 JDBC URL
     * @return 不包含查询参数或分号参数的 JDBC URL
     */
    String withoutProperties(String jdbcUrl);

    /**
     * 使用数据库方言定义的参数名重写当前 schema。
     *
     * <p>例如 PostgreSQL 使用 {@code currentSchema}、Oracle 使用 {@code defaultSchema}、
     * SQL Server 使用 {@code schemaName}、H2 使用 {@code schema}。不支持该语义的驱动会拒绝操作。</p>
     *
     * @param jdbcUrl 原始 JDBC URL
     * @param schema 要设置的 schema；为空时原样返回 URL
     * @return schema 更新后的 JDBC URL
     * @throws UnsupportedOperationException 当该数据库没有 URL schema 参数语义时抛出
     */
    String rewriteSchema(String jdbcUrl, String schema);

    /**
     * 读取 JDBC URL 中由当前方言定义的 schema 参数。
     *
     * @param jdbcUrl 原始 JDBC URL
     * @return 已解码的 schema；未配置或数据库不支持时返回 null
     */
    String getSchema(String jdbcUrl);
}
