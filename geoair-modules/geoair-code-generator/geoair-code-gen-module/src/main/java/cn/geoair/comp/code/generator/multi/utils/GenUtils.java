package cn.geoair.comp.code.generator.multi.utils;

import cn.geoair.base.Gir;
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
                        String[] ls = e.split(":", 2);
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

        column.setJavaField(StringUtils.toCamelCase(column.getColumnName()));

        String dataType = normalizeDbType(column.getColumnType());
        if (StringUtils.isEmpty(dataType)) {
            if (StringUtils.isEmpty(column.getJavaType())) {
                column.setJavaType(GenConstants.TYPE_STRING);
            }
            return;
        }

        if (arraysContains(GenConstants.COLUMNTYPE_TIME, dataType)) {
            column.setJavaType(GenConstants.TYPE_DATE);
        } else if (arraysContains(GenConstants.COLUMNTYPE_NUMBER, dataType)) {
            handleNumberType(column);
        } else if (isGeometryType(dataType)) {
            column.setJavaType(GenConstants.TYPE_Geometry);
        } else if ("uuid".equals(dataType)) {
            column.setJavaType(GenConstants.TYPE_STRING);
        }

        if (StringUtils.isEmpty(column.getJavaType())) {
            column.setJavaType(GenConstants.TYPE_STRING);
        }

        Gir.log.info(
                column.getJavaField()
                        + ": 转换后"
                        + column.getJavaType()
                        + ": 数据库类型 "
                        + column.getColumnType());
    }

    /** 处理数字类型转换 */
    private static void handleNumberType(GenTableColumn column) {
        String scale = column.getNumericScale();
        String precision = column.getNumericPrecision();
        if (StringUtils.isNotEmpty(scale) && !"0".equals(scale.trim())) {
            column.setJavaType(GenConstants.TYPE_BIGDECIMAL);
            return;
        }

        Integer precisionValue = null;
        if (StringUtils.isNotEmpty(precision)) {
            try {
                precisionValue = Integer.valueOf(precision.trim());
            } catch (Exception ignored) {
            }
        }

        if (precisionValue != null) {
            if (precisionValue <= 9) {
                column.setJavaType(GenConstants.TYPE_INTEGER);
            } else if (precisionValue <= 18) {
                column.setJavaType(GenConstants.TYPE_LONG);
            } else {
                column.setJavaType(GenConstants.TYPE_BIGDECIMAL);
            }
            return;
        }

        GenConstants.convertDbTypeToJavaType(normalizeDbType(column.getColumnType()), column);
        if (StringUtils.isEmpty(column.getJavaType())) {
            column.setJavaType(GenConstants.TYPE_BIGDECIMAL);
        }
    }

    /** 校验数组是否包含指定值 */
    public static boolean arraysContains(String[] arr, String targetValue) {
        if (arr == null || StringUtils.isEmpty(targetValue)) {
            return false;
        }
        String normalizedTarget = normalizeDbType(targetValue);
        for (String value : arr) {
            String normalizedValue = normalizeDbType(value);
            if (StringUtils.isEmpty(normalizedValue)) {
                continue;
            }
            if (normalizedTarget.equals(normalizedValue)
                    || normalizedTarget.startsWith(normalizedValue + " ")
                    || normalizedTarget.startsWith(normalizedValue + "(")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isGeometryType(String dataType) {
        return "geometry".equals(dataType)
                || "geography".equals(dataType)
                || "sdo_geometry".equals(dataType)
                || "mgeometry".equals(dataType);
    }

    private static String normalizeDbType(String dataType) {
        if (StringUtils.isEmpty(dataType)) {
            return "";
        }
        String normalized = dataType.trim().toLowerCase();
        int idx = normalized.indexOf('(');
        if (idx > 0) {
            normalized = normalized.substring(0, idx);
        }
        return normalized.trim();
    }

    public static String getBusinessName(String tableName) {
        return StringUtils.convertToCamelCase(tableName);
    }

    /** 表名转换成Java类名（修复数组越界） */
    public static String convertClassName(String tableName, GirGeneratorConfig config) {
        if (StringUtils.isEmpty(tableName)) {
            return "";
        }
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
        Arrays.sort(searchList, (a, b) -> b.length() - a.length());
        for (String searchString : searchList) {
            if (StringUtils.isNotEmpty(searchString) && text.startsWith(searchString)) {
                return text.substring(searchString.length());
            }
        }
        return text;
    }
}
