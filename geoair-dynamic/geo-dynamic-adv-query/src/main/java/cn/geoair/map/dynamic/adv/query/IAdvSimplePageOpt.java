package cn.geoair.map.dynamic.adv.query;


import cn.geoair.map.dynamic.adv.query.apo.OrderApo;
import cn.geoair.map.dynamic.adv.query.apo.PageApo;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsGeomOpt;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;

import java.util.List;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/10/10 10:34
 * @description： 简单分页相关操作接口
 * 约定： 以p开头
 */
public interface IAdvSimplePageOpt {

    /**
     * 统计指定SQL的查询结果总数（忽略分页，获取全量数据条数）
     * <p>适用场景：单独获取总数（如仅展示“共XX条”，无需分页数据）</p>
     * 返回结果与 bSelectRecordRowCount 等同
     * @param noPageSql 不带分页后缀的原始SQL（支持SELECT语句，无需含LIMIT/OFFSET）
     * @return 全量数据总条数（无数据返回0L，SQL错误抛异常）
     */
    Long pCount(String noPageSql);

    /**
     * 构建分页SQL（自定义起始页+空间操作+字段元数据）
     */
    String pBuildPageSql(String noPageSql, int pageSize, int pageNum, boolean pageNumStartZero);

    /**
     * 构建带排序的SQL（追加ORDER BY子句）
     *
     * @param baseSql    基础SQL（已重构的SQL）
     * @param orders     排序规则列表
     * @param tableAlias 临时表别名（用于排序字段定位，避免多表冲突）
     */
    String pBuildSqlWithOrder(String baseSql, List<OrderApo> orders, String tableAlias);
    /**
     * 构建带排序的SQL（追加ORDER BY子句）
     *
     * @param baseSql    基础SQL（已重构的SQL）
     * @param orders     排序规则列表
     */
    String pBuildSqlWithOrder(String baseSql, List<OrderApo> orders);


    /**
     * 全场景分页（自定义起始页+空间操作+字段元数据）
     * <p>适用场景：复杂综合场景（如GIS动态表格+前端0开始页码）</p>
     *
     * @param noPageSql        原始SQL（含空间字段）
     * @param pageNum          页码（按startZero规则）
     * @param pageSize         每页条数（需>0）
     * @param pageNumStartZero 页码起始规则（true=0开始，false=1开始）
     * @param advEnumsGeomOpt  空间操作规则（如转换为空字符串）
     * @param hasFieldsInfo    是否返回字段元数据（true=返回）
     * @param orders           排序键
     * @return 分页结果
     */
    PageApo<GirAdvOneRow> pPage(String noPageSql, int pageNum, int pageSize, boolean pageNumStartZero, AdvEnumsGeomOpt advEnumsGeomOpt, boolean hasFieldsInfo, List<OrderApo> orders);


    PageApo<GirAdvOneRow> pPage(String noPageSql, int pageNum, int pageSize);

    PageApo<GirAdvOneRow> pPage(String noPageSql, int pageNum, int pageSize, AdvEnumsGeomOpt advEnumsGeomOpt);

    PageApo<GirAdvOneRow> pPage(String noPageSql, int pageNum, int pageSize, List<OrderApo> orders);

    PageApo<GirAdvOneRow> pPage(String noPageSql, int pageNum, int pageSize, boolean pageNumStartZero, boolean hasFieldsInfo);

    PageApo<GirAdvOneRow> pPage(String noPageSql, int pageNum, int pageSize, boolean pageNumStartZero, AdvEnumsGeomOpt advEnumsGeomOpt, boolean hasFieldsInfo);

    PageApo<GirAdvOneRow> pPage(String noPageSql, int pageNum, int pageSize, boolean pageNumStartZero);

    PageApo<GirAdvOneRow> pPage(String noPageSql, int pageNum, int pageSize, boolean pageNumStartZero, List<OrderApo> orders);

    PageApo<GirAdvOneRow> pPage(String noPageSql, int pageNum, int pageSize, boolean pageNumStartZero, AdvEnumsGeomOpt advEnumsGeomOpt);

    PageApo<GirAdvOneRow> pPage(String noPageSql, int pageNum, int pageSize, boolean pageNumStartZero, AdvEnumsGeomOpt advEnumsGeomOpt, List<OrderApo> orders);

    PageApo<GirAdvOneRow> pPage(String noPageSql, int pageNum, int pageSize, AdvEnumsGeomOpt advEnumsGeomOpt, boolean hasFieldsInfo);

    PageApo<GirAdvOneRow> pPage(String noPageSql, int pageNum, int pageSize, AdvEnumsGeomOpt advEnumsGeomOpt, boolean hasFieldsInfo, List<OrderApo> orders);


}
