package cn.geoair.map.dynamic.adv.query.wherequery;

import java.util.Map;
import lombok.Getter;

/**
 * @author ：张俊
 * @date ：Created in 2026/4/16 13:43
 * @description： 转换配置选项
 */
@Getter
public class ConvertOptions {
    private boolean toUnderlineCase = true; // 是否转换为下划线命名
    private boolean ignoreNull = true; // 是否忽略null值
    private boolean ignoreEmptyString = true; // 是否忽略空字符串
    private boolean ignoreEmptyCollection = true; // 是否忽略空集合
    private boolean throwOnNull = false; // null值时是否抛出异常
    private boolean autoLike = false; // 是否自动识别LIKE
    private int autoLikeThreshold = 20; // 自动LIKE的阈值（字符串长度超过此值时自动转为LIKE）
    private Map<String, FieldMapping> fieldMappings; // 字段映射配置

    public static ConvertOptions defaultOptions() {
        return new ConvertOptions();
    }

    public static ConvertOptions strictOptions() {
        return new ConvertOptions().setIgnoreNull(false).setThrowOnNull(true);
    }

    public static ConvertOptions likeOptions() {
        return new ConvertOptions().setAutoLike(true).setAutoLikeThreshold(2);
    }

    public ConvertOptions setToUnderlineCase(boolean toUnderlineCase) {
        this.toUnderlineCase = toUnderlineCase;
        return this;
    }

    public ConvertOptions setIgnoreNull(boolean ignoreNull) {
        this.ignoreNull = ignoreNull;
        return this;
    }

    public ConvertOptions setIgnoreEmptyString(boolean ignoreEmptyString) {
        this.ignoreEmptyString = ignoreEmptyString;
        return this;
    }

    public ConvertOptions setIgnoreEmptyCollection(boolean ignoreEmptyCollection) {
        this.ignoreEmptyCollection = ignoreEmptyCollection;
        return this;
    }

    public ConvertOptions setThrowOnNull(boolean throwOnNull) {
        this.throwOnNull = throwOnNull;
        return this;
    }

    public ConvertOptions setAutoLike(boolean autoLike) {
        this.autoLike = autoLike;
        return this;
    }

    public ConvertOptions setAutoLikeThreshold(int autoLikeThreshold) {
        this.autoLikeThreshold = autoLikeThreshold;
        return this;
    }

    public ConvertOptions setFieldMappings(Map<String, FieldMapping> fieldMappings) {
        this.fieldMappings = fieldMappings;
        return this;
    }

    public boolean hasFieldMappings() {
        return fieldMappings != null && !fieldMappings.isEmpty();
    }
}
