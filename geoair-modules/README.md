# GeoAir Modules 使用指南

## 模块介绍

GeoAir Modules 是 GeoAir 框架的核心组件库，提供了一系列功能强大的模块，包括 API 文档生成、代码生成、地理空间处理、动态数据源管理、数据库服务和消息转换等。

## 目录结构

```
goair-modules/
├── geoair-apidoc/           # API 文档相关模块
│   ├── geoair-knife4j-core/                    # Knife4j 核心模块
│   ├── geoair-knife4j-spring-boot-demo/        # Knife4j Spring Boot 示例
│   └── geoair-knife4j-springdoc-spring-boot-starter/ # Knife4j SpringDoc Starter
├── geoair-code-generator/   # 代码生成模块
│   ├── geoair-code-gen-demo/    # 代码生成示例
│   └── geoair-code-gen-module/  # 代码生成核心模块
├── geoair-geo/              # 地理空间相关模块
│   └── geoair-adv-query/    # 高级查询模块
├── geoair-dynamic-ds/       # 动态数据源模块
├── geoair-db-service/       # 数据库服务模块
│   └── geoair-db-service-core/ # 数据库服务核心模块
└── geoair-message-converter/ # 消息转换器模块
    ├── geoair-message-jts-jackson/   # JTS Jackson 消息转换器
    └── geoair-message-jts-mybatis/   # JTS MyBatis 消息转换器
```

## 模块说明

### 1. geoair-apidoc

提供 API 文档生成功能，基于 Knife4j 实现，支持 Swagger 2.0 和 OpenAPI 3.0 规范。

### 2. geoair-code-generator

提供代码生成功能，支持从数据库表结构生成实体类、Mapper、Service 等代码。

### 3. geoair-geo

提供地理空间相关功能，包括高级查询、空间分析等。

### 4. geoair-dynamic-ds

提供动态数据源管理功能，支持运行时切换数据源。

### 5. geoair-db-service

提供数据库服务功能，包括数据源管理、表管理等。

### 6. geoair-message-converter

提供消息转换功能，支持 JTS 几何对象与 JSON、数据库类型的转换。

## 快速开始

### 1. 引入依赖

在 Maven 项目中，添加以下依赖：

```xml
<dependency>
    <groupId>cn.geoair.devkit</groupId>
    <artifactId>geoair-modules</artifactId>
    <version>J8.1.2-SNAPSHOT</version>
    <type>pom</type>
</dependency>
```

### 2. 选择需要的模块

根据需要，引入具体的子模块依赖，例如：

```xml
<!-- API 文档生成 -->
<dependency>
    <groupId>cn.geoair.devkit</groupId>
    <artifactId>geoair-knife4j-springdoc-spring-boot-starter</artifactId>
    <version>J8.1.2-SNAPSHOT</version>
</dependency>

<!-- 代码生成 -->
<dependency>
    <groupId>cn.geoair.devkit</groupId>
    <artifactId>geoair-code-gen-module</artifactId>
    <version>J8.1.2-SNAPSHOT</version>
</dependency>
```

## 依赖关系

- **geoair-modules** 依赖于 **geoair-base-parent**
- 各个子模块之间相互独立，可以根据需要单独引入

## 版本历史

- J8.1.2-SNAPSHOT：当前开发版本

## 贡献指南

1. Fork 本项目
2. 创建 feature 分支
3. 提交代码
4. 推送到远程分支
5. 创建 Pull Request

## 许可证

本项目采用 Apache License 2.0 许可证，详见 [LICENSE](LICENSE) 文件。

## 联系方式

- 开发者：张逢吉
- 邮箱：1159856928@qq.com
- 组织：geoair
- 官网：https://xmt.geoair.cn/
