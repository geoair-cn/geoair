package cn.geoair.map.dynamic.adv.query.utils;

import cn.geoair.map.dynamic.adv.query.apo.PageApo;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsGeomOpt;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.hutool.core.collection.CollectionUtil;

import java.util.List;

/**
 * @author ：张俊
 * @date ：Created in 2026/4/16 12:51
 * @description： 查询的相关通用处理逻辑
 */
public class GirAdvQueryCommonUtils {
    /**
     * 根据结果集创建分页对象
     *
     * @param total            总数
     * @param pageNum          页号
     * @param pageSize         页数
     * @param pageNumStartZero 页号是不是从0开始
     * @param lastPageNum      最后一页
     * @param startRow         开始行数
     * @param records          记录集
     * @return 分页对象
     */
    public static PageApo<GirAdvOneRow> createPageApo(
            long total,
            int pageNum,
            int pageSize,
            boolean pageNumStartZero,
            int lastPageNum,
            long startRow,
            List<GirAdvOneRow> records) {
        PageApo<GirAdvOneRow> pageApo = new PageApo<>();
        pageApo.setTotal(total)
                .setPageNum(pageNum)
                .setPageSize(pageSize)
                .setPageNumStartZero(pageNumStartZero)
                .setLastPageNum(lastPageNum)
                .setStartRow(startRow)
                .setRecords(records);
        return pageApo;
    }


    /**
     * 处理空间字段值（转换WKT/GeoJSON等）
     */
    public static void transGeometryField(
            List<GirAdvOneRow> records,
            AdvEnumsGeomOpt advEnumsGeomOpt,
            List<String> geomFieldNameList) {
        if (CollectionUtil.isEmpty(geomFieldNameList)) {
            return;
        }
        for (GirAdvOneRow record : records) {
            for (String geomFieldName : geomFieldNameList) {
                if (!record.containsKey(geomFieldName)) {
                    continue;
                }
                if (AdvEnumsGeomOpt.不做任何操作.equals(advEnumsGeomOpt)) {
                    continue;
                }
                if (AdvEnumsGeomOpt.转换成WKT.equals(advEnumsGeomOpt)) {
                    record.put(geomFieldName, record.getWktString(geomFieldName, ""));
                }
                if (AdvEnumsGeomOpt.转换成GeoJson.equals(advEnumsGeomOpt)) {
                    record.put(geomFieldName, record.getGeoJsonStr(geomFieldName, "{}"));
                }
                if (AdvEnumsGeomOpt.转换成WKB.equals(advEnumsGeomOpt)) {
                    record.put(geomFieldName, record.getWkBString(geomFieldName, ""));
                }
                if (AdvEnumsGeomOpt.转换为NULL.equals(advEnumsGeomOpt)) {
                    record.put(geomFieldName, null);
                }
                if (AdvEnumsGeomOpt.转换为空字符串.equals(advEnumsGeomOpt)) {
                    record.put(geomFieldName, "");
                }
                if (AdvEnumsGeomOpt.移除.equals(advEnumsGeomOpt)) {
                    record.remove(geomFieldName);
                }
            }
        }
    }
}
