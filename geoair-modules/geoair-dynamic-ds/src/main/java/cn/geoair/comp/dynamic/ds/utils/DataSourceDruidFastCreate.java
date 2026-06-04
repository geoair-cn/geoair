package cn.geoair.comp.dynamic.ds.utils;

import com.alibaba.druid.pool.DruidDataSource;
import lombok.Data;

import javax.sql.DataSource;

@Data
public class DataSourceDruidFastCreate {

    private String url;
    private String username;
    private String password;

    // Druid 可选配置
    private Integer initialSize = 1;
    private Integer queryTimeout = 15;
    private Integer minIdle = 5;
    private Integer maxActive = 20;
    private Long maxWait = 60000L;
    private Long timeBetweenEvictionRunsMillis = 60000L;
    private Long minEvictableIdleTimeMillis = 1800000L;

    private Boolean testWhileIdle = true;
    private Boolean testOnBorrow = false;
    private Boolean testOnReturn = false;
    private Boolean poolPreparedStatements = true;
    private Integer maxPoolPreparedStatementPerConnectionSize = 20;
    private String filters = "stat,wall";
    private Integer removeAbandonedTimeout = 300;      // 连接泄漏回收超时（秒）
    private Integer connectionErrorRetryAttempts = 3;  // 连接错误重试次数
    private String validationQuery;                    // 验证查询SQL
    private Integer numTestsPerEvictionRun = -1;       // 每次检测的连接数
    /**
     * 创建 Druid 数据源
     */
    public DataSource toDataSource() {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        // 连接池配置
        if (initialSize != null) {
            dataSource.setInitialSize(initialSize);
        }
        if (minIdle != null) {
            dataSource.setMinIdle(minIdle);
        }
        if (maxActive != null) {
            dataSource.setMaxActive(maxActive);
        }
        if (maxWait != null) {
            dataSource.setMaxWait(maxWait);
        }
        if (timeBetweenEvictionRunsMillis != null) {
            dataSource.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
        }
        if (minEvictableIdleTimeMillis != null) {
            dataSource.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
        }

        if (testWhileIdle != null) {
            dataSource.setTestWhileIdle(testWhileIdle);
        }
        if (testOnBorrow != null) {
            dataSource.setTestOnBorrow(testOnBorrow);
        }
        if (testOnReturn != null) {
            dataSource.setTestOnReturn(testOnReturn);
        }
        if (poolPreparedStatements != null) {
            dataSource.setPoolPreparedStatements(poolPreparedStatements);
        }
        if (queryTimeout != null) {
            dataSource.setQueryTimeout(queryTimeout);
        }
        if (maxPoolPreparedStatementPerConnectionSize != null) {
            dataSource.setMaxPoolPreparedStatementPerConnectionSize(maxPoolPreparedStatementPerConnectionSize);
        }
        if (removeAbandonedTimeout != null) {
            dataSource.setRemoveAbandonedTimeout(removeAbandonedTimeout);
        }
        if (connectionErrorRetryAttempts != null) {
            dataSource.setBreakAfterAcquireFailure(true);
            dataSource.setConnectionErrorRetryAttempts(connectionErrorRetryAttempts);
        }
        if (validationQuery != null) {
            dataSource.setValidationQuery(validationQuery);
        }
        if (numTestsPerEvictionRun != null) {
            dataSource.setNumTestsPerEvictionRun(numTestsPerEvictionRun);
        }
        dataSource.setBreakAfterAcquireFailure(true);
        dataSource.setRemoveAbandoned(true);
        dataSource.setLogAbandoned(true);

        return dataSource;
    }
}
