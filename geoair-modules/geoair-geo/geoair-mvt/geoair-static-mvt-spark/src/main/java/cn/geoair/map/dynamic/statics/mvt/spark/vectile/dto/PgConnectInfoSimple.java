package cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto;

import cn.geoair.comp.dynamic.ds.utils.DataSourceDruidFastCreate;
import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.sql.DataSource;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/12/29 09:55 @description：
 */
@Data
@Accessors(chain = true)
public class PgConnectInfoSimple implements Serializable {

    protected String ip;

    protected String port;

    protected String userName;

    protected String passwd;

    protected String dbName;

    protected String schemaName; // 模式名

    protected transient DataSource dataSource;

    public PgConnectInfoSimple() {
    }

    public DataSource toDataSource() {
        Map<String, String> params = toParams();
        if (dataSource == null) {
            DataSourceDruidFastCreate druidFastCreate = new DataSourceDruidFastCreate();
            druidFastCreate.setUrl(params.get("url"));
            druidFastCreate.setUsername(params.get("user"));
            druidFastCreate.setPassword(params.get("password"));
            dataSource = druidFastCreate.toDataSource();
        }
        return dataSource;
    }

    public Map<String, String> toParams() {
        Map<String, String> params = new HashMap<>();
        String format =
                String.format(
                        "jdbc:postgresql://%s:%s/%s",
                        this.getIp(), this.getPort(), this.getDbName());
        if (!StrUtil.isEmpty(this.getSchemaName())) {
            format = format + "?currentSchema=" + this.getSchemaName();
        }
        params.put("url", format);
        params.put("user", this.getUserName());
        params.put("password", this.getPasswd());

        return params;
    }
}
