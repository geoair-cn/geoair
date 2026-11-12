# Map Tile Forge Core

Map Tile Forge Core 是一个用于处理地图瓦片（Map Tiles）的核心库，支持多种瓦片存储格式与协议，包括但不限于 ArcGIS 缓存格式、WMTS 协议以及常见的 XYZ 瓦片服务。

## 功能特性

- **多格式支持**：支持本地文件系统和 S3 存储中的多种瓦片格式，包括 ArcGIS Compact Cache V1/V2、XYZ 瓦片、3D Terrain 等。
- **缓存机制**：内置多种缓存实现，例如 NoOp 缓存和 S3 缓存，并支持自定义缓存扩展。
- **瓦片服务**：提供 Servlet 支持，可快速部署 XYZ 和 D3 Tiles 等瓦片服务。
- **配置灵活**：支持通过配置文件管理不同类型的瓦片数据源及存储路径。
- **压缩解压支持**：集成 ZIP/GZIP/TAR.GZ 等常见压缩格式的处理逻辑，方便读取压缩包内的瓦片数据。
- **坐标系适配**：具备完整的地理坐标系统支持，能够自动识别并转换不同的空间参考系统(SRS)。

## 模块说明

### 核心模块 (`bygwc`)
基于 GeoWebCache 扩展的地图瓦片处理核心组件，主要功能包括：
- 解析 ArcGIS 缓存元信息（如 [CacheInfo](file://H:\gitee\map-tile-forge\map-tile-forge-core\src\main\java\cn\geoair\map\tile\forge\core\bygwc\config\CacheInfo.java#L57-L113), [TileCacheInfo](file://H:\gitee\map-tile-forge\map-tile-forge-core\src\main\java\cn\geoair\map\tile\forge\core\bygwc\config\TileCacheInfo.java#L38-L156)）
- 构建网格集（GridSet），用于组织和管理瓦片层级结构
- 提供 WMTS 服务生成器（[GetCapabilitiesGenerator](file://H:\gitee\map-tile-forge\map-tile-forge-core\src\main\java\cn\geoair\map\tile\forge\core\bygwc\wmts\GetCapabilitiesGenerator.java#L29-L396)）

### 缓存模块 ([cache](file://H:\gitee\map-tile-forge\map-tile-forge-boot\src\main\resources\static\lib\cesium\Cesium\ThirdParty\Workers\draco_decoder.js#L17-L17))
实现了统一的瓦片缓存接口 [TileCache](file://H:\gitee\map-tile-forge\map-tile-forge-core\src\main\java\cn\geoair\map\tile\forge\core\cache\TileCache.java#L9-L82)，当前支持以下几种缓存策略：
- [TileNoOpCache](file://H:\gitee\map-tile-forge\map-tile-forge-core\src\main\java\cn\geoair\map\tile\forge\core\cache\impl\TileNoOpCache.java#L8-L23): 不使用缓存
- [TileS3Cache](file://H:\gitee\map-tile-forge\map-tile-forge-core\src\main\java\cn\geoair\map\tile\forge\core\cache\impl\TileS3Cache.java#L9-L23): 使用 Amazon S3 作为后端存储

同时提供了缓存注册表 [TileCacheRegistry](file://H:\gitee\map-tile-forge\map-tile-forge-core\src\main\java\cn\geoair\map\tile\forge\core\cache\TileCacheRegistry.java#L12-L58) 来管理和获取具体的缓存实例。

### 数据访问层 (`xyz/storage`)
抽象了对不同类型存储介质（本地磁盘/S3）上 XYZ 瓦片数据的操作接口 [TileStorageAccessor](file://H:\gitee\map-tile-forge\map-tile-forge-core\src\main\java\cn\geoair\map\tile\forge\core\xyz\storage\TileStorageAccessor.java#L10-L34)，具体实现在对应子类中完成。

### 压缩处理模块 (`zip`)
负责处理各种压缩格式的数据流，为从 ZIP 或其他归档文件中提取瓦片资源提供了基础支撑。关键组件有：
- 各类压缩处理器（如 [ZipHandler](file://H:\gitee\map-tile-forge\map-tile-forge-core\src\main\java\cn\geoair\map\tile\forge\core\zip\decompression\ZipHandler.java#L10-L32), [GzipHandler](file://H:\gitee\map-tile-forge\map-tile-forge-core\src\main\java\cn\geoair\map\tile\forge\core\zip\decompression\GzipHandler.java#L9-L23)）
- 中央目录入口解析器 [CentralDirectoryEntry](file://H:\gitee\map-tile-forge\map-tile-forge-core\src\main\java\cn\geoair\map\tile\forge\core\zip\model\CentralDirectoryEntry.java#L7-L43)
- 针对特定数据库（SQLite/PostgreSQL）的图层文件持久化 DAO 实现

### Web 层 (`servlet`)
封装了一系列标准的 HTTP Servlet，用以对外暴露瓦片服务能力，主要包括：
- [XYZServlet](file://H:\gitee\map-tile-forge\map-tile-forge-core\src\main\java\cn\geoair\map\tile\forge\core\servlet\XYZServlet.java#L21-L115): 提供符合 XYZ 规范的地图瓦片服务
- [D3TilesServlet](file://H:\gitee\map-tile-forge\map-tile-forge-core\src\main\java\cn\geoair\map\tile\forge\core\servlet\D3TilesServlet.java#L23-L93): 面向三维地形数据的服务接口
- [HeaderFilter](file://H:\gitee\map-tile-forge\map-tile-forge-core\src\main\java\cn\geoair\map\tile\forge\core\servlet\HeaderFilter.java#L8-L46): 对请求头进行预处理的安全过滤器

### 工具类与其他辅助模块
- `utils`: 包含实用工具类，如 [TilePathParser](file://H:\gitee\map-tile-forge\map-tile-forge-core\src\main\java\cn\geoair\map\tile\forge\core\utils\TilePathParser.java#L8-L383) 用于解析瓦片路径参数
- `enums`: 定义通用枚举类型，如 [StorageType](file://H:\gitee\map-tile-forge\map-tile-forge-core\src\main\java\cn\geoair\map\tile\forge\core\enums\StorageType.java#L3-L21), [CompressionType](file://H:\gitee\map-tile-forge\map-tile-forge-core\src\main\java\cn\geoair\map\tile\forge\core\enums\CompressionType.java#L8-L51) 等
- `vo`: Value Object 封装请求对象，简化接口调用流程

## 快速开始

1. 添加依赖至您的 Maven 项目:
```
xml
<dependency>
<groupId>cn.geoair.map.tile.forge</groupId>
<artifactId>map-tile-forge-core</artifactId>
<version>{latest-version}</version>
</dependency>
```
2. 初始化相关 Bean 并注入所需服务类即可开始使用各项功能。

## 贡献指南

欢迎提交 Issue 或 Pull Request 参与共建！在贡献代码前请先阅读 [CONTRIBUTING.md](CONTRIBUTING.md) 获取更多细节。

## 许可证

本项目采用 LGPLv3 开源许可证发布，请查看 [LICENSE](LICENSE) 文件了解详细条款。

## ZIP 索引构建与完成语义（2026-11-07）

`ZipArchiveIndexBuilder` 统一 XYZ、Compact 和三维等 ZIP 适配器的中央目录索引构建。继续使用现有分块读取和有界队列，消费端同步提交批次；`getLayerPerCacheBatchSize` 决定每批条数，范围为 1 至 100000。每批使用一个 SQLite 事务，最后一批落盘后才更新 CACHED 状态并发布最终进度。回调中的完成数量表示已提交批次覆盖的归档条目数，包含目录和被格式过滤的条目。

构建异常向调用方抛出并保留失败状态。重试先清理未完成的派生索引，再重新扫描源 ZIP。同一 JVM 内对同一索引的重复提交会串行复核，完成后复用结果；业务任务 ID、任务表及跨进程执行权仍由接入方管理。源 ZIP 以只读方式访问，S3 源继续通过范围读取处理。

每图层仍使用 `sql_lite_meta/<标识>_tiles.sqlite`，表结构和已发布版本保持兼容。默认 NoOpCacheProvider 完成时仅保存本地状态，避免把整份 SQLite 索引读入内存。新增 FileCacheProvider 扩展用于流式备份和恢复。S3CacheProvider 已接入该扩展，通过 SDK 按大小选择分片上传；上传前确认 WAL 已归并，恢复完成后再移动到目标索引路径。第三方旧 CacheProvider 仍兼容原有字节数组接口，处理大型索引时应实现文件扩展。

迁移测试覆盖真实 ZIP 4501 条、批次失败回滚、重试去重、同归档并发构建、3D Tiles/S3M 元数据与关联文件读取、WAL 归并后的流式备份及恢复；保留 ZIP64、异常归档及扫描流水线回归。测试使用临时数据和本地资源，不连接客户 S3 或生产数据。

## ZIP 按需读取补强（2026-11-12，开发源码）

这次增强现有 ZIP 读取入口，不要求先执行预缓存。原生本地/S3 ZIP 适配器共用 `ZipResourceReader`：先复用当前源版本已提取的文件，再查已完成且版本匹配的 SQLite 索引，未命中或索引不可用时直接定位 ZIP 中央目录并提取所请求的资源。不会在请求线程自动建立完整索引，也不会为了读取 S3 ZIP 先下载整包。现有格式范围不变：本地 ZIP 八种，S3 ZIP 七种（无 LOOSE）；Compact V1/V2 继续使用原有 bundle 解析器，V1 会补齐 bundle 和 bundlx。

`ZipRootResolver` 统一识别数据根。支持零级/零行/零列 XYZ、嵌套数据目录、旧名称的三维根元数据，以及 `tileset.json` 位于配置目录前缀上一级的布局。多个同级数据根无法可靠区分时要求明确指定 `tilePathPrefix`。根文件对外使用标准名称，关联资源保留相对于根文件的路径；不会通过改写客户元数据来拼凑地址。ArcGIS 配置文件从当前瓦片目录及其祖先目录中选择。

源版本由配置身份和文件元信息共同确定。本地使用大小、修改时间和文件标识，S3 使用桶、大小、版本号、ETag 和修改时间；读取前后复核，变化时拒绝发布本次提取结果。派生文件保存到 `geoair.file.temp.temp-path/zip_resources/<身份哈希>/`，旧版本目录保留为可清理缓存，不自动删除源文件。原来的已提取目录不会被当成新版本文件复用。

SQLite 索引新增自动创建的 `archive_source` 表，保存构建时的源版本；完成构建时建立文件名和 XYZ 查询索引。新请求通过只读连接查询已发布记录，不建表、不触发索引下载。旧索引没有版本标记时安全回退到源 ZIP；下一次显式预缓存会自动重建，无需手工执行 SQL。`LayerPerFileDao` 的新方法提供默认实现，自定义 DAO 不实现时仍能读源 ZIP，但需要实现版本读写及 `findReadyByFileName` 才能使用新索引加速。

同一资源的提取请求共用现有分段锁，先写临时文件，再发布完整文件；失败清理临时文件并允许重试。源文件版本、根目录和已定位条目的缓存均有容量边界，未找到的资源仅短期缓存。继续复用已有 `CentralDirectoryReader`/`ZipDirectoryPipeline`，扫描缓冲大小可配置；不会复制另一套扫描器或为每个目录条目打开文件头。无索引时的首次定位仍可能扫描较大中央目录，完整索引仍是持续大量访问的推荐加速方式。

Spring Boot 启动配置示例（普通 Java 应在创建适配器前配置 `ZipReadOptions`）：

```yaml
geoair:
  zip:
    read:
      directory-block-bytes: 5242880 # 每次读取中央目录的缓冲，4KB 至 16MB
      root-cache-entries: 256
      entry-cache-entries: 4096
      missing-cache-seconds: 5       # 不存在资源及不可用索引的短期缓存；0 表示不保留
      lock-timeout-millis: 60000     # 等待同一资源提取锁的上限，不是 S3 网络超时
```

这些参数在启动前配置，不提供缓存容量的运行时热切换。自定义 `ICompressionHandler` 应覆盖 `getSourceVersion` 返回可靠版本标识；默认方法只按大小识别，用于兼容旧实现，不能识别同长度的内容替换。

不存在的普通瓦片仍返回 `exists=false`。损坏归档、非法路径、无法识别数据根、源文件变化、读取/提取失败、等待超时及中断通过 `ZipReadException.getFailure()` 区分，不再统一吞掉后变成“瓦片不存在”。索引故障在回退时记录一次短期受控日志。

测试覆盖15种格式组合建索引前后读取、中文/空格/加号名称、元数据目录层级、并发首次提取、半文件清理、重试、源版本变化、旧索引兼容、只读查询、查询索引、中断和等待超时。`ZipLargeOffsetTest` 通过稀疏模拟验证 800GB 偏移，不创建800GB文件。真实样本验收通过 `-Dgeoair.forge.test.archive=<ZIP绝对路径>` 显式开启，源 ZIP 只读，所有索引和提取文件写入 JUnit 临时目录；未指定时仅跳过该样本测试。

本次只完善开发源码及测试，不替换 TileFuser 当前引用的发布依赖，不执行 install/deploy。
