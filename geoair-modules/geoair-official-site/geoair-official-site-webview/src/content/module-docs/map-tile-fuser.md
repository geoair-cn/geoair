## 模块定位

`geoair-map-tile-fuser` 的关键价值不是“再做一个瓦片读取器”，而是：

- 在 **3857 网格** 和 **4490 网格** 之间完成瓦片请求与输出的互转
- 把不同来源的瓦片统一成一个可读、可缓存、可融合的服务输出
- 允许项目在不改前端引擎的前提下，通过后端完成网格体系适配

这也是这个模块最突出的亮点：

> **支持 3857 网格与 4490 网格的互转。**

## 这个亮点为什么重要

在实际项目里，很多互联网瓦片都是：

- 谷歌墨卡托投影
- 谷歌原点
- 也就是更偏 3857 这一套体系

但在一些实际项目约束下，又会出现：

- 项目强制要求使用 4490 网格
- 某些前端引擎或历史版本不方便动态叠加不同网格的图片
- 系统已经沿用旧项目中的地图引擎或旧版 OL 配置

这时，如果前端不能很好地处理多网格叠加，后端就需要承担：

- **3857 -> 4490** 的转换输出
- 或者 **4490 -> 3857** 的转换输出

`geoair-map-tile-fuser` 就是解决这类问题的后端模块。

## 请求入口：TileServiceTran

这个模块最值得先读的入口之一就是：

- `TileServiceTran`

它本质上是一个“瓦片请求转换服务类”，负责：

- 根据请求的网格类型选择不同转换方法
- 统一构造边界框
- 通过 `GirFuserExecFactory` 创建融合执行器
- 输出最终图片字节
- 按需删除缓存并重建瓦片

### 1. Google 服务转 4326 / 4490 方向

典型入口：

- `googleServiceTo4326Request(...)`
- `googleServiceTo4326RequestDelCache(...)`

这组方法适合处理：

- 源瓦片来自 Google / Web Mercator 体系
- 后端需要按 4326 / 4490 方向组织返回逻辑

### 2. Grid4490 服务转 3857 方向

典型入口：

- `grid4490ServiceTo3857Request(...)`
- `grid4490ServiceTo3857RequestDelCache(...)`

这组方法适合处理：

- 源瓦片本身是 4490 网格
- 目标前端或目标接口仍然需要 3857 风格的输出

也就是说，这个模块不是只支持单向转换，而是双向都考虑到了。

### 3. 最终统一处理

无论入口来自哪一种转换方向，最后都会收敛到：

- `processTileRequest(...)`

这一步统一负责：

- 获取 `HttpServletResponse`
- 推导输出格式
- 生成 `FuserExec`
- 输出 `TileResponse`
- 错误时构造标准错误响应

## 项目里的实际使用方式

在实际项目中，可以像你给出的控制器一样，直接让 Controller 继承 `TileServiceTran`：

```java
@RestController
@RequestMapping("my-service/tile/fuser")
public class TileServiceTranController extends TileServiceTran {

    @GetMapping("/{layerName}/{dataId}/{verify}/{sourceSrid}/{z}/{x}/{y}")
    void tran(
            @PathVariable String layerName,
            @PathVariable Integer z,
            @PathVariable Integer x,
            @PathVariable Integer y,
            @PathVariable String dataId,
            @PathVariable String verify,
            @PathVariable String sourceSrid,
            @RequestParam(defaultValue = "png") String format) {

        MimeType fromFormat = ImageMime.createFromExtension(format);
        if (Objects.equals(sourceSrid, "3857")) {
            googleServiceTo4326Request(layerName, z, x, y, fromFormat.getFormat());
        } else {
            grid4490ServiceTo3857Request(layerName, z, x, y, fromFormat.getFormat());
        }
    }
}
```

这说明这个模块不是停留在工具层，而是很适合直接接进实际业务项目的 Controller 层。

## 配置模型：PxyLayerInfo

`PxyLayerInfo` 是这个模块里最重要的配置对象之一。

它负责描述一个“瓦片实现者 / 图层实现者”，至少包括：

- `layerName`
- `path`
- `originType`
- `srcType`
- `imageType`
- `gridSrid`
- `enableCache`
- 代理配置

### 这个对象的意义

它不是简单的 DTO，而是决定了：

- 图层叫什么
- 瓦片从哪里来
- 是本地文件还是远程服务
- 是 Google 原点还是其他原点
- 是 3857 网格还是 4490 网格
- 是否走缓存
- 是否要走网络代理

换句话说，`PxyLayerInfo` 几乎就是这个模块的图层配置核心。

## SrcType：为什么这个枚举很重要

`SrcType` 的作用不只是做字符串转枚举，它定义了整套 getter 的分流策略。

典型值包括：

- `WEB`
- `LOCAL`
- `MBTILES`
- `CUSTOM`

### 它的价值在于

通过 `SrcType`，模块能明确知道：

- 是从远程 HTTP 取瓦片
- 还是从本地文件系统取瓦片
- 还是从 MBTiles 中读取
- 或者是否完全交给项目方自定义实现

所以 `SrcType` 不是附属字段，而是实现器路由的关键输入。

## TileGetterFactory：如何分发到不同实现器

`TileGetterFactory` 是这个模块真正的“分发工厂”。

它会根据：

- `PxyLayerInfo.getSrcTypeEnums()`
- `PxyLayerInfo.getGridSrid()`
- 是否启用缓存

决定最终构造哪一个 `LayerTileGetter`。

### 分发逻辑的核心思路

#### 1. `srcType.isCustom()`

如果是 `CUSTOM`，就走：

- `CustomTileGetterHelper`

这意味着项目方可以完全替换默认获取器。

#### 2. `srcType.isMbtiles()`

走：

- `MBTilesTileGetter`

#### 3. `srcType.isLocal()`

如果是本地文件：

- `gridSrid == 3857` -> `GoogleLocalFileTileGetter`
- 其他 -> `Grid4490LocalFileTileGetter`

#### 4. 网络来源

如果是远程服务：

- `gridSrid == 3857` -> `GoogleWebTileGetter`
- 其他 -> `Grid4490WebTileGetter`

这就是它把：

- 来源类型
- 网格类型
- 获取器实现

三者串起来的核心逻辑。

## LayerTileGetter：最小能力契约

`LayerTileGetter` 自身是一个最小契约接口，核心方法只有几个：

- `getTileResource(int z, int x, int y)`
- `getSrcFormat()`
- `getSrcGridSubset()`

它的意义是：

- 不管瓦片来自本地、远程还是 MBTiles
- 最终都统一成同一套 getter 契约
- 上层融合逻辑不用关心底层细节，只关心“怎么拿资源、格式是什么、网格范围是什么”

## CustomTileGetterHelper：客户自定义扩展点

这是这个模块非常值得强调的一个设计点。

接口：

- `CustomTileGetterHelper`

关键方法：

```java
LayerTileGetter getTileGetterByPxyLayerInfo(PxyLayerInfo layerInfo);
```

### 设计意义

这意味着：

- 默认工厂已经提供了一批通用实现器
- 但项目方如果有自己的瓦片来源或特殊逻辑
- 完全可以通过 `CUSTOM` + `CustomTileGetterHelper` 接进去
- 不需要改默认工厂分发逻辑

这和前面标准层里 `base / core` 的“默认实现可替换”思路是统一的：

> 默认实现存在，但不是唯一实现；项目方可以按约定挂接自己的实现。

## 核心 API 示例

### 示例1：Google 服务转 4326 请求

```java
googleServiceTo4326Request(layerName, z, x, y, "image/png");
```

适合：

- 互联网常见瓦片体系转到 4326 / 4490 方向的服务输出

### 示例2：Grid4490 服务转 3857 请求

```java
grid4490ServiceTo3857Request(layerName, z, x, y, "image/png");
```

适合：

- 历史 4490 项目接到 3857 前端体系时的后端转换

### 示例3：图层配置对象

```java
PxyLayerInfo webLayer = new PxyLayerInfo()
    .setLayerName("web_layer")
    .setPath("https://tile.example.com/{z}/{x}/{y}.png")
    .setSrcType(SrcType.WEB.getCode())
    .setOriginType(OriginType.TMS.getMode())
    .setGridSrid(4490)
    .setEnableCache("false");
```

### 示例4：通过工厂路由实现器

```java
LayerTileGetter getter = TileGetterFactory.create(pxyLayerInfo);
```

### 示例5：自定义实现器入口

```java
LayerTileGetter getter = CustomTileGetterHelper.getInstance()
    .getTileGetterByPxyLayerInfo(layerInfo);
```

## 核心源码入口

- GitHub 模块目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-map-tile-fuser`
- 请求转换入口：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-map-tile-fuser/src/main/java/cn/geoair/map/tile/forge/fuser/TileServiceTran.java`
- 配置模型目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-map-tile-fuser/src/main/java/cn/geoair/map/tile/forge/fuser/entity`
- 枚举目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-map-tile-fuser/src/main/java/cn/geoair/map/tile/forge/fuser/enums`
- getter 工厂目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-map-tile-fuser/src/main/java/cn/geoair/map/tile/forge/fuser/provider`
- 自定义扩展入口：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-map-tile-fuser/src/main/java/cn/geoair/map/tile/forge/fuser/CustomTileGetterHelper.java`

## 对应测试入口

- `GirMapTileFuserExample`
- `TileFuserConfigExample`
- `LayerTileGetterRouteExample`
- `FuserExecContractExample`

## 阅读建议

建议顺序：

1. `PxyLayerInfo`
2. `SrcType`
3. `TileGetterFactory`
4. `TileServiceTran`
5. `CustomTileGetterHelper`
6. `FuserExec`

这样可以先理解配置模型，再理解实现器路由，最后再进入请求转换与融合输出链路。
