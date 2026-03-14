# Geo-Dynamic 地理空间动态处理框架

[![License](https://img.shields.io/badge/license-Apache%202-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)

## 项目简介

Geo-Dynamic 是一套面向地理空间数据的动态处理框架，提供了动态查询、数据源管理和丰富的地理空间数据处理工具。该框架基于 Java
开发，集成了 GeoTools 和 PostGIS 等地理空间处理库，适用于各类 GIS 应用开发。

## 模块介绍

### geoair-adv-query - 动态查询器

提供对地理空间数据的高级动态查询能力，包括:

- 多数据源支持
- 地理空间查询优化
- 分页查询支持
- SQL 注入防护
- PostgreSQL 方言支持

主要组件:

- [IAdvExecutor]( adv-query\geoair-adv-query\src\main\java\cn\geoair\map\dynamic\adv\query\IAdvExecutor.java#L11-L12):
  查询执行器接口
- `PgAdv*Opt`: PostgreSQL 特化的查询操作实现
- [DataFieldsApo]( adv-query\geoair-adv-query\src\main\java\cn\geoair\map\dynamic\adv\query\apo\DataFieldsApo.java#L21-L163), [FieldBySchemaApo]( adv-query\geoair-adv-query\src\main\java\cn\geoair\map\dynamic\adv\query\apo\FieldBySchemaApo.java#L21-L140):
  查询字段定义
- [BBoxApo]( adv-query\geoair-adv-query\src\main\java\cn\geoair\map\dynamic\adv\query\apo\BBoxApo.java#L16-L219):
  边界框查询条件

### geoair-dynamic-ds - 数据源管理

提供动态数据源管理功能:

- 多数据源配置与切换
- 数据源元信息管理
- JDBC URL 解析工具

关键类:

- [DynamicDataSourceManager]( adv-query\geoair-dynamic-ds\src\main\java\cn\geoair\map\dynamic\ds\DynamicDataSourceManager.java#L13-L70):
  动态数据源管理器
- [DataSourceApo]( adv-query\geoair-dynamic-ds\src\main\java\cn\geoair\map\dynamic\ds\apo\DataSourceApo.java#L16-L99):
  数据源配置信息
- [JdbcUrlSplitter]( adv-query\geoair-dynamic-ds\src\main\java\cn\geoair\map\dynamic\ds\utils\JdbcUrlSplitter.java#L10-L43):
  JDBC URL 解析工具

### geoair-geo-tools - 地理空间处理工具集

提供丰富的地理空间数据处理工具:

- 坐标转换 (WGS84, GCJ02, BD09, 3857, 4490等)
- 几何对象转换 (GeoJSON, WKT, WKB)
- 空间测量 (距离、面积计算)
- 几何对象合并
- 瓦片坐标计算

主要工具类:

- [GirConvertUtils]( adv-query\geoair-geo-tools\src\main\java\cn\geoair\map\dynamic\tools\convert\GirConvertUtils.java#L23-L451):
  各种格式转换工具
- [GirCoordinateUtils]( adv-query\geoair-geo-tools\src\main\java\cn\geoair\map\dynamic\tools\coordinate\GirCoordinateUtils.java#L17-L682):
  坐标转换工具
- [GirGeoMeasureUtils]( adv-query\geoair-geo-tools\src\main\java\cn\geoair\map\dynamic\tools\measure\GirGeoMeasureUtils.java#L24-L436):
  测量工具
- [GirGeoMergeUtils]( adv-query\geoair-geo-tools\src\main\java\cn\geoair\map\dynamic\tools\merge\GirGeoMergeUtils.java#L20-L311):
  几何对象合并工具
- `GirTileConverter*Utils`: 瓦片坐标转换工具
- [GirSridConvertUtils]( adv-query\geoair-geo-tools\src\main\java\cn\geoair\map\dynamic\tools\srid\GirSridConvertUtils.java#L24-L246):
  SRID 坐标系转换工具

## 使用示例

### 坐标转换

```
java
// WGS84 转 GCJ02
double[] gcj = GirCoordinateUtils.getInstance().wgs84ToGcj02(116.40, 39.90);

// WGS84 转墨卡托
double[] mercator = GirCoordinateUtils.getInstance().wgs84ToMercator(116.40, 39.90);
```

### 几何对象处理

```
java
// GeoJSON 转 JTS Geometry
Geometry geom = GirConvertUtils.getInstance().geojsonToJtsGeometry(geojsonString);

// 计算面积
double area = GirGeoMeasureUtils.getInstance().calculateArea(geom, 4326, "km²");
```

### 动态查询

```
java
// 构造查询条件并执行
AdvExecutorPG executor = new AdvExecutorPG(dataSourceApo);
List<AdvOneRow> results = executor.select(dataFieldsApo, filterApo, pageApo);
```

## 依赖说明

- Java 8+
- GeoTools 相关组件
- PostGIS JDBC 驱动
- Hutool 工具库

## 许可证

本项目采用 Apache License 2.0 许可证，详见 [LICENSE](LICENSE) 文件。



