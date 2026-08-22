package cn.geoair.comp.dynamic.ds.spring;

import cn.geoair.comp.dynamic.ds.apo.DataSourceApo;
import cn.geoair.comp.jdbc.url.beans.JdbcEndpoint;
import cn.geoair.comp.jdbc.url.beans.JdbcUrl;
import cn.geoair.comp.jdbc.url.GirJdbcUrlCodecs;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.util.StringUtils;

import java.util.Date;

/**
 * @author ：张俊
 * @date ：Created in 2026/4/15 17:24
 * @description： Spring的DataSourceProperties 转换成 DataSourceApo
 */
public class GirSpringDataSourceUtils {


    /**
     * 将Spring Boot的DataSourceProperties转换为自定义的DataSourceApo
     *
     * @param properties Spring数据源配置属性
     * @return 转换后的DataSourceApo实例
     */
    public static DataSourceApo convertToDataSourceApo(DataSourceProperties properties) {
        if (properties == null) {
            throw new IllegalStateException("DataSourceProperties未在Spring容器中找到");
        }

        DataSourceApo apo = new DataSourceApo();


        // 设置驱动类名
        if (StringUtils.hasText(properties.getDriverClassName())) {
            apo.setDriver(properties.getDriverClassName());
        }
        JdbcUrl jdbcUrl = GirJdbcUrlCodecs.defaultCodec().parse(properties.getUrl());
        apo.setJdbcUrl(properties.getUrl());
        apo.setDbName(jdbcUrl.getDatabaseName());
        JdbcEndpoint endpoint = jdbcUrl.getPrimaryEndpoint();
        if (endpoint != null && endpoint.getPort() != null) {
            apo.setPort(endpoint.getPort());   // 端口是可以为空的，具体的数据源会去补充默认端口
        }
        apo.setSchemaName(GirJdbcUrlCodecs.defaultCodec().getSchema(properties.getUrl()));
        apo.setAddress(endpoint == null ? null : endpoint.getHost());
        // 设置用户名和密码
        apo.setUsername(properties.getUsername());
        apo.setPassword(properties.getPassword());


        // 设置时间戳
        Date now = new Date();
        apo.setCreateTime(now);
        apo.setUpdateTime(now);

        return apo;
    }
}
