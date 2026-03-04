package cn.geoair.comp.code.generator.multi.db;

import cn.geoair.comp.code.generator.multi.domian.GenTable;
import cn.geoair.comp.code.generator.multi.domian.GenTableColumn;
import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.geoair.map.dynamic.adv.query.apo.DataFieldsApo;
import cn.geoair.map.dynamic.adv.query.apo.FieldBySchemaApo;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamMap;
import cn.geoair.map.dynamic.adv.spring.AdvExecutorFactory;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;

import javax.sql.DataSource;
import java.util.List;

/**
 * @author ：张逢吉
 * @date ：Created in   13:24
 * @description： 数据库配置获取器
 */
public class CommonRuner {

    DataSource dataSource;

    IAdvExecutor executor;

    public CommonRuner(DataSource dataSource) {
        this.dataSource = dataSource;
        executor = AdvExecutorFactory.getAdvExecutorByDataSource(dataSource);
    }


    public String getTableCommentByTableName(String tableName) {
        String tableComment;
        tableComment = executor.dGetTableComment(tableName);
        return tableComment;
    }


    public List<GenTableColumn> getTableColumnsByTableName(String tableName) {
        // 初始化返回列表
        List<GenTableColumn> genTableColumnList = ListUtil.list(true);

        // 1. 获取数据库表字段信息
        DataFieldsApo dataFieldsApo = executor.dGetColumnsByTable(tableName);
        List<FieldBySchemaApo> dataFieldList = dataFieldsApo.getDataFieldList(true);

        // 2. 遍历转换每个字段
        for (FieldBySchemaApo fieldBySchemaApo : dataFieldList) {
            GenTableColumn genTableColumn = new GenTableColumn();
            genTableColumn.setTableName(tableName);
            // 列名称
            genTableColumn.setColumnName(fieldBySchemaApo.getColumnName());
            // 列描述
            String columnComment = fieldBySchemaApo.getColumnComment();
            if(StrUtil.isEmpty(columnComment)){
                columnComment = "";
            }else{
                columnComment= StrUtil.trim(columnComment);
                columnComment= StrUtil.removeAllLineBreaks(columnComment);
            }
            genTableColumn.setColumnComment(columnComment);
            // 列类型（使用PG的dataType）
            genTableColumn.setColumnType(fieldBySchemaApo.getDataType());

            // Java类型映射
            genTableColumn.setJavaType(fieldBySchemaApo.getJavaClassName());
            // 拓展Java类型（暂时和基础类型一致，可根据需要扩展）
            genTableColumn.setJavaTypeOther(fieldBySchemaApo.getJavaClassName());

            // Java字段名（下划线转驼峰）
            String columnName = fieldBySchemaApo.getColumnName();
            String javaField = StrUtil.toCamelCase(columnName);
            genTableColumn.setJavaField(javaField);

            // 是否主键
            genTableColumn.setIsPk(fieldBySchemaApo.isPrimaryKeyIs() ? "1" : "0" );

            genTableColumn.setIsIncrement("0" );
            // 是否必填（isNullable为NO表示必填）
            genTableColumn.setIsRequired("NO" .equals(fieldBySchemaApo.getIsNullable()) ? "1" : "0" );

            // 添加到列表
            genTableColumnList.add(genTableColumn);
        }

        return genTableColumnList;
    }


    public List<GenTable> selectDbTableListByNames(String[] tableNames) {
        String sql = "\t\tSELECT\n" +
                "\t\trelname AS table_name,\n" +
                "\t\tobj_description ( C.oid ) AS table_comment\n" +
                "\t\tFROM\n" +
                "\t\tpg_class C\n" +
                "\t\tWHERE\n" +
                "\t\tc.relkind IN ('r', 'v')\n" +
                "\t\tAND C.relnamespace = ( SELECT oid FROM pg_namespace WHERE nspname = ( SELECT CURRENT_SCHEMA ( ) ) )\n" +
                "\t\tAND C.relname NOT LIKE'qrtz_%'\n" +
                "\t\tAND C.relname NOT LIKE'gen_%'\n" +
                "\t\tand C.relname in\n" +
                "\t\t<foreach collection=\"array\" item=\"name\" open=\"(\" separator=\",\" close=\")\">\n" +
                "\t\t\t#{name}\n" +
                "\t\t</foreach>" ;
        return executor.bSelectObjList(sql, SqlParamMap.of().addOne("array" , tableNames), GenTable.class);
    }


}
