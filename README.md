# GeoAir Framework  

## 项目简介

GeoAir Framework 是一个  **企业级 Java 开发框架**,提供从基础依赖管理、核心工具库到业务组件的全套解决方案。框架集成了 GeoTools、PostGIS 等地理空间处理库,支持多数据源动态切换、高级空间查询、坐标转换等 GIS 核心功能,并内置 API 文档自动生成、代码生成器等开发效率工具。

## 版本历史
| 版本号          | 发布日期   | 核心变更说明                                                                                                                         |
|-----------------|------------|--------------------------------------------------------------------------------------------------------------------------------------|
| 23.1.0          | 2023-08-01 | 初始版本，包含核心功能模块：<br>• 基础标准库、核心工具库、Web 层公共组件、统一 SDK 输出<br>• GIS 地理空间处理、动态多数据源管理<br>• 数据库服务、瓦片地图工具 |
| 23.1.2-M2       | 2026-03-13 | 1. 修复若干已知 Bug<br>2. 优化核心模块性能<br>3. 新增部分功能模块                                                                     |
| J8.1.0-RC2      | 2026-03-15 | 1. 统一 Group ID：原 `cn.geoair`、`cn.geoair.comp`、`cn.geoair.dependencies`、`cn.geoair.orm`、`cn.geoair.geo.dynamic` 等全部替换为 `cn.geoair.devkit`<br>2. 版本号规则更新：Java版本号 + 子版本号 + RC版本号<br>3. 兼容 JDK 11+，主线基于 Java 8 开发<br>4. 建议 23 开头版本逐步迁移至 J8.x.x 版本体系 |
## 模块说明

| 模块分类 | 模块名称 | 功能描述 |
|---------|---------|---------|
| **标准库** | geoair-base | 基础标准库 (API 注解/数据转换/缓存/异常处理) |
| | geoair-core | 核心工具库 |
| | geoair-web | Web 层公共组件 |
| | geoair-sdk | 统一 SDK 输出 |
| | geoair-orm | ORM 框架集成 (MyBatis/MyBatis-Plus/JPA) |
| | geoair-tools | 通用工具集 |
| **业务组件** | geoair-apidoc | API 文档自动生成 (Swagger/SpringDoc) |
| | geoair-code-generator | 代码生成器 |
| | geoair-geo | GIS 地理空间处理 (坐标转换/空间查询/几何处理) |
| | geoair-dynamic-ds | 动态多数据源管理 |
| | geoair-db-service | 数据库服务 (含 Web 可视化界面) |
| | geoair-message-converter | JTS 几何对象消息转换器 |
 
## 核心功能模块

### 1. GIS 地理空间处理 (`geoair-geo`)

#### 📍 坐标转换工具
- 支持 WGS84、GCJ02(火星坐标系)、BD09(百度坐标系) 互转
- 支持 EPSG:3857(墨卡托)、EPSG:4490(CGCS2000) 等投影转换
- 提供 WKT、WKB、GeoJSON 格式互转

#### 📏 空间测量计算
- 距离计算 (测地线距离、平面的距离)
- 面积计算 (支持多种单位:m²、km²、亩等)
- 长度量算、缓冲区分析

#### 🔧 几何对象处理
- 几何对象合并、分割、简化
- 空间关系判断 (相交、包含、相邻等)
- 几何对象有效性检查

#### 🗺️ 瓦片地图工具
- XYZ/TMS/WMTS 瓦片坐标计算
- 瓦片金字塔生成
- 矢量瓦片 (MVT) 处理

### 2. 动态多数据源 (`geoair-dynamic-ds`, `geoair-db-service`)

- ✅ 运行时数据源动态切换
- ✅ 支持 PostgreSQL+PostGIS、Oracle Spatial、达梦等空间数据库
- ✅ 数据源元信息管理
- ✅ JDBC URL 智能解析工具
- ✅ 数据库连接池管理 (Druid)

### 3. 高级空间查询 (`geoair-adv-query`)

- ✅ 基于 GeoTools 的空间查询优化
- ✅ 边界框查询 (BBox)、空间过滤
- ✅ SQL 注入防护
- ✅ 分页查询支持
- ✅ PostgreSQL 方言特化支持

### 4. API 文档自动化 (`geoair-apidoc`)

#### Swagger 2 版本 (`geoair-knife4j-core`)
- 自动扫描 Controller 包路径
- 按包路径自动分组 API 文档
- 自定义配置属性
- Spring Boot 自动装配

#### SpringDoc OpenAPI 3 版本 (`geoair-knife4j-springdoc-spring-boot-starter`)
- 基于 SpringDoc 生成 OpenAPI 3 规范文档
- Knife4j 增强 UI 界面
- 自定义响应模型转换
- 操作行为定制

### 5. ORM 框架集成 (`geoair-orm`)

通过 SPI 机制支持多种 ORM 框架:
- **MyBatis** - 传统 XML 映射方式
- **MyBatis-Plus** - 增强工具集
- **Spring JPA** - JPA 规范实现
- **通用 Mapper** - 单表 CRUD 简化

### 6. 代码生成器 (`geoair-code-generator`)

- 根据数据库表结构自动生成实体类
- 生成 Mapper、Service、Controller 层代码
- 支持自定义代码模板
- 生成前端 Vue 组件代码

### 7. 数据库服务 (`geoair-db-service`)

- 统一的数据库访问层抽象
- 支持多种数据库方言
- 数据库元数据查询
- Web 可视化数据库管理界面 (基于 Vue)

### 8. 消息转换器 (`geoair-message-converter`)

- **JTS-Jackson 转换** - Geometry 对象 JSON 序列化/反序列化
- **JTS-MyBatis 转换** - Geometry 类型数据库映射处理器

## 技术栈

| 类别 | 技术选型 | 版本 |
|------|----------|------|
| **JDK** | Java Development Kit | 8+ |
| **构建工具** | Maven | 3.6+ |
| **核心框架** | Spring Boot | 2.7.18 |
| **Spring** | Spring Framework | 5.3.31 |
| **ORM 框架** | MyBatis / MyBatis-Plus | 3.5.9 / 3.5.2 |
| **GIS 库** | GeoTools / JTS Core | 28.6.1 / 1.19.0 |
| **空间数据库** | PostGIS / Oracle Spatial / 达梦 | - |
| **JDBC 驱动** | PostgreSQL / Oracle / DM | 42.3.8 / 19.3.0.0 / 18 |
| **连接池** | Druid | 1.2.23 |
| **API 文档** | SpringDoc / Knife4j | 1.7.0 / 4.4.0 |
| **Swagger 2** | Springfox / Knife4j | 3.0.0 / 3.0.3 |
| **JSON 处理** | Jackson / FastJSON2 | 2.13.5 / 2.0.61 |
| **工具库** | Hutool / Lombok | 5.8.42 / 1.18.30 |
| **前端框架** | Vue.js | - |

## 贡献指南

欢迎参与项目开发和改进!

1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 开发者信息

- **作者**: 张逢吉
- **邮箱**: 1159856928@qq.com
- **组织**: geoair
- **官网**: https://xmt.geoair.cn/
- **Gitee**: https://github.com/geoair-cn/geoair

## 许可证

本项目采用 **Apache License 2.0** 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 致谢

感谢以下开源项目:

- [GeoTools](https://geotools.org/) - 开源 GIS 工具包
- [JTS Topology Suite](https://locationtech.github.io/jts/) - 空间索引和几何处理
- [Spring Boot](https://spring.io/projects/spring-boot) - Java 应用框架
- [Knife4j](https://doc.xiaominfo.com/) - Swagger 增强工具
- [MyBatis-Plus](https://baomidou.com/) - MyBatis 增强工具

---

**GeoAir Framework** - 让地理信息系统开发更简单！🚀
