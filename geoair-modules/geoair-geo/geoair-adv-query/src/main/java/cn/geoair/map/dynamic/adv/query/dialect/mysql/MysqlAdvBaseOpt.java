package cn.geoair.map.dynamic.adv.query.dialect.mysql;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.query.IAdvBaseAccessOpt;
import cn.geoair.map.dynamic.adv.query.IAdvBaseDeleteOpt;
import cn.geoair.map.dynamic.adv.query.IAdvBaseSelectOpt;
import cn.geoair.map.dynamic.adv.query.IAdvBaseUpdateOpt;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractPxyAdvBaseOpt;
import cn.geoair.map.dynamic.adv.query.dialect.mysql.base.MysqlAdvBaseAccessOpt;
import cn.geoair.map.dynamic.adv.query.dialect.mysql.base.MysqlAdvBaseDeleteOpt;
import cn.geoair.map.dynamic.adv.query.dialect.mysql.base.MysqlAdvBaseSelectOpt;
import cn.geoair.map.dynamic.adv.query.dialect.mysql.base.MysqlAdvBaseUpdateOpt;
import java.util.function.Supplier;

/**
 * @author 张逢吉
 * @date 2025/10/9 10:16
 */
public class MysqlAdvBaseOpt extends AbstractPxyAdvBaseOpt {

    public MysqlAdvBaseOpt(
            IDataSourceGetter dataSourceGetter,
            Supplier<AdvQueryGlobalConfig> configAdvQueryGetter) {
        super(dataSourceGetter, configAdvQueryGetter);
    }

    /** 获取插入操作代理对象（懒加载+数据源注入） */
    @Override
    public IAdvBaseAccessOpt getAdvBaseAccessPxyOpt() {
        if (advBaseAccessPxyOpt == null) {
            advBaseAccessPxyOpt = new MysqlAdvBaseAccessOpt(this::getConfig);
            advBaseAccessPxyOpt.setDataSourceGetter(dataSourceGetter);
        }
        return advBaseAccessPxyOpt;
    }

    /** 获取查询操作代理对象（懒加载+数据源注入） */
    @Override
    public IAdvBaseSelectOpt getAdvBaseSelectPxyOpt() {
        if (advBaseSelectPxyOpt == null) {
            advBaseSelectPxyOpt = new MysqlAdvBaseSelectOpt(this::getConfig);
            advBaseSelectPxyOpt.setDataSourceGetter(dataSourceGetter);
        }
        return advBaseSelectPxyOpt;
    }

    /** 获取更新操作代理对象（懒加载+数据源注入） */
    @Override
    public IAdvBaseUpdateOpt getAdvBaseUpdatePxyOpt() {
        if (advBaseUpdatePxyOpt == null) {
            advBaseUpdatePxyOpt = new MysqlAdvBaseUpdateOpt(this::getConfig);
            advBaseUpdatePxyOpt.setDataSourceGetter(dataSourceGetter);
        }
        return advBaseUpdatePxyOpt;
    }

    /** 获取删除操作代理对象（懒加载+数据源注入） */
    @Override
    public IAdvBaseDeleteOpt getAdvBaseDeletePxyOpt() {
        if (advBaseDeletePxyOpt == null) {
            advBaseDeletePxyOpt = new MysqlAdvBaseDeleteOpt(this::getConfig);
            advBaseDeletePxyOpt.setDataSourceGetter(dataSourceGetter);
        }
        return advBaseDeletePxyOpt;
    }
}
