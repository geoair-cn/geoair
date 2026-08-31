package cn.geoair.comp.jdbc.url.beans;

import lombok.Data;

import java.io.Serializable;

/**
 * JDBC URL 中的一个网络连接端点，支持主机、端口和 SQL Server 实例名。
 *
 * @author 张逢吉
 */
@Data
public final class JdbcEndpoint implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 数据库服务器主机名、IPv4 或带方括号的 IPv6 地址。 */
    private final String host;

    /** 数据库服务端口；未在 URL 中指定时为 null。 */
    private final Integer port;

    /** SQL Server 等驱动可选的实例名。 */
    private final String instanceName;

    public JdbcEndpoint(String host, Integer port) {
        this(host, port, null);
    }

    public JdbcEndpoint(String host, Integer port, String instanceName) {
        this.host = host;
        this.port = port;
        this.instanceName = instanceName;
    }
}
