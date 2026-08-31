package cn.geoair.map.dynamic.adv.query.apo;

import cn.geoair.base.Gir;
import cn.geoair.map.dynamic.adv.dbmeta.*;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsTypeGeom;
import cn.hutool.db.dialect.DialectName;

import lombok.Data;

import java.io.Serializable;

/**
 * 数据库表字段元数据实体
 *
 * @author zhangjun
 * @date 2023-06-01
 */
@Data
public class FieldBySchemaApo implements Serializable {

    public FieldBySchemaApo() {}

    /** 所属数据库方言 */
    private DialectName dialectName;

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

    /** 检测是否为几何字段（纯查询，无副作用） */
    public boolean isGeometryFieldIs() {
        return geometryFieldIs;
    }

    /** 根据 udtName 和 dialectName，通过类型系统判断并设置是否为几何字段 */
    public boolean determineGeometryFieldIs() {
        if (udtName == null) {
            geometryFieldIs = false;
            return false;
        }
        TypeMetadata dbType = getDbType();
        if (dbType != null) {
            geometryFieldIs = dbType.getCategory() == CategoryEnum.GEOMETRY;
        } else {
            // 兜底：类型系统未匹配到（如 PG JDBC 驱动降级为 PgObject），
            // 用原始 udtName 做模式匹配
            String lower = udtName.toLowerCase();
            geometryFieldIs =
                    lower.contains("geometry")
                            || lower.contains("geography")
                            || lower.contains("pgobject")
                            || lower.contains("sdo_geometry");
        }
        return geometryFieldIs;
    }

    /** 根据内置 dialectName 获取数据库类型元数据 */
    public DataBaseFieldType getDbType() {
        if (udtName == null) return null;
        if (udtName.startsWith("_")) {
            Gir.log.info("该字段为数组类型{}:{}", originalColumnName, udtName);
            return PostgreSqlType.getByUdtName(udtName.substring(1));
        }
        if (dialectName == null) {
            return PostgreSqlType.getByUdtName(udtName);
        }
        switch (dialectName) {
            case MYSQL:
                return MysqlType.getByUdtName(udtName);
            case ORACLE:
            case DM:
                return OracleType.getByUdtName(udtName);
            case POSTGRESQL:
            default:
                return PostgreSqlType.getByUdtName(udtName);
        }
    }

    /** 获取对应的 Java 简单类名 */
    public String getJavaClassName() {
        DataBaseFieldType dbType = getDbType();
        if (dbType == null) {
            return DefaultJavaType.JAVA_STRING.supportClass().getSimpleName();
        }
        return dbType.supportClass().getSimpleName();
    }

    /**
     * @deprecated 请使用 getDbType() 代替
     */
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
