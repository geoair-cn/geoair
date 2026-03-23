# GeoAir Base Dependencies Parent

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Version](https://img.shields.io/badge/Version-J8.1.0--SNAPSHOT-orange.svg)](https://github.com/geoair-cn/geoair)

## 📚 模块概述

`geoair-dependencies-bom` 是 GeoAir Framework 的**统一依赖版本管理中心**，负责统一管理所有第三方依赖的版本号，确保整个框架的依赖版本一致性和兼容性。

本模块采用 **BOM (Bill of Materials)** 模式，将依赖按功能领域拆分为 5 个子模块，实现按需引入、灵活组合。

## 🏗️ 架构设计

### 继承关系
```
spring-boot-dependencies (2.7.18)
↑
geoair-dependencies-bom
├── geoair-geotools-dependencies  # GIS 空间数据处理依赖
├── geoair-spring-dependencies    # Spring 生态依赖
├── geoair-openapi-dependencies   # API 文档依赖
├── geoair-template-dependencies  # 模板预留模块
└── geoair-common-dependencies     # 其他第三方依赖
```

### 核心职责
1. **版本统一管理**：集中定义所有第三方依赖的版本号
2. **领域分组**：按功能领域拆分依赖，避免"全家桶"式强制引入
3. **冲突解决**：通过统一的 BOM 管理，避免传递依赖版本冲突
4. **升级便捷**：单点升级版本号，全局生效

## 📦 子模块详解

### 1. geoair-geotools-dependencies
**功能定位**：GIS 地理空间数据处理依赖集合

#### 核心依赖
| 依赖组 | 版本 | 说明 |
|--------|------|------|
| GeoTools | 28.6.1 | 开源 Java GIS 工具包 |
| JTS Topology Suite | 1.19.0 | 几何对象处理库 |
| PostGIS JDBC | 2025.1.1 | PostgreSQL 空间数据库驱动 |
| Proj4j | 1.3.0 | 坐标转换库 |
| Java Vector Tile | 1.3.16 | MVT 矢量瓦片处理 |

#### 管理的 GeoTools 组件 (部分)
```xml
<!-- 核心组件 -->
gt-opengis, gt-metadata, gt-main
<!-- 数据格式 -->
gt-geojson, gt-shapefile, gt-geopkg, gt-gml-geometry-streaming
<!-- 栅格处理 -->
gt-coverage, gt-grid, gt-imagemosaic, gt-geotiff
<!-- 渲染与可视化 -->
gt-render, gt-svg, gt-swing
<!-- 空间参考 -->
gt-referencing, gt-transform, gt-epsg-hsql
<!-- JDBC 支持 -->
gt-jdbc, gt-jdbc-postgis, gt-jdbc-mysql, gt-jdbc-oracle
<!-- Web 服务 -->
gt-wms, gt-wfs-ng
<!-- 高级功能 -->
gt-cql (查询语言), gt-process (处理引擎)
```

#### 适用场景
- 地理信息系统开发
- 空间数据库操作
- 地图服务发布
- 坐标转换与投影

---

### 2. geoair-spring-dependencies
**功能定位**：Spring 生态系统依赖集合

#### 核心依赖
| 依赖 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 2.7.18 | 应用框架核心 |
| Spring Cloud | 2021.0.4 | 微服务框架 |
| Spring Cloud Alibaba | 2021.0.4.0 | 阿里微服务生态 |
| Redisson | 3.18.0 | Redis 客户端 |
| Spring Context Support | 1.0.11 | Spring 上下文扩展 |

#### 特性
- ✅ 完整继承 Spring Boot Dependencies BOM
- ✅ 集成 Spring Cloud 微服务组件
- ✅ 整合 Redisson 分布式缓存
- ✅ 支持 Spring Cloud Alibaba 生态

#### 适用场景
- Spring Boot 应用开发
- 微服务架构项目
- 需要 Redis 缓存的项目

---

### 3. geoair-openapi-dependencies
**功能定位**：API 文档生成工具依赖集合

#### 核心依赖
| 依赖 | 版本 | 说明 |
|------|------|------|
| Knife4j OpenAPI3 | 4.4.0 | Knife4j 增强版 (OpenAPI 3) |
| SpringDoc OpenAPI | 1.7.0 | OpenAPI 3 规范实现 |
| Springfox Swagger2 | 3.0.0 | Swagger 2 规范实现 |
| Knife4j OpenAPI2 | 3.0.3 | Knife4j 经典版 (Swagger 2) |
| Reflections | 0.10.2 | 运行时类扫描工具 |

#### 双版本支持
- **OpenAPI 3 (推荐)**：SpringDoc + Knife4j 4.x
- **Swagger 2 (兼容)**：Springfox + Knife4j 3.x

#### 适用场景
- RESTful API 文档自动生成
- API 接口调试与测试
- 前后端分离项目

---

### 4. geoair-common-dependencies
**功能定位**：其他常用第三方依赖集合

---

### 5. geoair-template-dependencies
**功能定位**：预留模板模块
**当前状态**：空模块，用于未来扩展或作为新项目依赖模板参考

## 🚀 使用指南

### 方式一：通过 geoair-base-parent 间接引入 (推荐)
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>cn.geoair.devkit</groupId>
            <artifactId>geoair-base-parent</artifactId>
            <version>J8.1.0-SNAPSHOT</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 方式二：单独引入特定 BOM

#### 仅引入 GeoTools 依赖
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>cn.geoair.devkit</groupId>
            <artifactId>geoair-geotools-dependencies</artifactId>
            <version>J8.1.0-SNAPSHOT</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

#### 仅引入 Spring 依赖
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>cn.geoair.devkit</groupId>
            <artifactId>geoair-spring-dependencies</artifactId>
            <version>J8.1.0-SNAPSHOT</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 方式三：按需组合引入
```xml
<dependencyManagement>
    <dependencies>
        <!-- 引入 GIS 能力 -->
        <dependency>
            <groupId>cn.geoair.devkit</groupId>
            <artifactId>geoair-geotools-dependencies</artifactId>
            <version>J8.1.0-SNAPSHOT</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        
        <!-- 引入 API 文档能力 -->
        <dependency>
            <groupId>cn.geoair.devkit</groupId>
            <artifactId>geoair-openapi-dependencies</artifactId>
            <version>J8.1.0-SNAPSHOT</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

## 📖 相关资源
- **GeoAir Framework**: https://github.com/geoair-cn/geoair
- **Spring Boot**: https://spring.io/projects/spring-boot
- **GeoTools**: https://geotools.org/
- **Knife4j**: https://doc.xiaominfo.com/

## 👥 开发者信息
- **作者**: 张逢吉
- **邮箱**: 1159856928@qq.com
- **组织**: geoair
- **官网**: https://xmt.geoair.cn/

## 📄 许可证
本项目采用 **Apache License 2.0** 许可证。详情请参见 [LICENSE](LICENSE) 文件。

---
**最后更新**: 2026-03-14  
**当前版本**: J8.1.0-SNAPSHOT
```
