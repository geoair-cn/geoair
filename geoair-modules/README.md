# GeoAir Modules — 业务组件库

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![JDK](https://img.shields.io/badge/JDK-8+-green.svg)](https://www.oracle.com/java/technologies/downloads/#java8)

## 📖 模块介绍

GeoAir Modules 是框架的**业务功能组件库**，基于 `geoair-standard` 标准库构建，提供 GIS 空间处理、动态数据源、API 文档、代码生成等开箱即用的功能模块。

## 🗂️ 完整目录结构

```
geoair-modules/
│
├── geoair-apidoc/                        ← API 文档自动生成
│   ├── geoair-knife4j-core/                 核心配置模块
│   ├── geoair-knife4j-springdoc-spring-boot-starter/  OpenAPI 3 Starter（推荐）
│   ├── geoair-knife4j-springfox-spring-boot-starter/  Swagger 2 Starter（兼容）
│   └── geoair-knife4j-spring-boot-demo/     示例项目
│
├── geoair-code-generator/                ← 代码生成器
│   ├── geoair-code-gen-module/             核心生成逻辑
│   └── geoair-code-gen-demo/               示例项目
│
├── geoair-geo/                           ← 🌍 GIS 地理空间处理（最大模块组）
│   ├── geoair-geo-tools/                    GIS 工具集（坐标转换/格式/测量/合并/瓦片/SRID）
│   ├── geoair-adv-query/                    高级空间查询（PG/MySQL/Oracle 多方言）
│   ├── geoair-file-tran/                    空间文件互转
│   │   ├── geoair-file-core/                  核心抽象（Reader/Writer/Tran 管道）
│   │   ├── geoair-file-geojson/               GeoJSON 格式
│   │   ├── geoair-file-postgis/               PostGIS 格式
│   │   ├── geoair-file-shp/                   Shapefile 格式
│   │   └── geoair-file-test/                  集成测试
│   ├── geoair-mvt/                           Mapbox 矢量瓦片
│   │   ├── geoair-mvt-tools/                   工具库（密度优化/PBF 编码/管道构建）
│   │   ├── geoair-real-mvt/                    实时矢量瓦片服务
│   │   └── geoair-static-mvt-spark/            Spark 离线矢量瓦片生成
│   ├── geoair-map-tile-forge/               瓦片服务（多格式/本地+S3/压缩部署）
│   ├── geoair-map-tile-fuser/               瓦片融合（多源拼接/缓存预热/MBTiles）
│   ├── geoair-geoserver/                    GeoServer 嵌入式集成
│   ├── geoair-by-gwc/                       ArcGIS Compact Cache 直读
│   ├── geoair-jts-all/                      JTS 全量打包
│   └── geoair-geo-demo/                     GIS 功能演示
│
├── geoair-dynamic-ds/                    ← 动态多数据源
│
├── geoair-db-service/                    ← 数据库 Web 管理
│   ├── geoair-db-service-core/              核心服务层
│   ├── geoair-db-service-spring-boot-starter/ Auto-Configuration Starter
│   └── geoair-db-service-webview/           Vue 2 管理界面
│
├── geoair-message-jts-jackson/           ← JTS Geometry ↔ Jackson JSON
│
└── geoair-message-jts-mybatis/           ← JTS Geometry ↔ MyBatis 类型映射
```

## 🧩 模块详解

### 1. geoair-apidoc — API 文档自动生成

基于 Knife4j 实现，支持双版本规范：

| Starter | API 规范 | Knife4j UI | 适用场景 |
|---------|---------|------------|----------|
| `geoair-knife4j-springdoc-spring-boot-starter` | OpenAPI 3.0 | 4.x | **推荐**，新项目使用 |
| `geoair-knife4j-springfox-spring-boot-starter` | Swagger 2.0 | 3.x | 兼容旧项目 |

**核心特性:**
- 自动扫描 Controller 包路径，按包路径分组
- 支持文档导出（PDF / Markdown）
- Spring Boot Auto-Configuration，零配置启动
- 自定义响应模型转换、操作行为定制

```xml
<!-- 推荐使用 SpringDoc 版本 -->
<dependency>
    <groupId>cn.geoair.devkit</groupId>
    <artifactId>geoair-knife4j-springdoc-spring-boot-starter</artifactId>
    <version>J8.1.6</version>
</dependency>
```

访问地址: `http://localhost:8080/doc.html`

---

### 2. geoair-code-generator — 代码生成器

从数据库表结构自动生成代码：

| 生成目标 | 说明 |
|---------|------|
| **Entity** | Java 实体类，含 JPA/MyBatis 注解 |
| **Mapper** | 数据访问层接口 + XML |
| **Service** | 业务逻辑层 |
| **Controller** | RESTful 控制器 |
| **Vue Component** | 前端 Vue 组件（列表/表单） |

支持自定义模板，适配不同项目规范。

---

### 3. geoair-geo — GIS 地理空间处理 🌍

框架最核心的业务模块，提供完整的地理空间数据处理能力。

#### 3.1 geoair-geo-tools — GIS 工具集

**统一入口:** `GirGeoTools` 单例门面类，提供对所有工具的统一访问：

```java
GirGeoTools tools = GirGeoTools.defaultInstance();
tools.getCoordinateOpt();  // 坐标转换
tools.getFormatOpt();      // 格式转换
tools.getMeasureOpt();     // 空间测量
tools.getMergeOpt();       // 几何合并
tools.getSridOpt();        // 坐标系转换
tools.getTileGrid4326Opt(); // WGS84 瓦片
tools.getTileGrid3857Opt(); // Web 墨卡托瓦片
```

| 工具接口 | 核心能力 |
|---------|---------|
| `GirCoordinateConvertOpt` | WGS84 ↔ GCJ02 ↔ BD09；墨卡托投影；DMS ↔ DD |
| `GirGeoFormatOpt` | GeoJSON/WKT/WKB/JTS/PGGeometry 全互转；WKTReader/WKBReader/GeometryJSON 工厂 |
| `GirGeoMeasureOpt` | 面积（m²/km²/亩/公顷）、长度、点到点/线/面距离、UTM 自动投影 |
| `GirGeoMergeOpt` | 多几何合并为 Multi*/单几何；拓扑合并（重叠面融合） |
| `GirSridConvertOpt` | 任意坐标系间 Geometry 重投影（基于 GeoTools） |
| `GirTileConverterOpt` | XYZ/TMS/WMTS 瓦片 ↔ 经纬度/墨卡托；4326 等轴/分轴方案 |
| `GirBingMapQuadKeyOpt` | Bing 地图 QuadKey 生成与解析 |
| `GirGeom2ArrayOpt` | JTS Geometry ↔ 坐标数组 |

#### 3.2 geoair-adv-query — 高级空间查询

多数据库方言的空间 SQL 执行器，接口聚合了 CRUD + DDL + 空间操作 + 分页等 7 种能力：

```
IAdvExecutor 聚合接口
├── IDataSourceGetter        数据源连接管理
├── IDsTransactionTemplate   事务支持
├── IAdvBaseOpt              CRUD 操作
├── IAdvDDLOpt               DDL（表/索引/模式）
├── IAdvGeoOpt               空间几何操作（Intersects/BBox/距离/质心/修复）
├── IAdvGeoPreOpt            空间查询执行
├── IAdvWhereSelectOpt       Fluent Lambda 条件构造器
└── IAdvSimplePageOpt        分页
```

支持数据库方言: **PostgreSQL+PostGIS** / **MySQL** / **Oracle Spatial**

**内置动态 SQL 引擎:** 支持 `<if>`, `<where>`, `<set>`, `<foreach>`, `<trim>` XML 标签，OGNL 表达式求值，令牌参数替换。

**Fluent 查询构造器:**
```java
executor.wSelectList(User.class, builder -> builder
    .select("name", "geom")
    .where(w -> w.eq(User::getAge, 18).gt(User::getScore, 90))
    .orderBy(o -> o.asc(User::getId)));
```

#### 3.3 geoair-file-tran — 空间文件互转

**管道架构:** Reader → Transformer → Writer

```
GeoFileReader  →  GeoFileTran  →  GeoFileWriter
     ↑              ↑  ↑              ↑
 GeoJSON       进度监听  异常处理    GeoJSON
 Shapefile       消费者回调         Shapefile
 PostGIS                            PostGIS
 GeoPackage                         GeoPackage
 CSV/FlatGeobuf
```

支持格式矩阵:

| 源 \ 目标 | GeoJSON | Shapefile | PostGIS | GeoPackage | CSV | FlatGeobuf |
|-----------|---------|-----------|---------|------------|-----|------------|
| GeoJSON | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Shapefile | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| PostGIS | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |

#### 3.4 geoair-mvt — 矢量瓦片

- **geoair-mvt-tools**: MVT 基础工具库（密度优化/简化/PBF 编码/管道构建）
- **geoair-real-mvt**: 实时矢量瓦片 HTTP 服务，基于 PostGIS `ST_Intersects` 动态查询
- **geoair-static-mvt-spark**: Apache Spark 离线批量生成，支持 ID 分页和 BBox 分区策略

#### 3.5 geoair-map-tile-forge — 瓦片服务

多格式栅格瓦片统一服务层：
- **存储格式**: XYZ / ArcGIS Compact V1 / Compact V2 / 3D Terrain / Cesium 3D Tiles
- **存储后端**: Local File / AWS S3
- **压缩方式**: ZIP / Gzip / Bzip2 / TarGz / TarBzip2 / 无压缩
- **缓存层**: 内存 + S3 + 装饰器模式

#### 3.6 geoair-map-tile-fuser — 瓦片融合

多源瓦片拼接为连续栅格图像：
- 自适应分辨率选择（从金字塔层级中选择最佳匹配）
- 并行瓦片获取 + 边缘裁剪
- 多数据源（本地文件/网络/MBTiles）
- 缓存预热 + 完整性校验

#### 3.7 geoair-geoserver — GeoServer 集成

以嵌入式方式在应用内运行 GeoServer，无需独立 Servlet 容器：
- 编程式数据源/工作区/图层发布
- WMS/WFS 服务自动配置
- PostGIS 图层自动发布

#### 3.8 geoair-by-gwc — ArcGIS 缓存直读

直接读取 ArcGIS Compact Cache 格式（V1 10.3 之前 / V2 10.3+），无需依赖 ArcGIS Server：
- `.bundlx` 索引文件解析（LRU 缓存加速）
- `.bundle` 切片数据按偏移量读取
- WMTS 能力文档自动生成

---

### 4. geoair-dynamic-ds — 动态多数据源

运行时数据源动态切换，支持 AOP + 注解驱动：

```java
@EnableDynamicDs  // 启用数据源动态切换
@SpringBootApplication
public class Application { ... }
```

**核心架构:**
```
DynamicDataSourceManager          ← 数据源注册中心
       ↓
AdvDynamicDataSourceStorage      ← 运行时数据源池
       ↓
GirDynamicDataSourceAspect       ← AOP 切面路由
       ↓
GirDynamicStackDataSource        ← 栈式数据源（支持嵌套切换）
       ↓
DataSourceWrapper                ← 连接池包装（Druid/Hikari/BoneCP/C3P0/DBCP2）
```

**读写分离:** SQL 解析器自动识别 SELECT → 读库，INSERT/UPDATE/DELETE → 写库

**Web 上下文切换:** 通过请求参数或 Header 指定目标数据源 ID

**事务管理:** `GirDsTransactionManager` + `GirDsTransactionTemplate` 编程式事务，支持传播行为和隔离级别

**连接池支持:** Druid / HikariCP / BoneCP / C3P0 / DBCP2，统一的 `AdvDataSourceWrapper` 包装

---

### 5. geoair-db-service — 数据库 Web 管理

提供 Web 可视化的数据库管理界面：

| 组件 | 技术 | 说明 |
|------|------|------|
| 后端核心 | Java + Spring Boot | 统一数据库访问抽象层 |
| 自动配置 | Spring Boot Starter | 零配置集成 |
| 前端 UI | Vue 2 + Element UI + ECharts | SQL 编辑器、数据浏览、表结构管理 |

---

### 6. geoair-message-jts-jackson — JTS ↔ Jackson

JTS `Geometry` 对象的 JSON 序列化/反序列化器，支持 GeoJSON 格式：
- `GeometrySerializer` / `GeometryDeserializer` 自动注册
- 支持所有 JTS Geometry 类型（Point/LineString/Polygon/Multi*/GeometryCollection）

```xml
<dependency>
    <groupId>cn.geoair.devkit</groupId>
    <artifactId>geoair-message-jts-jackson</artifactId>
</dependency>
```

---

### 7. geoair-message-jts-mybatis — JTS ↔ MyBatis

JTS `Geometry` 对象的 MyBatis TypeHandler：
- 自动将数据库空间字段映射为 JTS Geometry 对象
- 支持 PostGIS、Oracle Spatial 等空间数据库类型

---

## 🚀 快速开始

### Maven 依赖

```xml
<!-- 引入全部模块 BOM -->
<dependency>
    <groupId>cn.geoair.devkit</groupId>
    <artifactId>geoair-modules</artifactId>
    <version>J8.1.6</version>
    <type>pom</type>
</dependency>

<!-- 或按需引入 -->
<dependency>
    <groupId>cn.geoair.devkit</groupId>
    <artifactId>geoair-geo-tools</artifactId>
    <version>J8.1.6</version>
</dependency>
```

### GIS 工具使用

```java
// 获取工具实例
GirGeoTools tools = GirGeoTools.defaultInstance();

// 坐标转换
double[] gcj02 = tools.getCoordinateOpt().wgs84ToGcj02(116.40, 39.90);

// GeoJSON 转 WKT
String wkt = tools.getFormatOpt().geojsonToWktString(geojsonStr, false);

// 面积计算：单位与测量方式均使用枚举
double area = tools.getMeasureOpt().calculateArea(
    polygon, 4326, MeasureUnitEnum.SQUARE_KILOMETER, MeasureMethodEnum.UTM);

// 瓦片坐标
TileZxyApo tile = tools.getTileGrid4326Opt().wgs84ToTileZxy(116.40, 39.90, 10);
```

## 📐 依赖关系

- `geoair-modules` → `geoair-base-parent`（依赖管理）
- `geoair-geo-*` → `geoair-geo-tools`（GIS 工具基础）
- `geoair-adv-query` → `geoair-dynamic-ds`（需要数据源管理）
- `geoair-db-service` → `geoair-base` + `geoair-core`（依赖标准库）
- 各模块间相互独立，可按需单独引入

## 👥 开发者

- **作者**: 张逢吉
- **邮箱**: zfj20250104@qq.com
- **组织**: GeoAir
- **官网**: https://xmt.geoair.cn/

## 📄 许可证

Apache License 2.0 — 详见 [LICENSE](LICENSE)
