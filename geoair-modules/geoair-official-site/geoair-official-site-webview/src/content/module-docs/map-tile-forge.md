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

它负责：

- 根据图层名查找图层配置
- 通过 `TileStorageSupportAdapter` 获取正确的存储支持实现
- 读取瓦片数据或能力描述
- 触发预缓存逻辑

### 存储适配层

核心入口是：

- `TileStorageSupportAdapter`
- `ITileStorageSupport`

这层负责根据：

- `GirStorageType`
- `GirMapTileType`

选择正确的具体实现，例如：

- `LocalZipXYZTileStorageSupport`
- `LocalUnzippedXYZTileStorageSupport`
- `S3ZipCompactV1TileStorageSupport`
- `S3UnzippedCompactV2TileStorageSupport`

### 图层配置模型

核心模型是：

- `GirLayerConfigContext`

这层负责保存：

- 图层名
- 瓦片类型
- 存储类型
- 对象路径 / objectKey
- 其他图层级配置

### Web 服务层

核心入口是：

- `XYZServlet`
- `D3TilesServlet`
- `D3TerrainServlet`

这层负责把读取到的瓦片数据通过 HTTP 暴露出去。

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

适用场景：

- 统一从图层服务中读取 XYZ 瓦片
- 前端通过图层名访问瓦片服务时的主入口

### 示例2：按配置对象读取瓦片

```java
GirLayerConfigContext config = contextHelper.getByLayerName("base_layer")
    .orElseThrow(() -> new RuntimeException("图层配置不存在"));

TileRequest tileRequest = GirMapTileService.getInstance()
    .getLayerTile(config, "10", "388", "845");
```

适用场景：

- 已经拿到图层配置对象，想跳过二次查找
- 后台任务或测试代码中直接控制图层配置

### 示例3：根据图层配置选择存储支持实现

```java
ITileStorageSupport support = tileStorageSupportAdapter.getSupport(config);
TileRequest tileRequest = support.getTileData(config, "10", "845", "388");
```

适用场景：

- 调试某个图层最终会走到哪一种存储支持实现
- 新增存储类型或瓦片类型时验证适配链路

### 示例4：触发预缓存

```java
GirMapTileService.getInstance().preCacheTiles("base_layer");
```

适用场景：

- 预热瓦片缓存
- 新图层上线后先把热点瓦片缓存准备好

### 示例5：直接走 XYZ ZIP 测试链路

```java
GirLayerConfigContext context = new GirLayerConfigContext();
context.setDataId("XYZ")
    .setMapTileType(GirMapTileType.XYZ)
    .setStorageType(GirStorageType.LOCAL_ZIP)
    .setObjectKey("E:/tiles/example.zip");

TileStorageSupportAdapter adapter = new TileStorageSupportAdapter(new TestGirLayerConfigContextHelper());
ITileStorageSupport support = adapter.getSupport(context);
support.preCacheTiles(context, new LogProgressConsumer());
```

适用场景：

- 本地 ZIP XYZ 瓦片快速验证
- 对照已有 `XyzTest` 理解图层配置和支持实现的关系

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

建议重点补和维护这些示例：

- 基于 `GirMapTileService` 的服务入口示例
- 基于 `TileStorageSupportAdapter` 的适配选择示例
- 基于 `XyzTest` 的 ZIP XYZ 预缓存示例
- 基于图层配置对象的本地 / S3 / 压缩格式切换示例

这些 test 能直接帮助理解模块的“配置 -> 适配 -> 读取 -> 输出”主流程。
