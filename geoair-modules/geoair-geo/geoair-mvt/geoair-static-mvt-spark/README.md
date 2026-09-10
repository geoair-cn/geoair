# geoair-static-mvt-spark

`geoair-static-mvt-spark` 是 GeoAir 的 Spark 离线矢量瓦片生成模块。它从数据库分批读取空间要素，按瓦片范围切分并编码为标准 Mapbox Vector Tile（MVT / PBF），再写入 PostgreSQL、目录、S3、MBTiles 或 PMTiles。

模块提供三个彼此独立的生成链路：

| 版本 | 入口 | 适用场景 |
| --- | --- | --- |
| V1 | `SparkVectorTileGenerator` | 历史单图层切片链路，保持既有行为 |
| V2 | `SparkVectorTileGeneratorV2` | 单图层切片；使用流式瓦片映射，降低内存压力 |
| V3 | `SparkVectorTileGeneratorV3` | 一个 PBF 内写入多个 MVT 内部图层；适合发布完整底图或专题图集 |

> V3 是新增实现，不修改 `TileSliceParameter`、V1 或 V2 的调用方式和输出行为。

## 能力概览

- 按 ID 分页或 BBox 空间分区读取数据库要素。
- 支持源数据坐标转换到输出瓦片网格。
- 支持按缩放级别将一个要素映射到其覆盖的全部瓦片。
- 支持属性字段筛选、几何简化、密度合并/过滤和单瓦片要素数量控制。
- 输出 gzip 压缩的标准 MVT PBF，可供 MapLibre、Mapbox GL JS、OpenLayers 等客户端读取。
- V3 支持不同数据源、不同 SQL 的多个内部图层汇总到一个 PBF。

## Maven 依赖

```xml
<dependency>
    <groupId>cn.geoair.devkit</groupId>
    <artifactId>geoair-static-mvt-spark</artifactId>
    <version>${geoair.version}</version>
</dependency>
```

该模块运行时需要 Spark。集群环境通常由 Spark 提供 `spark-core`、`spark-sql` 等依赖；本地运行时请保证依赖版本与 GeoAir 的依赖约束一致。

## 数据源配置

`DataSourceConfig` 支持逐项设置 JDBC 参数，也支持 GeoAir 的协议 URL：

```text
#jdbc:postgresql://用户名#密码/主机:端口/数据库/schema/表名
```

例如：

```java
DataSourceConfig input = DataSourceConfig.fromProtocolUrlStr(
        "#jdbc:postgresql://postgres#secret/10.0.0.1:5432/gis/public/road");
DataSourceConfig output = DataSourceConfig.fromProtocolUrlStr(
        "#jdbc:postgresql://postgres#secret/10.0.0.1:5432/gis/public/tile_cache");
```

不要把包含真实密码的协议 URL 写入 Git 仓库、日志或前端页面。生产环境建议由配置中心、环境变量或密钥管理服务注入。

## V2：单图层切片

V2 继续使用历史的 `TileSliceParameter`。下面的示例将道路数据切至 PostgreSQL 的 `tile_cache` 表：

```java
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.ReadStrategy;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.DataSourceConfig;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.TileSliceParameter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v2.SparkVectorTileGeneratorV2;
import org.apache.spark.sql.SparkSession;

SparkSession spark = SparkSession.builder()
        .appName("road-tile-v2")
        .master("local[*]")
        .getOrCreate();

TileSliceParameter parameter = new TileSliceParameter()
        .setLayerName("road")
        .setEdition("2026-09")
        .setInputSource(DataSourceConfig.fromProtocolUrlStr(
                "#jdbc:postgresql://postgres#secret/10.0.0.1:5432/gis/public/road"))
        .setOutputSource(DataSourceConfig.fromProtocolUrlStr(
                "#jdbc:postgresql://postgres#secret/10.0.0.1:5432/gis/public/tile_cache"))
        .setQueryStatement("select id, name, geom from public.road")
        .setIdFieldName("id")
        .setGeomFieldName("geom")
        .setSourceDataSrid(4326)
        .setOutGridSrid(3857)
        .setReadStrategy(ReadStrategy.ID_PAGE)
        .setMinZoom(6)
        .setMaxZoom(14)
        .setFeatureLimitEnabled(true)
        .setFeatureLimit(5000)
        .setDropDensestAsNeeded(true)
        .setCoalesceDensestAsNeeded(true);

new SparkVectorTileGeneratorV2(spark).doGenerate(parameter);
spark.stop();
```

## V3：一个 PBF 多个内部图层

V3 使用完全独立的参数模型：

- `MultiLayerTileSliceParameter`：一次瓦片构建任务。
- `MvtLayerSliceParameter`：PBF 内的一个 MVT layer。
- `tileSetName`：写入缓存表的 `layer_name`，它表示整个瓦片集合，而非 PBF 内部 layer。

下面的示例将道路和道路标注写到同一个 `tile_data` PBF 中：

```java
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.ReadStrategy;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.DataSourceConfig;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.MultiLayerTileSliceParameter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.MvtLayerGeometryMode;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.MvtLayerSliceParameter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.V3TileOutputConfig;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v3.SparkVectorTileGeneratorV3;
import org.apache.spark.sql.SparkSession;

DataSourceConfig input = DataSourceConfig.fromProtocolUrlStr(
        "#jdbc:postgresql://postgres#secret/10.0.0.1:5432/gis/public/road");
DataSourceConfig output = DataSourceConfig.fromProtocolUrlStr(
        "#jdbc:postgresql://postgres#secret/10.0.0.1:5432/gis/public/base_map_tile");

MvtLayerSliceParameter roads = new MvtLayerSliceParameter()
        .setLayerName("road")
        .setInputSource(input)
        .setQueryStatement("select id, name, road_class, geom from public.road")
        .setIdFieldName("id")
        .setGeomFieldName("geom")
        .setSourceDataSrid(4326)
        .setReadStrategy(ReadStrategy.ID_PAGE)
        .setMinZoom(6)
        .setMaxZoom(16)
        .setIncludeFields(java.util.Arrays.asList("id", "name", "road_class"))
        .setFeatureLimitEnabled(true)
        .setFeatureLimit(5000)
        .setPriority(100)
        .setGeometryMode(MvtLayerGeometryMode.ORIGINAL);

MvtLayerSliceParameter labels = new MvtLayerSliceParameter()
        .setLayerName("road_label")
        .setInputSource(input)
        .setQueryStatement("select id, name, geom from public.road where name is not null")
        .setIdFieldName("id")
        .setGeomFieldName("geom")
        .setSourceDataSrid(4326)
        .setMinZoom(10)
        .setMaxZoom(16)
        .setIncludeFields(java.util.Arrays.asList("id", "name"))
        .setPriority(80)
        .setGeometryMode(MvtLayerGeometryMode.CENTROID);

MultiLayerTileSliceParameter task = new MultiLayerTileSliceParameter()
        .setOutputConfig(V3TileOutputConfig.postgresql(output))
        .setTileSetName("base_map")
        .setEdition("2026-09")
        .setOutGridSrid(3857)
        .setMinZoom(6)
        .setMaxZoom(16)
        .setReducePartitionNum(3000)
        .setTileSizeLimitEnabled(true)
        .setTileSizeLimit("2MB")
        .setLayers(java.util.Arrays.asList(roads, labels));

SparkSession spark = SparkSession.builder()
        .appName("base-map-tile-v3")
        .master("local[*]")
        .getOrCreate();
try {
    new SparkVectorTileGeneratorV3(spark).doGenerate(task);
} finally {
    spark.stop();
}
```

### V3 输出介质

V3 的所有输出参数统一配置在 `V3TileOutputConfig`；不再使用顶层 `outputSource`。其中 PostgreSQL、目录和 S3 可直接写入；MBTiles、PMTiles 则先由 Spark 并行写入中间目录，再由 Driver 单进程归档，避免多个 executor 并发写单个文件。

```java
// 本地或共享目录：z/x/y.pbf
task.setOutputConfig(V3TileOutputConfig.localDirectory("/mnt/tiles/base-map"));

// S3 / MinIO
task.setOutputConfig(V3TileOutputConfig.s3("geoair-tiles", "base-map/v1")
        .setS3Endpoint("http://minio.example.com:9000")
        .setS3Region("us-east-1"));

// 标准 MBTiles：中间目录的 Y 默认是 XYZ，归档时自动转换为 MBTiles 所需 TMS
task.setOutputConfig(V3TileOutputConfig.mbtiles(
        "/mnt/staging/base-map", "/mnt/archive/base-map.mbtiles"));

// PMTiles V3：中间目录和最终 .pmtiles 都必须由 Driver 可访问
task.setOutputConfig(V3TileOutputConfig.pmtiles(
        "/mnt/staging/base-map", "/mnt/archive/base-map.pmtiles"));

// 默认 gzip；如对接端明确要求原始 MVT PBF，可在任务入口关闭。
task.setGzipPbf(false);
```

MBTiles、PMTiles 为保证主流客户端互操作性，仅支持 `outGridSrid = 3857`。在 Spark 集群模式，`localDirectory` 和归档的 `stagingDirectory` 必须是每个 executor 与 Driver 都可见的共享挂载路径。

### V3 的图层几何模式

| 枚举值 | 写入 PBF 的几何 | 典型用途 |
| --- | --- | --- |
| `ORIGINAL` | 原始点、线、面 | 道路、建筑、行政区、专题面 |
| `CENTROID` | 原始几何的质心点 | 名称标注、中心点展示 |
| `BOUNDARY` | 原始几何的边界 | 行政区边框、轮廓线 |

### V3 的限制策略

V3 依次在两个层面控制内存与输出大小：

1. 每内部图层的 `featureLimit`：业务级上限；开启后可使用空间密度合并和过滤。
2. 每内部图层的 `hardFeatureLimit`：默认 `8000` 的 Spark 聚合保护上限；用于防止热点瓦片 OOM。设为 `null` 或小于等于 `0` 可关闭，但生产环境不建议关闭。
3. 任务级 `tileSizeLimit`：限制最终 gzip PBF 的总大小。超限时，V3 从低 `priority` 图层开始逐步裁剪，并重新编码验证大小。

`priority` 越大，图层越优先保留。建议将道路、行政区等基础要素设为较高值，标注、辅助面或低价值专题设为较低值。

## PostgreSQL 输出表与查询语义

若输出表不存在，模块会自动创建包含以下字段的缓存表：

| 字段 | 说明 |
| --- | --- |
| `z`、`x`、`y` | XYZ 瓦片坐标 |
| `tms_y` | 同一瓦片的 TMS Y 坐标 |
| `grid_srid` | 输出瓦片网格 SRID |
| `tile_data` | gzip 压缩后的完整 MVT PBF |
| `layer_name` | V1/V2 为单图层名；V3 为 `tileSetName` |
| `edition` | 版本标识 |
| `insert_time` | 写入时间戳 |

V3 中，一个 `tile_data` 可以包含多个内部 layer。例如 `layer_name = base_map` 的一条记录，其 PBF 内部可能同时包含 `road`、`road_label`、`building`。

V3 重跑时会按照 `z + x + y + grid_srid + tileSetName + edition` 删除既有记录后写入新瓦片，因此同一版本不会无限累积重复数据。不同版本可通过 `edition` 并存。

## 调优建议

- 大范围明细数据不要从过低层级开始切。道路、建筑等通常应设置合适的 `minZoom`。
- 图层属性越少，PBF 越小；V3 建议明确设置 `includeFields`，而不是默认保留全部属性。
- 低层级热点瓦片优先配置 `featureLimit`，并保留默认 `hardFeatureLimit` 作为 OOM 保护。
- `reducePartitionNum` 应结合 Spark executor 数量、数据规模和热点分布调整；小任务可低于默认值，大范围任务需压测后确定。
- `tileSizeLimit` 推荐从 `2MB` 开始。设置过小会增加超限后的重复编码成本。
- V3 每个源图层独立读取；多图层共用一个数据库时，应评估数据库连接数与查询并发。

## 与 Tippecanoe 的定位

Tippecanoe 更适合将大规模 GeoJSON、CSV、FlatGeobuf 等文件离线编译为 MBTiles，并拥有成熟的低层级制图取舍策略。GeoAir V3 更适合直接从业务数据库、多个 SQL 或多个数据源构建瓦片，并可输出到数据库、目录、对象存储或离线归档。

如果目标是一次性生产独立离线底图文件，并且优先追求极致的低层级视觉质量，Tippecanoe 通常更合适；如果需要接入 GeoAir 的数据库、版本、服务和 Spark 集群体系，优先使用本模块。

## 验证

V3 已包含单元测试 `MultiLayerMvtEncoderV3Test`，覆盖：

- 一个 PBF 内编码两个不同的内部 MVT layer；
- 解压并按 MVT protobuf 解码后确认 layer 名与要素数量；
- 最终 PBF 大小限制和高优先级图层保留。

