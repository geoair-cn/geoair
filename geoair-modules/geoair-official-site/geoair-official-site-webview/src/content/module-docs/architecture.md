## 我希望 GeoAir 解决什么问题

我不希望每个 GIS 项目从连接池、空间 SQL、坐标转换、瓦片服务和前端数据源管理开始重复搭建。GeoAir 的目标是提供一套可按需选择的组件：基础能力稳定、工程版本可控，而地图、数据服务与瓦片等能力又能独立接入。

因此，这个工程不是一个“大而全的 starter”，而是分层的组件库。

```
工程与版本治理
  geoair-dependencies-bom → geoair-base-parent → geoair-framework-bom

标准能力层
  geoair-base / geoair-core / geoair-web / geoair-orm / geoair-sdk / geoair-tools

业务与数据层
  JDBC URL / 动态数据源 / 数据库服务 / API 文档 / 代码生成

GIS 能力层
  Geometry 与坐标 / 空间查询 / 文件转换 / MVT / 栅格瓦片 / GeoServer
```

## 从哪里开始选

### 普通 Spring Boot 业务服务

从 `geoair-project-parent` 开始，再按业务能力引入模块。例如，一个既要动态切库、又要提供空间查询的服务通常选择：

```xml
<dependency>
  <groupId>cn.geoair.devkit</groupId>
  <artifactId>geoair-dynamic-ds</artifactId>
</dependency>
<dependency>
  <groupId>cn.geoair.devkit</groupId>
  <artifactId>geoair-adv-query</artifactId>
</dependency>
```

连接串解析会由 `geoair-jdbc-url` 统一处理；不要在业务代码中再写 URL 拆分工具。

### 以空间数据处理为主的服务

- 坐标、Geometry、空间格式与瓦片数学：从 `geoair-geo-tools` 开始。
- 需要多方言 CRUD、DDL 与空间查询：增加 `geoair-adv-query`。
- 需要导入导出 GeoJSON、Shapefile、PostGIS：增加 `geoair-file-tran`。
- 需要在线矢量瓦片：选择 `geoair-real-mvt`；离线批量生产则选择 `geoair-static-mvt-spark`。

### 已有瓦片或多源地图服务

- 已有本地、ZIP、S3、ArcGIS Compact 或 3D 瓦片：选择 `geoair-map-tile-forge` 统一读取和服务输出。
- 需要把来源不同、网格不同的栅格瓦片转换或融合：选择 `geoair-map-tile-fuser`。

两者都以 `TileResponse` 为核心输出契约：业务逻辑可以直接传入 URI 得到结果，Servlet 只负责最后写回 HTTP。这让预热任务、网关转发和 Web 请求不再维护三套瓦片逻辑。

## 我刻意保留的边界

- **依赖版本**只在 BOM 与基础父 POM 中治理，业务模块不重复声明版本。
- **接口与实现**尽量分离：标准层定义通用抽象，`core` 或业务模块提供默认实现与扩展点。
- **Web 适配**不侵入核心逻辑：例如瓦片模块先返回 `TileResponse`，再由 servlet 输出。
- **兼容迁移**优先于硬切换：旧 API 保留并标记为 `@Deprecated`，新 API 提供更清晰的对象模型和调用方式。

## 阅读与接入顺序

1. 先选项目父 POM，确认工程与版本治理入口。
2. 再按“数据源、业务、GIS、瓦片”选择真正需要的模块。
3. 阅读每个模块页面里的最小示例，再进入对应源码和测试。
4. 如果需要替换默认实现，优先寻找模块暴露的 SPI、Helper 或 Provider 扩展点，不要复制默认实现的内部逻辑。

这样使用时，GeoAir 是一组可以组合的积木；维护时，依赖、运行时逻辑和 Web 输出也各有清晰边界。
