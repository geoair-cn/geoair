package cn.geoair.map.dynamic.file.jdbc.link;

import cn.geoair.map.dynamic.file.core.link.LinkInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * JDBC 空间数据源的公共连接参数。
 *
 * @author 张逢吉
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
public abstract class JdbcGeoLinkInfo extends LinkInfo {

    /** JDBC 地址。 */
    protected String jdbcUrl;

    /** 数据库用户名。 */
    protected String username;

    /** 数据库密码。 */
    protected String password;

    /** Schema；Oracle 中通常为用户对应的 schema。 */
    protected String schema;

    /** 几何数据的默认 SRID。 */
    protected int srid = 4326;

    /** JDBC 连接超时，单位毫秒；用于写入端创建连接池。 */
    protected int connectTimeoutMillis = 30000;

    /** 返回当前数据库 JDBC Driver 的完整类名。 */
    protected abstract String getDriverClassName();

    /** 打开一个仅用于探测或元数据读取的连接。 */
    public Connection openConnection() throws Exception {
        Class.forName(getDriverClassName());
        // Oracle、MySQL、PostgreSQL 对连接属性的名称并不完全一致，探测连接只传递通用凭据。
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    /** 校验公共连接字段并验证数据库可连接。 */
    protected void validateConnection() {
        if (isBlank(jdbcUrl) || isBlank(username) || password == null) {
            throw new IllegalArgumentException("jdbcUrl、username 和 password 不能为空");
        }
        try (Connection ignored = openConnection()) {
            // 连接成功即通过。
        } catch (Exception e) {
            throw new IllegalArgumentException("无法连接空间数据库：" + e.getMessage(), e);
        }
    }

    protected boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
