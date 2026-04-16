# GeoAir Standard 模块使用指南

## 模块介绍

GeoAir Standard 是 GeoAir 框架的基础标准库，设计初衷是通过 SPI 实现相关常用的工具，提供了一系列基础组件和工具类，为上层应用提供支持。

## 目录结构

```
goair-standard/
├── geoair-base/      # 基础核心模块
├── geoair-web/       # Web 相关模块
├── geoair-tools/     # 工具类模块
├── geoair-core/      # 核心功能模块
├── geoair-orm/       # ORM 相关模块
│   ├── geoair-orm-base/         # ORM 基础模块
│   ├── geoair-orm-mybatis/      # MyBatis 集成模块
│   ├── geoair-orm-mybatis-tk/   # MyBatis-Plus 集成模块
│   ├── geoair-orm-springjpa/    # Spring JPA 集成模块
│   └── geoair-orm-spi/          # ORM SPI 模块
└── geoair-sdk/       # SDK 输出工具模块
```

## 模块说明

### 1. geoair-base

基础核心模块，提供了框架的核心功能，包括：

- API 注解：`@GaApi`、`@GaApiAction` 等
- Bean 管理：`GiBeanFactory`、`GirBeanHelper` 等
- 缓存：`GiCache`、`GirCacheHelper` 等
- 转换：`GiConverter`、`GirConverterFactory` 等
- 数据模型：`GiModelable`、`GiTypeModelable` 等
- 分页：`GiPageParam`、`GiPager` 等
- 结果处理：`GiResult`、`GirResult` 等
- 环境配置：`GiEnvironmenter`、`GirEnvironmentHelper` 等
- 异常处理：`GirExceptionResultConverter` 等
- GPA（通用持久化架构）：`GiDao`、`GiEntityable` 等
- JSON 处理：`GirJSON` 等
- SPI 加载：`GirSpHelper`、`GkSpLoader` 等
- 工具类：`GutilAop`、`GutilBean` 等

### 2. geoair-web

Web 相关模块，提供了 Web 应用所需的功能。
#  geoair 开发基础工程

### 3. geoair-tools
## 工程结构
*  geoair-base    标准库 [README](./ geoair-base/README.md)

工具类模块，提供了各种工具类。
*  geoair-core    工具库 [README](./ geoair-core/README.md)

### 4. geoair-core
*  geoair-web    可复用的公共组件和一些通用实现 [README](./ geoair-web/README.md)

核心功能模块，提供了框架的核心功能。
*  geoair-sdk    统一的sdk输出工具

### 5. geoair-orm

ORM 相关模块，提供了多种 ORM 框架的集成：

- **geoair-orm-base**：ORM 基础模块
- **geoair-orm-mybatis**：MyBatis 集成模块
- **geoair-orm-mybatis-tk**：MyBatis-Plus 集成模块
- **geoair-orm-springjpa**：Spring JPA 集成模块
- **geoair-orm-spi**：ORM SPI 模块

### 6. geoair-sdk

统一的 SDK 输出工具模块。

## 快速开始

### 1. 引入依赖

在 Maven 项目中，添加以下依赖：

```xml
<dependency>
    <groupId>cn.geoair.devkit</groupId>
    <artifactId>geoair-standard</artifactId>
    <version>J8.1.2</version>
    <type>pom</type>
</dependency>
```

### 2. 选择需要的子模块

根据需要，引入具体的子模块依赖，例如：

```xml
<!-- 基础核心模块 -->
<dependency>
    <groupId>cn.geoair.devkit</groupId>
    <artifactId>geoair-base</artifactId>
    <version>J8.1.2</version>
</dependency>

<!-- ORM 模块 -->
<dependency>
    <groupId>cn.geoair.devkit</groupId>
    <artifactId>geoair-orm-mybatis</artifactId>
    <version>J8.1.2</version>
</dependency>
```

## 功能特性

- 提供了丰富的基础组件和工具类
- 支持 SPI 机制，便于扩展
- 提供了多种 ORM 框架的集成
- 提供了 Web 应用所需的功能
- 提供了统一的 SDK 输出工具

## 依赖关系

- **geoair-standard** 依赖于 **geoair-base-parent**
- 各个子模块之间相互独立，可以根据需要单独引入

## 版本历史

- J8.1.2：当前开发版本

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

*  geoair-orm    orm框架
