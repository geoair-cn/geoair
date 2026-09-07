package cn.geoair.map.dynamic.file.jdbc;

import cn.geoair.map.dynamic.file.jdbc.link.JdbcGeoLinkInfo;
import org.geotools.api.data.DataStore;

/**
 * JDBC 空间数据库方言。
 * <p>
 * 方言只负责由连接信息创建 GeoTools {@link DataStore}；SQL 分页、字段读取和批量写入等
 * 公共流程由 JDBC 基类统一处理。
 *
 * @author 张逢吉
 */
public interface JdbcGeoDialect {

    /** 方言名称，用于日志与诊断。 */
    String getName();

    /** 创建并返回当前数据库的空间 DataStore。 */
    DataStore createDataStore(JdbcGeoLinkInfo linkInfo) throws Exception;
}
