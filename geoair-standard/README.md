# GeoAir Standard — 标准基础库

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](../LICENSE)
[![JDK](https://img.shields.io/badge/JDK-8+-green.svg)](https://www.oracle.com/java/technologies/downloads/#java8)

## 📖 模块介绍

GeoAir Standard 是框架的**基础标准库**，采用 **SPI（Service Provider Interface）** 解耦架构设计：

- **`geoair-base`** — 接口/抽象层：定义纯 Java 接口，零外部依赖
- **`geoair-core`** — 实现层：基于 Spring 提供默认 SPI 实现
- **`geoair-web`** — Web 层公共组件（会话、权限、日志、MIME）
- **`geoair-orm`** — ORM 多框架集成抽象
- **`geoair-sdk`** — 统一 SDK 输出
- **`geoair-tools`** — 底层工具（方法分派、反射、集合等）

## 🗂️ 目录结构

```
geoair-standard/
├── geoair-base/        ← 接口定义层（158+ Java 文件）
│   ├── api/                注解 @GaApi / @GaApiAction
│   ├── bean/               Bean 容器抽象 GiBeanFactory
│   ├── cache/              缓存抽象 GiCache
│   ├── convert/            数据转换 GiConverter
│   ├── data/               数据模型（model/page/result/tuples）
│   ├── env/                环境配置 GiPropertier / GiEnvironmenter
│   ├── exception/          异常处理
│   ├── gpa/                通用持久化架构（DAO/Entity/ID生成）
│   ├── json/               JSON 抽象 GirJSON
│   ├── lang/               语言基础（调用者检测等）
│   ├── log/                日志抽象 GiLogger
│   ├── sp/                 SPI 加载机制 @GkSP + GirSpHelper
│   ├── tool/               内建工具（Console/ConsoleTable）
│   ├── util/               基础工具（AOP/Bean）
│   └── Gir.java            ！！！核心门面类 ！！！
│
├── geoair-core/         ← SPI 实现层（15 Java 文件）
│   └── cn/geoair/spi/
│       ├── bean/            SpringContextBean4Gir（Spring Bean 容器适配）
│       ├── cache/           Cache4Gir（JSR/Spring 缓存适配）
│       ├── convert/         类型转换
│       ├── env/             SpringEnvironment4Gir（环境/属性适配）
│       ├── json/            Json4Gir + 5 种 JSON 实现
│       ├── log/             Log4Gir + 3 种日志适配
│       ├── util/            ID 生成 / 泛型工具
│       └── web/             SpringServlet4Gir（Servlet 适配）
│
├── geoair-web/          ← Web 公共组件（38 Java 文件）
│   └── cn/geoair/web/
│       ├── session/         会话管理（Cookie/Token/Spring Session）
│       ├── permission/      权限模型
│       ├── log/             HTTP 请求/响应日志采集
│       ├── mime/            MIME 类型注册与解析（SPI 驱动）
│       ├── data/            Web 分页 / Web 结果
│       ├── util/            CORS / Cookie / Servlet 工具
│       ├── module/          模块化架构
│       └── enums/           HTTP 方法枚举
│
├── geoair-orm/          ← ORM 集成
│   ├── geoair-orm-spi/      抽象层（EntityResolve/Example/EntityHelper）
│   ├── geoair-orm-mybatis/  MyBatis 实现
│   ├── geoair-orm-mybatis-plus/ MyBatis-Plus 实现
│   ├── geoair-orm-mybatis-tk/  TK Mapper 实现
│   ├── geoair-orm-springjpa/   Spring Data JPA 实现
│   └── geoair-orm-base/     基础模块
│
├── geoair-sdk/          ← SDK 统一输出
└── geoair-tools/        ← 底层工具库
```

## 📐 命名规范

框架采用严格的前缀命名约定：

| 前缀 | 含义 | 示例 |
|------|------|------|
| `Ga*` | **An**notation 注解 | `@GaApi`, `@GaModel`, `@GkSP` |
| `Gi*` | **I**nterface 接口 | `GiCache`, `GiDao`, `GiResult` |
| `Gir*` | **I**mplementation + **R**ealization 实现/工具类 | `GirResult`, `GirSpHelper` |
| `Gutil*` | **Util**ity 工具类 | `GutilAop`, `GutilCookie` |
| `Gfun*` | **Fun**ction 函数接口 | `GfunPageExcute` |
| `Gk*` | **K**it 内建工具/数据结构 | `GkPair`, `GkConsole` |
| `Gem*` | **E**num **M**odel 枚举模型 | `GemBoolean` |

## 🔌 SPI 机制

框架的核心解耦机制是通过自定义 SPI 实现的：

```
应用代码
    ↓ 调用
Gir.property / Gir.log / Gir.beans ...   ← 门面入口
    ↓ 委托
GirPropertyHelper / GirLoggerFactory ...  ← Helper 层（@GaMethodHandDefine）
    ↓ 方法分派
GkMethodHand.invokeSelf()                 ← 方法句柄分派
    ↓ 查找实现
SpringEnvironment4Gir / Log4Gir ...       ← 实现层（@GaMethodHandImpl）
    ↓ 实际执行
Spring Environment / SLF4J / Jackson ...  ← 底层库
```

**关键设计:**
- `@GkSP` 注解标记需要 SPI 发现的服务接口
- `GirSpHelper.load(Class)` 按优先级链加载：BeanFactory → JDK ServiceLoader → PlaceHolder
- `GkMethodHand` 提供方法级别的动态绑定（编译时不依赖 Spring）
- 支持单例缓存、弱引用、线程安全

## 🧩 核心门面: Gir 类

```java
// 统一入口，无需关心底层实现
Gir.log.info("Hello");           // 日志 → 自动发现 SLF4J/Log4j/...
Gir.property.getProperty("key"); // 配置 → 自动适配 Spring Environment
Gir.beans.getBean(User.class);   // Bean  → 自动适配 Spring Context
Gir.toJson(obj);                 // JSON  → 自动选择 Jackson/FastJSON/...
Gir.println(obj);                // 控制台输出
Gir.printTable("a", "b", "c");   // 表格输出
```

## 📊 GPA 持久化架构

通过 Entity 接口组合实现类似 Active Record 的编程模型：

```
GiEntityable<PK>              基础实体
  ├── GiEntitySaveable         可新增 → entity.save()
  ├── GiEntityRemovable        可删除 → entity.removeSelf()
  ├── GiEntityAlterable        可更新 → entity.updateByPK()
  ├── GiEntityQueryable        可查询 → entity.queryBySelf()
  └── GiEntityVisuable         可视化查询
       └── GiCrudEntity       全 CRUD
            └── GiLogicCrudEntity  逻辑删除版
```

```java
// 实体即 DAO 的 Active Record 风格
User user = new User();
user.setName("张三");
user.save();  // 直接调用保存
```

## 🚀 快速开始

```xml
<!-- 引入整个 standard -->
<dependency>
    <groupId>cn.geoair.devkit</groupId>
    <artifactId>geoair-standard</artifactId>
    <version>J8-dev-SNAPSHOT</version>
    <type>pom</type>
</dependency>

<!-- 或按需引入 -->
<dependency>
    <groupId>cn.geoair.devkit</groupId>
    <artifactId>geoair-base</artifactId>
    <version>J8-dev-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>cn.geoair.devkit</groupId>
    <artifactId>geoair-core</artifactId>
    <version>J8-dev-SNAPSHOT</version>
</dependency>
```

## 👥 开发者

- **作者**: 张逢吉
- **邮箱**: zhangjun7570@qq.com
- **组织**: GeoAir
- **官网**: https://xmt.geoair.cn/

## 📄 许可证

Apache License 2.0 — 详见 [LICENSE](../LICENSE)
