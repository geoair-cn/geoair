package cn.geoair.map.dynamic.geoserver.beans;

import cn.geoair.map.dynamic.geoserver.enums.DataSourceType;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** PostGIS 数据源配置 继承 BaseDatastore，扩展 PostGIS 专属连接参数 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PgDatastore extends BaseDatastore {

    /** 数据库主机地址 */
    private String host = "localhost";

    /** 数据库端口 */
    private int port = 5432;

    /** 数据库名称 */
    private String database;

    /** 数据库用户名 */
    private String username;

    /** 数据库密码 */
    private String password;

    /** 数据库 Schema */
    private String schema = "public";

    /** 构造函数：默认设置数据源类型为 PostGIS */
    public PgDatastore() {
        super.setDataSourceType(DataSourceType.POSTGIS);
    }

    /** 转换为 GeoServer PostGIS 连接参数 */
    @Override
    public Map<String, String> toConnectionParams() {
        Map<String, String> params = new HashMap<>();
        params.put("dbtype", "postgis");
        params.put("host", this.getHost());
        params.put("port", String.valueOf(this.getPort()));
        params.put("database", this.getDatabase());
        params.put("user", this.getUsername());
        params.put("passwd", this.getPassword());
        params.put("schema", this.getSchema());
        // 可选：添加 PostGIS 额外参数
        params.put("Estimated extends", "true");
        params.put("validate connections", "true");
        return params;
    }
}
