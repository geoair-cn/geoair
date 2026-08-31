package cn.geoair.comp.code.generator.multi.domian;

import cn.geoair.base.util.GutilStr;

import lombok.Data;

import java.util.Map;

/**
 * 代码生成业务字段表 gen_table_column
 *
 * @author ray
 */
@Data
public class GenTableColumn {

    private static final long serialVersionUID = 1L;

    private String tableName;

    /** 列名称 */
    private String columnName;

    /** 列描述 */
    private String columnComment;

    /** 列类型 */
    private String columnType;

    /** 数值精度 */
    private String numericPrecision;

    /** 数值小数位 */
    private String numericScale;

    /** JAVA类型 */
    private String javaType;

    private String javaField;

    /** 是否主键（1是） */
    private String isPk;

    /** 是否自增（1是） */
    private String isIncrement;

    /** 是否必填（1是） */
    private String isRequired;

    /** 枚举 */
    private Map enums;

    /** 枚举类名 */
    private String enumsName;

    public boolean isPk() {
        return isPk(this.isPk);
    }

    public boolean isPk(String isPk) {
        return isPk != null && GutilStr.equals("1", isPk);
    }

    public boolean isIncrement() {
        return isIncrement(this.isIncrement);
    }

    public boolean isIncrement(String isIncrement) {
        return isIncrement != null && GutilStr.equals("1", isIncrement);
    }

    public boolean isRequired() {
        return isRequired(this.isRequired);
    }

    public boolean isRequired(String isRequired) {
        return isRequired != null && GutilStr.equals("1", isRequired);
    }

    public boolean isEdit(String isEdit) {
        return isEdit != null && GutilStr.equals("1", isEdit);
    }

    public boolean isQuery(String isQuery) {
        return isQuery != null && GutilStr.equals("1", isQuery);
    }

    public boolean isSuperColumn() {
        return isSuperColumn(this.javaField);
    }

    public static boolean isSuperColumn(String javaField) {
        return GutilStr.equalsAnyIgnoreCase(
                javaField,
                // BaseEntity
                "createBy",
                "createTime",
                "updateBy",
                "updateTime",
                "remark",
                // TreeEntity
                "parentName",
                "parentId",
                "orderNum",
                "ancestors");
    }

    public boolean isUsableColumn() {
        return isUsableColumn(javaField);
    }

    public static boolean isUsableColumn(String javaField) {
        // isSuperColumn()中的名单用于避免生成多余Domain属性，若某些属性在生成页面时需要用到不能忽略，则放在此处白名单
        return GutilStr.equalsAnyIgnoreCase(javaField, "parentId", "orderNum", "remark");
    }
}
