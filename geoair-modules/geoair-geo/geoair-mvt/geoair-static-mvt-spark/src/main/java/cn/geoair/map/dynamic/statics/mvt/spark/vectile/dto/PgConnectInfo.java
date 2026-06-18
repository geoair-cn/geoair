package cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto;

import cn.hutool.core.bean.BeanUtil;

import java.io.Serializable;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.sql.DataSource;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/12/29 09:55
 * 该参数有歧义，所以把参数进行了替换修改
 */
@Data
@Accessors(chain = true)
@Deprecated
public class PgConnectInfo implements Serializable {

    protected String ip;

    protected String port;

    protected String userName;

    protected String passwd;

    protected String dbName;

    protected String schemaName; // 模式名

    private String schemaTableName; // 完整的schema/table（或仅schema）

    private String tableName; // 表名（可选，null表示未传）

    public PgConnectInfo() {
    }

    @Deprecated
    PgConnectInfoSimple toPgConnectInfoSimple() {
        return BeanUtil.copyProperties(this, PgConnectInfoSimple.class);
    }

    @Deprecated
    PgConnectInfoWithTable toPgConnectInfoWithTable() {
        return BeanUtil.copyProperties(this, PgConnectInfoWithTable.class);
    }

    public static PgConnectInfo fromPgConnectInfoSimple(PgConnectInfoSimple pgConnectInfoSimple) {
        if (pgConnectInfoSimple == null) {
            return null;
        }
        return BeanUtil.copyProperties(pgConnectInfoSimple, PgConnectInfo.class);
    }

    public static PgConnectInfo fromPgConnectInfoWithTable(PgConnectInfoWithTable pgConnectInfoWithTable) {
        if (pgConnectInfoWithTable == null) {
            return null;
        }
        return BeanUtil.copyProperties(pgConnectInfoWithTable, PgConnectInfo.class);
    }


    @Deprecated
    public DataSource toDataSource() {
        return toPgConnectInfoSimple().toDataSource();
    }

}
