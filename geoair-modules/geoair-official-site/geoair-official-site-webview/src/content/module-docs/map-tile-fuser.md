## 模块定位

`geoair-map-tile-fuser` 解决的问题不是“如何读取一份瓦片”，而是“如何把多个瓦片源组合成一个统一图层”。

它的职责更偏：

- 根据图层配置构造 `LayerTileGetter`
- 支持本地文件、网络服务、MBTiles 等多种来源
- 结合缓存输出统一瓦片
- 在需要时执行预缓存、修复和检查任务

如果 `map-tile-forge` 更像单图层读取层，`map-tile-fuser` 更像多图层融合层。

## 模块结构

### 顶层入口

核心入口是：

- `GirFuser`

对应测试示例：

- `GirMapTileFuserExample`

### 图层帮助器

核心入口是：

- `GirFuserLayerTileHelper`

这层负责：

- 根据图层名查出 `PxyLayerInfo`
- 结合缓存工厂，构造最终的 `LayerTileGetter`

### 获取器工厂

核心入口是：

- `TileGetterFactory`

对应测试示例：

- `LayerTileGetterRouteExample`

### 核心配置模型

最重要的配置实体是：

- `PxyLayerInfo`
- `SrcType`
- `OriginType`

对应测试示例：

- `TileFuserConfigExample`

### 融合执行层

核心接口是：

- `FuserExec`

对应测试示例：

- `FuserExecContractExample`

## 核心 API 示例

### 示例1：按图层名直接获取 LayerTileGetter

```java
LayerTileGetter tileGetter = GirFuser.getLayerTileGetter("base_layer");
```

对应测试：`GirMapTileFuserExample`

### 示例2：按图层名读取 PxyLayerInfo

```java
PxyLayerInfo layerInfo = GirFuser.getPxyLayerInfo("base_layer");
```

对应测试：`GirMapTileFuserExample`

### 示例3：通过工厂按配置创建获取器

```java
LayerTileGetter tileGetter = TileGetterFactory.create(pxyLayerInfo);
```

对应测试：`LayerTileGetterRouteExample`

### 示例4：配置不同来源类型

```java
PxyLayerInfo webLayer = new PxyLayerInfo()
    .setLayerName("web_layer")
    .setPath("https://tile.example.com/{z}/{x}/{y}.png")
    .setSrcType(SrcType.WEB.getCode())
    .setOriginType(OriginType.TMS.getMode())
    .setGridSrid(4490);
```

对应测试：`TileFuserConfigExample`

### 示例5：融合执行契约

```java
byte[] imageBytes = fuserExec.toImageBytes();
ImageMime outputMime = fuserExec.getOutputFormat();
RangeApo srcRange = fuserExec.getSrcRange();
```

对应测试：`FuserExecContractExample`

## 核心源码入口

- GitHub 模块目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-map-tile-fuser`
- Getter 工厂目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-map-tile-fuser/src/main/java/cn/geoair/map/tile/forge/fuser/provider`
- Fuser 执行层目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-map-tile-fuser/src/main/java/cn/geoair/map/tile/forge/fuser/fuser`
- 缓存目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-map-tile-fuser/src/main/java/cn/geoair/map/tile/forge/fuser/cache`

## 测试建议

建议阅读顺序：

1. `TileFuserConfigExample`
2. `LayerTileGetterRouteExample`
3. `GirMapTileFuserExample`
4. `FuserExecContractExample`

这样可以先理解配置模型，再理解 getter 路由，最后再看融合执行契约。
