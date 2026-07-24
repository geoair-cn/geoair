package cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto;

import cn.geoair.map.dynamic.statics.mvt.spark.vectile.utils.DataSourceGetterFunction;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.utils.DefaultDataSourceGetterFunction;
import cn.hutool.core.util.StrUtil;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

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

    protected DataSourceGetterFunction dataSourceGetterFunction =
            new DefaultDataSourceGetterFunction();

    public PgConnectInfoSimple() {}

    public DataSource toDataSource() {
        return dataSourceGetterFunction.apply(this);
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
