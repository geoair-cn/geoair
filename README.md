# GeoAir Framework

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![JDK](https://img.shields.io/badge/JDK-8+-green.svg)](https://www.oracle.com/java/technologies/downloads/#java8)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.18-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![GeoTools](https://img.shields.io/badge/GeoTools-28.6.1-orange.svg)](https://geotools.org/)

## 📖 项目简介

GeoAir Framework 是一个**面向企业级 GIS（地理信息系统）开发的 Java 基础框架**，采用 SPI（Service Provider Interface）解耦架构，提供从底层工具库、空间数据处理到 API 文档生成的全套解决方案。

框架集成了 GeoTools、JTS、PostGIS 等主流 GIS 库，支持动态多数据源切换、高级空间查询、多坐标系转换等核心能力，帮助开发者快速构建地理信息应用。

## 🏗️ 整体架构

```
geoair-framework (根 POM, J8.1.5)
│
├── geoair-dependencies-bom/         ← 依赖版本管理中心（5 个领域 BOM）
│   ├── geoair-geotools-dependencies/     GeoTools 28.6.1 + JTS 1.19.0
│   ├── geoair-spring-dependencies/       Spring Boot 2.7.18 + Cloud + Alibaba
│   ├── geoair-openapi-dependencies/      Knife4j + SpringDoc + Springfox
│   ├── geoair-common-dependencies/       第三方基础库
│   └── geoair-template-dependencies/     预留模块
│
├── geoair-base-parent/              ← 核心枢纽：聚合所有 BOM + 统一构建配置
│
├── geoair-framework-bom/            ← 工程级父 POM（控制不同项目类型）
│   ├── geoair-api-parent/                API 层项目
│   ├── geoair-project-parent/            普通业务项目
│   └── geoair-spring-boot-starter-parent/ Spring Boot Starter 项目
│
├── geoair-standard/                 ← 标准基础库（SPI 接口 + 实现）
│   ├── geoair-base/                     接口/抽象层（Gi* 接口）
│   ├── geoair-core/                     SPI 实现层（Spring 适配）
│   ├── geoair-web/                      Web 公共组件（会话/权限/日志/MIME）
│   ├── geoair-orm/                      ORM 抽象（5 种实现）
│   ├── geoair-sdk/                      SDK 统一输出
│   └── geoair-tools/                    通用工具集
│
└── geoair-modules/                  ← 业务功能组件
    ├── geoair-apidoc/                   API 文档（Knife4j）
    ├── geoair-code-generator/           代码生成器
    ├── geoair-geo/                      🌍 GIS 空间处理（核心）
    ├── geoair-dynamic-ds/               动态多数据源
    ├── geoair-db-service/               数据库 Web 管理
    ├── geoair-message-jts-jackson/      JTS ↔ Jackson 序列化
    └── geoair-message-jts-mybatis/      JTS ↔ MyBatis 类型映射
```

### 依赖继承链

```
geoair-framework
  └→ geoair-base-parent（导入 5 个 BOM）
       ├→ geoair-standard
       │    ├→ geoair-base（纯接口，无外部依赖）
       │    ├→ geoair-core（实现层，依赖 Spring）
       │    └→ geoair-web / geoair-orm / ...
       └→ geoair-modules
            ├→ geoair-geo（依赖 GeoTools）
            ├→ geoair-dynamic-ds（依赖 Druid）
            └→ ...
```

## 📐 命名规范

框架遵循严格的命名前缀约定，通过前缀即可判断类的职责：

| 前缀 | 含义 | 示例 | 说明 |
|------|------|------|------|
| `Ga*` | **An**notation 注解 | `@GaApi`, `@GaApiAction` | 框架级自定义注解 |
| `Gi*` | **I**nterface 接口 | `GiCache`, `GiDao`, `GiResult` | 业务抽象接口 |
| `Gir*` | **I**mplementation + **R**ealization 实现类 | `GirResult`, `GirSpHelper`, `GirJSON` | 接口的实现/工具类 |
| `Gutil*` | **Util**ity 工具类 | `GutilAop`, `GutilCookie` | 纯静态工具方法 |
| `Gfun*` | **Fun**ction 函数接口 | `GfunParamPageExcute` | 函数式接口（`@FunctionalInterface`） |
| `Gk*` | **K**it 内建工具 | `GkPair`, `GkSpLoader`, `GkConsole` | 内建数据结构/工具 |
| `Gem*` | **E**num **M**odel 枚举模型 | `GemBoolean`, `GemDatePattern` | 枚举型数据模型 |
| `Gi*able` | 特征接口 | `GiModelable`, `GiEntityable` | 标记某个对象具有某种特征 |

### 设计模式

- **Facade 门面** — `Gir` 抽象类聚合 `property`、`env`、`log`、`beans` 等静态字段，提供统一入口
- **SPI 插件** — `@GkSP` 注解 + `GirSpHelper.load()` 动态加载实现类，支持 JDK SPI 和 Spring Bean 两种发现机制
- **Helper + Provider** — `GirBeanHelper` / `GirCacheHelper` 等 Helper 类通过 Provider 委托到具体实现
- **策略模式** — JSON（Jackson/FastJSON/Gson/Hutool）、日志（SLF4J/CommonsLog/HutoolLog）、缓存等均通过统一接口切换实现

## 📦 版本历史

| 版本号 | 发布日期 | 核心变更 |
|--------|----------|----------|
| `23.1.0` | 2023-08-01 | 初始版本：标准库、GIS 空间处理、动态数据源、瓦片工具 |
| `23.1.2-M2` | 2026-03-13 | Bug 修复、性能优化、新功能模块 |
| `J8.1.0-RC2` | 2026-03-15 | 统一 Group ID 为 `cn.geoair.devkit`；新版本号体系 `J8.x.x`；兼容 JDK 11+ |
| `J8.1.5` | 当前开发版 | 持续迭代中 |

> **版本号规则**: `J8` 表示基于 Java 8 开发（兼容 JDK 11+），后跟主版本号、次版本号。旧版 `23.x.x` 建议逐步迁移至 `J8.x.x` 体系。

## 🧩 核心功能模块

### 1. 标准基础库 (`geoair-standard`)

#### geoair-base — SPI 接口定义层
- **Bean 容器**: `GiBeanFactory` 统一 Bean 获取接口，解耦 Spring 依赖
- **缓存抽象**: `GiCache` 仿 Spring Cache 设计，支持过期时间
- **JSON 处理**: `GirJSON` 统一 JSON 入口，支持多实现切换
- **统一结果**: `GiResult<T>` 链式 API（`success().andAlertMsg()`），统一前后端返回格式
- **GPA 持久化架构**: `GiDao`（增删改查分离）、`GiEntityable`（实体标记）、ID 生成策略
- **数据模型**: `GiModelable`（模型）、`GiTypeModelable`（类型模型）、`GiVisualModelable`（可视化模型）
- **分页模型**: `GiPageParam` / `GiPager` 统一分页参数和执行器
- **SPI 加载**: `@GkSP` 注解驱动的服务发现机制
- **环境配置**: `GiPropertier` / `GiEnvironmenter` 抽象环境变量和配置文件读取

#### geoair-core — SPI 实现层（Spring 适配）
- **SpringContextBean4Gir**: 基于 Spring Context 的 Bean 工厂实现
- **Cache4Gir**: 支持 JSR Cache / Spring Cache 两种缓存后端
- **JSON 四重奏**: `GirJacksonJson` / `GirFastJson` / `GirGsonJson` / `GirHutoolJson`
- **日志三剑客**: SLF4J / Apache Commons Log / Hutool Log
- **SpringEnvironment4Gir**: Spring Environment 适配器
- **SpringServlet4Gir**: Servlet API 适配器

#### geoair-web — Web 层公共组件
- **会话管理**: HTTP Session / Cookie / Token / Spring Session 多策略支持
- **权限模型**: `GiWebPermissionUser` 用户权限抽象
- **请求日志**: `HttpContextLoggingFilter` 请求/响应体采集
- **跨域支持**: `GirCorsFilter` / `GirCorsInterceptor`
- **MIME 类型**: SPI 驱动的 MIME 类型解析（Image/XML/Text/Application）
- **统一 Web 结果**: `GiWebResult` 扩展 GiResult，支持 Web 上下文

#### geoair-orm — ORM 多框架集成
通过 SPI 机制 (`GirEntityResolve` / `GirExampleExecutor`) 适配多种 ORM：

| 实现模块 | ORM 框架 | 适用场景 |
|----------|----------|----------|
| `geoair-orm-mybatis` | MyBatis | 传统 XML 映射 |
| `geoair-orm-mybatis-plus` | MyBatis-Plus | 增强工具、Lambda 查询 |
| `geoair-orm-mybatis-tk` | 通用 Mapper | 单表 CRUD 简化 |
| `geoair-orm-springjpa` | Spring Data JPA | JPA 规范实现 |
| `geoair-orm-spi` | SPI 抽象层 | 框架扩展点 |

### 2. GIS 地理空间处理 (`geoair-geo`)

#### 坐标转换 (`geoair-geo-tools`)
- 🇨🇳 **中国坐标系**: WGS84 ↔ GCJ02（火星）↔ BD09（百度），支持单点、批量、Geometry 全类型
- 🌐 **投影转换**: WGS84 ↔ EPSG:3857（Web 墨卡托）、EPSG:4490（CGCS2000）
- 📐 **格式互转**: DMS（度分秒）↔ DD（十进制度）
- 🎯 **SRID 转换**: 任意坐标系之间的 Geometry 重投影

#### 空间格式转换
- **GeoJSON** ↔ **WKT** ↔ **WKB** ↔ **JTS Geometry** ↔ **PostGIS PGgeometry**
- 全类型支持: Point / LineString / Polygon / Multi* / GeometryCollection

#### 空间测量
- **面积**: 支持 UTM 投影精确计算 + 球面快速计算，单位：m²/km²/亩/公顷
- **长度**: 线/面周长计算，支持地理/投影坐标系
- **距离**: 点到点、点到线、点到面、线到线最短距离，地理坐标系使用测地线算法

#### 几何对象合并
- Multi 几何生成：数组 → MultiPoint / MultiLineString / MultiPolygon
- 拓扑合并：重叠/相邻面合并为单个 Polygon

#### 瓦片地图
- **瓦片坐标**: XYZ / TMS / WMTS 瓦片坐标 ↔ WGS84 经纬度 / 墨卡托投影
- **等轴瓦片**: WGS84 等轴分轴方案 (`Wgs84EqualAxisTileUtils` / `Wgs84SeparateAxisTileUtils`)
- **Web 墨卡托**: EPSG:3857 瓦片方案 (`TileConverter3857Utils`)
- **Bing 地图**: QuadKey 生成与解析 (`BingMapQuadKeyUtils`)
- **矢量瓦片**: MVT 处理 (`geoair-mvt`)
- **瓦片融合**: 多源瓦片拼接 (`geoair-map-tile-fuser`)
- **瓦片锻造**: 瓦片预生成 (`geoair-map-tile-forge`)

#### 高级空间查询 (`geoair-adv-query`)
- **IAdvExecutor** 查询执行器：统一查询接口，支持 PostgreSQL + Oracle + 达梦
- **BBox 空间过滤**: 边界框查询条件自动生成
- **SQL 注入防护**: 参数化查询，白名单校验
- **分页查询**: 内置分页支持
- **PostgreSQL 方言优化**: `PgAdv*Opt` 特化实现，利用 PostGIS 空间索引

#### 文件格式转换 (`geoair-file-tran`)
- **Shapefile** ↔ **GeoJSON** ↔ **PostGIS**
- 插件式架构：`geoair-file-core`（抽象） + 各格式实现

#### GeoServer 集成
- `geoair-geoserver`: GeoServer 2.22.6 集成
- `geoair-by-gwc`: GeoWebCache 缓存集成

### 3. 动态多数据源 (`geoair-dynamic-ds`)

```
@EnableDynamicDs / @EnableDynamicWebDs   ← 启用注解
         ↓
DynamicDataSourceManager                  ← 数据源注册/获取/移除
         ↓
AdvDynamicDataSourceStorage              ← 运行时数据源存储
         ↓
GirDynamicDataSourceAspect / GirDynamicStackDataSource  ← AOP 切换
         ↓
DataSourceWrapper (Druid/Hikari/BoneCP/C3P0/DBCP2)     ← 连接池适配
```

- **运行时切换**: 通过 AOP + ThreadLocal 实现数据源动态路由
- **读写分离**: `GirReadWriteDataSource` + SQL 解析自动路由（SELECT → 读库，其他 → 写库）
- **连接池支持**: Druid / HikariCP / BoneCP / C3P0 / DBCP2，统一 `AdvDataSourceWrapper` 包装
- **事务管理**: `GirDsTransactionManager` + `GirDsTransactionTemplate` 编程式事务
- **Web 上下文**: `GirDataSourceWebContextInterceptor` 从请求参数/Header 获取目标数据源 ID
- **JDBC URL 解析**: `JdbcUrlSplitter` 智能解析，`DataSourceDruidFastCreate` 快速创建
- **GeoTools 集成**: `GtDataStoreGetter` 统一 DataStore 获取

### 4. API 文档 (`geoair-apidoc`)

| Starter | 规范 | UI |
|---------|------|-----|
| `geoair-knife4j-springdoc-spring-boot-starter` | OpenAPI 3.0（推荐） | Knife4j 4.x |
| `geoair-knife4j-springfox-spring-boot-starter` | Swagger 2.0（兼容） | Knife4j 3.x |

- 自动扫描 Controller 包路径
- 按包路径自动分组
- 支持文档导出（PDF / Markdown）
- Spring Boot Auto-Configuration 零配置启动

### 5. 代码生成器 (`geoair-code-generator`)

- DB 表结构 → Java Entity / Mapper / Service / Controller
- 支持自定义代码模板
- 生成前端 Vue 组件代码

### 6. 数据库服务 (`geoair-db-service`)

- 统一数据库访问抽象层
- 多数据库方言支持
- **Web 可视化界面**（Vue 2 + Element UI + ECharts）：SQL 编辑器、数据浏览、表结构管理

### 7. 消息转换器

- **JTS-Jackson**: JTS Geometry ↔ JSON，支持 GeoJSON 格式序列化
- **JTS-MyBatis**: JTS Geometry ↔ 数据库空间字段类型映射

## 🛠️ 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| **JDK** | Java Development Kit | 8（兼容 11+） |
| **构建** | Maven | 3.6+ |
| **核心框架** | Spring Boot | 2.7.18 |
| **Spring** | Spring Framework | 5.3.31 |
| **GIS 核心** | GeoTools / JTS | 28.6.1 / 1.19.0 |
| **GIS 扩展** | Proj4j / Java Vector Tile | 1.3.0 / 1.3.9 |
| **空间数据库** | PostGIS JDBC / Oracle Spatial / 达梦 DM | 2025.1.1 / 21.5.0.0 / 18 |
| **连接池** | Druid / HikariCP | 1.2.23 |
| **ORM** | MyBatis / MyBatis-Plus / Spring Data JPA | 3.5.9 / 3.5.2 / 5.6.15 |
| **API 文档** | Knife4j + SpringDoc / Springfox | 4.4.0 / 3.0.0 |
| **JSON** | Jackson / FastJSON2 / Gson | 2.13.5 / 2.0.61 / 2.9.1 |
| **缓存** | Redis (Redisson) / JSR Cache / Spring Cache | 3.18.0 |
| **工具** | Hutool / Lombok | 5.8.42 / 1.18.30 |
| **代码质量** | SpotBugs / Modernizer / fmt-maven-plugin | 4.7.3.6 / 2.7.0 / 2.6.0 |
| **前端** | Vue.js 2 + Element UI + ECharts（db-service-webview） | 2.7.14 |

## 🚀 快速开始

### 1. 添加依赖

```xml
<!-- 方式一：继承父 POM（推荐） -->
<parent>
    <groupId>cn.geoair.devkit</groupId>
    <artifactId>geoair-base-parent</artifactId>
    <version>J8.1.5</version>
</parent>

<!-- 方式二：仅导入依赖管理 -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>cn.geoair.devkit</groupId>
            <artifactId>geoair-base-parent</artifactId>
            <version>J8.1.5</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 2. 按需引入模块

```xml
<!-- 基础工具（无 GIS 依赖） -->
<dependency>
    <groupId>cn.geoair.devkit</groupId>
    <artifactId>geoair-standard</artifactId>
    <version>J8.1.5</version>
    <type>pom</type>
</dependency>

<!-- GIS 空间处理 -->
<dependency>
    <groupId>cn.geoair.devkit</groupId>
    <artifactId>geoair-geo-tools</artifactId>
    <version>J8.1.5</version>
</dependency>

<!-- 动态数据源 -->
<dependency>
    <groupId>cn.geoair.devkit</groupId>
    <artifactId>geoair-dynamic-ds</artifactId>
    <version>J8.1.5</version>
</dependency>

<!-- API 文档 -->
<dependency>
    <groupId>cn.geoair.devkit</groupId>
    <artifactId>geoair-knife4j-springdoc-spring-boot-starter</artifactId>
    <version>J8.1.5</version>
</dependency>
```

### 3. 使用示例

```java
// 统一日志
Gir.log.info("Hello GeoAir!");

// 读取配置
String dbUrl = Gir.property.getProperty("spring.datasource.url");

// 统一结果返回
GiResult<String> result = GiResult.successMsg("操作成功").andValue("data");

// GIS 坐标转换
GirGeoTools tools = GirGeoTools.defaultInstance();
double[] gcj02 = tools.getCoordinateOpt().wgs84ToGcj02(116.40, 39.90);

// GIS 空间测量
double area = tools.getMeasureOpt().calculateArea(geometry, 4326, "km²");
```

## 🔧 构建配置

### Maven Profiles

| Profile | 用途 |
|---------|------|
| `central` | 发布到 Maven Central（含 GPG 签名） |
| `geoair-group-repo` | 发布到集团私有仓库 |
| `code-style` | 代码格式化（fmt-maven-plugin + sortpom） |
| `code-check` | 静态分析（SpotBugs + Modernizer） |
| `git-commit-id-gen` | 生成 git.properties |

```bash
# 安装到本地仓库
mvn clean install

# 格式化代码
mvn clean install -P code-style

# 发布到集团仓库
mvn clean deploy -P geoair-group-repo
```

## 👥 开发者

- **作者**: 张逢吉
- **邮箱**: zhangjun7570@qq.com
- **组织**: GeoAir
- **官网**: https://xmt.geoair.cn/
- **仓库**: https://github.com/geoair-cn/geoair

## 📄 许可证

本项目采用 **Apache License 2.0** 许可证 — 详见 [LICENSE](LICENSE) 文件。

## 🙏 致谢

- [GeoTools](https://geotools.org/) — 开源 Java GIS 工具包
- [JTS Topology Suite](https://locationtech.github.io/jts/) — 空间索引与几何处理
- [Spring Boot](https://spring.io/projects/spring-boot) — Java 应用开发框架
- [Knife4j](https://doc.xiaominfo.com/) — Swagger 增强 UI
- [MyBatis-Plus](https://baomidou.com/) — MyBatis 增强工具
- [Hutool](https://hutool.cn/) — Java 工具库

---

**GeoAir Framework** — 让地理信息系统开发更简单！🚀
