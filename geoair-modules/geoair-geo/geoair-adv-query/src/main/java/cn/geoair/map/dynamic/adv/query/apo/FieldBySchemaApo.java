package cn.geoair.map.dynamic.adv.query.apo;

import cn.geoair.base.Gir;
import cn.geoair.map.dynamic.adv.dbmeta.DefaultJavaType;
import cn.geoair.map.dynamic.adv.dbmeta.MysqlType;
import cn.geoair.map.dynamic.adv.dbmeta.OracleType;
import cn.geoair.map.dynamic.adv.dbmeta.PostgreSqlType;
import cn.geoair.map.dynamic.adv.dbmeta.TypeMetadata;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsTypeGeom;
import java.io.Serializable;
import lombok.Data;

/**
 * 数据库表字段元数据实体
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

    /** 原始字段名称，当存在 plot_geom as geom 时，此值为 plot_geom 而 columnName 为 geom */
    private String originalColumnName;

    /** 列排序 */
    private Integer ordinalPosition;

    /** 默认值 */
    private String columnDefault;

    /** 字段注释 */
    private String columnComment;

    /** 是否为空 */
    private String isNullable;

    /** 字符类型最大长度 */
    private Integer characterMaximumLength;

    /** 数值类型的精度 (precision) */
    private Integer numericPrecision;

    /** 数值类型的小数位数 (scale) */
    private Integer numericScale;

    /** 数据库内部类型名 (udt_name) */
    private String udtName;

    /** 当前字段所属Schema */
    private String tableSchema;

    /** 当前字段所属表名 */
    private String tableName;

    /** 字段类型描述，如 character varying、USER-DEFINED */
    private String dataType;

    /** 是否为空间字段 */
    private boolean geometryFieldIs = false;

    /** 空间字段类型 */
    private AdvEnumsTypeGeom geomType = AdvEnumsTypeGeom.unknown;

    /** 空间参考系ID */
    private Integer srid;

    /** 检测并标记是否为几何字段（纯查询，无副作用） */
    public boolean isGeometryFieldIs() {
        return geometryFieldIs;
    }

    /** 根据 udtName 判断并设置是否为几何字段 */
    public boolean determineGeometryFieldIs() {
        if (udtName == null) {
            geometryFieldIs = false;
            return false;
        }
        geometryFieldIs = "geometry".equals(udtName)
                || "geography".equals(udtName)
                || udtName.equalsIgnoreCase("SDO_GEOMETRY")
                || udtName.equalsIgnoreCase("MDSYS.SDO_GEOMETRY")
                || "\"public\".\"geometry\"".equals(udtName);
        return geometryFieldIs;
    }

    /** 根据 dialect 获取数据库类型元数据 */
    public TypeMetadata getDbType(String dialectName) {
        if (udtName == null) return null;
        if (udtName.startsWith("_")) {
            Gir.log.info("该字段为数组类型{}:{}", originalColumnName, udtName);
            String baseName = udtName.substring(1);
            return PostgreSqlType.getByUdtName(baseName);
        }
        // 根据方言分发
        if ("postgresql".equalsIgnoreCase(dialectName) || "pg".equalsIgnoreCase(dialectName)) {
            return PostgreSqlType.getByUdtName(udtName);
        } else if ("mysql".equalsIgnoreCase(dialectName)) {
            return MysqlType.getByUdtName(udtName);
        } else if ("oracle".equalsIgnoreCase(dialectName)) {
            return OracleType.getByUdtName(udtName);
        }
        // 兜底：尝试所有已知类型
        TypeMetadata result = PostgreSqlType.getByUdtName(udtName);
        if (result == null) result = MysqlType.getByUdtName(udtName);
        if (result == null) result = OracleType.getByUdtName(udtName);
        return result;
    }

    /** 获取对应的 Java 简单类名 */
    public String getJavaClassName() {
        // 默认为 String：先尝试 PG，再尝试其他
        TypeMetadata dbType = PostgreSqlType.getByUdtName(udtName);
        if (dbType == null && udtName != null && udtName.startsWith("_")) {
            dbType = PostgreSqlType.getByUdtName(udtName.substring(1));
        }
        if (dbType == null) dbType = MysqlType.getByUdtName(udtName);
        if (dbType == null) dbType = OracleType.getByUdtName(udtName);
        if (dbType == null) {
            return DefaultJavaType.JAVA_STRING.supportClass().getSimpleName();
        }
        return dbType.supportClass().getSimpleName();
    }

    /** @deprecated 请使用 getDbType(dialectName) 代替 */
    @Deprecated
    public PostgreSqlType getPostgreSqlType() {
        if (udtName == null) return null;
        if (udtName.startsWith("_")) {
            Gir.log.info("该字段为数组类型{}:{}", originalColumnName, udtName);
            return PostgreSqlType.getByUdtName(udtName.substring(1));
        }
        return PostgreSqlType.getByUdtName(udtName);
    }
}
