//package cn.geoair.map.dynamic.adv.query.dialect.pgback;
//
//import cn.geoair.map.dynamic.adv.mybatis.SqlEngineUtil;
//import cn.geoair.map.dynamic.adv.mybatis.SqlMeta;
//import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
//import cn.geoair.map.dynamic.adv.query.IAdvGeoPreOpt;
//import cn.geoair.map.dynamic.adv.query.apo.DataFieldsApo;
//import cn.geoair.map.dynamic.adv.query.apo.FieldBySchemaApo;
//import cn.geoair.map.dynamic.adv.query.apo.SqlParamMap;
//import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsGeomOpt;
//import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsTypeGeom;
//import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
//import cn.geoair.map.dynamic.ds.IDataSourceGetter;
//import cn.hutool.core.collection.CollectionUtil;
//import cn.hutool.core.collection.ListUtil;
//import cn.hutool.core.map.MapUtil;
//import cn.hutool.core.util.ObjectUtil;
//import cn.hutool.core.util.StrUtil;
//import lombok.extern.slf4j.Slf4j;
//
//import java.sql.*;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
///**
// * @author ：张逢吉
// * @date ：Created in   13:51
// * @description： TODO
// */
//@Slf4j
//public class PgAdvGeoPreOpt extends PgAdvGeoOpt implements IAdvGeoPreOpt {
//    DialectTableNameProcessor dialectTableNameProcessor = PgDialectTableNameUtil.getInstance();
//
//    PgAdvBaseOpt baseOpt;
//    PgAdvDDLOpt pgAdvDDLOpt;
//
//    final IDataSourceGetter dataSourceGetter;
//
//    public PgAdvGeoPreOpt(IDataSourceGetter dataSourceGetter) {
//        super(dataSourceGetter);
//        this.dataSourceGetter = dataSourceGetter;
//        baseOpt = new PgAdvBaseOpt(dataSourceGetter);
//        pgAdvDDLOpt = new PgAdvDDLOpt(dataSourceGetter);
//    }
//
//    @Override
//    public GirAdvOneRow eSelectOne(String sqlStatement, SqlParamMap sqlParam, AdvEnumsGeomOpt advEnumsGeomOpt) {
//        List<String> geomFieldNameBySql = eGetGeomColumnNameListBySql(sqlStatement, sqlParam);
//        return eSelectOne(sqlStatement, sqlParam, advEnumsGeomOpt, geomFieldNameBySql);
//    }
//
//    @Override
//    public GirAdvOneRow eSelectOne(String sqlStatement, SqlParamMap sqlParam, AdvEnumsGeomOpt advEnumsGeomOpt, String geomFieldName) {
//        return eSelectOne(sqlStatement, sqlParam, advEnumsGeomOpt, ListUtil.of(geomFieldName));
//    }
//
//    @Override
//    public GirAdvOneRow eSelectOne(String sqlStatement, SqlParamMap sqlParam, AdvEnumsGeomOpt advEnumsGeomOpt, List<String> geomFieldNameList) {
//        GirAdvOneRow girAdvOneRow = baseOpt.bSelectOne(sqlStatement, sqlParam);
//        if (ObjectUtil.isNotNull(geomFieldNameList)) {
//            List<GirAdvOneRow> girAdvOneRows = ListUtil.of(girAdvOneRow);
//            PgAdvGeoOpt.processGeometryField(girAdvOneRows, advEnumsGeomOpt, geomFieldNameList);
//            girAdvOneRow = girAdvOneRows.get(0);
//        }
//        return girAdvOneRow;
//    }
//
//    @Override
//    public List<GirAdvOneRow> eSelectList(String sqlStatement, SqlParamMap sqlParam, AdvEnumsGeomOpt advEnumsGeomOpt) {
//        List<String> geomFieldNameBySql = eGetGeomColumnNameListBySql(sqlStatement, sqlParam);
//        return eSelectList(sqlStatement, sqlParam, advEnumsGeomOpt, geomFieldNameBySql);
//    }
//
//    @Override
//    public List<GirAdvOneRow> eSelectList(String sqlStatement, SqlParamMap sqlParam, AdvEnumsGeomOpt advEnumsGeomOpt, String geomFieldName) {
//        return eSelectList(sqlStatement, sqlParam, advEnumsGeomOpt, ListUtil.of(geomFieldName));
//    }
//
//    @Override
//    public List<GirAdvOneRow> eSelectList(String sqlStatement, SqlParamMap sqlParam, AdvEnumsGeomOpt advEnumsGeomOpt, List<String> geomFieldNameList) {
//        List<GirAdvOneRow> girAdvOneRows = baseOpt.bSelectList(sqlStatement, sqlParam);
//        if (ObjectUtil.isNotNull(geomFieldNameList)) {
//            PgAdvGeoOpt.processGeometryField(girAdvOneRows, advEnumsGeomOpt, geomFieldNameList);
//        }
//        return girAdvOneRows;
//    }
//
//
//    @Override
//    public AdvEnumsTypeGeom eGetGeoTypeBySql(String sqlStatement, SqlParamMap sqlParam) {
//        String geomField = eGetGeomColumnNameBySql(sqlStatement, sqlParam);
//        return eGetGeoTypeBySql(sqlStatement, sqlParam, geomField);
//    }
//
//    @Override
//    public AdvEnumsTypeGeom eGetGeoTypeBySql(String sqlStatement, SqlParamMap sqlParam, String geomFieldName) {
//        Map<String, AdvEnumsTypeGeom> re = eGetGeoTypeBySql(sqlStatement, sqlParam, ListUtil.of(geomFieldName));
//        if (MapUtil.isEmpty(re)) {
//            return null;
//        }
//        return re.get(geomFieldName);
//    }
//
//    @Override
//    public Map<String, AdvEnumsTypeGeom> eGetGeoTypeBySql(String sqlStatement, SqlParamMap sqlParam, List<String> geomFieldNames) {
//        // 验证输入参数
//        if (StrUtil.isEmpty(sqlStatement) || CollectionUtil.isEmpty(geomFieldNames)) {
//            return MapUtil.empty();
//        }
//
//        // 移除SQL中的空格，保持与单个查询方法一致的处理
//        sqlStatement = dialectTableNameProcessor.tbRemoveSqlSpaces(sqlStatement);
//
//        // 构建查询字段部分和非空过滤条件
//        StringBuilder fieldsSql = new StringBuilder();
//        StringBuilder whereSql = new StringBuilder();
//
//        for (int i = 0; i < geomFieldNames.size(); i++) {
//            String fieldName = geomFieldNames.get(i);
//            // 为每个字段生成ST_GeometryType查询，并指定别名
//            fieldsSql.append("public.ST_GeometryType(").append(fieldName).append(") AS ").append(fieldName).append("_type");
//
//            // 构建非空过滤条件
//            whereSql.append(fieldName).append(" IS NOT NULL");
//
//            // 不是最后一个字段则添加分隔符
//            if (i != geomFieldNames.size() - 1) {
//                fieldsSql.append(", ");
//                whereSql.append(" AND ");
//            }
//        }
//
//        // 构建完整的SQL语句，将原始SQL作为子查询并添加WHERE条件
//        String sql = StrUtil.format("SELECT {} FROM ({}) AS temp WHERE {} LIMIT 1;",
//                fieldsSql.toString(), sqlStatement, whereSql.toString());
//
//        // 执行查询
//        GirAdvOneRow row = baseOpt.bSelectOne(sql, sqlParam);
//        if (row == null) {
//            return MapUtil.empty();
//        }
//
//        // 处理查询结果，映射到Map中
//        Map<String, AdvEnumsTypeGeom> resultMap = new HashMap<>(geomFieldNames.size());
//        for (String fieldName : geomFieldNames) {
//            String geomType = row.getStr(fieldName + "_type");
//            if (StrUtil.isNotEmpty(geomType)) {
//                resultMap.put(fieldName, getTypeGeomEnum(geomType));
//            }
//        }
//
//        return resultMap;
//    }
//
//    @Override
//    public boolean eIsGeomBySql(String sqlStatement, SqlParamMap sqlParam) {
//        return StrUtil.isNotEmpty(eGetGeomColumnNameBySql(sqlStatement, sqlParam));
//    }
//
//    @Override
//    public String eGetGeomColumnNameBySql(String sqlStatement, SqlParamMap sqlParam) {
//        if (StrUtil.isEmpty(sqlStatement)) {
//            return null;
//        }
//        sqlStatement = dialectTableNameProcessor.tbRemoveSqlSpaces(sqlStatement);
//        Connection connection = null;
//        PreparedStatement statement = null;
//        ResultSet resultSet = null;
//        String aliasTableName = dialectTableNameProcessor.tbGetTempAliasTableName();
//        try {
//            // 构建查询元数据的SQL
//            String fieldQuerySql = StrUtil.format("SELECT * FROM ({}) AS {} LIMIT 0", sqlStatement, aliasTableName);
//            SqlMeta parse = SqlEngineUtil.getEngine().parse(fieldQuerySql, sqlParam);
//
//
//            connection = dataSourceGetter.getConnection();
//            if (connection == null) {
//                throw new IllegalStateException("无法获取数据库连接");
//            }
//
//            statement = connection.prepareStatement(parse.getSql());
//            List<Object> jdbcParamValues = parse.getJdbcParamValues();
//
//            for (int i = 1; i <= jdbcParamValues.size(); i++) {
//                statement.setObject(i, jdbcParamValues.get(i - 1));
//            }
//            resultSet = statement.executeQuery();
//            ResultSetMetaData metaData = resultSet.getMetaData();
//            if (metaData == null) {
//                return null;
//            }
//            int columnCount = metaData.getColumnCount();
//            // 遍历所有字段，查找空间类型字段
//            for (int i = 1; i <= columnCount; i++) {
//                String columnName = metaData.getColumnName(i);
//                String columnTypeName = metaData.getColumnTypeName(i);
//
//                // 判断是否为PostGIS几何类型
//                if ("geometry".equals(columnTypeName) || "geography".equals(columnTypeName) || "\"public\".\"geometry\"".equals(columnTypeName)) {
//                    return columnName;
//                }
//            }
//        } catch (SQLException e) {
//            log.error("通过SQL视图查询空间字段失败，SQL: {}, 错误: {}", sqlStatement, e.getMessage(), e);
//        } finally {
//            dataSourceGetter.closeResources(resultSet, statement, connection);
//        }
//        return null;
//    }
//
//    @Override
//    public List<String> eGetGeomColumnNameListBySql(String sqlStatement, SqlParamMap sqlParam) {
//        List<FieldBySchemaApo> fieldBySchemaApos = eGetGeomColumnListBySql(sqlStatement, sqlParam);
//        DataFieldsApo dataFieldsApo = new DataFieldsApo();
//        dataFieldsApo.setDataFieldList(fieldBySchemaApos);
//        return dataFieldsApo.getFieldNameList();
//    }
//
//    @Override
//    public List<FieldBySchemaApo> eGetGeomColumnListBySql(String sqlStatement, SqlParamMap sqlParam) {
//        DataFieldsApo dataFieldsApo = pgAdvDDLOpt.dGetColumnsBySQL(sqlStatement, sqlParam);
//        return dataFieldsApo.getGeomFields();
//    }
//
//    @Override
//    public FieldBySchemaApo eGetGeomColumnBySql(String sqlStatement, SqlParamMap sqlParam) {
//        List<FieldBySchemaApo> fieldBySchemaApos = eGetGeomColumnListBySql(sqlStatement, sqlParam);
//        if (ObjectUtil.isNotEmpty(fieldBySchemaApos)) {
//            return fieldBySchemaApos.get(0);
//        }
//        return null;
//    }
//}
