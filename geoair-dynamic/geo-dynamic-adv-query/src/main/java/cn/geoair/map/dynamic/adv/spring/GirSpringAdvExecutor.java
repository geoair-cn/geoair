package cn.geoair.map.dynamic.adv.spring;

import cn.geoair.map.dynamic.adv.query.IAdvExecutor;

import cn.geoair.map.dynamic.adv.query.dialect.pgv2.AdvExecutorPG;
import cn.hutool.extra.spring.SpringUtil;

import javax.sql.DataSource;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/11/20 09:40
 * @description：Spring环境下的高级查询执行器
 */
public class GirSpringAdvExecutor extends AdvExecutorPG implements IAdvExecutor {

    public static GirSpringAdvExecutor getInstance() {
        return SpringUtil.getBean(GirSpringAdvExecutor.class);
    }

    public static GirSpringAdvExecutor newInstance() {
        GirSpringAdvExecutor advExecutor = new GirSpringAdvExecutor();
        advExecutor.initByDataSource(getDataSourceBySpring());
        return advExecutor;
    }


    public static DataSource getDataSourceBySpring() {
        return SpringUtil.getBean(DataSource.class);
    }

    public GirSpringAdvExecutor() {
    }

}
