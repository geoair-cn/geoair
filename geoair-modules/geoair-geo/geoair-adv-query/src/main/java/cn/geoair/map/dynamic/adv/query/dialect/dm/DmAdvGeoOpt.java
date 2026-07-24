package cn.geoair.map.dynamic.adv.query.dialect.dm;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.IAdvBaseOpt;
import cn.geoair.map.dynamic.adv.query.IAdvDDLOpt;
import cn.geoair.map.dynamic.adv.query.apo.SchemaTableApo;
import cn.geoair.map.dynamic.adv.query.dialect.oracle.OracleAdvGeoOpt;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsTypeGeom;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ：张逢吉
 * @date ：Created in 2026/7/22
 * @description： 达梦空间实现类（第一版复用Oracle实现骨架）
 */
public class DmAdvGeoOpt extends OracleAdvGeoOpt {

    public DmAdvGeoOpt(IDataSourceGetter dataSourceGetter, IAdvBaseOpt baseOpt, IAdvDDLOpt ddlOpt) {
        super(dataSourceGetter, baseOpt, ddlOpt);
    }

    @Override
    protected DialectTableNameProcessor getDialectTableNameProcessor() {
        return DmDialectTableNameUtil.getInstance();
    }

    @Override
    protected AdvEnumsTypeGeom getTypeGeomEnum(String nativeGeomType) {
        return super.getTypeGeomEnum(nativeGeomType);
    }

    @Override
    public Integer eGetSrid(String tableNameOrSqlView, String geomFieldName) {
        if (StrUtil.isEmpty(tableNameOrSqlView) || StrUtil.isEmpty(geomFieldName)) {
            return 0;
        }

        Integer sridFromMeta = getSridFromMetadata(tableNameOrSqlView, geomFieldName);
        if (sridFromMeta != null && sridFromMeta > 0) {
            return sridFromMeta;
        }

        String qualifiedName = getQualifiedName(tableNameOrSqlView);
        String quotedGeomFieldName = dialectTableNameProcessor.tbQuoteFieldName(geomFieldName);
        String sql =
                StrUtil.format(
                        "SELECT {}.SDO_SRID AS \"srid\" FROM {} WHERE {} IS NOT NULL AND ROWNUM = 1",
                        quotedGeomFieldName,
                        qualifiedName,
                        quotedGeomFieldName);
        GirAdvOneRow row = getAdvBaseOpt().bSelectOne(sql);
        return row != null ? row.getInt("srid", 0) : 0;
    }

    @Override
    public Map<String, Integer> eGetSrid(String tableNameOrSqlView, List<String> geomFieldNames) {
        if (StrUtil.isEmpty(tableNameOrSqlView) || CollectionUtil.isEmpty(geomFieldNames)) {
            return MapUtil.empty();
        }
        Map<String, Integer> sridMap = new HashMap<>();
        for (String geomFieldName : geomFieldNames) {
            sridMap.put(geomFieldName, eGetSrid(tableNameOrSqlView, geomFieldName));
        }
        return sridMap;
    }

    private Integer getSridFromMetadata(String tableNameOrSqlView, String geomFieldName) {
        String owner = resolveOwner(tableNameOrSqlView);
        if (StrUtil.isEmpty(owner)) {
            return null;
        }

        String tableName = dialectTableNameProcessor.tbGetTableNameNotSchema(tableNameOrSqlView);
        String columnName = dialectTableNameProcessor.tbUnquoteTableName(geomFieldName);

        try {
            String allMetaSql =
                    StrUtil.format(
                            "SELECT SRID AS \"srid\" FROM ALL_SDO_GEOM_METADATA WHERE OWNER = UPPER('{}') AND TABLE_NAME = UPPER('{}') AND COLUMN_NAME = UPPER('{}')",
                            owner,
                            tableName,
                            columnName);
            GirAdvOneRow row = getAdvBaseOpt().bSelectOne(allMetaSql);
            if (row != null) {
                return row.getInt("srid", 0);
            }
        } catch (Exception ignored) {
        }

        try {
            String userMetaSql =
                    StrUtil.format(
                            "SELECT SRID AS \"srid\" FROM USER_SDO_GEOM_METADATA WHERE TABLE_NAME = UPPER('{}') AND COLUMN_NAME = UPPER('{}')",
                            tableName,
                            columnName);
            GirAdvOneRow row = getAdvBaseOpt().bSelectOne(userMetaSql);
            if (row != null) {
                return row.getInt("srid", 0);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String getQualifiedName(String tableNameOrSqlView) {
        if (isTableOrViewName(tableNameOrSqlView)) {
            return dialectTableNameProcessor.tbGetTableNameWithSchema(
                    dataSourceGetter, tableNameOrSqlView);
        }
        return StrUtil.format(
                "({}) {}",
                dialectTableNameProcessor.tbRemoveSqlSpaces(tableNameOrSqlView),
                dialectTableNameProcessor.tbGetTempAliasTableName());
    }

    private boolean isTableOrViewName(String tableNameOrSqlView) {
        if (StrUtil.isEmpty(tableNameOrSqlView)
                || dialectTableNameProcessor.tbTableIsSqlView(tableNameOrSqlView)) {
            return false;
        }
        String name = dialectTableNameProcessor.tbGetTableNameNotSchema(tableNameOrSqlView);
        String schema = dialectTableNameProcessor.tbExtractSchemaName(tableNameOrSqlView);
        List<SchemaTableApo> objects =
                StrUtil.isNotEmpty(schema)
                        ? getAdvDDLOpt().dGetTableAndViewBySchema(schema)
                        : getAdvDDLOpt().dGetTableAndViewBySchema();
        if (CollectionUtil.isEmpty(objects)) {
            return false;
        }
        for (SchemaTableApo object : objects) {
            if (object != null && StrUtil.equalsIgnoreCase(name, object.getName())) {
                return true;
            }
        }
        return false;
    }

    private String resolveOwner(String tableNameOrSqlView) {
        String schemaName = dialectTableNameProcessor.tbExtractSchemaName(tableNameOrSqlView);
        if (StrUtil.isNotEmpty(schemaName)) {
            return schemaName.toUpperCase();
        }

        String currentSchema = dataSourceGetter.getSchemaName();
        if (StrUtil.isNotEmpty(currentSchema)) {
            return currentSchema.toUpperCase();
        }

        String detectedSchema = getAdvDDLOpt().dGetCurrentSchema();
        return StrUtil.isEmpty(detectedSchema) ? null : detectedSchema.toUpperCase();
    }
}
