package cn.geoair.comp.dynamic.ds.base;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 数据源链接获取器接口
 *
 * @author zhangjun
 * @date Created in 2025/10/9 10:38
 */
public interface IDsConnectionOpt {


    /**
     * 获取数据库连接
     *
     * <p>从数据源中获取一个数据库连接对象。
     *
     * @return 数据库连接对象
     */
    Connection getConnection();


    /**
     * 关闭数据库连接
     *
     * <p>关闭指定的数据库连接，释放资源。
     *
     * @param connection 需要关闭的数据库连接
     */
    void connectionClose(Connection connection);

    /**
     * 关闭数据库资源
     *
     * <p>关闭结果集、语句和连接等数据库资源，释放系统资源。
     *
     * @param rs   结果集对象
     * @param stmt 语句对象
     * @param conn 连接对象
     */
    void closeResources(ResultSet rs, Statement stmt, Connection conn);
}
