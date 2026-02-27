package cn.geoair.map.dynamic.ds.utils;

/**
 * 创建人: 张逢吉
 * 创建时间: 2025/9/30 09:28
 * 描述: 将JDBC URL拆分为各个组件。
 * 从类似以下格式的 JDBC URI 中提取组件：
 * String url = "jdbc:derby://localhost:1527/netld;collation=TERRITORY_BASED:PRIMARY";
 * 在各自的公共变量中。
 */
@Deprecated
public class JdbcUrlSplitter {
    public String driverName, host, port, database, params;

    public JdbcUrlSplitter(String jdbcUrl) {
        int pos, pos1, pos2;
        String connUri;

        if (jdbcUrl == null || !jdbcUrl.startsWith("jdbc:")
                || (pos1 = jdbcUrl.indexOf(':', 5)) == -1)
            throw new IllegalArgumentException("Invalid JDBC url.");

        driverName = jdbcUrl.substring(5, pos1);
        if ((pos2 = jdbcUrl.indexOf(';', pos1)) == -1) {
            connUri = jdbcUrl.substring(pos1 + 1);
        } else {
            connUri = jdbcUrl.substring(pos1 + 1, pos2);
            params = jdbcUrl.substring(pos2 + 1);
        }

        if (connUri.startsWith("//")) {
            if ((pos = connUri.indexOf('/', 2)) != -1) {
                host = connUri.substring(2, pos);
                database = connUri.substring(pos + 1);

                if ((pos = host.indexOf(':')) != -1) {
                    port = host.substring(pos + 1);
                    host = host.substring(0, pos);
                }
            }
        } else {
            database = connUri;
        }
    }
}
