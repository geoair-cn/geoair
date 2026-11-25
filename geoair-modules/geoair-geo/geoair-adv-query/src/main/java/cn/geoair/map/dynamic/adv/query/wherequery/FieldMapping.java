package cn.geoair.map.dynamic.adv.query.wherequery;

import cn.geoair.map.dynamic.adv.query.enums.AdvOperatorEnums;
import lombok.Getter;

/**
 * @author ：张俊
 * @date ：Created in 2026/4/16 13:44
 * @description： 字段映射配置类
 */
@Getter
public class FieldMapping {
    private String columnName; // 数据库列名
    private AdvOperatorEnums operator; // 操作符
    private boolean ignoreNull = true; // 是否忽略null值

    public FieldMapping() {}

    public FieldMapping(String columnName) {
        this.columnName = columnName;
    }

    public FieldMapping(String columnName, AdvOperatorEnums operator) {
        this.columnName = columnName;
        this.operator = operator;
    }

    public FieldMapping setColumnName(String columnName) {
        this.columnName = columnName;
        return this;
    }

    public FieldMapping setOperator(AdvOperatorEnums operator) {
        this.operator = operator;
        return this;
    }

    public FieldMapping setIgnoreNull(boolean ignoreNull) {
        this.ignoreNull = ignoreNull;
        return this;
    }

    // 静态工厂方法
    public static FieldMapping eq(String columnName) {
        return new FieldMapping(columnName, AdvOperatorEnums.等于);
    }

    public static FieldMapping ne(String columnName) {
        return new FieldMapping(columnName, AdvOperatorEnums.不等于);
    }

    public static FieldMapping gt(String columnName) {
        return new FieldMapping(columnName, AdvOperatorEnums.大于);
    }

    public static FieldMapping ge(String columnName) {
        return new FieldMapping(columnName, AdvOperatorEnums.大于等于);
    }

    public static FieldMapping lt(String columnName) {
        return new FieldMapping(columnName, AdvOperatorEnums.小于);
    }

    public static FieldMapping le(String columnName) {
        return new FieldMapping(columnName, AdvOperatorEnums.小于等于);
    }

    public static FieldMapping like(String columnName) {
        return new FieldMapping(columnName, AdvOperatorEnums.LIKE_ALL);
    }

    public static FieldMapping likeLeft(String columnName) {
        return new FieldMapping(columnName, AdvOperatorEnums.LIKE_LEFT);
    }

    public static FieldMapping likeRight(String columnName) {
        return new FieldMapping(columnName, AdvOperatorEnums.LIKE_RIGHT);
    }

    public static FieldMapping in(String columnName) {
        return new FieldMapping(columnName, AdvOperatorEnums.IN);
    }

    public static FieldMapping between(String columnName) {
        return new FieldMapping(columnName, AdvOperatorEnums.BETWEEN);
    }

    public static FieldMapping isNull(String columnName) {
        return new FieldMapping(columnName, AdvOperatorEnums.IS_NULL);
    }

    public static FieldMapping isNotNull(String columnName) {
        return new FieldMapping(columnName, AdvOperatorEnums.IS_NOT_NULL);
    }
}
