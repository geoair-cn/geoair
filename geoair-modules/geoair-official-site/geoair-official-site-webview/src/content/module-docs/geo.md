## 模块定位

`geoair-geo` 是 GeoAir 里和 GIS 处理最直接相关的一组模块集合。它本身不是一个单体工具类，而是把空间处理、空间查询、空间文件、矢量瓦片、瓦片读取和瓦片融合这些能力组织到一起。

`geoair-geo` 适合作为 GIS 方向 Java 项目的模块总入口，再按需要进入具体子模块。

## 模块分层

从当前官网内容和源码结构看，这一层主要可以分成几类：

### 1. 核心工具层

- `geoair-geo-tools`

负责：

- 坐标转换
- 格式互转
- 测量
- 几何合并
- SRID 转换
- 瓦片与 QuadKey

### 2. 查询层

- `geoair-adv-query`

负责：

- 空间查询请求组织
- SQL 生成
- 条件构造
- 分页与排序
- Geometry 相关 typehandler

### 3. 数据源层

- `geoair-dynamic-ds`

负责：

- 动态数据源
- Spring 切面切库
- 主从读写分离
- SQL 读写识别

### 4. 文件层

- `geoair-file-tran`

负责：

- GeoJSON / Shapefile / PostGIS 之间的文件与数据转换

### 5. 瓦片与矢量瓦片层

- `geoair-mvt`
- `geoair-map-tile-forge`
- `geoair-map-tile-fuser`
- `geoair-by-gwc`

这几块负责：

- 实时 / 离线矢量瓦片
- 瓦片读取与服务适配
- 多源瓦片融合
- ArcGIS Compact Cache 读取

### 6. JTS 聚合与消息转换衔接层

- `geoair-jts-all`
- `geoair-message-jts-jackson`
- `geoair-message-jts-mybatis`

负责：

- JTS 能力聚合
- Geometry 与 Jackson 的对接
- Geometry 与 MyBatis 的对接

## 推荐阅读路径

第一次阅读 `geoair-geo` 时，推荐按这个顺序：

1. `geoair-geo-tools`
2. `geoair-adv-query`
3. `geoair-dynamic-ds`
4. `geoair-file-tran`
5. `geoair-mvt`
6. `geoair-map-tile-forge`
7. `geoair-map-tile-fuser`
8. `geoair-by-gwc`
9. `geoair-jts-all`

这个顺序大致对应：

- 先理解 Geometry / 坐标 / 基础工具
- 再理解查询和数据源
- 最后进入文件、瓦片与缓存这一层

## GitHub 源码入口

- `geoair-geo` 总目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo`

## 说明

这一页本身不是讲某一个具体 API，而是给整个 GIS 子模块体系做导航。真正的 API、test 和 GitHub 目录会在各个子模块页面里展开。
