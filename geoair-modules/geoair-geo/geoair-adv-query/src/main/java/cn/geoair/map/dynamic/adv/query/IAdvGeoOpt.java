package cn.geoair.map.dynamic.adv.query;

import java.util.List;
import java.util.Map;

import cn.geoair.map.dynamic.adv.query.apo.BBoxApo;
import cn.geoair.map.dynamic.adv.query.apo.DataFieldsApo;
import cn.geoair.map.dynamic.adv.query.apo.FieldBySchemaApo;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsGeomOpt;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsTypeGeom;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;

/**
 * 空间处理相关操作接口 约定：所有的方法都以 e开头 (本来设计用g开头，但是后面发现get方法的g开头存在了太多，导致了干扰)
 */
public interface IAdvGeoOpt {

	/**
	 * 查询一行数据
	 * @param sql 查询的sql
	 * @param advEnumsGeomOpt 对于空间字段的处理
	 * @return
	 */
	GirAdvOneRow eSelectOne(String sql, AdvEnumsGeomOpt advEnumsGeomOpt);

	/**
	 * 查询一行数据
	 * @param sql 查询的sql
	 * @param advEnumsGeomOpt 对于空间字段的处理
	 * @param geomFieldName 空间字段名称
	 * @return
	 */
	GirAdvOneRow eSelectOne(String sql, AdvEnumsGeomOpt advEnumsGeomOpt, String geomFieldName);

	/**
	 * 查询一行数据
	 * @param sql 查询的sql
	 * @param advEnumsGeomOpt 对于空间字段的处理
	 * @param geomFieldNameList 空间字段名称
	 * @return
	 */
	GirAdvOneRow eSelectOne(String sql, AdvEnumsGeomOpt advEnumsGeomOpt, List<String> geomFieldNameList);

	/**
	 * 查询一个列表
	 * @param sql 查询的sql
	 * @param advEnumsGeomOpt 对于空间字段的处理
	 * @return
	 */
	List<GirAdvOneRow> eSelectList(String sql, AdvEnumsGeomOpt advEnumsGeomOpt);

	/**
	 * 查询一个列表
	 * @param sql 查询的sql
	 * @param advEnumsGeomOpt 对于空间字段的处理
	 * @param geomFieldName 空间字段名称
	 * @return
	 */
	List<GirAdvOneRow> eSelectList(String sql, AdvEnumsGeomOpt advEnumsGeomOpt, String geomFieldName);

	/**
	 * 查询一个列表
	 * @param sql 查询的sql
	 * @param advEnumsGeomOpt 对于空间字段的处理
	 * @param geomFieldNameList 空间字段名称
	 * @return
	 */
	List<GirAdvOneRow> eSelectList(String sql, AdvEnumsGeomOpt advEnumsGeomOpt, List<String> geomFieldNameList);

	DataFieldsApo dGetColumnsByTable(String tableName);

	/**
	 * 获取所有包含空间字段的图层（表）名称
	 * @return 空间图层名称列表，若不存在则返回空列表
	 */
	List<String> eGetAllGeoLayerName();

	/**
	 * 判断指定表是否包含空间字段
	 * @param tableName 表名称，支持带schema的格式（如"schema.table"）
	 * @return 包含空间字段返回true，否则返回false
	 */
	boolean eIsGeomByTable(String tableName);

	/**
	 * 获取指定表的空间字段类型
	 * @param tableName 表名称，支持带schema的格式（如"schema.table"）
	 * @return 空间类型枚举（如点、线、面等），若表不包含空间字段则返回null
	 */
	AdvEnumsTypeGeom eGetGeoTypeByTable(String tableName);

	/**
	 * 获取指定表的空间字段类型
	 * @param tableName 表名称，支持带schema的格式（如"schema.table"）
	 * @param geomFieldName 空间字段名称
	 * @return 空间类型枚举（如点、线、面等），若表不包含空间字段则返回null
	 */
	AdvEnumsTypeGeom eGetGeoTypeByTable(String tableName, String geomFieldName);

	/**
	 * 获取指定表的空间字段类型
	 * @param tableName 表名称，支持带schema的格式（如"schema.table"）
	 * @param geomFieldNames 空间字段名称列表
	 * @return 空间类型枚举（如点、线、面等），若表不包含空间字段则返回null
	 */
	Map<String, AdvEnumsTypeGeom> eGetGeoTypeByTable(String tableName, List<String> geomFieldNames);

	/**
	 * 获取SQL视图结果中包含的空间字段类型
	 * @param sqlView 用于生成视图的SQL语句
	 * @return 空间类型枚举（如点、线、面等），若视图不包含空间字段则返回null
	 */
	AdvEnumsTypeGeom eGetGeoTypeBySql(String sqlView);

	/**
	 * 获取SQL视图结果中包含的空间字段类型
	 * @param sqlView 用于生成视图的SQL语句
	 * @param geomFieldName 空间字段名称
	 * @return 空间类型枚举（如点、线、面等），若视图不包含空间字段则返回null
	 */
	AdvEnumsTypeGeom eGetGeoTypeBySql(String sqlView, String geomFieldName);

	/**
	 * 获取SQL视图结果中包含的空间字段类型
	 * @param sqlView 用于生成视图的SQL语句
	 * @param geomFieldNames 空间字段名称列表
	 * @return 空间类型枚举（如点、线、面等），若视图不包含空间字段则返回null
	 */
	Map<String, AdvEnumsTypeGeom> eGetGeoTypeBySql(String sqlView, List<String> geomFieldNames);

	/**
	 * 判断SQL视图结果是否包含空间字段
	 * @param sqlView 用于生成视图的SQL语句
	 * @return 包含空间字段返回true，否则返回false
	 */
	boolean eIsGeomBySql(String sqlView);

	/**
	 * 获取指定表中的空间字段名称 存在多个，返回一个
	 * @param tableName 表名称，支持带schema的格式（如"schema.table"）
	 * @return 空间字段名称，若表不包含空间字段则返回null
	 */
	String eGetGeomColumnNameByTable(String tableName);

	/**
	 * 获取指定表中的空间字段名称 存在多个，返回多个
	 * @param tableName 表名称，支持带schema的格式（如"schema.table"）
	 * @return 空间字段名称，若表不包含空间字段则返回null
	 */
	List<String> eGetGeomColumnNameListByTable(String tableName);

	/**
	 * 获取表的空间字段元数据
	 * @param tableName 表名
	 * @return 如果存在多个，返回多个
	 */
	List<FieldBySchemaApo> eGetGeomColumnListByTable(String tableName);

	/**
	 * 获取表的空间字段元数据
	 * @param tableName 表名
	 * @return 如果存在多个，返回最前面的一个
	 */
	FieldBySchemaApo eGetGeomColumnByTable(String tableName);

	/**
	 * 获取SQL视图结果中的空间字段名称 存在多个，返回一个
	 * @param sqlView 用于生成视图的SQL语句
	 * @return 空间字段名称，若视图不包含空间字段则返回null
	 */
	String eGetGeomColumnNameBySql(String sqlView);

	/**
	 * 获取SQL视图结果中的空间字段名称
	 * @param sqlView 用于生成视图的SQL语句
	 * @return 空间字段名称，若视图不包含空间字段则返回null
	 */
	List<String> eGetGeomColumnNameListBySql(String sqlView);

	/**
	 * 获取SQL视图结果中的空间字段列表
	 * @param sqlView 用于生成视图的SQL语句
	 * @return 如果存在多个，返回多个
	 */
	List<FieldBySchemaApo> eGetGeomColumnListBySql(String sqlView);

	/**
	 * 获取SQL视图结果中的空间字段名称
	 * @param sqlView 用于生成视图的SQL语句
	 * @return 如果存在多个，返回最前面的一个
	 */
	FieldBySchemaApo eGetGeomColumnBySql(String sqlView);

	/**
	 * 判断指定表是否为点类型空间表
	 * @param tableName 表名称，支持带schema的格式（如"schema.table"）
	 * @return 若表的空间类型为Point或MultiPoint则返回true，否则返回false
	 */
	boolean eIsPointTable(String tableName);

	/**
	 * 判断指定表是否为面类型空间表
	 * @param tableName 表名称，支持带schema的格式（如"schema.table"）
	 * @return 若表的空间类型为Polygon或MultiPolygon则返回true，否则返回false
	 */
	boolean eIsPolygonTable(String tableName);

	/**
	 * 判断指定表是否为线类型空间表
	 * @param tableName 表名称，支持带schema的格式（如"schema.table"）
	 * @return 若表的空间类型为LineString或MultiLineString则返回true，否则返回false
	 */
	boolean eIsLineStringTable(String tableName);

	/**
	 * 添加空间字段
	 * @param tableName 表名
	 * @param geomFieldName 空间字段名
	 * @param geomType 空间类型
	 * @param srid 空间参考系ID (如4326)
	 */
	void eAddGeomColumn(String tableName, String geomFieldName, AdvEnumsTypeGeom geomType, int srid);

	/**
	 * 删除空间字段
	 * @param tableName 表名
	 * @param geomFieldName 空间字段名
	 */
	void eDropGeomColumn(String tableName, String geomFieldName);

	/**
	 * 删除空间字段
	 * @param tableName 表名
	 */
	void eDropGeomColumn(String tableName);

	/**
	 * 转换空间字段的坐标参考系
	 * @param tableName 表名
	 * @param geomFieldName 空间字段名
	 * @param targetSrid 目标坐标系SRID
	 */
	void eTransformSrid(String tableName, String geomFieldName, int targetSrid);

	/**
	 * 转换空间字段的坐标参考系
	 * @param tableName 表名
	 * @param targetSrid 目标坐标系SRID
	 */
	void eTransformSrid(String tableName, int targetSrid);

	/**
	 * 获取空间字段的坐标参考系
	 * @param tableNameOrSqlView 表名
	 * @param geomFieldName 空间字段名
	 * @return SRID值
	 */
	Integer eGetSrid(String tableNameOrSqlView, String geomFieldName);

	/**
	 * 获取空间字段的坐标参考系
	 * @param tableNameOrSqlView 表名
	 * @return SRID值
	 */
	Integer eGetSrid(String tableNameOrSqlView);

	/**
	 * 获取指定表的空间字段类型
	 * @param tableNameOrSqlView 表名
	 * @param geomFieldNames 空间字段名称列表
	 * @return 空间类型枚举（如点、线、面等），若表不包含空间字段则返回null
	 */
	Map<String, Integer> eGetSrid(String tableNameOrSqlView, List<String> geomFieldNames);

	/**
	 * 创建空间索引
	 * @param tableName 表名
	 * @param geomFieldName 空间字段名
	 * @param indexName 索引名称
	 */
	void eCreateSpatialIndex(String tableName, String geomFieldName, String indexName);

	/**
	 * 创建空间索引
	 * @param tableName 表名
	 * @param indexName 索引名称
	 */
	void eCreateSpatialIndex(String tableName, String indexName);

	/**
	 * 删除空间索引
	 * @param tableName 表名
	 * @param indexName 索引名称
	 */
	void eDropSpatialIndex(String tableName, String indexName);

	/**
	 * 执行空间查询：判断两个几何体是否相交
	 * @param tableName 表名
	 * @param geomFieldName 空间字段名
	 * @param geometry 用于查询的几何体WKT字符串
	 * @param srid 几何体的SRID
	 * @return 查询结果
	 */
	List<GirAdvOneRow> eQueryIntersects(String tableName, String geomFieldName, String geometry, int srid);

	/**
	 * 执行空间查询：判断两个几何体是否相交
	 * @param tableName 表名
	 * @param geometry 用于查询的几何体WKT字符串
	 * @param srid 几何体的SRID
	 * @return 查询结果
	 */
	List<GirAdvOneRow> eQueryIntersects(String tableName, String geometry, int srid);

	/**
	 * 执行空间查询：判断几何体是否在指定范围内
	 * @param tableName 表名
	 * @param geomFieldName 空间字段名
	 * @param bbox 边界框 [minx, miny, maxx, maxy]
	 * @param srid 边界框的SRID
	 * @return 查询结果
	 */
	List<GirAdvOneRow> eQueryWithinBBox(String tableName, String geomFieldName, double[] bbox, int srid);

	/**
	 * 执行空间查询：判断几何体是否在指定范围内
	 * @param tableName 表名
	 * @param bbox 边界框 [minx, miny, maxx, maxy]
	 * @param srid 边界框的SRID
	 * @return 查询结果
	 */
	List<GirAdvOneRow> eQueryWithinBBox(String tableName, double[] bbox, int srid);

	/**
	 * 计算两个几何体之间的距离
	 * @param tableName 表名
	 * @param geomFieldName 空间字段名
	 * @param geometry 用于计算的几何体WKT字符串
	 * @param srid 几何体的SRID
	 * @param distanceAlias 距离字段别名
	 * @return 包含距离的查询结果
	 */
	List<GirAdvOneRow> eCalculateDistance(String tableName, String geomFieldName, String geometry, int srid,
			String distanceAlias);

	List<GirAdvOneRow> eCalculateDistance(String tableName, String geometry, int srid, String distanceAlias);

	/**
	 * 获取几何体的中心点
	 * @param tableNameOrSqlView 表名
	 * @param geomFieldName 空间字段名
	 * @param centerAlias 中心点字段别名
	 * @return 包含中心点的查询结果
	 */
	List<GirAdvOneRow> eGetCentroid(String tableNameOrSqlView, String geomFieldName, String centerAlias);

	/**
	 * 获取几何体的中心点
	 * @param tableNameOrSqlView 表名
	 * @param centerAlias 中心点字段别名
	 * @return 包含中心点的查询结果
	 */
	List<GirAdvOneRow> eGetCentroid(String tableNameOrSqlView, String centerAlias);

	/**
	 * 验证几何体是否有效
	 * @param tableName 表名
	 * @param geomFieldName 空间字段名
	 * @return 无效的几何体ID列表
	 */
	List<Object> eValidateGeometries(String tableName, String geomFieldName);

	List<Object> eValidateGeometries(String tableName);

	/**
	 * 修复无效的几何体
	 * @param tableName 表名
	 * @param geomFieldName 空间字段名
	 * @return 修复的记录数
	 */
	int eRepairGeometries(String tableName, String geomFieldName);

	int eRepairGeometries(String tableName);

	/**
	 * 获取表中所有几何体的边界范围
	 * @param tableNameOrSqlView 表名
	 * @param geomFieldName 空间字段名
	 * @return 边界范围 [minx, miny, maxx, maxy]
	 */
	BBoxApo eGetExtent(String tableNameOrSqlView, String geomFieldName);

	BBoxApo eGetExtent(String tableNameOrSqlView);

}
