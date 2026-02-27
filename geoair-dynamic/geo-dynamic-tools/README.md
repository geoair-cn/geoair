# Geo Dynamic Tools

地理空间动态工具包，提供坐标转换、几何计算、单位换算等功能。

## 功能模块

### 1. 坐标转换 (`coordinate`)
- [GirCoordinateUtils]( geo-dynamic-tools\src\main\java\cn\geoair\map\dynamic\tools\coordinate\GirCoordinateUtils.java#L13-L378): 实现了度分秒(DMS)与十进制度(DD)之间的相互转换，以及坐标字符串解析功能。

### 2. 几何计算 (`measure`)
- [GirGeoMeasureUtils]( geo-dynamic-tools\src\main\java\cn\geoair\map\dynamic\tools\measure\GirGeoMeasureUtils.java#L24-L436): 提供面积、长度及各种距离的计算功能：
    - 面积计算: 支持 Polygon 和 MultiPolygon 类型
    - 长度计算: 支持 LineString、MultiLineString、Polygon 和 MultiPolygon 类型
    - 距离计算:
        - 点到点距离
        - 点到线最短距离
        - 点到几何体距离
        - 线到线最短距离
- 单位转换: 支持多种长度和面积单位间的转换

### 3. 几何合并 (`merge`)
- [GirGeoMergeUtils]( geo-dynamic-tools\src\main\java\cn\geoair\map\dynamic\tools\merge\GirGeoMergeUtils.java#L21-L314): 提供几何图形合并功能：
    - 合并多个 LineString 为 MultiLineString
    - 将首尾相连的 LineString 合并为单一 LineString
    - 合并多个 Polygon 为 MultiPolygon 或单一 Polygon
    - 合并多个 Point 为 MultiPoint

### 4. 坐标系转换 (`srid`)
- [GirSridConvertUtils]( geo-dynamic-tools\src\main\java\cn\geoair\map\dynamic\tools\srid\GirSridConvertUtils.java#L24-L246): 基于 GeoTools 的 SRID 坐标系转换工具：
    - 支持不同 EPSG 标准 SRID 之间的互转
    - 内置常用 CRS 缓存提升性能
    - 提供便捷方法如 WGS84(4326) 与 Web Mercator(3857) 互相转换

### 5. 格式转换 ([convert]( geo-dynamic-tools\src\main\java\cn\geoair\map\dynamic\tools\srid\GirSridConvertUtils.java#L59-L62))
- [GirConvertUtils]( geo-dynamic-tools\src\main\java\cn\geoair\map\dynamic\tools\convert\GirConvertUtils.java#L23-L451): 提供多种几何数据格式之间的转换：
    - GeoJSON <-> JTS Geometry
    - WKT <-> JTS Geometry
    - WKB <-> JTS Geometry
    - PostGIS PGgeometry <-> JTS Geometry

## 使用说明

所有工具类均采用单例模式设计，通过 [getInstance()]( geo-dynamic-tools\src\main\java\cn\geoair\map\dynamic\tools\merge\GirGeoMergeUtils.java#L41-L50) 方法获取实例。

示例：
