package cn.geoair.comp.dynamic.ds.spring;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.comp.dynamic.ds.IAdvDataSourceHelper;
import cn.geoair.comp.dynamic.ds.apo.DataSourceApo;
import cn.hutool.extra.spring.SpringUtil;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.util.StringUtils;

/**
 * @author ：张逢吉
 * @date ：Created in 15:34 @description： spring默认的数据源获取器
 */
public class DefaultAdvDataSourceHelper implements IAdvDataSourceHelper {
    public static GiLogger log = GirLoggerFactory.getLogger();

    @Override
    public DataSourceApo getDataSourceApoById(String dataSourceId) {
        return getDataSourceApoBySpring();
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
        DataSourceApo apo = GirSpringDataSourceUtils.convertToDataSourceApo(properties);
        // 生成唯一ID
        apo.setId(generateDataSourceId());
        apo.setName(generateDataSourceName(properties));
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
