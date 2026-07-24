## 模块定位

`geoair-file-tran` 负责空间文件与数据库之间的读写和转换。它不是单个 Reader 或 Writer，而是一套由核心抽象和具体格式实现组成的管道。

适用于以下 GIS 文件处理场景：

- GeoJSON 导入导出
- Shapefile 读写
- PostGIS 数据读写
- 格式之间的批量转换

那么这组模块就是文件层面的入口。

## 模块结构

### 核心抽象层

核心接口 / 实现：

- `GeoFileReader`
- `GeoFileWriter`
- `GeoFileTran`
- `GeoFileTranImpl`

这层负责定义：

- 如何读取空间数据
- 如何输出空间数据
- 如何组织一条 Reader -> Transformer -> Writer 的转换链路

### 具体格式实现

已经可以看到的典型实现包括：

- `GeoJsonGeoFileReader`
- `ShpGeoFileReader`
- `PostgisGeoFileReader`

这说明这个模块的思路是：

- 核心抽象负责管道
- 各具体子模块负责格式细节

## 真实示例位置

当前最直接的测试入口是：

- `geoair-geo/geoair-file-tran/geoair-file-core/src/test/java/cn/geoair/map/dynamic/file/core/tran/GeoFileTranImplTest.java`

另外还有演示型代码：

- `geoair-file-test/src/main/java/...`
  - `ShpToPg`
  - `PgToShp`
  - `GeoJsonToPg`
  - `PgToGeoJson`

## 核心 API 示例

### 示例1：核心转换实现

```java
GeoFileTran tran = new GeoFileTranImpl();
```

对应测试：`GeoFileTranImplTest`

### 示例2：GeoJSON Reader

```java
GeoJsonGeoFileReader reader = new GeoJsonGeoFileReader();
```

适用场景：读取 GeoJSON 文件或字符串中的空间数据。

### 示例3：Shapefile Reader

```java
ShpGeoFileReader reader = new ShpGeoFileReader();
```

适用场景：读取 Shapefile 数据源。

### 示例4：PostGIS Reader

```java
PostgisGeoFileReader reader = new PostgisGeoFileReader();
```

适用场景：从 PostGIS 表中读取空间数据，再接入转换链路。

## GitHub 源码入口

- 模块目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-file-tran`
- 核心抽象目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-file-tran/geoair-file-core/src/main/java/cn/geoair/map/dynamic/file/core`
- 演示目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-file-tran/geoair-file-test/src/main/java/cn/geoair/map/dynamic/file/test`

## 阅读建议

建议顺序：

1. `GeoFileTranImplTest`
2. `GeoFileTranImpl`
3. `GeoJsonGeoFileReader`
4. `ShpGeoFileReader`
5. `PostgisGeoFileReader`
6. `geoair-file-test` 里的演示类

这样会先理解管道，再理解具体格式实现。
