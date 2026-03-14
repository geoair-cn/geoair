# GeoAir Code Generator 模块使用指南

## 模块介绍

GeoAir Code Generator 模块提供了代码生成功能，支持从数据库表结构生成实体类、Mapper、Service 等代码，大大提高了开发效率。

## 目录结构

```
goair-code-generator/
├── geoair-code-gen-demo/    # 代码生成示例
└── geoair-code-gen-module/  # 代码生成核心模块
```

## 模块说明

### 1. geoair-code-gen-module

代码生成核心模块，提供了代码生成的核心功能，包括模板管理、代码生成等。

### 2. geoair-code-gen-demo

代码生成示例模块，展示了如何使用代码生成功能。

## 快速开始

### 1. 引入依赖

在 Maven 项目中，添加以下依赖：

```xml
<dependency>
    <groupId>cn.geoair.devkit</groupId>
    <artifactId>geoair-code-gen-module</artifactId>
    <version>J8.1.0-SNAPSHOT</version>
</dependency>
```

### 2. 配置

创建代码生成配置类：

```java
public class CodeGenConfig {
    public static void main(String[] args) {
        GirGeneratorConfig config = new GirGeneratorConfig();
        config.setJdbcUrl("jdbc:mysql://localhost:3306/test");
        config.setUsername("root");
        config.setPassword("123456");
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setBasePackage("com.example");
        config.setModuleName("demo");
        config.setTableName("user");
        
        GirGenerator generator = new GirGenerator(config);
        generator.generate();
    }
}
```

### 3. 运行

运行配置类，生成代码。生成的代码会按照以下结构组织：

```
com.example.demo/
├── controller/    # 控制器
├── service/       # 服务层
├── dao/           # 数据访问层
├── model/         # 实体类
└── vo/            # 值对象
```

## 功能特性

- 支持从数据库表结构生成代码
- 支持多种 ORM 框架（MyBatis、MyBatis-Plus 等）
- 支持自定义模板
- 支持生成 Controller、Service、Dao、Model、VO 等代码
- 支持代码自动覆盖或跳过

## 依赖关系

- **geoair-code-gen-module**：核心功能模块
- **geoair-code-gen-demo**：示例模块，依赖于 geoair-code-gen-module

## 版本历史

- J8.1.0-SNAPSHOT：当前开发版本

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
 