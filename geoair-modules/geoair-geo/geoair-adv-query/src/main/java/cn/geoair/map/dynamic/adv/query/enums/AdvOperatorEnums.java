package cn.geoair.map.dynamic.adv.query.enums;

import cn.geoair.base.data.GiVisualValuable;
import cn.geoair.base.data.common.GemUtil;
import cn.geoair.base.data.model.annotation.GaModelField;

/**
 * 条件操作符枚举
 *
 * <p>用于WHERE条件中的具体操作符
 *
 * @author zhangjun
 * @date Created in 2023/6/11 13:49
 */
public enum AdvOperatorEnums implements GiVisualValuable<String> {

    // ==================== 比较操作符 (1xx) ====================
    /** 大于 > */
    大于("101", ">"),

    /** 大于等于 >= */
    大于等于("102", ">="),

    /** 小于 < */
    小于("103", "<"),

    /** 小于等于 <= */
    小于等于("104", "<="),

    /** 等于 = */
    等于("105", "="),

    /** 不等于 != */
    不等于("106", "!="),

    /** NULL安全等于 <=> (MySQL) */
    EQUAL_NULL_SAFE("107", "<=>"),

    // ==================== 集合操作符 (3xx) ====================
    /** IN 查询 */
    IN("301", "in"),

    /** NOT IN 查询 */
    NOT_IN("302", "not in"),

    /** EXISTS 存在查询 */
    EXISTS("303", "exists"),

    /** NOT EXISTS 不存在查询 */
    NOT_EXISTS("304", "not exists"),

    /** ANY 任意匹配 */
    ANY("305", "any"),

    /** ALL 全部匹配 */
    ALL("306", "all"),

    // ==================== NULL操作符 (4xx) ====================
    /** IS NULL 为空 */
    IS_NULL("401", "is null"),

    /** IS NOT NULL 不为空 */
    IS_NOT_NULL("402", "is not null"),

    // ==================== 模糊匹配 (5xx) ====================
    /** 左模糊匹配 LIKE 'value%' */
    LIKE_LEFT("501", "like", MatchMode.LEFT),

    /** 右模糊匹配 LIKE '%value' */
    LIKE_RIGHT("502", "like", MatchMode.RIGHT),

    /** 全模糊匹配 LIKE '%value%' */
    LIKE_ALL("503", "like", MatchMode.ALL),

    /** NOT左模糊匹配 NOT LIKE 'value%' */
    NOT_LIKE_LEFT("504", "not like", MatchMode.LEFT),

    /** NOT右模糊匹配 NOT LIKE '%value' */
    NOT_LIKE_RIGHT("505", "not like", MatchMode.RIGHT),

    /** NOT全模糊匹配 NOT LIKE '%value%' */
    NOT_LIKE_ALL("506", "not like", MatchMode.ALL),

    /** 左模糊匹配 ILIKE 'value%' (PostgreSQL大小写不敏感) */
    ILIKE_LEFT("511", "ilike", MatchMode.LEFT),

    /** 右模糊匹配 ILIKE '%value' (PostgreSQL大小写不敏感) */
    ILIKE_RIGHT("512", "ilike", MatchMode.RIGHT),

    /** 全模糊匹配 ILIKE '%value%' (PostgreSQL大小写不敏感) */
    ILIKE_ALL("513", "ilike", MatchMode.ALL),

    // ==================== 范围操作符 (6xx) ====================
    /** BETWEEN 范围查询 */
    BETWEEN("601", "between"),

    /** NOT BETWEEN 范围查询 */
    NOT_BETWEEN("602", "not between"),
    ;

    @GaModelField(isID = true)
    private final String code;

    private final String value;
    private final MatchMode matchMode;

    /** 构造函数（无匹配模式） */
    AdvOperatorEnums(String code, String value) {
        this(code, value, null);
    }

    /** 构造函数（带匹配模式） */
    AdvOperatorEnums(String code, String value, MatchMode matchMode) {
        this.code = code;
        this.value = value;
        this.matchMode = matchMode;
    }

    /** 获取SQL操作符 */
    public String getSqlValue() {
        return value;
    }

    public String getValue() {
        return value;
    }

    public String getCode() {
        return code;
    }

    public MatchMode getMatchMode() {
        return matchMode;
    }

    /** 是否为LIKE操作 */
    public boolean isLike() {
        return this == LIKE_LEFT
                || this == LIKE_RIGHT
                || this == LIKE_ALL
                || this == NOT_LIKE_LEFT
                || this == NOT_LIKE_RIGHT
                || this == NOT_LIKE_ALL
                || this == ILIKE_LEFT
                || this == ILIKE_RIGHT
                || this == ILIKE_ALL;
    }

    /** 是否为NOT操作 */
    public boolean isNot() {
        return this == NOT_IN
                || this == NOT_EXISTS
                || this == NOT_BETWEEN
                || this == NOT_LIKE_LEFT
                || this == NOT_LIKE_RIGHT
                || this == NOT_LIKE_ALL;
    }

    /** 是否为NULL判断 */
    public boolean isNullCheck() {
        return this == IS_NULL || this == IS_NOT_NULL;
    }

    /** 是否为范围查询 */
    public boolean isRange() {
        return this == BETWEEN || this == NOT_BETWEEN;
    }

    /** 是否为IN查询 */
    public boolean isIn() {
        return this == IN || this == NOT_IN;
    }

    /** 是否为EXISTS查询 */
    public boolean isExists() {
        return this == EXISTS || this == NOT_EXISTS;
    }

    /** 匹配模式枚举 */
    public enum MatchMode {
        /** 左匹配：value% */
        LEFT,
        /** 右匹配：%value */
        RIGHT,
        /** 全匹配：%value% */
        ALL
    }

    /** 根据code获取枚举 */
    public static AdvOperatorEnums getEnumsByCode(String code) {
        if (code == null) return null;
        for (AdvOperatorEnums f : AdvOperatorEnums.values()) {
            if (f.getCode().equals(code)) {
                return f;
            }
        }
        return null;
    }

    /** 根据value获取枚举 */
    public static AdvOperatorEnums getEnumsByValue(String value) {
        if (value == null) return null;
        for (AdvOperatorEnums f : AdvOperatorEnums.values()) {
            if (f.getValue().equalsIgnoreCase(value)) {
                return f;
            }
        }
        return null;
    }

    public static String getValueByCode(String code) {
        AdvOperatorEnums enums = getEnumsByCode(code);
        return enums != null ? enums.getValue() : null;
    }

    @Override
    public String display() {
        return this.name();
    }

    @Override
    public String value() {
        return this.code;
    }

    public static AdvOperatorEnums valueOf(String value, AdvOperatorEnums ifNull) {
        return GemUtil.valueOf(AdvOperatorEnums.class, value, ifNull);
    }
}
