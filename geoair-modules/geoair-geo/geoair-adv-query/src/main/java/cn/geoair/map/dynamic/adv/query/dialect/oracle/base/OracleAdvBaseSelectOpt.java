package cn.geoair.map.dynamic.adv.query.dialect.oracle.base;

import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvBaseSelectOpt;
import cn.geoair.map.dynamic.adv.query.dialect.oracle.OracleDialectTableNameUtil;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandlerRegistry;
import cn.hutool.core.util.StrUtil;

import java.util.function.Supplier;

/**
 * PostgreSQL查询操作实现类
 */
public class OracleAdvBaseSelectOpt extends AbstractExecAdvBaseSelectOpt {

    // 初始化表名处理器
    public OracleAdvBaseSelectOpt(Supplier<AdvQueryGlobalConfig> configAdvQueryGetter, AdvTypeHandlerRegistry registry) {
        super(configAdvQueryGetter, registry);
        this.dialectTableNameProcessor = OracleDialectTableNameUtil.getInstance();
    }

    /**
     * Oracle单条查询包装（使用ROWNUM，不支持LIMIT）
     */
    @Override
    protected String buildSelectOneWrapSql(String cleanSql) {
        // Oracle使用 ROWNUM 实现取第一条
        return StrUtil.format("SELECT * FROM ({}) WHERE ROWNUM = 1", cleanSql);
    }

    /**
     * Oracle COUNT查询包装
     */
    @Override
    protected String buildCountQuerySql(String cleanSql) {
        // Oracle中AS可以省略，子查询别名不能重复
        return StrUtil.format("SELECT COUNT(1) FROM ({})", cleanSql);
    }
}
