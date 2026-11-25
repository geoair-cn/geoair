package cn.geoair.comp.jdbc.url.impl;

import cn.geoair.comp.jdbc.url.JdbcUrlCodec;
import cn.geoair.comp.jdbc.url.JdbcUrlDialect;
import cn.geoair.comp.jdbc.url.beans.JdbcEndpoint;
import cn.geoair.comp.jdbc.url.beans.JdbcUrl;
import cn.geoair.comp.jdbc.url.beans.JdbcUrlProperty;
import cn.geoair.comp.jdbc.url.enums.DatabaseType;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * JDBC URL 的默认编解码器。
 *
 * <p>URL 解析通过独立的 {@link JdbcUrlDialect} 实现完成。未知驱动仅作为不透明 URL 返回， 不会尝试重组其私有语法；这样可防止改 schema
 * 等操作意外破坏原有连接串。
 *
 * @author 张逢吉
 */
public class DefaultJdbcUrlCodec implements JdbcUrlCodec {
    /** 已注册方言，列表靠前的方言拥有更高匹配优先级。 */
    private final List<JdbcUrlDialect> dialects = new ArrayList<JdbcUrlDialect>();

    /** 创建包含项目内置数据库方言的编解码器。 */
    public DefaultJdbcUrlCodec() {
        this(
                Arrays.<JdbcUrlDialect>asList(
                        new NetworkJdbcUrlDialect(
                                DatabaseType.POSTGRESQL, "postgresql", "currentSchema"),
                        new NetworkJdbcUrlDialect(DatabaseType.MYSQL, "mysql", null),
                        new NetworkJdbcUrlDialect(DatabaseType.DM, "dm", null),
                        new SapHanaJdbcUrlDialect(),
                        new OracleJdbcUrlDialect(),
                        new SqlServerJdbcUrlDialect(),
                        new LocalJdbcUrlDialect(DatabaseType.H2, "h2", "schema"),
                        new LocalJdbcUrlDialect(DatabaseType.SQLITE, "sqlite", null),
                        new PhoenixJdbcUrlDialect()));
    }

    /**
     * 使用指定方言创建编解码器，便于业务模块注册自定义数据库。
     *
     * @param dialects 方言列表，按列表顺序匹配
     */
    public DefaultJdbcUrlCodec(List<JdbcUrlDialect> dialects) {
        this.dialects.addAll(dialects);
    }

    /**
     * 注册自定义方言。后注册的方言优先匹配，用于覆盖内置驱动实现。
     *
     * @param dialect 自定义 JDBC URL 方言
     * @return 当前编解码器
     */
    public DefaultJdbcUrlCodec register(JdbcUrlDialect dialect) {
        if (dialect == null) {
            throw new IllegalArgumentException("JDBC URL 方言不能为空");
        }
        dialects.add(0, dialect);
        return this;
    }

    @Override
    public JdbcUrl parse(String jdbcUrl) {
        requireJdbcUrl(jdbcUrl);
        for (JdbcUrlDialect dialect : dialects) {
            if (dialect.supports(jdbcUrl)) {
                return dialect.parse(jdbcUrl);
            }
        }
        return opaque(jdbcUrl);
    }

    @Override
    public JdbcUrl create(
            DatabaseType databaseType, String host, Integer port, String databaseName) {
        JdbcUrlDialect dialect = findDialect(databaseType);
        if (dialect == null) {
            throw new IllegalArgumentException("不支持构建 JDBC URL 的数据库类型：" + databaseType);
        }
        return dialect.create(host, port, databaseName);
    }

    /** 按当前 URL 的参数风格进行渲染：查询参数使用 {@code ?/&}，SQL Server/H2 使用 {@code ;}。 */
    @Override
    public String format(JdbcUrl jdbcUrl) {
        StringBuilder result = new StringBuilder(jdbcUrl.getCoreUrl());
        if (jdbcUrl.getProperties().isEmpty()) {
            return result.toString();
        }
        JdbcUrl.PropertyStyle style = jdbcUrl.getPropertyStyle();
        if (style == JdbcUrl.PropertyStyle.NONE) {
            throw new IllegalArgumentException("该 JDBC URL 不支持参数区：" + jdbcUrl.getRawUrl());
        }
        result.append(style == JdbcUrl.PropertyStyle.QUERY ? '?' : ';');
        for (int i = 0; i < jdbcUrl.getProperties().size(); i++) {
            if (i > 0) {
                result.append(style == JdbcUrl.PropertyStyle.QUERY ? '&' : ';');
            }
            result.append(jdbcUrl.getProperties().get(i).render());
        }
        return result.toString();
    }

    @Override
    public JdbcUrl withProperty(JdbcUrl jdbcUrl, String name, String value) {
        if (JdbcUrlDialectSupport.isBlank(name)) {
            throw new IllegalArgumentException("JDBC URL 参数名不能为空");
        }
        return replaceProperty(
                jdbcUrl,
                name,
                value,
                true,
                jdbcUrl.getPropertyStyle() == JdbcUrl.PropertyStyle.NONE
                        ? defaultPropertyStyle(jdbcUrl)
                        : jdbcUrl.getPropertyStyle());
    }

    @Override
    public String withoutProperties(String jdbcUrl) {
        return parse(jdbcUrl).getCoreUrl();
    }

    /** 按对应数据库方言重写当前 schema 参数。没有 schema 参数语义的数据库会明确拒绝该操作。 */
    @Override
    public String rewriteSchema(String jdbcUrl, String schema) {
        if (JdbcUrlDialectSupport.isBlank(schema)) {
            return jdbcUrl;
        }
        JdbcUrl parsed = parse(jdbcUrl);
        JdbcUrlDialect dialect = findDialect(parsed.getDatabaseType());
        String schemaProperty = dialect == null ? null : dialect.getSchemaPropertyName();
        if (schemaProperty == null) {
            throw new UnsupportedOperationException(
                    "JDBC URL 不支持当前 schema 参数：" + parsed.getDatabaseType());
        }
        JdbcUrl rewritten =
                replaceProperty(
                        parsed,
                        schemaProperty,
                        encode(schema),
                        true,
                        parsed.getPropertyStyle() == JdbcUrl.PropertyStyle.NONE
                                ? defaultPropertyStyle(parsed)
                                : parsed.getPropertyStyle());
        return format(rewritten);
    }

    @Override
    public String getSchema(String jdbcUrl) {
        JdbcUrl parsed = parse(jdbcUrl);
        JdbcUrlDialect dialect = findDialect(parsed.getDatabaseType());
        if (dialect == null || dialect.getSchemaPropertyName() == null) {
            return null;
        }
        String value = parsed.getProperty(dialect.getSchemaPropertyName());
        return value == null ? null : decode(value);
    }

    private JdbcUrl replaceProperty(
            JdbcUrl jdbcUrl,
            String name,
            String value,
            boolean hasEquals,
            JdbcUrl.PropertyStyle propertyStyle) {
        List<JdbcUrlProperty> properties = new ArrayList<JdbcUrlProperty>();
        boolean replaced = false;
        for (JdbcUrlProperty property : jdbcUrl.getProperties()) {
            if (property.getName().equalsIgnoreCase(name)) {
                // 同名参数只保留一个，避免驱动对“第一个/最后一个生效”的差异。
                if (!replaced) {
                    properties.add(new JdbcUrlProperty(name, value, hasEquals));
                    replaced = true;
                }
            } else {
                properties.add(property);
            }
        }
        if (!replaced) {
            properties.add(new JdbcUrlProperty(name, value, hasEquals));
        }
        return jdbcUrl.withProperties(properties, propertyStyle);
    }

    private JdbcUrl.PropertyStyle defaultPropertyStyle(JdbcUrl jdbcUrl) {
        return jdbcUrl.getDatabaseType() == DatabaseType.SQLSERVER
                        || jdbcUrl.getDatabaseType() == DatabaseType.H2
                ? JdbcUrl.PropertyStyle.SEMICOLON
                : JdbcUrl.PropertyStyle.QUERY;
    }

    private JdbcUrlDialect findDialect(DatabaseType databaseType) {
        for (JdbcUrlDialect dialect : dialects) {
            if (dialect.getDatabaseType() == databaseType) {
                return dialect;
            }
        }
        return null;
    }

    private static JdbcUrl opaque(String jdbcUrl) {
        int end = jdbcUrl.indexOf(':', 5);
        String driverName = end < 0 ? "" : jdbcUrl.substring(5, end);
        return new JdbcUrl(
                jdbcUrl,
                DatabaseType.UNKNOWN,
                driverName,
                null,
                jdbcUrl,
                Collections.<JdbcEndpoint>emptyList(),
                null,
                JdbcUrl.PropertyStyle.NONE,
                Collections.<JdbcUrlProperty>emptyList());
    }

    private static void requireJdbcUrl(String jdbcUrl) {
        if (JdbcUrlDialectSupport.isBlank(jdbcUrl)
                || !jdbcUrl.regionMatches(true, 0, "jdbc:", 0, 5)) {
            throw new IllegalArgumentException("无效的 JDBC URL：必须以 jdbc: 开头");
        }
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
