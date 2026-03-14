

# GeoAir API Components

## 项目简介

GeoAir API Components 是一个用于 API 文档自动生成的组件库，提供了对 Swagger/Knife4j 的深度集成支持。该项目包含两个主要的 Starter 模块，分别支持传统的 Swagger 2 和现代的 SpringDoc OpenAPI 3 规范。

## 项目结构

```
geoair-comp/
├── geoair-apidoc/
│   ├── geoair-knife4j-core/                           # Swagger 2 + Knife4j 集成核心模块
│   │   ├── src/main/java/cn/geoair/comp/knife4j/ext/
│   │   │   ├── auto/AutoApiConfig.java                # 自动配置类
│   │   │   ├── config/                                 # 配置属性类
│   │   │   │   ├── GirSwaggerApiConfig.java           # API配置接口
│   │   │   │   └── GirSwaggerProperties.java          # 配置属性
│   │   │   └── model/                                  # 数据模型
│   │   │       ├── ApiModelInfo.java                  # API模型信息
│   │   │       └── DocketInfo.java                     # 分组配置信息
│   │   └── pom.xml
│   │
│   ├── geoair-knife4j-springdoc-spring-boot-starter/  # SpringDoc OpenAPI 3 + Knife4j 集成
│   │   ├── src/main/java/cn/geoair/comp/knife4j/ext/springdoc/
│   │   │   ├── auto/                                   # 自动配置
│   │   │   │   └── SpringDocApiRunner.java            # 启动运行器
│   │   │   ├── builder/                                # 构建器
│   │   │   │   ├── GaModelFieldConverter.java         # 模型字段转换器
│   │   │   │   ├── GiResultModelConverter.java        # 结果模型转换器
│   │   │   │   ├── GiResultOperationConfig.java        # 操作配置
│   │   │   │   └── SpringDocCustomConfig.java         # 自定义配置
│   │   │   └── controller/                             # 控制器
│   │   │       └── GroupedApiDocsController.java       # 分组API文档控制器
│   │   ├── src/main/resources/static/                  # 静态资源
│   │   │   └── gtcapi/                                 # 前端资源
│   │   └── pom.xml
│   │
│   └── geoair-knife4j-spring-boot-demo/               # 演示项目
│       ├── src/main/java/cn/geoair/comp/knife4j/demo/
│       │   ├── config/Swagger2Configuration.java       # 配置示例
│       │   ├── controller/                              # 控制器示例
│       │   │   ├── group1/                             # 分组1
│       │   │   └── group2/                             # 分组2
│       │   └── model/                                  # 模型示例
│       └── pom.xml
│
└── pom.xml
```

## 核心功能

### geoair-knife4j-core
- **自动扫描包路径**：自动扫描 Controller 所在的根包
- **分组支持**：支持按包路径自动分组 API 文档
- **自定义配置**：提供灵活的配置属性自定义
- **Spring Boot 自动装配**：支持 Spring Boot Starter 自动配置

### geoair-knife4j-springdoc-spring-boot-starter
- **SpringDoc OpenAPI 3 支持**：基于 SpringDoc 生成 OpenAPI 3 规范的文档
- **Knife4j 前端集成**：提供 Knife4j 增强的前端界面
- **分组 API 文档**：支持多分组文档展示
- **自定义模型转换**：支持自定义响应模型转换
- **操作定制**：支持自定义操作行为

## 技术栈

- **Java 版本**：JDK 8+
- **构建工具**：Maven 3.6+
- **主要框架**：
  - Spring Boot 2.7.x
  - SpringDoc OpenAPI 3
  - Knife4j 4.x
- **API 规范**：
  - OpenAPI 3 (SpringDoc)
  - Swagger 2 (传统)

## 快速开始

### 环境要求

- JDK 8 或更高版本
- Maven 3.6 或更高版本

### 引入依赖

**SpringDoc 版本 (推荐)**：
```xml
<dependency>
   <groupId>cn.geoair.eight</groupId>
    <artifactId>geoair-knife4j-springdoc-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

**Swagger 2 版本**：
```xml
<dependency>
   <groupId>cn.geoair.eight</groupId>
    <artifactId>geoair-knife4j-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 配置说明

在 `application.yml` 中添加配置：

```yaml
geoair:
  apidoc:
    enable: true
    title: API 文档标题
    version: 1.0.0
    author: 作者
    description: API 描述信息
    controller-root-package: com.example.demo.controller
```

### 自定义配置

实现 `GirSwaggerApiConfig` 接口进行自定义配置：

```java
@Configuration
public class Swagger2Configuration implements GirSwaggerApiConfig {
    
    @Override
    public List<DocketInfo> getDocketInfos() {
        // 返回分组配置列表
    }
    
    @Override
    public ApiModelInfo getApiModelInfo() {
        // 返回API基本信息
    }
}
```

## 使用示例

### 控制器注解

```java
@Controller
@GaApi(tags = "用户管理")
public class UserController {
    
    @PostMapping("/user")
    @ResponseBody
    @GaApiAction(text = "创建用户")
    public GiResult<UserVo> createUser(@RequestBody UserVo user) {
        // ...
    }
}
```

### 模型注解

```java
@GaModel(text = "用户信息")
public class UserVo {
    
    @GaModelField(text = "用户名")
    private String username;
    
    @GaModelField(text = "邮箱")
    private String email;
}
```

## 访问文档

启动应用后，访问以下地址查看 API 文档：

- Knife4j 界面：`http://localhost:8080/doc.html`
- Swagger 原始文档：`http://localhost:8080/swagger-ui.html`
- OpenAPI 3 规范：`http://localhost:8080/v3/api-docs`

## 模块说明

| 模块 | 说明 |
|------|------|
| geoair-knife4j-core | Swagger 2 + Knife4j 集成核心 |
| geoair-knife4j-springdoc-spring-boot-starter | SpringDoc OpenAPI 3 + Knife4j 集成 |
| geoair-knife4j-spring-boot-demo | 演示项目 |

## 贡献指南

1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改
4. 推送到分支
5. 开启 Pull Request

## 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。