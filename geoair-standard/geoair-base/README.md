# GeoAir Base 模块使用指南

## 模块介绍

GeoAir Base 是 GeoAir 框架的基础核心模块，提供了框架的核心功能，包括 API 注解、Bean 管理、缓存、转换、数据模型、分页、结果处理、环境配置、异常处理、GPA（通用持久化架构）、JSON 处理、SPI 加载和工具类等。

## 目录结构

```
goair-base/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── cn/geoair/base/ # 基础核心代码
│   │   │       ├── api/         # API 注解
│   │   │       ├── bean/        # Bean 管理
│   │   │       ├── cache/       # 缓存
│   │   │       ├── convert/     # 转换
│   │   │       ├── data/        # 数据模型
│   │   │       ├── env/         # 环境配置
│   │   │       ├── exception/   # 异常处理
│   │   │       ├── gpa/         # 通用持久化架构
│   │   │       ├── json/        # JSON 处理
│   │   │       ├── sp/          # SPI 加载
│   │   │       ├── util/        # 工具类
│   │   │       └── Gir.java     # 核心类
│   │   └── resources/ # 资源文件
│   └── test/ # 测试代码
├── target/ # 构建输出
├── README.md
└── pom.xml # Maven 配置
```

## 模块说明

### 1. api 包

- **annotation**：API 注解，包括 `@GaApi` 和 `@GaApiAction` 等。

### 2. bean 包

- **GaBeanConfig**：Bean 配置类
- **GiBeanFactory**：Bean 工厂接口
- **GirBeanHelper**：Bean 助手类
- **各种异常类**：如 `GirBeanException`、`GirNoSuchBeanException` 等

### 3. cache 包

- **GiCache**：缓存接口
- **GirCacheHelper**：缓存助手类
- **support**：缓存实现，如 `GirMemoryCache`、`GirMemoryCacheManager` 等

### 4. convert 包

- **GiConvertable**：可转换接口
- **GiConverter**：转换器接口
- **GiConverterProvider**：转换器提供者接口
- **GirConverterFactory**：转换器工厂类
- **support**：转换实现，如 `GirConverterImpl`
- **util**：转换工具，如 `GirConvertHelper`

### 5. data 包

#### 5.1 common
- 通用数据类型，如 `GemBoolean`、`GemDatePattern`、`GemStatus` 等

#### 5.2 model
- **annotation**：模型注解，如 `@GaModel`、`@GaModelField`
- **applyer**：模型应用器，如 `GiModelApplyer`、`GiModelFieldApplyer`
- **attribute**：属性提供者，如 `GiAttributeProvider`、`GiAttributeable`
- **support**：模型支持类，如 `GirModelKid`、`GirTypeModelKid` 等
- **各种模型接口**：如 `GiModelable`、`GiTypeModelable`、`GiVisualModelable` 等

#### 5.3 page
- **分页接口**：如 `GiPageParam`、`GiPager`、`GiPageExcuter` 等
- **support**：分页支持类，如 `GirPageConfig`、`GirPager` 等

#### 5.4 result
- **结果接口**：如 `GiResult`、`GiResultCode`、`GiResultConfig` 等
- **support**：结果支持类，如 `GirResult`、`GirResultCode` 等

#### 5.5 support
- 支持类，如 `GirGroupKid`、`GirValueKid` 等

#### 5.6 tuples
- 元组类，如 `GkPair`、`GkTriplet`、`GkQuartet` 等

#### 5.7 其他
- **GiDescriptive**：可描述接口
- **GiGroup**：组接口
- **GiType**：类型接口
- **GiValuable**：可取值接口
- **GiVisuable**：可可视化接口
- **GirValidateException**：验证异常类

### 6. env 包

- **GiEnvironmenter**：环境接口
- **GiPropertier**：属性接口
- **GirEnvironmentHelper**：环境助手类
- **GirPropertyHelper**：属性助手类
- **support**：环境支持类，如 `GirSystemEnvironmentOffice`、`GirSystemPropertierOffice` 等

### 7. exception 包

- **GirExceptionResultConverter**：异常结果转换器

### 8. gpa 包

#### 8.1 annotation
- 注解，如 `@GaGenericGenerator`、`@GaGenericGenerators`

#### 8.2 common
- 通用类，如 `GirEmModelApply`

#### 8.3 dao
- DAO 接口，如 `GiDao`、`GiEntityDao`、`GiCreateDao`、`GiRetrieveDao`、`GiUpdateDao`、`GiDeleteDao` 等

#### 8.4 entity
- 实体接口，如 `GiEntityable`、`GiCrudEntity`、`GiLogicCrudEntity` 等

#### 8.5 id
- ID 生成器，如 `GiEntityIdGenerator`、`GiGenId`、`GirIdGenerator`

#### 8.6 section
- 区间查询，如 `SectionDao`、`DateSectionDao`、`SectionModel`、`DateSectionModel`

#### 8.7 support
- 支持类，如 `GirOrder`、`GirSort`

### 9. json 包

- **GirJSON**：JSON 处理类
- **GirJSONException**：JSON 异常类

### 10. sp 包

- **GkSP**：SPI 注解
- **GirSpHelper**：SPI 助手类
- **GkSpLoader**：SPI 加载器接口
- **support**：SPI 加载器实现，如 `GirBeanFactorySpLoader`、`GirCacheSpLoader` 等

### 11. util 包

- **GutilAop**：AOP 工具类
- **GutilBean**：Bean 工具类

### 12. Gir.java

- 核心类，提供了框架的核心功能

## 快速开始

### 1. 引入依赖

在 Maven 项目中，添加以下依赖：

```xml
<dependency>
    <groupId>cn.geoair.devkit</groupId>
    <artifactId>geoair-base</artifactId>
    <version>J8.1.5</version>
</dependency>
```

### 2. 使用

#### 2.1 使用 Bean 管理

```java
// 获取 Bean
Object bean = GirBeanHelper.getBean("beanName");

// 获取指定类型的 Bean
UserService userService = GirBeanHelper.getBean(UserService.class);
```

#### 2.2 使用缓存

```java
// 获取缓存
GiCache cache = GirCacheHelper.getCache();

// 使用缓存
cache.put("key", "value");
Object value = cache.get("key");
```
#  geoair-base    开发标准库

#### 2.3 使用结果处理
该工程作为基础在项目中应用，封装了代码约束和一些常用解耦api

```java
// 创建成功结果
GiResult successResult = GirResult.success("操作成功", data);

// 创建失败结果
GiResult errorResult = GirResult.error("操作失败");
```
## 目录说明：

#### 2.4 使用分页
* bean  容器Bean 提供从容器获取bean的工具
  * 提供容器获取bean方法，使用者不用关心具体实现是由spring容器还是其他容器提供bean服务;
  * 调用常用方法获取Bean   GirBeanHelper.getBean(...);
* cache 缓存
  * 提供对缓存的使用。使用者不用关心具体实现是使用的何种缓存组件，由动态适配去实现。
  * 调用常用方法   GirCacheHelper.xxx(...);
* env  提供环境和属性的读取
  * 提供对环境访问的api，由动态适配去实现。
  * 调用常用方法   GirPropertyHelper.xxx(...);
* convert   数据转换
  * 提供通用数据转换接口
* data  数据 提供模型数据规范

```java
// 创建分页参数
GiPageParam pageParam = new GirPageParam(1, 10);
* lang   基础工具

// 执行分页查询
GiPager<?> pager = GirPagerProvider.getPager();
pager.setPageParam(pageParam);
pager.execute();
* log  日志封装

// 获取分页结果
List<?> dataList = pager.getDataList();
long total = pager.getTotal();
```
* gpa  持久化定义

## 功能特性
* user   用户定义，包含用户会话，权限

- 提供了丰富的基础组件和工具类
- 支持 SPI 机制，便于扩展
- 提供了多种数据模型接口
- 支持分页查询
- 支持结果统一处理
- 提供了通用持久化架构
- 支持 JSON 处理
- 提供了环境配置管理
- 提供了异常处理机制
* util   常用静态工具类

## 依赖关系
*  gir   提供常用工具方法

- **geoair-base** 是框架的基础模块，被其他模块依赖
## 其他：

## 版本历史

- J8.1.5：当前开发版本

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
- 邮箱：zhangjun7570@qq.com
- 组织：geoair
- 官网：https://xmt.geoair.cn/

* 对于其他可以抽象的标准实现，可以在该工程下添加更多的规范和通用接口
