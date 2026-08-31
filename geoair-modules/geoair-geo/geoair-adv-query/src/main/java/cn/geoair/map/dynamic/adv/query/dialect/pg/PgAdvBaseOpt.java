package cn.geoair.map.dynamic.adv.query.dialect.pg;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.query.IAdvBaseAccessOpt;
import cn.geoair.map.dynamic.adv.query.IAdvBaseDeleteOpt;
import cn.geoair.map.dynamic.adv.query.IAdvBaseSelectOpt;
import cn.geoair.map.dynamic.adv.query.IAdvBaseUpdateOpt;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractPxyAdvBaseOpt;
import cn.geoair.map.dynamic.adv.query.dialect.pg.base.PgAdvBaseAccessOpt;
import cn.geoair.map.dynamic.adv.query.dialect.pg.base.PgAdvBaseDeleteOpt;
import cn.geoair.map.dynamic.adv.query.dialect.pg.base.PgAdvBaseSelectOpt;
import cn.geoair.map.dynamic.adv.query.dialect.pg.base.PgAdvBaseUpdateOpt;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandlerRegistry;
import cn.hutool.db.dialect.DialectName;
import java.util.function.Supplier;

/**
 * PostgreSQL数据库的动态高级查询基础操作实现类
 *
 * <p>实现了IAdvBaseOpt接口，通过组合方式复用各细分操作类（插入/查询/更新/删除）的实现， 统一对外提供PostgreSQL数据库的全量基础操作，封装了代理对象的初始化和数据源注入。
 *
 * @author 张逢吉
 * @date 2025/10/9 10:16
 */
public class PgAdvBaseOpt extends AbstractPxyAdvBaseOpt {

    private final AdvTypeHandlerRegistry typeHandlerRegistry;

    public PgAdvBaseOpt(
            IDataSourceGetter dataSourceGetter,
            Supplier<AdvQueryGlobalConfig> configAdvQueryGetter) {
        super(dataSourceGetter, configAdvQueryGetter);
        this.typeHandlerRegistry =
                AdvTypeHandlerRegistry.create(
                        DialectName.POSTGRESQL, configAdvQueryGetter.get().getTypeHandlers());
    }

    /** 获取插入操作代理对象（懒加载+数据源注入） */
    @Override
    public IAdvBaseAccessOpt getAdvBaseAccessPxyOpt() {
        if (advBaseAccessPxyOpt == null) {
            advBaseAccessPxyOpt = new PgAdvBaseAccessOpt(this::getConfig, typeHandlerRegistry);
            advBaseAccessPxyOpt.setDataSourceGetter(dataSourceGetter);
        }
        return advBaseAccessPxyOpt;
    }

    /** 获取查询操作代理对象（懒加载+数据源注入） */
    @Override
    public IAdvBaseSelectOpt getAdvBaseSelectPxyOpt() {
        if (advBaseSelectPxyOpt == null) {
            advBaseSelectPxyOpt = new PgAdvBaseSelectOpt(this::getConfig, typeHandlerRegistry);
            advBaseSelectPxyOpt.setDataSourceGetter(dataSourceGetter);
        }
        return advBaseSelectPxyOpt;
    }

    /** 获取更新操作代理对象（懒加载+数据源注入） */
    @Override
    public IAdvBaseUpdateOpt getAdvBaseUpdatePxyOpt() {
        if (advBaseUpdatePxyOpt == null) {
            advBaseUpdatePxyOpt = new PgAdvBaseUpdateOpt(this::getConfig, typeHandlerRegistry);
            advBaseUpdatePxyOpt.setDataSourceGetter(dataSourceGetter);
        }
        return advBaseUpdatePxyOpt;
    }

    /** 获取删除操作代理对象（懒加载+数据源注入） */
    @Override
    public IAdvBaseDeleteOpt getAdvBaseDeletePxyOpt() {
        if (advBaseDeletePxyOpt == null) {
            advBaseDeletePxyOpt = new PgAdvBaseDeleteOpt(this::getConfig, typeHandlerRegistry);
            advBaseDeletePxyOpt.setDataSourceGetter(dataSourceGetter);
        }
        return advBaseDeletePxyOpt;
    }
}
