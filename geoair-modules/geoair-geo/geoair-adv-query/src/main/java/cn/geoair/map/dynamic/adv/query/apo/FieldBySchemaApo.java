package cn.geoair.map.dynamic.adv.query.apo;

import cn.geoair.base.Gir;
import cn.geoair.map.dynamic.adv.dbmeta.DefaultJavaType;
import cn.geoair.map.dynamic.adv.dbmeta.PostgreSqlType;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsTypeGeom;
import java.io.Serializable;
import lombok.Data;

/**
 * 数据库表的信息流水表(TableSchema)实体类
 *
 * @author zhangjun
 * @date 2023-06-01
 */
@Data
public class FieldBySchemaApo implements Serializable {

    public FieldBySchemaApo() {}

    /** 是否为主键 */
    private boolean primaryKeyIs = false;

    /** 列名称 */
    private String columnName;

    /** 原始字段名称，当存在 plot_geom as geom 的时候 ，这个字段的值是 plot_geom 而 columnName 为 geom */
    private String originalColumnName;

    /** 列排序 */
    private String ordinalPosition;

    /** 默认值 */
    private String columnDefault;

    /** 字段注释 */
    private String columnComment;

    /** 是否为空 */
    private String isNullable;

    /** 支付类型最大长度 */
    private String characterMaximumLength;

    /** 数值类型的长度 */
    private String numericPrecision;

    /** \ 数值类型的精度 */
    private String numericPrecisionRadix;

    /** 创建sql的数据类型 */
    private String udtName;

    /** 创建sql的当前字段的Schema */
    private String tableSchema;

    /** 创建sql的当前字段的所对应的表名字 */
    private String tableName;

    /** 字段类型 （这个字段类型是PG内部使用的 ） 类似于这种格式 character varying，USER-DEFINED */
    private String dataType;

    /** 是否为空间字段 */
    private boolean geometryFieldIs = false;

    /** 空间字段类型 */
    private AdvEnumsTypeGeom geomType = AdvEnumsTypeGeom.unknown;

    public String getJavaClassName() {
        PostgreSqlType byUdtName = getPostgreSqlType();
        if (byUdtName == null) {
            return DefaultJavaType.JAVA_STRING.getJavaClazz().getSimpleName();
        }
        Class aClass = byUdtName.getJavaType().getJavaClazz();
        return aClass.getSimpleName();
    }

    /** 暂时没有脱藕，后期再做多数据库适配 */
    public PostgreSqlType getPostgreSqlType() {
        if (udtName == null) {
            return null;
        }
        if (udtName.startsWith("_")) {
            Gir.log.info("该字段为数组类型{}:{}", originalColumnName, udtName);
            return PostgreSqlType.getByUdtName(udtName.substring(1));
        }
        return PostgreSqlType.getByUdtName(udtName);
    }

    public boolean isGeometryFieldIs() {
        geometryFieldIs =
                udtName.equals("geometry")
                        || udtName.equals("geography")
                        || udtName.equals("SDO_GEOMETRY")
                        || udtName.equals("\"public\".\"geometry\"");
        return geometryFieldIs;
    }
}
