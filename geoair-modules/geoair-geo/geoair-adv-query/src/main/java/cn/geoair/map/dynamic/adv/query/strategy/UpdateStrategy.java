package cn.geoair.map.dynamic.adv.query.strategy;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 实体类更新的策略配置
 *
 * @author 张俊
 * @date 2026/6/3
 */
@Data
@Accessors(chain = true)
public class UpdateStrategy {

    /** 主键字段名（优先级高于注解） */
    String idKey;

    /** 表名（优先级高于注解） */
    String tableName;

    /** 需要忽略的字段列表（实体属性名） */
    List<String> ignoreFieldNames = new ArrayList<>();

    /** 冲突判定字段列表（UPSERT时使用，通常是主键或唯一索引） */
    List<String> conflictKeys = new ArrayList<>();

    /** 是否对实体类进行驼峰转下划线 */
    boolean toUnderlineCase = true;

    /** 是否忽略null值字段 */
    boolean ignoreNullValue = true;

    /** 是否忽略空字符串 */
    boolean ignoreEmptyString = true;

    /** 批量更新的时候，一个批次提交的大小 */
    int batchSize = 1000;

    /** 辅助方法：添加忽略字段 */
    public UpdateStrategy ignoreField(String... fieldNames) {
        this.ignoreFieldNames.addAll(Arrays.asList(fieldNames));
        return this;
    }

    /** 辅助方法：添加冲突键 */
    public UpdateStrategy conflictKey(String... keys) {
        this.conflictKeys.addAll(Arrays.asList(keys));
        return this;
    }

    public UpdateStrategy tableName(String tableName) {
        this.tableName = tableName;
        return this;
    }

    public UpdateStrategy idKey(String idKey) {
        this.idKey = idKey;
        return this;
    }

    /** 设置为选择性插入（自动过滤null） */
    public UpdateStrategy selective() {
        this.ignoreNullValue = true;
        return this;
    }
}
