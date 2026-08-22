package cn.geoair.comp.jdbc.url.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.geoair.comp.jdbc.url.JdbcUrlDialect;
import cn.geoair.comp.jdbc.url.enums.DatabaseType;
import lombok.Data;

/**
 * JDBC URL 的无损结构化表示。
 *
 * <p>coreUrl 保留原始连接主体；参数按出现顺序保存，因此重写单个参数时不会改变其它参数。</p>
 *
 * @author 张逢吉
 */
@Data
public final class JdbcUrl implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum PropertyStyle {
        NONE, QUERY, SEMICOLON
    }

    /** 调用方传入的原始 JDBC URL。 */
    private final String rawUrl;
    /** 识别出的数据库类型。 */
    private final DatabaseType databaseType;
    /** URL 中 jdbc: 后的驱动协议名。 */
    private final String driverName;
    /** Oracle thin/oci 等子协议；其它数据库为 null。 */
    private final String subProtocol;
    /** 移除属性区后的 JDBC URL 主体。 */
    private final String coreUrl;
    /** 网络型 URL 的所有端点，顺序与 URL 一致。 */
    private final List<JdbcEndpoint> endpoints;
    /** URL 表示的数据库、SID、服务名或本地文件名。 */
    private final String databaseName;
    /** 属性区所使用的分隔语法。 */
    private final PropertyStyle propertyStyle;
    /** 保持原始顺序的连接参数列表。 */
    private final List<JdbcUrlProperty> properties;

    /**
     * 创建结构化 JDBC URL。供自定义 {@link JdbcUrlDialect} 实现构造解析结果使用。
     */
    public JdbcUrl(String rawUrl, DatabaseType databaseType, String driverName, String subProtocol,
            String coreUrl, List<JdbcEndpoint> endpoints, String databaseName,
            PropertyStyle propertyStyle, List<JdbcUrlProperty> properties) {
        this.rawUrl = rawUrl;
        this.databaseType = databaseType;
        this.driverName = driverName;
        this.subProtocol = subProtocol;
        this.coreUrl = coreUrl;
        this.endpoints = Collections.unmodifiableList(new ArrayList<JdbcEndpoint>(endpoints));
        this.databaseName = databaseName;
        this.propertyStyle = propertyStyle;
        this.properties = Collections.unmodifiableList(new ArrayList<JdbcUrlProperty>(properties));
    }

    public JdbcEndpoint getPrimaryEndpoint() {
        return endpoints.isEmpty() ? null : endpoints.get(0);
    }

    public String getProperty(String name) {
        for (JdbcUrlProperty property : properties) {
            if (property.getName().equalsIgnoreCase(name)) {
                return property.getValue();
            }
        }
        return null;
    }

    /**
     * 创建仅参数区不同的新 URL 对象，供编解码器执行不可变重写。
     */
    public JdbcUrl withProperties(List<JdbcUrlProperty> newProperties, PropertyStyle newPropertyStyle) {
        return new JdbcUrl(rawUrl, databaseType, driverName, subProtocol, coreUrl, endpoints,
                databaseName, newPropertyStyle, newProperties);
    }
}
