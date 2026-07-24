## 模块定位

`geoair-geo-tools` 是 GeoAir 中最偏日常开发工具箱的一层，统一封装坐标转换、格式互转、空间测量、几何合并、SRID 转换和瓦片计算。

和 `geoair-adv-query` 这类更偏查询执行器的模块不同，`geoair-geo-tools` 更关注 Geometry、坐标和瓦片本身的处理。

## 统一入口

```java
GirGeoTools tools = GirGeoTools.defaultInstance();

GirCoordinateConvertOpt coordinateOpt = tools.getCoordinateOpt();
GirGeoFormatOpt formatOpt = tools.getFormatOpt();
GirGeoMeasureOpt measureOpt = tools.getMeasureOpt();
GirGeoMergeOpt mergeOpt = tools.getMergeOpt();
GirSridConvertOpt sridOpt = tools.getSridOpt();
GirTileConverterOpt tileOpt = tools.getTileGrid4326Opt();
```

## 坐标转换

`GirCoordinateConvertOpt` 负责：

- WGS84 / GCJ02 / BD09 互转
- WGS84 与墨卡托互转
- 单点、Point、批量数组、Geometry 全类型转换
- DMS / DD 格式转换

```java
double[] gcj02 = GirGeoTools.defaultInstance()
    .getCoordinateOpt()
    .wgs84ToGcj02(116.40, 39.90);

Point pointBd09 = GirGeoTools.defaultInstance()
    .getCoordinateOpt()
    .wgs84ToBd09(point);
```

## 格式转换

`GirGeoFormatOpt` 负责：

- GeoJSON <-> JTS Geometry
- WKT <-> JTS Geometry
- WKB <-> JTS Geometry
- PGGeometry <-> JTS Geometry
- Point、Reader、Writer、GeometryJSON 等底层入口

```java
Geometry geometry = GirGeoTools.defaultInstance()
    .getFormatOpt()
    .geojsonToJtsGeometry(geojson, false);

String wkt = GirGeoTools.defaultInstance()
    .getFormatOpt()
    .jtsGeometryToWktString(geometry, false);
```

## 测量计算

`GirGeoMeasureOpt` 负责：

- 面积计算
- 长度计算
- 点点 / 点线 / 点面 / 线线距离
- UTM 投影精确计算
- 单位换算

```java
double area = GirGeoTools.defaultInstance()
    .getMeasureOpt()
    .calculateArea(polygon, 4326, GirGeoMeasureOpt.UNIT_SQUARE_KILOMETER);

double distance = GirGeoTools.defaultInstance()
    .getMeasureOpt()
    .calculatePointToPointDistance(point1, point2, 4326, GirGeoMeasureOpt.UNIT_METER);
```

## 几何合并

`GirGeoMergeOpt` 负责：

- 合并成 `MultiPoint`
- 合并成 `MultiLineString`
- 合并成 `MultiPolygon`
- 合并成单条线或单个面

```java
MultiLineString multiLine = GirGeoTools.defaultInstance()
    .getMergeOpt()
    .mergeToMultiLineString(lineStrings);
```

## SRID 转换

`GirSridConvertOpt` 负责：

- Geometry / Envelope / 点坐标转换
- CRS 获取
- `4326 <-> 3857` 等常见 SRID 互转

```java
Geometry geometry3857 = GirGeoTools.defaultInstance()
    .getSridOpt()
    .convert(geometry4326, 4326, 3857);
```

## 瓦片与 QuadKey

`GirTileConverterOpt` 和 `GirBingMapQuadKeyOpt` 负责：

- `xyzToTileBox`
- `tileRangeByBox`
- `tileRangeByGeom`
- `zxyListByGeom`
- QuadKey 生成与解析

```java
BoxReferencedEnvelope tileBox = GirGeoTools.defaultInstance()
    .getTileGrid4326Opt()
    .xyzToTileBox(10, 845, 388, 4326);

String quadKey = GirGeoTools.defaultInstance()
    .getTileGridBingMapOpt()
    .xyzToQuadKey(845, 388, 10);
```

## 阅读建议

建议顺序：

1. 先看统一入口
2. 再看坐标 / 格式转换
3. 然后看测量与 SRID
4. 最后看瓦片与 QuadKey

如果需要直接对照 Java 示例，可优先查看 `src/test/java/cn/geoair/map/dynamic/tools/test` 下的示例类。
