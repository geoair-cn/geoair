package cn.geoair.map.dynamic.adv.query.apo;

import cn.geoair.map.dynamic.adv.query.enums.AdvSchemaTableTypeOpt;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author ：张俊
 * @date ：Created in 2026/3/19 14:19 @description： TODO
 */
@Data
@Accessors(chain = true)
public class SchemaTableApo {

    /** 数据库名称 */
    String databaseName;

    /** 模式名称 */
    String schema;

    /** 表名称或者视图名称或者方法名称 */
    String name;

    /** 表类型 */
    AdvSchemaTableTypeOpt type;
}
