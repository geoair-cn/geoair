package cn.geoair.map.dynamic.adv.query.dialect.pgv2.base;


import cn.geoair.map.dynamic.adv.query.dialect.AbstractAdvBaseSelectOpt;
import cn.geoair.map.dynamic.adv.query.dialect.pgv2.PgDialectTableNameUtil;


/**
 * PostgreSQL查询操作实现类
 */
public class PgAdvBaseSelectOpt extends AbstractAdvBaseSelectOpt {


    // 初始化表名处理器
    public PgAdvBaseSelectOpt() {
        this.dialectTableNameProcessor = PgDialectTableNameUtil.getInstance();
    }


}
