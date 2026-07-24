## 模块定位

`geoair-static-mvt-spark` 负责静态矢量瓦片切片，它的定位不是完全复刻 `tippecanoe`，而是：

- 用 Java + Spark 的方式组织离线切片流程
- 让现有 Java 体系内的数据库读取、Geometry 处理、任务配置和输出写入保持在一条统一链路上
- 覆盖常见的矢量瓦片离线生成场景

它没有 `tippecanoe` 那么全面，但已经实现了“数据库读取 -> 要素映射 -> 聚合 -> 生成 PBF -> 写入输出”的主要流程。

并且这一层一个很突出的特点是：**支持 4490 网格与 3857 网格两套输出体系**。

这意味着：

- 切片任务可以通过 `outGridSrid` 直接指定输出网格
- 同一套切片逻辑可以覆盖互联网地图常见的 3857，也可以覆盖 4490 这类地理坐标网格体系
- 这在国内 GIS 项目里是一个非常实用的能力点

## 设计想法

这一层最值得关注的不是某一个工具类，而是整体参数模型和执行链：

1. 先把切片任务描述成 `TileSliceParameter`
2. 再通过 Base32 把任务参数压缩成一个可传递字符串
3. 再由本地 Spark 或集群 Spark 启动切片任务
4. 由 `SparkVectorTileGenerator` 执行完整切片流程

这样带来的好处是：

- 切片任务参数是结构化的
- 任务可以在本地或集群中启动
- 参数和执行逻辑都在 Java 体系内，便于和现有项目整合
- 比直接依赖外部命令行工具更容易和业务系统集成

## TileSliceParameter：为什么它是核心

`TileSliceParameter` 是这一层最核心的参数对象。它不是简单 DTO，而是把切片任务几乎所有关键开关都统一收口了。

### 1. 输入信息配置

这一层定义：

- 输入连接信息 `inputConnectSimple`
- 输出连接信息 `outPutConnectWithTable`
- 几何字段名 `geomFieldName`
- ID 字段 `idFieldName`
- 查询语句 `queryStatement`
- 图层名称 `layerName`
- 版本号 `edition`
- 数据源坐标系 `sourceDataSrid`

### 2. 输出信息配置

这一层定义：

- 输出网格坐标 `outGridSrid`

这里需要特别强调：**`outGridSrid` 直接决定静态切片输出走 4490 还是 3857 网格。**

也就是说，这个参数不是普通附加字段，而是这套切片方案支持双网格体系的关键开关。

### 3. 缩放级别配置

这一层定义：

- `minZoom`
- `maxZoom`

这组字段本质上就对应了离线切片任务中“从哪一级切到哪一级”。

### 4. 瓦片限制配置

这一层定义：

- 是否开启要素数限制 `enableFeatureLimitIs`
- 是否开启瓦片大小限制 `enableFeatureSizeLimit`
- `featureLimit`
- `tileSizeLimit`

这里的设计思路非常明显：

- 不是只给一个“最大条数”参数
- 而是把“是否启用限制”和“限制值本身”分开
- 同时允许按字节大小限制瓦片体积

### 5. 要素过滤与优化配置

这一层定义：

- `includeFields`
- `dropDensestAsNeeded`
- `coalesceDensestAsNeeded`
- `simplificationLevel`
- `coalesceDistance`

这部分就是它和 `tippecanoe` 思路最接近的地方：

- 控制保留哪些字段
- 是否按密度丢弃
- 是否按密度合并
- 是否简化
- 聚合距离是多少

### 6. 任务扩展与系统参数

例如：

- `maxPartionNum`
- `createBoundary`
- `createLabel`
- `statisticsIs`
- `staticTableName`
- `typeGeom`
- `trackId`

这说明这个参数对象不仅管“切片本身”，也管：

- 是否额外生成边界层
- 是否生成标签层
- 是否做统计信息输出
- 当前任务流水号和系统字段

### 7. Base32 编解码

`TileSliceParameter` 还内置了：

- `toBase32()`
- `fromBase32(...)`

这非常关键，因为它把复杂切片配置压缩成一个字符串，便于：

- 命令行传递
- 远程任务提交
- 本地 Spark 启动时通过参数输入

## 第三方如何快速使用

对于第三方开发者，最快的接入方式其实不是先读 `SparkVectorTileGenerator` 全部实现，而是按下面顺序：

### 第一步：构造 TileSliceParameter

```java
TileSliceParameter parameter = new TileSliceParameter();
parameter.setLayerName("road_layer")
    .setEdition("v1")
    .setGeomFieldName("geom")
    .setIdFieldName("id")
    .setReadStrategy(ReadStrategy.ID_PAGE)
    .setSourceDataSrid(4326)
    .setOutGridSrid(3857)
    .setMinZoom(6)
    .setMaxZoom(14)
    .setDropDensestAsNeeded(true)
    .setCoalesceDensestAsNeeded(true);
```

对应测试：`TileSliceParameterExample`

### 第二步：编码参数

```java
String encoded = parameter.toBase32();
```

如果任务是外部传递的，则反向解码：

```java
TileSliceParameter decoded = TileSliceParameter.fromBase32(encoded);
```

### 第三步：本地 Spark 启动切片

```java
String base32 = args[0];
TileSliceParameter tileSliceParameter = TileSliceParameter.fromBase32(base32);

SparkSession spark = SparkSession.builder()
    .appName("spark-tile-app")
    .master("local[*]")
    .config("spark.executor.memory", "4g")
    .config("spark.driver.memory", "4g")
    .config("spark.extraListeners", "cn.geoair.map.dynamic.statics.mvt.spark.listener.SparkSQLListener")
    .getOrCreate();

SparkVectorTileGenerator sparkVectorTileGenerator = new SparkVectorTileGenerator(spark);
sparkVectorTileGenerator.doGenerate(tileSliceParameter);
spark.stop();
```

这就是 `SparkJavaTileLocalApp` 的主流程。

### 第四步：需要更复杂逻辑时，再进入 Generator

也就是说：

- 第三方最先应该理解参数对象
- 然后理解参数如何传递
- 最后再去读 `SparkVectorTileGenerator` 的完整实现

这样上手会比一开始直接钻进 Spark 任务链路更容易。

## 核心类

最重要的入口包括：

- `TileSliceParameter`
- `SparkJavaTileLocalApp`
- `SparkVectorTileGenerator`
- `SparkVectorTileGeneratorAll`
- `VectorTileCommonUtils`
- `SparkTaskSerializableUtil`

### TileSliceParameter

切片任务的总参数模型，是整个离线切片流程的主配置入口。

### SparkJavaTileLocalApp

本地 Spark 模式的启动入口，适合单机或开发机快速验证。

### SparkVectorTileGenerator

真正执行“读取数据 -> 映射瓦片 -> 聚合 -> 生成 PBF -> 写入”的主执行器。

## 对应测试入口

- `TileSliceParameterExample`

## GitHub 源码入口

- 模块目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-mvt/geoair-static-mvt-spark`
- dto 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-mvt/geoair-static-mvt-spark/src/main/java/cn/geoair/map/dynamic/statics/mvt/spark/vectile/dto`
- impl 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-mvt/geoair-static-mvt-spark/src/main/java/cn/geoair/map/dynamic/statics/mvt/spark/vectile/impl`
- 启动入口目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-mvt/geoair-static-mvt-spark/src/main/java/cn/geoair/map/dynamic/statics/mvt/spark/vectile`

## 阅读建议

建议顺序：

1. `TileSliceParameter`
2. `TileSliceParameterExample`
3. `SparkJavaTileLocalApp`
4. `SparkVectorTileGenerator`

先看参数模型，再看本地启动，再看主执行器，会更容易理解这层的设计思路和快速接入方式。
