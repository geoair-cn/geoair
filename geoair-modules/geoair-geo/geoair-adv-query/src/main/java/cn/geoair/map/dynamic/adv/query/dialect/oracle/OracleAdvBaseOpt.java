package cn.geoair.map.dynamic.adv.query.dialect.oracle;

import cn.geoair.comp.dynamic.ds.IDsDataSourceManger;
import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.query.IAdvBaseAccessOpt;
import cn.geoair.map.dynamic.adv.query.IAdvBaseDeleteOpt;
import cn.geoair.map.dynamic.adv.query.IAdvBaseSelectOpt;
import cn.geoair.map.dynamic.adv.query.IAdvBaseUpdateOpt;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractPxyAdvBaseOpt;
import cn.geoair.map.dynamic.adv.query.dialect.oracle.base.OracleAdvBaseAccessOpt;
import cn.geoair.map.dynamic.adv.query.dialect.oracle.base.OracleAdvBaseDeleteOpt;
import cn.geoair.map.dynamic.adv.query.dialect.oracle.base.OracleAdvBaseSelectOpt;
import cn.geoair.map.dynamic.adv.query.dialect.oracle.base.OracleAdvBaseUpdateOpt;

import java.util.function.Supplier;

/**
 * Oracle数据库的动态高级查询基础操作实现类
 *
 * <p>实现了IAdvBaseOpt接口，通过组合方式复用各细分操作类（插入/查询/更新/删除）的实现， 统一对外提供Oracle数据库的全量基础操作，
 * 封装了代理对象的初始化和数据源注入。
 *
 * @author 张逢吉
 * @date 2025/10/9 10:16
 */
public class OracleAdvBaseOpt extends AbstractPxyAdvBaseOpt {

    public OracleAdvBaseOpt(IDsDataSourceManger dataSourceGetter, Supplier<AdvQueryGlobalConfig> configAdvQueryGetter) {
        super(dataSourceGetter,configAdvQueryGetter);
    }

    /** 获取插入操作代理对象（懒加载+数据源注入） */
    @Override
    public IAdvBaseAccessOpt getAdvBaseAccessPxyOpt() {
        if (advBaseAccessPxyOpt == null) {
            advBaseAccessPxyOpt = new OracleAdvBaseAccessOpt(this::getConfig);
            advBaseAccessPxyOpt.setDataSourceGetter(dataSourceGetter);
        }
        return advBaseAccessPxyOpt;
    }

    /** 获取查询操作代理对象（懒加载+数据源注入） */
    @Override
    public IAdvBaseSelectOpt getAdvBaseSelectPxyOpt() {
        if (advBaseSelectPxyOpt == null) {
            advBaseSelectPxyOpt = new OracleAdvBaseSelectOpt(this::getConfig);
            advBaseSelectPxyOpt.setDataSourceGetter(dataSourceGetter);
        }
        return advBaseSelectPxyOpt;
    }

    /** 获取更新操作代理对象（懒加载+数据源注入） */
    @Override
    public IAdvBaseUpdateOpt getAdvBaseUpdatePxyOpt() {
        if (advBaseUpdatePxyOpt == null) {
            advBaseUpdatePxyOpt = new OracleAdvBaseUpdateOpt(this::getConfig);
            advBaseUpdatePxyOpt.setDataSourceGetter(dataSourceGetter);
        }
        return advBaseUpdatePxyOpt;
    }

    /** 获取删除操作代理对象（懒加载+数据源注入） */
    @Override
    public IAdvBaseDeleteOpt getAdvBaseDeletePxyOpt() {
        if (advBaseDeletePxyOpt == null) {
            advBaseDeletePxyOpt = new OracleAdvBaseDeleteOpt(this::getConfig);
            advBaseDeletePxyOpt.setDataSourceGetter(dataSourceGetter);
        }
        return advBaseDeletePxyOpt;
    }
}
