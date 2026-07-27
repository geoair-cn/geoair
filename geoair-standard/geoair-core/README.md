# GeoAir Core 模块使用指南

## 模块介绍

GeoAir Core 是 GeoAir 框架的 **Spring / runtime 侧桥接实现模块**，主要职责不是定义公共 API，而是把 `geoair-base` / `geoair-tools` 暴露出来的 facade/helper 在 Spring 运行时环境下接到具体实现上。

换句话说：
- `geoair-base` / `geoair-tools`：定义 facade、接口、工具入口
- `geoair-core`：提供 bridge / provider / SPI runtime 实现

## 目录结构

```
geoair-core/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── cn/geoair/
│   │   │       ├── core/ # 示例/测试性核心代码
│   │   │       └── spi/  # runtime bridge / provider 实现
│   │   │           ├── bean/       # Spring Bean 容器桥接
│   │   │           ├── cache/      # Spring / JSR cache 桥接
│   │   │           ├── convert/    # convert provider bridge
│   │   │           ├── env/        # Spring environment / property 桥接
│   │   │           ├── json/       # JSON backend provider / bridge
│   │   │           ├── log/        # 日志 backend provider / bridge
│   │   │           ├── util/       # Spring AOP / 泛型 / ID 等 bridge
│   │   │           └── web/        # Spring Web context bridge
│   │   └── resources/
│   │       └── META-INF/
│   │           └── spring.factories # Spring 自动装配入口
│   └── test/
├── target/
├── README.md
└── pom.xml
```

## 模块说明

### 1. bridge / provider 实现

#### 1.1 bean 包

- **SpringContextBean4Gir**：Spring Bean 容器桥接，实现 `GiBeanFactory` 的运行时 provider。
- **SpringBeanProviderResolver**：provider 解析器，帮助 `GirBeanHelper` 走 direct fast path。

#### 1.2 cache 包

- **Cache4Gir**：缓存 bridge，负责在 Spring Cache / JSR Cache / fallback 之间分发。
- **SpringCacheManagerProvider**：Spring `CacheManager` 获取 provider。

#### 1.3 convert 包

- **GirConverterProviderBridge**：convert runtime bridge，把 `GiConverterProvider` 接回 base facade `GirConvertHelper`。

#### 1.4 env 包

- **SpringEnvironment4Gir**：Spring environment / property bridge，同时实现 `GiEnvironmenter` 与 `GiPropertier`。
- **SpringEnvironmentProviderResolver**：provider 解析器，帮助 `GirEnvironmentHelper` / `GirPropertyHelper` 走 direct fast path。

#### 1.5 json 包

- **GirFastJson**：FastJson backend 适配实现。
- **GirFastJson2**：FastJson2 backend 适配实现。
- **GirGsonJson**：Gson backend 适配实现。
- **GirHutoolJson**：Hutool JSON backend 适配实现。
- **GirJacksonJson**：Jackson backend 适配实现。
- **JacksonObjectMapperProvider**：Jackson `ObjectMapper` provider。
- **JsonProviderResolver**：JSON backend 探测 resolver。
- **Json4Gir**：`GirJSON` 的 runtime provider bridge / backend dispatcher。

#### 1.6 log 包

- **LogProviderResolver**：日志 backend 探测 resolver。
- **Log4Gir**：`GirLoggerFactory` / `GirLogger` 的 runtime provider bridge / backend dispatcher。

#### 1.7 util 包

- **SpringGenericTypeBridge**：Spring 泛型桥接工具，提供基于 Spring `GenericTypeResolver` 的泛型类型解析能力。
- **GspIdGenerator4Gir**：ID bridge，把 `GirIdGenerator` 接到具体实现上。
- **SpringBeanCopyProvider4Gir**：Spring Bean 拷贝 provider，接入 `GutilBean.copyProperties(...)` fast path。
- **SpringAopProvider4Gir**：Spring AOP provider，接入 `GutilAop` fast path。

#### 1.8 web 包

- **SpringWebContextBridge**：Spring Web 上下文桥接实现，提供基于 Spring 请求上下文的 Web 能力接入。

## 快速开始

### 1. 引入依赖

在 Maven 项目中，添加以下依赖：

```xml
<dependency>
    <groupId>cn.geoair.devkit</groupId>
    <artifactId>geoair-core</artifactId>
    <version>J17-dev-SNAPSHOT</version>
</dependency>
```

该模块主要作为 `geoair-base` / `geoair-tools` facade 的 Spring/runtime 适配实现。

### 2. 运行方式理解

#### 2.1 JSON 处理

通常不直接 new `Json4Gir`，而是通过 facade：

```java
GirJSON json = GirJSON.toJson(obj);
String text = json.toJSONString();
```

其中：
- facade：`GirJSON`
- runtime bridge：`Json4Gir`
- 具体 backend：`GirJacksonJson` / `GirFastJson` / `GirGsonJson` / ...

#### 2.2 缓存处理

通常通过 facade：

```java
GiCache cache = GirCacheHelper.getCache("demo");
cache.put("key", "value");
Object value = cache.getObject("key");
```

#### 2.3 日志处理

通常通过 facade：

```java
GiLogger log = GirLoggerFactory.getLogger("demo");
log.info("This is an info message");
log.error("This is an error message");
```

## 功能特性

- 为 facade/helper 提供 Spring/runtime 侧 bridge
- 支持多种 JSON backend
- 支持多种日志 backend
- 支持 Spring Cache / JSR Cache 适配
- 支持 Spring Bean / Environment / Property 适配
- 支持 Spring Web context 适配
- 支持泛型解析 / ID / AOP / Bean copy 等运行时能力接入

## 依赖关系

- **geoair-core** 依赖于 **geoair-base**
- 运行时主要为 `geoair-base` / `geoair-tools` 中的 facade 提供实现

## 版本历史

- J17-dev-SNAPSHOT：当前开发版本

## 许可证

本项目采用 Apache License 2.0 许可证，详见 [LICENSE](LICENSE) 文件。

## 联系方式

- 开发者：张逢吉
- 邮箱：zfj20250104@qq.com
- 组织：geoair
- 官网：https://xmt.geoair.cn/
