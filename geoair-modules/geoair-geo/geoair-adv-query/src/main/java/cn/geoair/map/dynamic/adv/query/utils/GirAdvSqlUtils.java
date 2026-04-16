package cn.geoair.map.dynamic.adv.query.utils;

import cn.geoair.map.dynamic.adv.mybatis.SqlEngineUtil;
import cn.geoair.map.dynamic.adv.mybatis.SqlMeta;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.apo.PageApo;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamMap;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsGeomOpt;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;

import java.util.List;

/**
 * @author ：张俊
 * @date ：Created in 2026/4/16 12:51
 * @description： 查询的相关通用处理逻辑
 */
public class GirAdvSqlUtils {
    /**
     * 解析带参数的SQL语句，生成可执行的SQL和参数列表
     */
    public static SqlMeta parseSqlWithParam(String sqlStatement, SqlParamMap sqlParam, DialectTableNameProcessor dialectTableNameProcessor) {
        if (StrUtil.isEmpty(sqlStatement)) {
            throw new IllegalArgumentException("SQL语句不能为空");
        }
        String cleanSql = dialectTableNameProcessor.tbRemoveSqlSpaces(sqlStatement);
        return SqlEngineUtil.getEngine().parse(cleanSql, sqlParam);
    }

}
