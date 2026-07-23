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

## 核心入口

### 实时服务入口

```java
GirRealMvtHelper helper = GirRealMvtHelper.getInstance();
```

这个入口负责：

- 提供矢量瓦片构建器消费者
- 校验请求参数
- 解析当前请求里的 `TileRequestParams`

### 实时执行器入口

```java
VectorTileExecutorV2 executor = VectorTileExecutorV2.getInstance(requestParams, layerName);
```

这类执行器负责：

- 组装实际 SQL
- 根据瓦片范围做 `ST_Intersects` 查询
- 控制分页、密度、裁剪与输出字段

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

适用场景：

- 调试某一层级、某一块瓦片对应的几何范围
- 在实时瓦片生成前先确认当前瓦片的空间边界

### 示例2：获取实时 MVT 辅助入口

```java
GirRealMvtHelper helper = GirRealMvtHelper.getInstance();
TileRequestParams requestParams = helper.getTileRequestParams("road_layer");
ParamCheckResult result = helper.checkTileRequestParams(requestParams, "road_layer");
```

适用场景：

- 统一处理请求参数
- 在服务层提前校验请求是否合法

### 示例3：构建实时瓦片执行器

```java
VectorTileExecutorV2 executor = VectorTileExecutorV2.getInstance(requestParams, "road_layer");
TileGlobalConfig config = executor.getTileGlobalConfig();
```

适用场景：

- 进入实时矢量瓦片执行链
- 在服务端查看某一层当前请求的执行上下文

### 示例4：离线 Spark 生成器

```java
SparkVectorTileGenerator generator = new SparkVectorTileGenerator(sparkSession);
generator.doGenerate(parameter);
```

适用场景：

- 需要把大批量数据库要素切成静态矢量瓦片
- 希望结果落库或落文件，而不是实时按请求生成

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

建议把示例重点放在：

- `AdvMvtTileUtils` 的瓦片范围计算
- `GirRealMvtHelper` 的参数解析与入口调用
- `VectorTileExecutorV2` 的执行配置读取
- `SparkVectorTileGenerator` 的离线生成入口

这几类示例更适合做 test / example，因为它们能直接体现模块结构和职责，而不是只停留在 DTO 层。
