# GeoAir Core 模块使用指南

## 模块介绍

GeoAir Core 是 GeoAir 框架的核心功能模块，提供了各种 SPI（Service Provider Interface）实现，为框架的其他模块提供基础支持。

## 目录结构

```
goair-core/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── cn/geoair/
│   │   │       ├── core/ # 核心代码
│   │   │       └── spi/  # SPI 实现
│   │   │           ├── bean/       # Bean 管理 SPI
│   │   │           ├── cache/      # 缓存 SPI
│   │   │           ├── convert/    # 转换 SPI
│   │   │           ├── env/        # 环境 SPI
│   │   │           ├── json/       # JSON 处理 SPI
│   │   │           ├── log/        # 日志 SPI
│   │   │           ├── util/       # 工具类 SPI
│   │   │           └── web/        # Web 相关 SPI
│   │   └── resources/
│   │       └── META-INF/
│   │           └── spring.factories # Spring 自动配置
│   └── test/ # 测试代码
├── target/ # 构建输出
├── README.md
└── pom.xml # Maven 配置
```

## 模块说明

### 1. SPI 实现

#### 1.1 bean 包

- **SpringContextBean4Gir**：Spring 上下文 Bean 管理实现，提供了基于 Spring 上下文的 Bean 管理功能。

#### 1.2 cache 包

- **Cache4Gir**：缓存实现，支持多种缓存类型，包括 JSR 缓存和 Spring 缓存。

#### 1.3 convert 包

- **GirConvertHelper**：转换助手，提供了类型转换功能。

#### 1.4 env 包

- **SpringEnvironment4Gir**：Spring 环境实现，提供了基于 Spring 环境的配置管理功能。

#### 1.5 json 包

- **GirFastJson**：FastJson 实现，提供了基于 FastJson 的 JSON 处理功能。
- **GirGsonJson**：Gson 实现，提供了基于 Gson 的 JSON 处理功能。
- **GirHutoolJson**：Hutool JSON 实现，提供了基于 Hutool 的 JSON 处理功能。
- **GirJacksonJson**：Jackson 实现，提供了基于 Jackson 的 JSON 处理功能。
- **Json4Gir**：JSON 处理接口，定义了 JSON 处理的标准接口。

#### 1.6 log 包

- **Log4Gir**：日志实现，支持多种日志类型，包括 Apache Commons Log、Hutool Log 和 Slf4j Log。

#### 1.7 util 包

- **GenericTypeUtil4Gir**：泛型类型工具，提供了泛型类型的处理功能。
- **GspIdGenerator4Gir**：ID 生成器，提供了 ID 生成功能。

#### 1.8 web 包

- **SpringServlet4Gir**：Spring Servlet 实现，提供了基于 Spring Servlet 的 Web 功能。

## 快速开始

### 1. 引入依赖

在 Maven 项目中，添加以下依赖：
#  geoair-core    开发基础库的适配实现

```xml
<dependency>
    <groupId>cn.geoair.devkit</groupId>
    <artifactId>geoair-core</artifactId>
    <version>J8.1.4</version>
</dependency>
```
该工程作为基础开发库的解耦实现，给 geoair-base库提供了基础能力

### 2. 使用

#### 2.1 使用 JSON 处理
## 目录说明：

```java
// 使用 Jackson 处理 JSON
Json4Gir json4Gir = new GirJacksonJson();
String json = json4Gir.toJson(obj);
Object obj = json4Gir.fromJson(json, Object.class);
```
* core  工具目录
* spi  SPI方式实现 geoair-base的能力

#### 2.2 使用缓存

```java
// 使用缓存
Cache4Gir cache = new Cache4Gir();
cache.put("key", "value");
Object value = cache.get("key");
```

#### 2.3 使用日志
## 其他：

```java
// 使用日志
Log4Gir log = new Log4Gir();
log.info("This is an info message");
log.error("This is an error message");
```

## 功能特性

- 提供了多种 SPI 实现
- 支持多种 JSON 处理库
- 支持多种缓存类型
- 支持多种日志类型
- 提供了基于 Spring 的集成
- 提供了泛型类型处理功能
- 提供了 ID 生成功能

## 依赖关系

- **geoair-core** 依赖于 **geoair-base**

## 版本历史

- J8.1.4：当前开发版本

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

* 对于其他通用工具，可在此处添加
