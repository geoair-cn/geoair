package cn.geoair.map.dynamic.adv;

import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsTypeGeom;
import cn.geoair.map.dynamic.adv.spring.AdvExecutorFactory;
import cn.geoair.map.dynamic.tools.GirService;

import javax.sql.DataSource;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/10/9 11:02 @description： 短方法，用于敲代码的时候快速定位
 */
public class GirAdvQuery {

    /**
     * 获取执行器
     *
     * @param dataSourceId
     * @param schema
     * @return
     */
    public static IAdvExecutor getIAdvExecutor(String dataSourceId, String schema) {
        IAdvExecutorAdapter pxyBeanC = GirService.getPxyBeanC(IAdvExecutorAdapter.class);
        return pxyBeanC.getIAdvExecutor(dataSourceId, schema);
    }

    /**
     * 通过数据源快速获取执行器
     *
     * @param dataSource
     * @return
     */
    public static IAdvExecutor getIAdvExecutor(DataSource dataSource) {
        return AdvExecutorFactory.getAdvExecutorByDataSource(dataSource);
    }

    public static <T extends IAdvExecutor> T getIAdvExecutor(String dataSourceId, String schema, Class<T> clazz) {
        IAdvExecutorAdapter pxyBeanC = GirService.getPxyBeanC(IAdvExecutorAdapter.class);
        return pxyBeanC.getIAdvExecutor(dataSourceId, schema, clazz);
    }

    public static void main(String[] args) {
        IAdvExecutor iAdvExecutor = GirAdvQuery.getIAdvExecutor("", "");
        iAdvExecutor.bSelectOne("");
        iAdvExecutor.bSelectList("");
        String s = iAdvExecutor.eGetGeomColumnNameBySql("");
        AdvEnumsTypeGeom advEnumsTypeGeom = iAdvExecutor.eGetGeoTypeBySql("");

    }

}
