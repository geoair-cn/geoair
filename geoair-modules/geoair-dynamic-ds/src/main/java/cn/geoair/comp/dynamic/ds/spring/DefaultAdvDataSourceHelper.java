package cn.geoair.comp.dynamic.ds.spring;

import cn.geoair.comp.dynamic.ds.IAdvDataSourceHelper;
import cn.geoair.comp.dynamic.ds.apo.DataSourceApo;
import cn.geoair.comp.dynamic.ds.utils.AdvJdbcUrlUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.druid.pool.DruidDataSource;
import java.util.Date;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.util.StringUtils;

/**
 * @author ：张逢吉
 * @date ：Created in 15:34 @description： spring默认的数据源获取器
 */
@Slf4j
public class DefaultAdvDataSourceHelper implements IAdvDataSourceHelper {

    @Override
    public DataSourceApo getDataSourceApoById(String dataSourceId) {
        return getDataSourceApoBySpring();
    }

    @Override
    public DataSource getDbDataSourceByApo(DataSourceApo dataSourceApo) {
        try {
            // 创建新的Druid数据源
            DruidDataSource dataSourceNew = new DruidDataSource();
            // 设置连接池参数
            dataSourceNew.setInitialSize(20); // 初始连接数（提升：从5→20）
            // 理由：连接难获取，提前创建足够初始连接，减少首次获取等待
            dataSourceNew.setMaxActive(500); // 最大连接数（适度提升：从300→500）
            // 理由：长耗时SQL会占用连接更久，需更多连接支撑并发，避免连接池耗尽
            dataSourceNew.setMinIdle(50); // 最小空闲连接（提升：从20→50）
            // 理由：保留更多空闲连接，减少重新创建连接的开销（连接难获取场景关键）
            // 理由：限制空闲连接上限，避免闲置连接过多占用资源，同时保证足够复用

            // 连接等待与超时（核心适配长耗时SQL）
            dataSourceNew.setMaxWait(10000); // 获取连接的最大等待时间（从3000→10000，单位：毫秒）
            // 理由：连接难获取时，放宽等待时间，避免过早抛出连接超时异常
            dataSourceNew.setRemoveAbandonedTimeout(1800); // 连接超时回收时间（从300→1800，单位：秒=30分钟）
            // 理由：适配长耗时SQL（比如复杂统计SQL可能执行10+分钟），避免误判连接泄露
            dataSourceNew.setRemoveAbandonedTimeoutMillis(1800 * 1000); // 兼容不同版本的参数（部分池化框架用毫秒级参数）

            // 连接可用性校验（强化：避免拿到失效连接）
            dataSourceNew.setValidationQuery(
                    DataSourceApo.getValidationQuery(
                            dataSourceApo.getDbType())); // 连接校验SQL（轻量查询，保留）
            dataSourceNew.setTestOnBorrow(false); // 调整：获取连接时不校验（从true→false）
            // 理由：TestOnBorrow=true会每次获取连接都校验，高并发下性能损耗大；改为空闲时校验更高效
            dataSourceNew.setTestOnReturn(false); // 保留：归还连接时不校验（减少开销）
            dataSourceNew.setTestWhileIdle(true); // 保留：空闲时校验（核心）
            dataSourceNew.setTimeBetweenEvictionRunsMillis(30000); // 空闲连接检测间隔（从60000→30000，单位：毫秒）
            // 理由：缩短检测间隔，更快剔除失效连接，保证空闲连接可用
            dataSourceNew.setNumTestsPerEvictionRun(-1); // 新增：每次检测所有空闲连接（-1表示不限制）
            // 理由：连接难获取，确保所有空闲连接都是可用的
            dataSourceNew.setMinEvictableIdleTimeMillis(1800000); // 新增：连接最小空闲时间（30分钟）
            // 理由：避免频繁销毁/创建连接，适配连接难获取场景

            // 连接失败重试（适配连接难获取）
            dataSourceNew.setConnectionErrorRetryAttempts(3); // 从0→3：连接失败重试3次
            dataSourceNew.setBreakAfterAcquireFailure(false); // 新增：获取连接失败后不中断，继续重试
            // 理由：连接难获取时，增加重试机会，提升连接获取成功率

            // 连接泄露与日志（强化监控）
            dataSourceNew.setRemoveAbandoned(true); // 保留：自动回收超时未关闭的连接
            dataSourceNew.setLogAbandoned(true); // 保留：记录连接泄露日志

            // // 构建JDBC连接URL
            String url = dataSourceApo.getJdbcUrl();
            dataSourceNew.setUrl(url);
            dataSourceNew.setUsername(dataSourceApo.getUsername());
            dataSourceNew.setPassword(dataSourceApo.getPassword());
            dataSourceNew.setDriverClassName(dataSourceApo.getDriver());

            // 初始化数据源
            dataSourceNew.init();
            return dataSourceNew;
        } catch (Exception e) {
            log.error("加载动态连接池错误", e);
            return null;
        }
    }

    /**
     * 从Spring容器中获取DataSourceProperties并转换为DataSourceApo
     *
     * @return DataSourceApo实例
     */
    public static DataSourceApo getDataSourceApoBySpring() {
        DataSourceProperties properties = SpringUtil.getBean(DataSourceProperties.class);
        return convertToDataSourceApo(properties);
    }

    /**
     * 将Spring Boot的DataSourceProperties转换为自定义的DataSourceApo
     *
     * @param properties Spring数据源配置属性
     * @return 转换后的DataSourceApo实例
     */
    private static DataSourceApo convertToDataSourceApo(DataSourceProperties properties) {
        if (properties == null) {
            throw new IllegalStateException("DataSourceProperties未在Spring容器中找到");
        }

        DataSourceApo apo = new DataSourceApo();

        // 生成唯一ID
        apo.setId(generateDataSourceId());

        // 设置驱动类名
        if (StringUtils.hasText(properties.getDriverClassName())) {
            apo.setDriver(properties.getDriverClassName());
        }
        AdvJdbcUrlUtil jdbcUrlSplitter = new AdvJdbcUrlUtil(properties.getUrl());
        apo.setJdbcUrl(properties.getUrl());
        apo.setDbName(jdbcUrlSplitter.database);
        apo.setPort(Integer.valueOf(jdbcUrlSplitter.port));
        apo.setSchemaName(jdbcUrlSplitter.params.get("currentSchema"));
        apo.setAddress(jdbcUrlSplitter.host);
        // 设置用户名和密码
        apo.setUsername(properties.getUsername());
        apo.setPassword(properties.getPassword());

        // 设置名称（可以使用URL或生成默认名称）
        apo.setName(generateDataSourceName(properties));

        // 设置时间戳
        Date now = new Date();
        apo.setCreateTime(now);
        apo.setUpdateTime(now);

        return apo;
    }

    /** 生成数据源唯一ID */
    private static String generateDataSourceId() {
        return "DS_"
                + System.currentTimeMillis()
                + "_"
                + java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    /** 生成数据源显示名称 */
    private static String generateDataSourceName(DataSourceProperties properties) {
        if (StringUtils.hasText(properties.getName())) {
            return properties.getName();
        }

        // 基于URL生成名称
        if (StringUtils.hasText(properties.getUrl())) {
            String url = properties.getUrl();
            if (url.contains("://")) {
                String dbPart = url.substring(url.lastIndexOf("/") + 1);
                String dbName = dbPart.split("\\?")[0];
                return "DataSource-" + dbName;
            }
        }

        return "Spring-DataSource";
    }
}
