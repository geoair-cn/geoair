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
