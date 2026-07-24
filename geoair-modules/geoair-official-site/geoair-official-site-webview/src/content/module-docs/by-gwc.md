## 模块定位

`geoair-by-gwc` 负责直接读取 ArcGIS Compact Cache 相关数据结构，并围绕瓦片缓存、GridSet 与 WMTS 能力描述组织读取逻辑。

它关注的重点是：

- ArcGIS Compact Cache V1 / V2
- GridSet / GridSubset 构建
- WMTS 能力描述生成
- 读取缓存文件中的瓦片资源

如果已有 ArcGIS Compact Cache 数据且不希望重新走服务构建链，这一层就是直接读取缓存格式的关键入口。

## 核心类

最值得先读的类：

- `ArcGISCompactCache`
- `ArcGISCompactCacheV1`
- `ArcGISCompactCacheV2`
- `GridSetBuilder`
- `GetCapabilitiesGenerator`

### ArcGISCompactCache / V1 / V2

这几类负责：

- 直接面向 ArcGIS Compact Cache 文件结构
- 处理 V1 / V2 两种缓存格式差异
- 提供底层瓦片资源读取能力

### GridSetBuilder

负责：

- 根据缓存配置构建 GridSet
- 组织层级、分辨率、网格范围等信息

### GetCapabilitiesGenerator

负责：

- 生成 WMTS 能力描述
- 把底层缓存信息转换成标准服务元数据

## GitHub 源码入口

- 模块目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-by-gwc`
- Compact Cache 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-by-gwc/src/main/java/cn/geoair/map/tile/forge/core/bygwc/compact`
- Grid 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-by-gwc/src/main/java/cn/geoair/map/tile/forge/core/bygwc/grid`
- WMTS 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-by-gwc/src/main/java/cn/geoair/map/tile/forge/core/bygwc/wmts`

## 阅读建议

建议顺序：

1. `ArcGISCompactCache`
2. `ArcGISCompactCacheV1` / `ArcGISCompactCacheV2`
3. `GridSetBuilder`
4. `GetCapabilitiesGenerator`

先理解缓存格式如何被抽象，再看它如何被组织成 WMTS 能力输出。
