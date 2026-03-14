# GeoAir Framework Parent

## 项目介绍

GeoAir Framework Parent 是一个用于工程模块的依赖定义的父项目，为 GeoAir 框架提供统一的依赖管理和构建配置。

## 项目结构

```
geoair-framework-parent/
├── geoair-api-parent/          # API 模块父项目
├── geoair-project-parent/      # 项目模块父项目
├── geoair-spring-boot-starter-parent/  # Spring Boot Starter 模块父项目
├── pom.xml                    # 父项目配置
└── README.md                  # 项目说明
```

## 核心功能

- **依赖管理**：统一管理 GeoAir 框架的依赖版本
- **构建配置**：提供统一的 Maven 构建插件配置
- **模块组织**：组织和管理 GeoAir 框架的各个模块
- **发布配置**：配置 Maven 中央仓库发布相关插件

## 技术依赖

- Java 8+
- Maven 3.6+
- geoair-base-parent

## 子模块说明

### 1. geoair-api-parent

- **功能**：API 模块的父项目，用于管理 API 相关的依赖
- **用途**：为 API 模块提供统一的依赖管理和构建配置

### 2. geoair-project-parent

- **功能**：项目模块的父项目，用于管理项目相关的依赖
- **用途**：为项目模块提供统一的依赖管理和构建配置

### 3. geoair-spring-boot-starter-parent

- **功能**：Spring Boot Starter 模块的父项目，用于管理 Spring Boot 相关的依赖
- **用途**：为 Spring Boot Starter 模块提供统一的依赖管理和构建配置

## 安装使用

### Maven 依赖

在项目的 pom.xml 文件中添加以下配置：

```xml
<parent>
    <groupId>cn.geoair.devkit</groupId>
    <artifactId>geoair-framework-parent</artifactId>
    <version>J8.1.0-SNAPSHOT</version>
</parent>
```

## 构建配置

项目使用以下 Maven 插件进行构建和发布：

- **maven-gpg-plugin**：用于对发布的构件进行签名
- **central-publishing-maven-plugin**：用于发布到 Maven 中央仓库

## 许可证

Apache License 2.0

## 开发团队

- **开发者**：张逢吉
- **邮箱**：1159856928@qq.com
- **组织**：geoair
- **官网**：https://xmt.geoair.cn/

## 项目地址

- **Gitee**：https://gitee.com/geoair/geoair

## 版本信息

当前版本：J8.1.0-SNAPSHOT

## 贡献指南

欢迎提交 Issue 和 Pull Request 来帮助改进这个项目！

## 选择合适的 Parent

### geoair-api-parent
适用于：API 接口层项目
特点：包含基础的 GeoTools 和 Spring 依赖

### geoair-project-parent  
适用于：普通业务项目
特点：包含完整的框架依赖

### geoair-spring-boot-starter-parent
适用于：Spring Boot Starter 开发
特点：包含完整的 Spring Boot 插件配置