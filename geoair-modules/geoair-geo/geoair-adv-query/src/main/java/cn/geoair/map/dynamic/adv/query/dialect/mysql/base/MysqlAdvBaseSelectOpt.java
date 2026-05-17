package cn.geoair.map.dynamic.adv.query.dialect.mysql.base;

import cn.geoair.map.dynamic.adv.config.ConfigAdvQuery;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvBaseSelectOpt;
import cn.geoair.map.dynamic.adv.query.dialect.mysql.MysqlDialectTableNameUtil;

import java.util.function.Supplier;

/** MySQL数据库的动态高级查询基础操作实现类 仅实现MySQL专属的差异化语法逻辑，复用抽象父类的所有通用查询能力 功能与PG版本完全对齐，仅适配MySQL的标识符/别名规则 */
public class MysqlAdvBaseSelectOpt extends AbstractExecAdvBaseSelectOpt {

    public MysqlAdvBaseSelectOpt(Supplier<ConfigAdvQuery> configAdvQueryGetter) {
        super(configAdvQueryGetter);
        // 绑定MySQL专属的表名处理器
        this.dialectTableNameProcessor = MysqlDialectTableNameUtil.getInstance();
    }
}
