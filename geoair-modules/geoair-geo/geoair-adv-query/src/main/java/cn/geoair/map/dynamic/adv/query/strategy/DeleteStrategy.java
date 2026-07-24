package cn.geoair.map.dynamic.adv.query.strategy;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 实体类删除的策略配置
 *
 * @author 张俊
 * @date 2026/6/3
 */
@Data
@Accessors(chain = true)
public class DeleteStrategy {

    /** 主键字段名（优先级高于注解） */
    String idKey;

    /** 表名（优先级高于注解） */
    String tableName;

    /** 需要忽略的字段列表（实体属性名，用于逻辑删除时排除某些字段） */
    List<String> ignoreFieldNames = new ArrayList<>();

    /** 是否对实体类进行驼峰转下划线 */
    boolean toUnderlineCase = true;

    /** 辅助方法：添加忽略字段 */
    public DeleteStrategy ignoreField(String... fieldNames) {
        this.ignoreFieldNames.addAll(Arrays.asList(fieldNames));
        return this;
    }

    public DeleteStrategy tableName(String tableName) {
        this.tableName = tableName;
        return this;
    }

    public DeleteStrategy idKey(String idKey) {
        this.idKey = idKey;
        return this;
    }
}
