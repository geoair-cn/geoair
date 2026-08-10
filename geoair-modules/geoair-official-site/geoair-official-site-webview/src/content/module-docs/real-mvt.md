## 模块定位

`geoair-real-mvt` 负责实时矢量瓦片服务。它的目标不是离线预生成，而是：

- 接收当前瓦片请求参数
- 计算当前瓦片范围
- 按需查询数据库里的 Geometry
- 做裁剪、简化、密度控制
- 直接返回当前瓦片的 PBF 数据

如果 `geoair-static-mvt-spark` 更像离线切片生产层，那么 `geoair-real-mvt` 更像在线服务层。

并且这一层同样支持 **4490 网格与 3857 网格**。

这意味着：

- 实时瓦片请求不只适用于常见互联网地图的 3857 体系
- 也能围绕 4490 组织瓦片范围与输出逻辑
- 在线服务和离线切片在网格体系上可以保持一致

## 适用场景

适合：

- 需要根据前端当前视域实时生成矢量瓦片
- 数据变化频繁，不适合提前全量切片
- 希望把数据库查询、空间过滤和瓦片编码放在一个实时链路里

## 核心类

最重要的入口包括：

- `GirRealMvtHelper`
- `TileExecutorFactory`（多方言：Postgis/Oracle/Mysql）
- `PostgisVectorTileExecutor` / `OracleVectorTileExecutor` / `MysqlVectorTileExecutor`
- `TileRequestParams`
- `TileExecutorConfig`
- `TileGlobalConfig`

### GirRealMvtHelper

负责：

- 提供辅助入口
- 校验请求参数
- 生成或解析 `TileRequestParams`

### 瓦片执行器（多方言）

`TileExecutorFactory` 根据数据源类型自动选择：

- **PostGIS** → `PostgisVectorTileExecutor`
- **Oracle** → `OracleVectorTileExecutor`
- **MySQL** → `MysqlVectorTileExecutor`
- **达梦** → 复用 `OracleVectorTileExecutor`

负责：

- 按图层和请求参数组织 SQL
- 结合瓦片范围做查询
- 把查询结果交给矢量瓦片构建器处理

### TileRequestParams

负责描述一次实时瓦片请求的输入参数，例如：

- 数据源 ID
- schema / 表名
- geometry 字段
- zoom 限制
- 保留字段

### TileExecutorConfig / TileGlobalConfig

负责描述执行器的运行配置和全局运行上下文。

## 核心 API 示例

### 示例1：获取实时入口

```java
GirRealMvtHelper helper = GirRealMvtHelper.getInstance();
```

对应测试：`GirRealMvtEntryExample`

### 示例2：请求参数组织与编码

```java
TileRequestParams params = new TileRequestParams();
params.setDsId("gis_ds");
params.setSchemaName("public");
params.setTbNameOrSql("road_layer");
params.setGeomFieldName("geom");

String encoded = params.toBase32();
TileRequestParams decoded = TileRequestParams.fromBase32(encoded);
```

对应测试：`TileRequestParamsExample`

### 示例3：构建执行器

```java
ITileExecutor executor = TileExecutorFactory.getInstance(requestParams, "road_layer");
TileGlobalConfig config = executor.getTileGlobalConfig();
```

对应测试：`GirRealMvtEntryExample`、`TileGlobalConfigExample`

### 示例4：执行器配置对象

```java
TileExecutorConfig config = new TileExecutorConfig();
config.setLowLevelOptStrategy(TileExecutorConfig.LowLevelOptStrategy.PAGING)
    .setDensityOptStrategy(TileExecutorConfig.DensityOptStrategy.DENSITY_MERGING)
    .setPagingStartLevel(10)
    .setMaxLimitCount(3000L);
```

对应测试：`TileExecutorConfigExample`

### 示例5：全局配置对象

```java
TileGlobalConfig globalConfig = new TileGlobalConfig()
    .setLayerName("road_layer")
    .setVersion(2)
    .setTileRequestParams(requestParams)
    .setTileExecConfig(config);
```

对应测试：`TileGlobalConfigExample`

## 核心源码入口

- GitHub 模块目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-mvt/geoair-real-mvt`
- dto 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-mvt/geoair-real-mvt/src/main/java/cn/geoair/map/dynamic/mvt/dto`
- exec 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-mvt/geoair-real-mvt/src/main/java/cn/geoair/map/dynamic/mvt/exec`

## 对应测试入口

- `GirRealMvtEntryExample`（参考 `TileExecutorFactory` 用法）
- `TileRequestParamsExample`
- `TileExecutorConfigExample`
- `TileGlobalConfigExample`

## 阅读建议

建议顺序：

1. `TileRequestParamsExample`
2. `GirRealMvtEntryExample`
3. `TileExecutorConfigExample`
4. `TileGlobalConfigExample`

先看输入参数，再看执行入口，最后再看执行配置和全局配置，会更容易理解整个实时服务链路。
