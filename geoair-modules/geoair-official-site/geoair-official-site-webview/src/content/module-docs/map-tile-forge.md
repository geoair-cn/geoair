## 模块定位

`geoair-map-tile-forge` 更像一层“瓦片读取与服务适配层”。它解决的问题不是地图渲染本身，而是：

- 图层配置怎么组织
- 不同存储类型和不同瓦片格式如何统一读取
- 瓦片如何通过统一服务入口暴露出来
- 本地 ZIP / 本地目录 / S3 等多种存储如何适配

如果已经有现成瓦片数据，这个模块负责把它们变成可读、可缓存、可通过服务访问的对象。

如果 `map-tile-fuser` 更偏“多源瓦片转换与融合”，那么 `map-tile-forge` 更偏“单图层瓦片读取与服务暴露”。

## 设计重点

### 1. 图层配置与读取逻辑分离

这个模块没有把“图层配置”直接写进某个读取器里，而是通过：

- `GirLayerConfigContext`
- `GirLayerConfigContextHelper`

先把图层怎么描述、如何按图层名拿到配置抽出来。

这样带来的好处是：

- 图层配置来源可以替换
- 服务层不用关心配置是数据库、配置文件还是别的来源
- 读取器选择逻辑可以完全依赖配置对象而不是硬编码

### 2. 存储适配器负责分发，而不是服务层自己写 if/else

最重要的设计点之一是：

- `GirMapTileService` 不自己判断存储类型和瓦片类型
- 它把这个职责交给 `TileStorageSupportAdapter`

也就是说，服务层只做：

1. 根据图层名查配置
2. 找到合适的 `ITileStorageSupport`
3. 调用 `getTileData(...)`

而真正的分发逻辑放在适配器层。

这让：

- 服务层保持很薄
- 新增存储类型时不必改 `GirMapTileService`
- 新增瓦片格式时也不必改服务主流程

### 3. 存储类型与瓦片类型是两层维度

`TileStorageSupportAdapter` 的设计非常清楚：

第一层先看：

- `GirStorageType`

第二层再看：

- `GirMapTileType`

也就是说，它不是只按“文件在哪”分发，也不是只按“瓦片格式是什么”分发，而是把：

- 存储方式
- 瓦片格式

组合成一套适配矩阵。

例如：

- `LOCAL_ZIP + XYZ`
- `LOCAL_ZIP + COMPACT_V1`
- `S3_ZIP + XYZ`
- `LOCAL_UNZIPPED + COMPACT_V2`

都会对应到不同的具体 `ITileStorageSupport` 实现。

## 核心类

### GirMapTileService

这一层是瓦片服务入口。它的核心思路是：

- `getLayerTile(layerName, z, y, x)`：按图层名查配置并读取瓦片
- `getLayerTile(config, z, y, x)`：直接用配置对象读取瓦片
- `getCapabilities(layerName)`：获取能力描述
- `preCacheTiles(layerName)`：触发图层预缓存

它的特点是：

- 自己不关心具体瓦片怎么读取
- 只关心图层配置查找 + support 分发 + 返回 `TileRequest`

### TileStorageSupportAdapter

这是整个模块最关键的设计类之一。

它内部会：

1. 根据 `GirLayerConfigContext` 取出：
   - `GirStorageType`
   - `GirMapTileType`
2. 生成缓存 key
3. 通过 `computeIfAbsent(...)` 缓存 support 实例
4. 再按组合规则创建真实 `ITileStorageSupport`

这说明：

- support 实例是有缓存的
- 相同的“存储类型 + 瓦片格式”不会重复创建
- 模块本身已经考虑到了适配实例复用问题

### GirLayerConfigContext

这层是图层配置对象，负责描述：

- `dataId`
- `objectKey`
- `storageType`
- `mapTileType`
- `tilePathPrefix`
- `format`
- `minZ / maxZ / maxX / maxY`

它的作用是：

- 把“图层是什么”和“图层在哪里”统一描述出来
- 让后续的适配器和 service 都以配置对象为中心工作

### TileRequest

`TileRequest` 是这个模块统一返回瓦片结果的对象。

它负责描述：

- 图层名
- 瓦片格式
- 存储类型
- 字节内容
- 文件大小
- MIME 类型
- 是否存在
- 最后修改时间

这意味着：

- 读取器层不直接返回裸字节
- 服务层也不直接拼响应
- 所有瓦片返回结果会先统一落到 `TileRequest`

## 适配矩阵是怎么工作的

### LOCAL_ZIP

例如：

- `COMPACT_V1` -> `LocalZipCompactV1TileStorageSupport`
- `COMPACT_V2` -> `LocalZipCompactV2TileStorageSupport`
- `XYZ` -> `LocalZipXYZTileStorageSupport`
- `TILE_3D / S3M` -> `LocalZip3DTileStorageSupport`
- `TERRAIN_3D` -> `LocalZip3DTerrainStorageSupport`

### S3_ZIP

例如：

- `COMPACT_V1` -> `S3ZipCompactV1TileStorageSupport`
- `COMPACT_V2` -> `S3ZipCompactV2TileStorageSupport`
- `XYZ` -> `S3ZipXYZTileStorageSupport`

### LOCAL_UNZIPPED / S3_UNZIPPED

同理也会按不同 `GirMapTileType` 分配到不同 support。

这就体现了这个模块的真正设计价值：

> 不是只写一个“读本地文件”的读取器，而是把多种存储方式和多种瓦片格式组织成了一整套适配层。

## 核心 API 示例

### 示例1：按图层名读取瓦片

```java
TileRequest tileRequest = GirMapTileService.getInstance()
    .getLayerTile("base_layer", "10", "388", "845");
```

对应测试：`GirMapTileForgeExample`

### 示例2：按配置对象读取瓦片

```java
GirLayerConfigContext config = contextHelper.getByLayerName("base_layer")
    .orElseThrow(() -> new RuntimeException("图层配置不存在"));

TileRequest tileRequest = GirMapTileService.getInstance()
    .getLayerTile(config, "10", "388", "845");
```

对应测试：`GirMapTileForgeExample`

### 示例3：通过适配器选择 support

```java
ITileStorageSupport support = tileStorageSupportAdapter.getSupport(config);
TileRequest tileRequest = support.getTileData(config, "10", "845", "388");
```

对应测试：`TileStorageSupportAdapterRouteExample`

### 示例4：图层配置模型

```java
GirLayerConfigContext context = new GirLayerConfigContext()
    .setDataId("base_layer")
    .setStorageType(GirStorageType.LOCAL_ZIP)
    .setMapTileType(GirMapTileType.XYZ)
    .setObjectKey("E:/tiles/base_layer.zip");
```

对应测试：`GirLayerConfigContextExample`

### 示例5：读取结果对象

```java
TileRequest empty = TileRequest.emptyByContext(context);
```

对应测试：`TileRequestExample`

### 示例6：本地 ZIP XYZ 预缓存

```java
TileStorageSupportAdapter adapter = new TileStorageSupportAdapter(new TestGirLayerConfigContextHelper());
ITileStorageSupport support = adapter.getSupport(context);
support.preCacheTiles(context, new LogProgressConsumer());
```

对应测试：`XyzTest`

## 核心源码入口

- GitHub 模块目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-map-tile-forge`
- 服务入口目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-map-tile-forge/src/main/java/cn/geoair/map/tile/forge/core/service`
- 适配层目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-map-tile-forge/src/main/java/cn/geoair/map/tile/forge/core/support`
- Servlet 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-map-tile-forge/src/main/java/cn/geoair/map/tile/forge/core/servlet`

## 对应测试入口

- `GirMapTileForgeExample`
- `GirLayerConfigContextExample`
- `TileForgeEnumExample`
- `TileStorageSupportAdapterRouteExample`
- `TileRequestExample`
- `XyzTest`

## 阅读建议

建议顺序：

1. `GirLayerConfigContextExample`
2. `TileForgeEnumExample`
3. `TileStorageSupportAdapterRouteExample`
4. `GirMapTileForgeExample`
5. `TileRequestExample`
6. `XyzTest`

这样能先建立配置模型，再理解适配器选择逻辑，最后看服务入口和预缓存流程。
