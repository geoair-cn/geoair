## 模块定位

`geoair-geo-tools` 是 GeoAir 中最偏日常开发工具箱的一层，统一封装坐标转换、格式互转、空间测量、几何合并、SRID 转换和瓦片计算。

和 `geoair-adv-query` 这类更偏查询执行器的模块不同，`geoair-geo-tools` 更关注 Geometry、坐标和瓦片本身的处理。

## 统一入口

```java
GirGeoTools tools = GirGeoTools.defaultInstance();

GirCoordinateConvertOpt coordinateOpt = tools.getCoordinateOpt();
GirGeoFormatOpt formatOpt = tools.getFormatOpt();
GirGeoMeasureOpt measureOpt = tools.getMeasureOpt();
GirGeoMergeOpt mergeOpt = tools.getMergeOpt();
GirSridConvertOpt sridOpt = tools.getSridOpt();
GirTileConverterOpt tileOpt = tools.getTileGrid4326Opt();
```

## 坐标转换

`GirCoordinateConvertOpt` 负责：

- WGS84 / GCJ02 / BD09 互转
- WGS84 与墨卡托互转
- 单点、Point、批量数组、Geometry 全类型转换
- DMS / DD 格式转换

```java
double[] gcj02 = GirGeoTools.defaultInstance()
    .getCoordinateOpt()
    .wgs84ToGcj02(116.40, 39.90);

Point pointBd09 = GirGeoTools.defaultInstance()
    .getCoordinateOpt()
    .wgs84ToBd09(point);
```

## 格式转换

`GirGeoFormatOpt` 负责：

- GeoJSON <-> JTS Geometry
- WKT <-> JTS Geometry
- WKB <-> JTS Geometry
- PGGeometry <-> JTS Geometry
- Point、Reader、Writer、GeometryJSON 等底层入口

```java
Geometry geometry = GirGeoTools.defaultInstance()
    .getFormatOpt()
    .geojsonToJtsGeometry(geojson, false);

String wkt = GirGeoTools.defaultInstance()
    .getFormatOpt()
    .jtsGeometryToWktString(geometry, false);
```

## 测量计算

`GirGeoMeasureOpt` 负责：

- 面积计算
- 长度计算
- 点点 / 点线 / 点面 / 线线距离
- Web Mercator 快速测量与 UTM 局部测量
- 单位换算

```java
GirGeoMeasureOpt measureOpt = GirGeoTools.defaultInstance().getMeasureOpt();

// 面向瓦片地图和快速展示：明确使用 Web Mercator，纬度越高形变越明显。
double mapArea = measureOpt.calculateArea(
    polygon, 4326, MeasureUnitEnum.SQUARE_KILOMETER, MeasureMethodEnum.WEB_MERCATOR);

// 范围较小且基本位于同一 UTM 投影带：使用局部 UTM 平面测量。
double localArea = measureOpt.calculateArea(
    polygon, 4326, MeasureUnitEnum.SQUARE_KILOMETER, MeasureMethodEnum.UTM);

// 两点距离使用大地线；其余方式也可作为 method 传入。
double distance = measureOpt.calculatePointToPointDistance(
    point1, point2, 4326, MeasureUnitEnum.METER, MeasureMethodEnum.GEODETIC);
```

测量方式统一由 `MeasureMethodEnum` 输入：`WEB_MERCATOR` 用于快速展示，`UTM` 用于局部平面测量，
`GEODETIC` 用于两点和线、面边界的大地线长度。单位由 `MeasureUnitEnum` 输入，长度与面积量纲不能混用。
测量接口只接收 JTS `Geometry` / `Point`；WKT 请先用 `GirFormatUtils` 转换，坐标数组请先构造为 JTS 几何对象。
UTM 以几何中心选择一个投影带，不适合跨带、跨半球、极区或测绘级结算；大地线面积暂未提供，传入时会显式报错。

```java
double distanceInKilometers = measureOpt.calculatePointToPointDistance(
    point1, point2, 4326, MeasureUnitEnum.KILOMETER, MeasureMethodEnum.GEODETIC);
```

## 几何合并

`GirGeoMergeOpt` 负责：

- 合并成 `MultiPoint`
- 合并成 `MultiLineString`
- 合并成 `MultiPolygon`
- 合并成单条线或单个面

```java
MultiLineString multiLine = GirGeoTools.defaultInstance()
    .getMergeOpt()
    .mergeToMultiLineString(lineStrings);
```

## SRID 转换

`GirSridConvertOpt` 负责：

- Geometry / Envelope / 点坐标转换
- CRS 获取
- `4326 <-> 3857` 等常见 SRID 互转

```java
Geometry geometry3857 = GirGeoTools.defaultInstance()
    .getSridOpt()
    .convert(geometry4326, 4326, 3857);
```

## 瓦片与 QuadKey

`GirTileConverterOpt` 和 `GirBingMapQuadKeyOpt` 负责：

- `xyzToTileBox`
- `tileRangeByBox`
- `tileRangeByGeom`
- `zxyListByGeom`
- QuadKey 生成与解析

```java
BoxReferencedEnvelope tileBox = GirGeoTools.defaultInstance()
    .getTileGrid4326Opt()
    .xyzToTileBox(10, 845, 388, 4326);

String quadKey = GirGeoTools.defaultInstance()
    .getTileGridBingMapOpt()
    .xyzToQuadKey(845, 388, 10);
```

### 范围、Y 轴与 4326 网格约定

在 GeoAir 中，`tileRangeByBox(...)` 与 `tileRangeByGeom(...)` 返回的
`RangeApo` 统一采用尾部闭区间：

```text
[minX, maxX] × [minY, maxY]
```

也就是说，`maxX`、`maxY` 都是最后一个要处理的瓦片索引，遍历时直接使用
`<=`，不要再对最大值减一。这与 `zxyListByGeom(...)`、预缓存和瓦片融合任务使用的
范围一致。

默认瓦片接口保持 Google/XYZ（左上角原点）语义；需要和 TMS（左下角原点）交互时，
请明确传入 `TileYAxis`，而不是在业务代码中手写 `2^z - 1 - y`：

```java
GirTileConverterOpt tileOpt = GirGeoTools.defaultInstance().getTileGrid3857Opt();

int tmsY = tileOpt.convertY(10, xyzY, TileYAxis.XYZ, TileYAxis.TMS);
BoxReferencedEnvelope box = tileOpt.xyzToTileBox(10, x, tmsY, TileYAxis.TMS, 4326);
```

`getTileGrid4326SeparateOpt()` 表示非等轴 4326 网格：经度列数为 `2^z`，纬度行数为
`max(1, 2^(z-1))`。例如 z=3 时是 **8 列 × 4 行**，每行覆盖 45° 纬度并完整覆盖
`[-90°, 90°]`。这适合需要保持历史 4326 非等轴瓦片定义的项目。

Separate 与 Equal 两套 4326 网格当前按同一份实际 Y 行数工作，z=3 都只有 4 行。因此
`convertSeparateAxisYToEqualAxisY(...)` 和反向方法会按当前层级元数据校验 `y`，并在
`0..3` 间一一对应；不能再把 `2^z` 当作 Separate 的 Y 行数。保留的 `roundingType`
参数只是为了兼容旧调用，不参与当前映射：

```java
GirTileConverterOpt separate = GirGeoTools.defaultInstance().getTileGrid4326SeparateOpt();

int equalY = separate.convertSeparateAxisYToEqualAxisY(
    3, 3, AbstractWgs84TileConverter.RoundingType.FLOOR);
// equalY == 3；Y=4 会因越过 z=3 的最后一行而抛出 IllegalArgumentException。
```

## TileResponse 与流式瓦片输出

瓦片服务可以先把读取逻辑收敛为 `TileResponse`，再交给 Servlet 输出。字节数据用
`TileResponseByByte`；来源是文件、HTTP 或对象存储流时使用 `TileResponseByInputStream`。

```java
TileResponse response = TileResponseByInputStream.success(inputStream, GirImageMime.png, contentLength);
GirTileResponseUtil.buildFromTileResponse(response, servletResponse);
```

如果上游无法提供可靠的 `contentLength`，使用不带长度的 `success(inputStream, mimeType)`。
GeoAir 不会再以 `InputStream.available()` 猜测总长度，也不会写入错误的 `Content-Length`；
Servlet 容器会按流式响应处理。原始输入流只可消费一次；需要重复读取或进行图像增强时，
调用 `toByteArrays()` 将其缓存为字节数据。

### CRS 使用建议

`GirSridConvertOpt` 对常见 CRS 提供快速类型判断，并会在无法解析 CRS 时显式抛错；
不要把未知 SRID 默认为地理坐标系。常见的 4326、4490、4480、4979 等地理 CRS 会被
直接识别，3857、常见 UTM 与 CGCS2000 高斯-克吕格分带会识别为投影 CRS。

## 阅读建议

建议顺序：

1. 先看统一入口
2. 再看坐标 / 格式转换
3. 然后看测量与 SRID
4. 最后看瓦片与 QuadKey

如果需要直接对照 Java 示例，可优先查看 `src/test/java/cn/geoair/map/dynamic/tools/test` 下的示例类。
