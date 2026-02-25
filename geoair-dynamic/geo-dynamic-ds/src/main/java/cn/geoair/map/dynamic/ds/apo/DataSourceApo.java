package cn.geoair.map.dynamic.ds.apo;

import cn.hutool.db.dialect.DriverNamePool;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 数据源的Api传递对象(Application Persistence Object)
 * <p>
 * 用于在应用程序各层之间传递数据源相关信息的数据载体，
 * 包含数据库连接所需的各类配置参数及元数据信息
 * </p>
 */
@Data
public class DataSourceApo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据源唯一标识ID
     */
    private String id;

    /**
     * 数据库驱动类名
     * 默认使用PostgreSQL驱动，取值于Hutool工具类的DriverNamePool
     */
    private String driver = DriverNamePool.DRIVER_POSTGRESQL;

    /**
     * 数据源名称（通常用于显示和标识）
     */
    private String name;

    /**
     * 数据库服务器地址（IP或域名）
     */
    private String address;

    /**
     * 数据库服务端口号
     */
    private Integer port;

    /**
     * 数据库实例名称
     */
    private String dbName;

    /**
     * 数据库模式名称（Schema）
     */
    private String schemaName;

    /**
     * 数据库登录用户名
     */
    private String username;

    /**
     * 数据库登录密码
     */
    private String password;

    /**
     * 数据源创建时间
     */
    private Date createTime;

    /**
     * 数据源最后更新时间
     */
    private Date updateTime;



    /**
     * 判断当前数据源与传入的连接参数是否匹配
     * <p>
     * 比较规则：数据库地址、端口、数据库名、用户名完全一致则认为匹配
     * </p>
     *
     * @param address 数据库地址
     * @param port    端口号
     * @param dbName  数据库名
     * @param user    用户名
     * @return true-匹配，false-不匹配
     * @throws NullPointerException 当任一参数为null时可能抛出空指针异常
     */
    public boolean equals(String address, Integer port, String dbName, String user) {
        return this.address.equals(address) && this.port.equals(port) && this.dbName.equals(dbName) && this.username.equals(user);
    }
}
