package cn.geoair.map.dynamic.adv.spring;

import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.geoair.map.dynamic.adv.query.dialect.pg.AdvExecutorPG;
import cn.geoair.map.dynamic.adv.utils.AdvJdbcUrlUtil;
import cn.geoair.map.dynamic.ds.apo.DataSourceApo;
import cn.geoair.map.dynamic.ds.utils.JdbcUrlSplitter;
import cn.hutool.extra.spring.SpringUtil;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.Date;

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
