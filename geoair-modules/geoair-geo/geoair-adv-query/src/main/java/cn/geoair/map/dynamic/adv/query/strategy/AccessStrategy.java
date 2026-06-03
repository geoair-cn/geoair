package cn.geoair.map.dynamic.adv.query.strategy;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 实体类插入的策略配置
 *
 * @author 张俊
 * @date 2026/6/3
 */
@Data
@Accessors(chain = true)
public class AccessStrategy {

    /**
     * 主键字段名（优先级高于注解）
     */
    String idKey;

    /**
     * 表名（优先级高于注解）
     */
    String tableName;

    /**
     * 需要忽略的字段列表（实体属性名）
     */
    List<String> ignoreFieldNames = new ArrayList<>();

    /**
     * 冲突判定字段列表（INSERT IGNORE时使用）
     */
    List<String> conflictKeys = new ArrayList<>();

    /**
     * 是否对实体类进行驼峰转下划线
     */
    boolean toUnderlineCase = true;

    /**
     * 是否忽略空值字段
     */
    boolean ignoreNullValue = true;

    /**
     * 是否忽略空字符串
     */
    boolean ignoreEmptyString = true;


    /**
     *  批量插入的时候，一个批次提交的大小
     */
    int batchSize = 1000;

    /**
     * 辅助方法：添加忽略字段
     */
    public AccessStrategy ignoreField(String... fieldNames) {
        this.ignoreFieldNames.addAll(Arrays.asList(fieldNames));
        return this;
    }

    /**
     * 辅助方法：添加冲突键
     */
    public AccessStrategy conflictKey(String... keys) {
        this.conflictKeys.addAll(Arrays.asList(keys));
        return this;
    }

    public AccessStrategy tableName(String tableName) {
        this.tableName = tableName;
        return this;
    }

    /**
     * 设置为选择性插入（自动过滤null）
     */
    public AccessStrategy selective() {
        this.ignoreNullValue = true;
        return this;
    }

    public AccessStrategy idKey(String idKey) {
        this.idKey = idKey;
        return this;
    }
}
