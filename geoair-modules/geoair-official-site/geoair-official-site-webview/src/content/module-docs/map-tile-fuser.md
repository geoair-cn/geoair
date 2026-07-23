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

它提供短入口：

- `getLayerTileGetter(layerName)`
- `getPxyLayerInfo(layerName)`

适合业务代码快速拿到图层读取器或图层配置。

### 图层帮助器

核心入口是：

- `GirFuserLayerTileHelper`

它负责：

- 根据图层名查出 `PxyLayerInfo`
- 结合缓存工厂，构造最终的 `LayerTileGetter`

### 获取器工厂

核心入口是：

- `TileGetterFactory`

这层根据：

- `SrcType`
- `gridSrid`
- 是否启用缓存

选择不同实现，例如：

- `GoogleLocalFileTileGetter`
- `GoogleWebTileGetter`
- `Grid4490LocalFileTileGetter`
- `Grid4490WebTileGetter`
- `MBTilesTileGetter`
- `CachedTileGetter`

### 融合执行层

核心接口是：

- `FuserExec`

它负责定义融合输出的最小能力：

- `toImageBytes()`
- `getOutputFormat()`
- `getSrcFormat()`
- `getSrcRange()`

这是最终把融合结果转成图像字节的执行层抽象。

## 核心配置模型

这一层最重要的配置实体是：

- `PxyLayerInfo`

它定义：

- `layerName`
- `path`
- `srcType`
- `originType`
- `imageType`
- `gridSrid`
- `enableCache`
- 代理配置

所以在这个模块里，很多行为不是直接由 API 参数决定，而是由 `PxyLayerInfo` 决定。

## 核心 API 示例

### 示例1：按图层名直接获取 LayerTileGetter

```java
LayerTileGetter tileGetter = GirFuser.getLayerTileGetter("base_layer");
```

适用场景：

- 业务代码中只知道图层名，想直接拿到读取器
- 把图层读取逻辑封装在统一服务后调用

### 示例2：按图层名读取 PxyLayerInfo

```java
PxyLayerInfo layerInfo = GirFuser.getPxyLayerInfo("base_layer");
```

适用场景：

- 调试当前图层到底用的是哪种来源和网格体系
- 页面或后台任务需要查看图层配置

### 示例3：通过帮助器构造 LayerTileGetter

```java
PxyLayerInfo pxyLayerInfo = helper.getPxyLayerInfo("base_layer");
LayerTileGetter tileGetter = helper.getLayerTileGetter("base_layer");
```

适用场景：

- 你已经在 Spring 中注入了 `GirFuserLayerTileHelper`
- 想在获取 getter 前先看配置对象

### 示例4：通过工厂按配置创建获取器

```java
LayerTileGetter tileGetter = TileGetterFactory.create(pxyLayerInfo);
```

如果要显式带缓存：

```java
LayerTileGetter tileGetter = TileGetterFactory.create(pxyLayerInfo, tileCache);
```

适用场景：

- 需要调试配置最终路由到哪一种具体 getter
- 新增 getter 类型或缓存策略时做单点验证

### 示例5：融合执行接口

```java
byte[] imageBytes = fuserExec.toImageBytes();
ImageMime outputMime = fuserExec.getOutputFormat();
RangeApo srcRange = fuserExec.getSrcRange();
```

适用场景：

- 需要获取融合后的最终图像输出
- 需要分析源瓦片覆盖范围和输出格式

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

建议重点补和维护这些示例：

- 基于 `GirFuser` 的快速入口示例
- 基于 `TileGetterFactory` 的 getter 路由示例
- 基于 `PxyLayerInfo` 的本地 / 网络 / MBTiles 配置示例
- 基于 `FuserExec` 的输出格式和范围读取示例

这几类示例能把这个模块最核心的“图层配置 -> getter 选择 -> 融合输出”链路讲清楚。
