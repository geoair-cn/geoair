package cn.geoair.map.dynamic.adv.dbmeta;

import java.util.List;

/**
 * 数据库字段类型查询接口，用于根据 udtName 解析数据库字段的类型信息。
 *
 * <p>继承 {@link TypeMetadata}，在通用类型元数据的基础上增加了数据库特定的能力：
 *
 * <ul>
 *   <li>{@link #getUdtNames()} — 支持多别名映射（如 PG 的 geometry / "public"."geometry"）
 *   <li>{@link #getStandardName()} — 标准大写类型名（如 VARCHAR2、NUMERIC）
 *   <li>{@link #getJavaType()} — 对应的 Java 类型枚举 {@link DefaultJavaType}
 * </ul>
 *
 * <p>典型使用场景：{@link cn.geoair.map.dynamic.adv.query.apo.FieldBySchemaApo#getDbType()} 返回此接口，调用方通过
 * {@link #supportClass()} 获取 Java 类型，或通过 {@link #getCategory()} 判断类型分组。
 *
 * @author zhangjun
 * @date 2026/8/14
 */
public interface DataBaseFieldType extends TypeMetadata {

    /**
     * 获取该数据库类型的所有 UDT 名称变体（小写）。
     *
     * <p>一个数据库类型可能有多个别名，例如 PostgreSQL 的 geometry 类型 可匹配 "geometry" 和 "\"public\".\"geometry\""。
     *
     * @return 不可变的 UDT 名称列表，至少包含一个元素
     */
    List<String> getUdtNames();

    /**
     * 获取该类型的标准大写名称，用于显示和 SQL 生成。
     *
     * <p>例如：VARCHAR2、NUMERIC、TIMESTAMP WITH TIME ZONE
     *
     * @return 标准大写类型名
     */
    String getStandardName();

    /**
     * 获取该数据库类型对应的 Java 类型枚举。
     *
     * <p>通过 {@link DefaultJavaType} 可进一步获取 Java 类、忽略策略等信息。
     *
     * @return 对应的 {@link DefaultJavaType} 枚举值
     */
    DefaultJavaType getJavaType();
}
