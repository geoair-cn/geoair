# GeoAir Dependencies 父工程

## 项目简介

GeoAir Dependencies 是一个用于管理 GeoAir 项目依赖的父工程。它统一定义了所有子模块的版本依赖，确保整个项目的技术栈版本一致性，简化依赖管理流程。

## 项目结构

geoair-dependencies/
├── pom.xml                 # 父工程 POM 文件，定义统一依赖版本
├── README.md              # 项目说明文档
└── src/
    └── main/
        └── resources/
            └── application.yml  # 全局配置文件

## 核心功能

- **统一依赖管理**：集中管理所有第三方库的版本号
- **版本锁定**：避免子模块间依赖版本冲突
- **快速集成**：子模块只需继承即可获得完整的依赖配置
- **环境隔离**：支持多环境配置管理

## 技术栈

- **构建工具**：Maven 3.6+
- **Java 版本**：JDK 8+
- **核心框架**：
  - Spring Boot 2.7.x
  - Spring Cloud 2021.x
  - MyBatis Plus 3.5.x
- **数据库**：
  - MySQL 8.0
  - Redis 6.0
- **其他组件**：
  - Nacos 2.0（服务注册发现）
  - Sentinel（流量控制）
  - RocketMQ（消息队列）

## 快速开始

### 环境要求

- JDK 8 或更高版本
- Maven 3.6 或更高版本
- Git

### 构建项目

# 克隆项目
git clone https://gitee.com/your-account/geoair-dependencies.git

# 进入项目目录
cd geoair-dependencies

# 编译打包
mvn clean install

### 使用方式

在子模块的 `pom.xml` 中继承父工程：

<parent>
    <groupId>com.geoair</groupId>
    <artifactId>geoair-dependencies</artifactId>
    <version>1.0.0</version>
    <relativePath/>
</parent>

## 配置说明

### 版本管理

父工程通过 `<dependencyManagement>` 统一管理所有依赖版本：

<dependencyManagement>
    <dependencies>
        <!-- Spring Boot -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>${spring-boot.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        
        <!-- 自定义依赖 -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>

### 属性配置

主要版本属性定义在 `<properties>` 节点中：

<properties>
    <java.version>1.8</java.version>
    <spring-boot.version>2.7.15</spring-boot.version>
    <spring-cloud.version>2021.0.8</spring-cloud.version>
    <mybatis-plus.version>3.5.4</mybatis-plus.version>
</properties>

## 模块依赖关系

geoair-dependencies (父工程)
├── geoair-common          # 公共工具模块
├── geoair-gateway         # 网关服务
├── geoair-auth            # 认证授权服务
├── geoair-system          # 系统管理服务
└── geoair-business        # 业务服务模块

## 开发规范

### 代码规范

- 遵循阿里巴巴 Java 开发手册
- 使用统一的代码格式化模板
- 所有公共方法必须添加 JavaDoc 注释

### 提交规范

采用 conventional commit 规范：

<type>(<scope>): <subject>

<body>

<footer>

常用 type 类型：
- feat: 新功能
- fix: 修复 bug
- docs: 文档更新
- style: 代码格式调整
- refactor: 重构
- test: 测试相关
- chore: 构建过程或辅助工具变动

### 分支管理

- `main`: 主分支，生产环境代码
- `develop`: 开发分支
- `feature/*`: 功能开发分支
- `hotfix/*`: 紧急修复分支
- `release/*`: 发布分支

## 常见问题

### Q: 如何添加新的依赖？

A: 在父工程的 `pom.xml` 中的 `<dependencyManagement>` 节点添加相应依赖，子模块中直接引用即可。

### Q: 版本冲突如何解决？

A: 检查是否有多个版本的同一依赖，统一在父工程中指定版本号。

### Q: 如何升级 Spring Boot 版本？

A: 修改 `pom.xml` 中的 `spring-boot.version` 属性值，同时检查兼容性。

## 贡献指南

1. Fork 本仓库
2. 创建 feature 分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

 

---
*最后更新时间: 2024年*
