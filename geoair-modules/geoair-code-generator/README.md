# GeoAir ApiDoc 模块使用指南

## 模块介绍

GeoAir ApiDoc 模块提供了 API 文档生成功能，基于 Knife4j 实现，支持 Swagger 2.0 和 OpenAPI 3.0 规范，为 RESTful API 提供了美观、实用的文档界面。

## 目录结构

```
goair-apidoc/
├── geoair-knife4j-core/                    # Knife4j 核心模块
├── geoair-knife4j-spring-boot-demo/        # Knife4j Spring Boot 示例
└── geoair-knife4j-springdoc-spring-boot-starter/ # Knife4j SpringDoc Starter
```

## 模块说明

### 1. geoair-knife4j-core

Knife4j 核心模块，提供了 API 文档生成的核心功能，包括配置管理、文档生成等。

### 2. geoair-knife4j-spring-boot-demo

Knife4j Spring Boot 示例模块，展示了如何在 Spring Boot 项目中集成 Knife4j。

### 3. geoair-knife4j-springdoc-spring-boot-starter

Knife4j SpringDoc Starter 模块，提供了与 SpringDoc 集成的启动器，支持 OpenAPI 3.0 规范。

## 快速开始

### 1. 引入依赖

在 Maven 项目中，添加以下依赖：

```xml
<!-- SpringDoc 版本（推荐） -->
<dependency>
    <groupId>cn.geoair.devkit</groupId>
    <artifactId>geoair-knife4j-springdoc-spring-boot-starter</artifactId>
    <version>J17.1.6</version>
</dependency>
```

### 2. 配置

在 application.yml 或 application.properties 中添加配置：

```yaml
springdoc:
  api-docs:
    enabled: true
  swagger-ui:
    enabled: true

knife4j:
  enable: true
  setting:
    language: zh_cn
```

### 3. 使用

在控制器类上添加 Swagger 注解：

```java
@RestController
@RequestMapping("/api")
@Tag(name = "示例接口", description = "示例接口描述")
public class DemoController {

    @Operation(summary = "获取示例数据", description = "获取示例数据的详细描述")
    @GetMapping("/demo")
    public ResponseEntity<String> getDemo() {
        return ResponseEntity.ok("Hello, Knife4j!");
    }
}
```

### 4. 访问文档

启动应用后，访问以下地址查看 API 文档：
- Swagger UI: http://localhost:8080/swagger-ui.html
- Knife4j UI: http://localhost:8080/doc.html

## 功能特性

- 支持 Swagger 2.0 和 OpenAPI 3.0 规范
- 提供美观的文档界面
- 支持接口测试
- 支持文档导出（PDF、Markdown 等）
- 支持接口分组
- 支持参数验证

## 依赖关系

- **geoair-knife4j-core**：核心功能模块
- **geoair-knife4j-springdoc-spring-boot-starter**：SpringDoc 集成模块，依赖于 geoair-knife4j-core
- **geoair-knife4j-spring-boot-demo**：示例模块，依赖于 geoair-knife4j-springdoc-spring-boot-starter

## 版本历史

- J17.1.6：当前开发版本

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
- 邮箱：zfj20250104@qq.com
- 组织：geoair
- 官网：https://xmt.geoair.cn/
