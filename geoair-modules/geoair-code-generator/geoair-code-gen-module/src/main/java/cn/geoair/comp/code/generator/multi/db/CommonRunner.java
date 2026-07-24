package cn.geoair.comp.code.generator.multi.db;

import cn.geoair.base.Gir;
import cn.geoair.base.util.GutilObject;
import cn.geoair.comp.code.generator.multi.domian.GenTable;
import cn.geoair.comp.code.generator.multi.domian.GenTableColumn;
import cn.geoair.comp.code.generator.multi.utils.GenUtils;
import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.geoair.map.dynamic.adv.query.apo.DataFieldsApo;
import cn.geoair.map.dynamic.adv.query.apo.FieldBySchemaApo;
import cn.geoair.map.dynamic.adv.query.apo.SchemaTableApo;
import cn.geoair.map.dynamic.adv.spring.AdvExecutorFactory;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

/**
 * @author ：张逢吉
 * @date ：Created in 13:24 @description： 数据库配置获取器
 */
public class CommonRunner implements ICommonRunner {

    DataSource dataSource;

    IAdvExecutor executor;

    public CommonRunner(DataSource dataSource) {
        this.dataSource = dataSource;
        executor = AdvExecutorFactory.getAdvExecutorByDataSource(dataSource);
    }

    public List<GenTableColumn> getTableColumnsByTableName(String tableName) {
        List<GenTableColumn> genTableColumnList = ListUtil.list(true);
        DataFieldsApo dataFieldsApo = executor.dGetColumnsByTable(tableName);
        if (dataFieldsApo == null) {
            return genTableColumnList;
        }

        List<FieldBySchemaApo> dataFieldList = dataFieldsApo.getDataFieldList(true);
        GenTable genTable = new GenTable();
        genTable.setTableName(tableName);

        for (FieldBySchemaApo fieldBySchemaApo : dataFieldList) {
            Gir.log.info("数据库字段信息：{}", fieldBySchemaApo);
            GenTableColumn genTableColumn = new GenTableColumn();
            genTableColumn.setTableName(tableName);
            genTableColumn.setColumnName(fieldBySchemaApo.getColumnName());

            String columnComment = fieldBySchemaApo.getColumnComment();
            if (StrUtil.isEmpty(columnComment)) {
                columnComment = "";
            } else {
                columnComment = StrUtil.trim(columnComment);
                columnComment = StrUtil.removeAllLineBreaks(columnComment);
            }
            genTableColumn.setColumnComment(columnComment);
            genTableColumn.setColumnType(fieldBySchemaApo.getUdtName());
            genTableColumn.setNumericPrecision(fieldBySchemaApo.getNumericPrecision());
            genTableColumn.setNumericPrecisionRadix(fieldBySchemaApo.getNumericPrecisionRadix());
            genTableColumn.setIsPk(fieldBySchemaApo.isPrimaryKeyIs() ? "1" : "0");
            genTableColumn.setIsIncrement("0");
            genTableColumn.setIsRequired("NO".equals(fieldBySchemaApo.getIsNullable()) ? "1" : "0");

            GenUtils.initColumnField(genTableColumn, genTable);
            Gir.log.info("转换后字段信息：{}", genTableColumn);
            genTableColumnList.add(genTableColumn);
        }
        Gir.log.info("获取到{}个字段", genTableColumnList.size());
        return genTableColumnList;
    }

    public List<GenTable> selectDbTableListByNames(String[] tableNames) {
        List<GenTable> tableList = ListUtil.list(true);
        if (tableNames == null || tableNames.length == 0) {
            return tableList;
        }

        List<SchemaTableApo> schemaTableList = executor.dGetTableAndViewBySchema();
        Map<String, SchemaTableApo> schemaTableMap = new LinkedHashMap<>();
        if (GutilObject.isNotEmpty(schemaTableList)) {
            for (SchemaTableApo schemaTable : schemaTableList) {
                if (schemaTable != null && StrUtil.isNotEmpty(schemaTable.getName())) {
                    schemaTableMap.put(normalizeTableName(schemaTable.getName()), schemaTable);
                }
            }
        }

        for (String tableName : tableNames) {
            if (StrUtil.isEmpty(tableName)) {
                continue;
            }
            SchemaTableApo schemaTable = schemaTableMap.get(normalizeTableName(tableName));
            if (schemaTable == null) {
                continue;
            }

            GenTable genTable = new GenTable();
            genTable.setTableName(schemaTable.getName());

            String tableComment = executor.dGetTableComment(schemaTable.getName());
            if (StrUtil.isNotEmpty(tableComment)) {
                tableComment = StrUtil.trim(tableComment);
                tableComment = StrUtil.removeAllLineBreaks(tableComment);
            } else {
                tableComment = "";
            }
            genTable.setTableComment(tableComment);
            tableList.add(genTable);
        }
        return tableList;
    }

    private String normalizeTableName(String tableName) {
        if (StrUtil.isEmpty(tableName)) {
            return "";
        }
        String normalized = StrUtil.subAfter(tableName, ".", true);
        return StrUtil.isEmpty(normalized)
                ? tableName.trim().toLowerCase()
                : normalized.trim().toLowerCase();
    }
}
