# GeoAir Base Parent

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Version](https://img.shields.io/badge/Version-J8.1.0--SNAPSHOT-orange.svg)](https://gitee.com/geoair/geoair)
[![JDK](https://img.shields.io/badge/JDK-8+-green.svg)](https://www.oracle.com/java/technologies/downloads/#java8)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.18-brightgreen.svg)](https://spring.io/projects/spring-boot)

## 📚 模块概述

`geoair-base-parent` 是 GeoAir Framework 的**基础依赖聚合与构建配置中心**，负责统一管理框架的核心依赖约束和构建插件配置。

本模块作为整个框架的**核心枢纽**：
- 🔼 **向上**：继承根 POM (`geoair-framework`) 的全局配置
- 🔽 **向下**：为所有子模块提供统一的依赖管理和构建标准
- 🧩 **核心价值**：确保框架内所有模块的依赖版本一致性、构建流程标准化

## 🎯 核心职责

### 1. 依赖版本管理
- 📦 导入 Spring Boot Dependencies BOM (2.7.18) 作为基础
- 🧰 聚合 4 个领域专用 BOM（GeoTools、Spring、Swagger、Other）
- 🔒 统一版本号定义，从根源避免版本冲突
- 📌 提供版本属性覆盖机制，支持灵活定制

### 2. 构建配置标准化
| 配置项 | 核心参数 | 作用 |
|--------|----------|------|
| Maven 编译器 | JDK 8、UTF-8 编码 | 统一编译环境 |
| 源码生成插件 | attach-sources | 自动生成源码包 |
| Javadoc 插件 | UTF-8 编码、中文支持 | 自动生成 API 文档 |
| Release 插件 | 标准化发布流程 | 版本发布与管理 |
| 代码格式化 | 统一代码风格 | 规范代码提交 |

### 3. 发布流程管理
- 🚀 Maven Central 中央仓库发布配置
- 🏢 集团私有 Maven 仓库发布配置
- 🔐 GPG 签名验证（确保包完整性）
- ✅ 发布前代码格式化检查

## 🏗️ 架构位置

### 层级结构
```
geoair-framework (根 POM)
↑
geoair-base-parent
├── 继承：全局配置 + 通用插件
├── 导入：Spring Boot BOM (2.7.18)
└── 聚合：4 个 base-dependencies BOMs
├── geoair-base-dependencies-geotools # GIS 空间数据处理依赖
├── geoair-base-dependencies-spring   # Spring 生态依赖
├── geoair-base-dependencies-swagger  # API 文档依赖
└── geoair-base-dependencies-other    # 其他第三方依赖
```

## 📦 依赖管理

### 导入的 BOM 列表（核心配置）
```xml
<dependencyManagement>
    <dependencies>
        <!-- 1. Spring Boot 官方 BOM（基础依赖） -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>${spring-boot.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>

        <!-- 2. GeoTools GIS 依赖 BOM -->
        <dependency>
            <groupId>cn.geoair.devkit</groupId>
            <artifactId>geoair-base-dependencies-geotools</artifactId>
            <version>${geoair.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>

        <!-- 3. Spring 生态依赖 BOM -->
        <dependency>
            <groupId>cn.geoair.devkit</groupId>
            <artifactId>geoair-base-dependencies-spring</artifactId>
            <version>${geoair.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        
        <!-- 4. Swagger/API 文档依赖 BOM -->
        <dependency>
            <groupId>cn.geoair.devkit</groupId>
            <artifactId>geoair-base-dependencies-swagger</artifactId>
            <version>${geoair.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        
        <!-- 5. 其他第三方依赖 BOM -->
        <dependency>
            <groupId>cn.geoair.devkit</groupId>
            <artifactId>geoair-base-dependencies-other</artifactId>
            <version>${geoair.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 强制继承的依赖

⚠️ **重要**: 所有继承 `geoair-base-parent` 的模块会自动引入以下 4 个 BOM：

```xml
<dependencies>
    <dependency>
        <groupId>cn.geoair.devkit</groupId>
        <artifactId>geoair-base-dependencies-geotools</artifactId>
        <version>${geoair.version}</version>
        <type>pom</type>
    </dependency>

    <dependency>
        <groupId>cn.geoair.devkit</groupId>
        <artifactId>geoair-base-dependencies-spring</artifactId>
        <version>${geoair.version}</version>
        <type>pom</type>
    </dependency>

    <dependency>
        <groupId>cn.geoair.devkit</groupId>
        <artifactId>geoair-base-dependencies-swagger</artifactId>
        <version>${geoair.version}</version>
        <type>pom</type>
    </dependency>

    <dependency>
        <groupId>cn.geoair.devkit</groupId>
        <artifactId>geoair-base-dependencies-other</artifactId>
        <version>${geoair.version}</version>
        <type>pom</type>
    </dependency>
</dependencies>
```

#### 影响说明
- ✅ **优点**: 简化子模块配置，无需重复声明依赖版本
- ⚠️ **注意**: 即使只需要部分功能，也会引入全部 BOM（可通过 exclusion 排除）

## 🔧 构建插件配置

### 核心插件列表
| 插件名称 | 版本 | 核心作用 | 关键配置 |
|----------|------|----------|----------|
| `maven-compiler-plugin` | 3.10.1 | Java 编译 | source/target=1.8，encoding=UTF-8 |
| `maven-source-plugin` | 3.2.1 | 生成源码包 | attach-sources=true |
| `maven-javadoc-plugin` | 3.4.1 | 生成 API 文档 | encoding=UTF-8，支持中文 |
| `maven-release-plugin` | 3.1.0 | 版本发布管理 | 自动打 Tag，更新版本号 |
| `versions-maven-plugin` | 2.7 | 版本管理工具 | 批量更新模块版本 |
| `maven-jar-plugin` | 3.2.2 | JAR 包打包 | 统一打包规范 |

### 插件特性
- ✅ 仅引入版本管理，不强制依赖（按需启用）
- ✅ 适合外部项目复用配置
- ❌ 子模块不会自动继承插件配置（需显式声明）

## 🔄 继承关系

### 直接继承者
以下模块直接继承 `geoair-base-parent`，享受统一的依赖和构建配置：
1. **geoair-standard** - 框架标准库模块
2. **geoair-modules** - 业务功能组件模块
3. **geoair-dependencies-parent** - 工程级依赖管理父 POM
4. **geoair-base-dependencies-parent** - 基础依赖管理父 POM（间接继承）

### 完整继承链
```
geoair-framework (根 POM)
├── geoair-base-parent
│   ├── geoair-standard
│   │   ├── geoair-base       # 基础工具类
│   │   ├── geoair-core       # 核心功能
│   │   ├── geoair-web        # Web 相关
│   │   ├── geoair-sdk        # SDK 封装
│   │   ├── geoair-orm        # 数据访问层
│   │   └── geoair-tools      # 工具模块
│   └── geoair-modules
│       ├── geoair-apidoc         # API 文档模块
│       ├── geoair-code-generator # 代码生成器
│       ├── geoair-geo            # GIS 功能模块
│       ├── geoair-db-service     # 数据库服务模块
│       └── ... (其他业务模块)
└── geoair-dependencies-parent
    ├── geoair-api-parent             # API 模块父 POM
    ├── geoair-project-parent         # 项目级父 POM
    └── geoair-spring-boot-starter-parent # Starter 模块父 POM
```

## ⚠️ 注意事项

### 1. 依赖体积问题
- **问题**：强制引入 4 个 BOM 可能导致依赖体积偏大（即使仅需部分功能）
- **解决方案**：
    - 轻量级项目：单独引入所需的 BOM（不继承本 POM）
    - 按需排除：使用 `<exclusions>` 排除不需要的传递依赖
  ```xml
  <!-- 示例：排除不需要的 GeoTools 依赖 -->
  <dependency>
      <groupId>cn.geoair.devkit</groupId>
      <artifactId>geoair-base-dependencies-geotools</artifactId>
      <version>${geoair.version}</version>
      <type>pom</type>
      <exclusions>
          <exclusion>
              <groupId>org.geotools</groupId>
              <artifactId>gt-swing</artifactId>
          </exclusion>
      </exclusions>
  </dependency>
  ```

### 2. Spring Boot 版本约束
- **默认版本**：Spring Boot 2.7.18（框架稳定版本）
- **版本修改方案**：
    - 方案 A（推荐）：在子模块 POM 中覆盖版本属性
      ```xml
      <properties>
          <spring-boot.version>2.7.17</spring-boot.version>
      </properties>
      ```
    - 方案 B：不继承本 POM，单独导入所需 BOM

### 3. JDK 版本要求
- **强制要求**：JDK 8+（编译配置固定为 1.8）
- **不支持**：无法通过属性修改编译 JDK 版本（如需其他版本请单独配置编译器插件）

## 🔍 最佳实践

### 实践 1：选择合适的继承方式
| 使用场景 | 推荐方式 | 配置示例 |
|----------|----------|----------|
| 框架内部模块 | 直接继承 |
```xml
<parent>
    <groupId>cn.geoair.devkit</groupId>
    <artifactId>geoair-base-parent</artifactId>
    <version>J8.1.0-SNAPSHOT</version>
</parent>
```
| | 外部项目引用 | 仅导入 dependencyManagement | 
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
|

### 实践 2：按需引入具体依赖
即使继承了所有 BOM，实际使用时只需声明需要的依赖（无需指定版本）：
```xml
<!-- 使用 GeoTools 核心依赖（版本由 BOM 管理） -->
<dependency>
    <groupId>org.geotools</groupId>
    <artifactId>gt-main</artifactId>
</dependency>

<!-- 使用 Spring Boot Web 依赖（版本由 BOM 管理） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

### 实践 3：版本覆盖最佳实践
如需自定义依赖版本，建议在项目根 POM 中统一声明：
```xml
<properties>
    <!-- 覆盖 Spring Boot 版本 -->
    <spring-boot.version>2.7.20</spring-boot.version>
    <!-- 覆盖 GeoTools 版本 -->
    <geotools.version>28.7</geotools.version>
</properties>
```

## 📖 相关资源
- **Maven 依赖管理官方文档**: https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html
- **Spring Boot BOM 使用指南**: https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#using.build-systems.dependency-management
- **Maven Release 插件文档**: https://maven.apache.org/maven-release/maven-release-plugin/
- **GeoAir Framework 源码**: https://gitee.com/geoair/geoair

## 👥 开发者信息
- **作者**: 张逢吉
- **邮箱**: 1159856928@qq.com
- **组织**: geoair
- **官网**: https://xmt.geoair.cn/
- **Gitee 仓库**: https://gitee.com/geoair/geoair

## 📄 许可证
本项目采用 **Apache License 2.0** 开源许可证。详情请参见 [LICENSE](LICENSE) 文件。

---
**最后更新**: 2026-03-14  
**当前版本**: J8.1.0-SNAPSHOT
```
 