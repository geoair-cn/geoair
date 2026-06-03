package cn.geoair.map.dynamic.adv.query.strategy;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ：张俊
 * @date ：Created in 2026/6/3 09:39
 * @description： 实体类更新的实体类处理策略
 */
@Data
@Accessors(chain = true)
public class AccessStrategy {

    /**
     * ID字段的名称
     */
    String idKey;
    /**
     * 表的名称
     */
    String tableName;


    /**
     * 需要忽略哪些字段
     */
    List<String> ignoreFieldNames = new ArrayList<>();


    /**
     * 是否对实体类进行驼峰转下划线
     */
    boolean isToUnderlineCase = true;

    /**
     * 是否忽略空值
     */
    boolean ignoreNullValue = true;
}
