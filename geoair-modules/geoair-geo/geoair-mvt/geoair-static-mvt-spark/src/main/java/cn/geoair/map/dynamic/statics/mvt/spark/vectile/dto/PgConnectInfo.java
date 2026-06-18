package cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto;

import cn.geoair.comp.dynamic.ds.simple.DriverManagerDataSource;
import cn.geoair.comp.dynamic.ds.utils.DataSourceDruidFastCreate;
import cn.hutool.core.util.StrUtil;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/12/29 09:55 @description：
 */
@Data
@Accessors(chain = true)
public class PgConnectInfo extends PgConnectInfoBase {

    private String schemaTableName; // 完整的schema/table（或仅schema）

    private String tableName; // 表名（可选，null表示未传）

    public PgConnectInfo() {
    }

    public PgConnectInfo(String url) {
        // pg路径：pg://user@pa/ip:port/db/a.b
        // 第一步：校验URL前缀，避免非法格式
        if (!url.startsWith("pg://")) {
            throw new IllegalArgumentException("PG URL格式错误，必须以pg://开头：" + url);
        }
        // 第二步：分割URL（按/拆分，过滤空字符串，避免首尾/导致的空元素）
        String[] infos = url.split("/");
        // 清理空元素（例如url末尾有/时，split会产生空字符串）
        infos = java.util.Arrays.stream(infos).filter(str -> !str.isEmpty()).toArray(String[]::new);

        if (infos.length < 4) {
            throw new IllegalArgumentException("PG URL格式不完整，至少需要：pg://用户名#密码/IP:端口/数据库名：" + url);
        }

        // 解析IP和端口（第5个元素：infos[4]，因为索引从0开始：0=pg:, 1=空, 2=user#pass, 3=ip:port,
        // 4=dbName...）
        // 修正索引：split后数组结构（以完整URL为例）：
        // pg://postgres#tcsd1234/116.198.227.117:35432/address/test1/big_mian
        // split("/") → ["pg:", "", "postgres#tcsd1234", "116.198.227.117:35432",
        // "address", "test1", "big_mian"]
        // 过滤空元素后 → ["pg:", "postgres#tcsd1234", "116.198.227.117:35432", "address",
        // "test1", "big_mian"]
        String ipPortStr = infos[2]; // 过滤后索引2：IP:端口
        String[] ipPort = ipPortStr.split(":");
        if (ipPort.length != 2) {
            throw new IllegalArgumentException("IP:端口格式错误：" + ipPortStr);
        }
        this.ip = ipPort[0];
        this.port = ipPort[1];

        // 解析用户名和密码（过滤后索引1：user#pass）
        String userPassStr = infos[1];
        String[] userPass = userPassStr.split("#");
        if (userPass.length != 2) {
            throw new IllegalArgumentException("用户名#密码格式错误：" + userPassStr);
        }
        this.userName = userPass[0];
        this.passwd = userPass[1];

        // 解析数据库名（过滤后索引3）
        this.dbName = infos[3];

        // 解析schema和表名（表名可选）
        try {
            this.schemaName = infos[4]; // 过滤后索引4：schema名
        } catch (Exception e) {

        }
        try {
            this.tableName = infos[5]; // 过滤后索引5：
        } catch (Exception e) {

        }
        try {
            // 拼接schemaTableName（兼容原有字段，格式：schema/table 或 仅schema）
            if (this.tableName != null && !this.tableName.isEmpty()) {
                this.schemaTableName = this.schemaName + "." + this.tableName;
            } else {
                this.schemaTableName = this.schemaName;
            }
        } catch (Exception e) {

        }
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
        params.put("table", this.getSchemaTableName());
        params.put("user", this.getUserName());
        params.put("password", this.getPasswd());

        return params;
    }
}
