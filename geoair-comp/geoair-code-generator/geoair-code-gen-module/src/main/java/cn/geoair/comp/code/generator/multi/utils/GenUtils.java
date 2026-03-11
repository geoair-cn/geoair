package cn.geoair.comp.code.generator.multi.utils;

import cn.geoair.comp.code.generator.multi.config.GirGeneratorConfig;
import cn.geoair.comp.code.generator.multi.domian.GenTable;
import cn.geoair.comp.code.generator.multi.domian.GenTableColumn;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class GenUtils {

    /** 初始化表信息（移除静态依赖，改为传入配置） */
    public static void initTable(GenTable genTable, GirGeneratorConfig config) {
        genTable.setClassName(convertClassName(genTable.getTableName(), config));
        genTable.setPackageName(config.getSourceRootPackage());
        genTable.setModuleName(config.getModuleName());
        genTable.setBusinessName(getBusinessName(genTable.getTableName()));
        genTable.setFunctionName(genTable.getTableComment());
        genTable.setFunctionAuthor(config.getAuthor());
    }

    /** 初始化列属性字段（修复空指针） */
    public static void initColumnField(GenTableColumn column, GenTable table) {
        if (column == null || StringUtils.isEmpty(column.getColumnName())) {
            return;
        }
        // 处理枚举注释
        String columnComment = column.getColumnComment();
        if (StringUtils.isNotEmpty(columnComment) && columnComment.contains("#em=")) {
            String[] emms = columnComment.split("#em=");
            String comm1 = emms[1];
            if (comm1.contains(";")) {
                String[] en = comm1.split(";");
                Map<String, String> map = new HashMap<>();
                for (String e : en) {
                    if (e.contains(":")) {
                        String[] ls = e.split(":", 2); // 限制分割次数，避免值包含冒号
                        map.put(ls[0], ls[1]);
                    }
                }
                column.setEnums(map);
                column.setEnumsName(
                        convertClassName(table.getTableName(), null)
                                + convertClassName(column.getColumnName(), null)
                                + "Enum");
            } else {
                column.setEnumsName(comm1);
            }
            column.setColumnComment(emms[0]);
        }

        // 设置java字段名
        column.setJavaField(StringUtils.toCamelCase(column.getColumnName()));
        // 设置默认类型
        column.setJavaType(GenConstants.TYPE_STRING);

        // 处理数据库类型转换
        String dataType = getDbType(column.getColumnType());
        if (StringUtils.isEmpty(dataType)) {
            return;
        }

        if (arraysContains(GenConstants.COLUMNTYPE_TIME, dataType)) {
            column.setJavaType(GenConstants.TYPE_DATE);
        } else if (arraysContains(GenConstants.COLUMNTYPE_NUMBER, dataType)) {
            handleNumberType(column, dataType);
        }
        if (dataType.equals("geometry")) {
            column.setJavaTypeOther("Geometry");
        }
        if (dataType.equals("bytea")) {
            column.setJavaType(GenConstants.TYPE_BYTE);
        }
    }

    /** 处理数字类型转换 */
    private static void handleNumberType(GenTableColumn column, String dataType) {
        String columnType = column.getColumnType();
        GenConstants.convertDbTypeToJavaType(columnType, column);
        //
        //        String[] str = StringUtils.split(StringUtils.substringBetween(columnType, "(",
        // ")"), ",");
        //        // 浮点型（包含小数位）
        //        if (str != null && str.length == 2 && Integer.parseInt(str[1]) > 0) {
        //            column.setJavaType(GenConstants.TYPE_BIGDECIMAL);
        //        }
        //        // 整型（长度<=10）
        //        else if ((str != null && str.length == 1 && Integer.parseInt(str[0]) <= 10)
        //                || (str == null && columnType.toUpperCase().contains("INT"))) {
        //            column.setJavaType(GenConstants.TYPE_INTEGER);
        //        } else {
        //            column.setJavaType(GenConstants.TYPE_LONG);
        //        }
        //        String[] decimalType = {"decimal"};
        //        if (Arrays.asList(decimalType).contains(columnType)) {
        //            column.setJavaType(GenConstants.TYPE_BIGDECIMAL);
        //        }
        //        String[] doubleType = {"numeric", "number", "double", "float8", "float4"};
        //        if (Arrays.asList(doubleType).contains(columnType)) {
        //            column.setJavaType(GenConstants.TYPE_DOUBLE);
        //        }
        //
        //        String[] longType = {"int8"};
        //        if (Arrays.asList(longType).contains(columnType)) {
        //            column.setJavaType(GenConstants.TYPE_LONG);
        //        }
    }

    /** 校验数组是否包含指定值 */
    public static boolean arraysContains(String[] arr, String targetValue) {
        return arr != null && Arrays.asList(arr).contains(targetValue);
    }

    public static String getBusinessName(String tableName) {
        return StringUtils.convertToCamelCase(tableName);
    }

    /** 表名转换成Java类名（修复数组越界） */
    public static String convertClassName(String tableName, GirGeneratorConfig config) {
        if (StringUtils.isEmpty(tableName)) {
            return "";
        }
        // 移除表前缀
        if (config != null
                && config.isRemovePre()
                && StringUtils.isNotEmpty(config.getTablePrefix())) {
            String[] searchList = StringUtils.split(config.getTablePrefix(), ",");
            tableName = replaceFirst(tableName, searchList);
        }
        return StringUtils.convertToCamelCase(tableName);
    }

    /** 批量替换前缀（修复替换逻辑） */
    public static String replaceFirst(String text, String[] searchList) {
        if (StringUtils.isEmpty(text) || searchList == null || searchList.length == 0) {
            return text;
        }
        // 按前缀长度降序排序，优先替换长前缀
        Arrays.sort(searchList, (a, b) -> b.length() - a.length());
        for (String searchString : searchList) {
            if (StringUtils.isNotEmpty(searchString) && text.startsWith(searchString)) {
                return text.substring(searchString.length());
            }
        }
        return text;
    }

    /** 获取数据库类型字段（修复空指针） */
    public static String getDbType(String columnType) {
        if (StringUtils.isEmpty(columnType)) {
            return "";
        }
        int index = StringUtils.indexOf(columnType, "(");
        if (index > 0) {
            return StringUtils.substringBefore(columnType, "(");
        } else {
            return columnType;
        }
    }
}
