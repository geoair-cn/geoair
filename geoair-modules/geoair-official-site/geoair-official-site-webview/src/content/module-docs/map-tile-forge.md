## 模块定位

`geoair-map-tile-forge` 更像一层“瓦片读取与服务适配层”。它解决的问题不是地图渲染本身，而是：

- 图层配置怎么组织
- 不同存储类型和不同瓦片格式如何统一读取
- 瓦片如何通过统一服务入口暴露出来
- 本地 ZIP / 本地目录 / S3 等多种存储如何适配

如果你已经有现成瓦片数据，这个模块负责把它们变成可读、可缓存、可通过服务访问的对象。

## 模块结构

### 服务入口

核心入口是：

- `GirMapTileService`

对应测试示例：

- `GirMapTileForgeExample`

### 存储适配层

核心入口是：

- `TileStorageSupportAdapter`
- `ITileStorageSupport`

对应测试示例：

- `TileStorageSupportAdapterRouteExample`

### 图层配置模型

核心模型是：

- `GirLayerConfigContext`

对应测试示例：

- `GirLayerConfigContextExample`

### 枚举与返回对象

关键对象包括：

- `GirStorageType`
- `GirMapTileType`
- `TileRequest`

对应测试示例：

- `TileForgeEnumExample`
- `TileRequestExample`

### ZIP XYZ 快速验证

对应测试示例：

- `XyzTest`

## 核心服务链路

整体流程可以概括成：

1. 根据图层名查出 `GirLayerConfigContext`
2. `TileStorageSupportAdapter` 根据图层配置挑选正确实现
3. `ITileStorageSupport#getTileData(...)` 读取瓦片
4. `GirMapTileService` 返回 `TileRequest`
5. Servlet / Controller 层把 `TileRequest` 输出给前端

## 核心 API 示例

### 示例1：按图层名读取瓦片

```java
TileRequest tileRequest = GirMapTileService.getInstance()
    .getLayerTile("base_layer", "10", "388", "845");
```

对应测试：`GirMapTileForgeExample`

### 示例2：根据图层配置选择存储支持实现

```java
ITileStorageSupport support = tileStorageSupportAdapter.getSupport(config);
TileRequest tileRequest = support.getTileData(config, "10", "845", "388");
```

对应测试：`TileStorageSupportAdapterRouteExample`

### 示例3：图层配置对象

```java
GirLayerConfigContext context = new GirLayerConfigContext()
    .setDataId("base_layer")
    .setStorageType(GirStorageType.LOCAL_ZIP)
    .setMapTileType(GirMapTileType.XYZ)
    .setObjectKey("E:/tiles/base_layer.zip");
```

对应测试：`GirLayerConfigContextExample`

### 示例4：读取结果对象

```java
TileRequest empty = TileRequest.emptyByContext(context);
```

对应测试：`TileRequestExample`

### 示例5：本地 ZIP XYZ 预缓存

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

## 测试建议

建议阅读顺序：

1. `GirLayerConfigContextExample`
2. `TileForgeEnumExample`
3. `TileStorageSupportAdapterRouteExample`
4. `GirMapTileForgeExample`
5. `TileRequestExample`
6. `XyzTest`

这样能先建立配置模型，再理解适配器选择逻辑，最后看服务入口和预缓存流程。
