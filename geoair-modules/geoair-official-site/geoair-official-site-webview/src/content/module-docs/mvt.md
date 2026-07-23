## 模块定位

`geoair-mvt` 负责矢量瓦片相关能力，但内部并不是一个单体模块，而是按用途拆成了三层：

- `geoair-mvt-tools`：矢量瓦片工具层
- `geoair-real-mvt`：实时矢量瓦片服务层
- `geoair-static-mvt-spark`：离线批量生成层

如果只看功能边界，这个模块更像一套“从几何数据到 PBF 瓦片”的处理链，而不是单个工具类。

## 模块结构

### geoair-mvt-tools

这一层偏算法与转换工具，负责：

- 几何到屏幕坐标的转换
- 要素密度控制
- 瓦片范围与执行参数计算
- PBF 相关工具能力

关键类：

- `PipelineBuilder`
- `AdvMvtTileUtils`
- `AdvMvtDensityUtils`

对应测试示例：

- `AdvMvtTileUtilsExample`
- `PipelineBuilderExample`

### geoair-real-mvt

这一层偏在线实时服务，负责：

- 接收瓦片请求参数
- 计算当前请求的瓦片范围
- 查询数据库中的 Geometry
- 进行裁剪、简化、密度控制
- 输出实时 PBF

关键类：

- `GirRealMvtHelper`
- `VectorTileExecutorV2`
- `TileRequestParams`
- `TileGlobalConfig`
- `TileExecutorConfig`

对应测试示例：

- `GirRealMvtEntryExample`
- `TileRequestParamsExample`
- `TileExecutorConfigExample`
- `TileGlobalConfigExample`

### geoair-static-mvt-spark

这一层偏离线批处理，负责：

- 从数据库批量读取空间数据
- 计算每个要素对应的瓦片范围
- 生成 PBF 并写入目标存储
- 统计瓦片数据

关键类：

- `SparkVectorTileGenerator`
- `SparkVectorTileGeneratorAll`
- `VectorTileCommonUtils`
- `SparkTaskSerializableUtil`
- `TileSliceParameter`

对应测试示例：

- `TileSliceParameterExample`

## 核心入口

### 实时服务入口

```java
GirRealMvtHelper helper = GirRealMvtHelper.getInstance();
```

### 实时执行器入口

```java
VectorTileExecutorV2 executor = VectorTileExecutorV2.getInstance(requestParams, layerName);
```

### 工具层入口

```java
Envelope tileEnvelope = AdvMvtTileUtils.getTileRect(level, x, y, sourceGrid);
TileExecParams params = AdvMvtTileUtils.getTileExecParamsNotHasSql(level, x, y, sourceGrid, sourceDataSrid);
```

### 离线生成入口

```java
SparkVectorTileGenerator generator = new SparkVectorTileGenerator(sparkSession);
generator.doGenerate(parameter);
```

## 实时服务链路

实时矢量瓦片的核心过程可以概括成：

1. 前端传入 `layerName / z / x / y / paramTile`
2. `TileRequestParams` 解析参数
3. `VectorTileExecutorV2` 根据层级、范围、SRID 和输出字段组装 SQL
4. 使用 `ST_Intersects` 把数据库中的 Geometry 裁到当前瓦片范围
5. 通过 `VectorTileBuilderConsumer` 组织为 MVT 要素
6. 输出 PBF 字节流

## 核心 API 示例

### 示例1：根据瓦片坐标计算瓦片范围

```java
Envelope tileEnvelope = AdvMvtTileUtils.getTileRect(10, 845, 388, 4326);
TileExecParams params = AdvMvtTileUtils.getTileExecParamsNotHasSql(10, 845, 388, 4326, 4326);
```

对应测试：`AdvMvtTileUtilsExample`

### 示例2：获取实时 MVT 辅助入口

```java
GirRealMvtHelper helper = GirRealMvtHelper.getInstance();
TileRequestParams requestParams = helper.getTileRequestParams("road_layer");
ParamCheckResult result = helper.checkTileRequestParams(requestParams, "road_layer");
```

对应测试：`GirRealMvtEntryExample`

### 示例3：构建实时瓦片执行器

```java
VectorTileExecutorV2 executor = VectorTileExecutorV2.getInstance(requestParams, "road_layer");
TileGlobalConfig config = executor.getTileGlobalConfig();
```

对应测试：`GirRealMvtEntryExample`、`TileGlobalConfigExample`

### 示例4：执行器配置对象

```java
TileExecutorConfig config = new TileExecutorConfig()
    .setLowLevelOptStrategy(TileExecutorConfig.LowLevelOptStrategy.PAGING)
    .setDensityOptStrategy(TileExecutorConfig.DensityOptStrategy.DENSITY_MERGING);
```

对应测试：`TileExecutorConfigExample`

### 示例5：离线 Spark 生成参数

```java
TileSliceParameter parameter = new TileSliceParameter()
    .setLayerName("road_layer")
    .setMinZoom(6)
    .setMaxZoom(14);
```

对应测试：`TileSliceParameterExample`

## 核心源码入口

- GitHub 源码目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-mvt`
- `geoair-mvt-tools`：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-mvt/geoair-mvt-tools/src/main/java/cn/geoair/map/dynamic/mvt/tools`
- `geoair-real-mvt`：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-mvt/geoair-real-mvt/src/main/java/cn/geoair/map/dynamic/mvt`
- `geoair-static-mvt-spark`：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-mvt/geoair-static-mvt-spark/src/main/java/cn/geoair/map/dynamic/statics/mvt/spark`

## 测试建议

建议优先从以下顺序阅读：

1. `AdvMvtTileUtilsExample`
2. `GirRealMvtEntryExample`
3. `TileRequestParamsExample`
4. `TileExecutorConfigExample`
5. `TileGlobalConfigExample`
6. `TileSliceParameterExample`

这样可以从工具层一路读到实时服务层，再进入离线切片层。
