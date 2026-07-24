## 模块定位

`geoair-mvt` 是 GeoAir 中与矢量瓦片相关的模块总览页。

它不是单一实现，而是一组分层能力的组合：

- `geoair-mvt-tools`：工具层
- `geoair-real-mvt`：实时矢量瓦片服务层
- `geoair-static-mvt-spark`：基于 Java + Spark 的静态切片层

其中一个非常重要的特性是：**这套模块同时支持 4490 网格与 3857 网格**。

这意味着：

- 在更偏互联网地图体系时，可以直接走 3857
- 在更偏国内测绘 / 天地图 / 地理坐标网格体系时，也可以走 4490
- 实时服务和离线切片都能围绕这两套网格组织瓦片范围与输出逻辑

这三层合在一起，覆盖了从“瓦片范围计算”到“实时服务输出”再到“离线批量切片”的整条链路。

## 三层职责划分

### 1. geoair-mvt-tools

负责：

- 当前瓦片范围计算
- 坐标到屏幕坐标转换
- 几何简化
- 瓦片执行参数组织

典型类：

- `AdvMvtTileUtils`
- `PipelineBuilder`
- `AdvMvtDensityUtils`

对应测试：

- `AdvMvtTileUtilsExample`
- `PipelineBuilderExample`

### 2. geoair-real-mvt

负责：

- 接收实时瓦片请求
- 组织 `TileRequestParams`
- 构建 `VectorTileExecutorV2`
- 查询数据库中的 Geometry
- 实时返回 PBF 矢量瓦片

对应独立页面：

- `real-mvt`

### 3. geoair-static-mvt-spark

负责：

- 用 Java + Spark 组织离线切片任务
- 通过 `TileSliceParameter` 描述切片任务
- 读取空间数据、映射瓦片、聚合、生成 PBF 并写出
- 作为一种在 Java 体系内替代 `tippecanoe` 的切片方案

对应独立页面：

- `static-mvt-spark`

## 适合怎样阅读

如果目标是快速理解整个模块，建议按下面顺序：

1. 先看 `geoair-mvt-tools`
2. 再看 `geoair-real-mvt`
3. 最后看 `geoair-static-mvt-spark`

这样更容易把：

- 工具层
- 在线实时层
- 离线切片层

三者的关系看清楚。

## GitHub 源码入口

- 模块目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-mvt`
- `geoair-mvt-tools`：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-mvt/geoair-mvt-tools`
- `geoair-real-mvt`：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-mvt/geoair-real-mvt`
- `geoair-static-mvt-spark`：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-mvt/geoair-static-mvt-spark`

## 对应测试入口

- `AdvMvtTileUtilsExample`
- `PipelineBuilderExample`
- `GirRealMvtEntryExample`
- `TileRequestParamsExample`
- `TileExecutorConfigExample`
- `TileGlobalConfigExample`
- `TileSliceParameterExample`

## 阅读建议

如果重点是：

- **在线服务**：优先进入 `real-mvt`
- **离线切片**：优先进入 `static-mvt-spark`
- **工具链路**：优先进入 `geoair-mvt-tools`

这页只负责总览，不再承担所有实现细节。
