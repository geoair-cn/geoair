## 模块定位

`geoair-base` 是标准基础库中最底层的一层，负责定义抽象接口、结果模型、环境访问辅助类、持久化抽象、分页模型以及统一门面入口。

它有两个很明确的约束：

1. **只定义接口、工具和抽象约束**
2. **不在这里放具体实现，不引入除 `javax` 之外的重型第三方依赖**

也就是说，`geoair-base` 更像“规则层”和“抽象层”，不是功能实现层。

## 设计原则

### 1. 上层只接触统一 API

例如 ORM 脱耦风格中，应用层只接触 `gtc*` 风格的方法，而不直接耦合具体 ORM：

```java
default int gtcUpdateByPK(List<T> records) {
    if (GutilObject.isNotEmpty(records)) {
        for (T record : records) {
            gtcUpdateByPK(record);
        }
        return records.size();
    }
    return 0;
}
```

这种做法的核心思想就是：

- 基础能力先抽到 `base`
- 具体框架行为放到 `orm` 或其他实现层
- 应用层只依赖统一抽象 API

### 2. 抽象与实现分层

- `geoair-base`：接口、模型、工具、抽象约束
- `geoair-core`：Spring、JSON、日志、缓存等默认实现
- `geoair-orm`：把不同 ORM 框架接回统一持久化 API

## 包级设计说明

### api

这一层放 API 注解，例如：

- `@GaApi`
- `@GaApiAction`

它的职责是把接口层需要的注解约定放在最基础层，而不是散落到各个业务模块。

### bean

这一层负责 Bean 获取抽象。

核心入口：

- `GiBeanFactory`
- `GirBeanHelper`

它的价值在于：

- 上层不直接依赖 Spring BeanFactory
- 只通过 `GirBeanHelper` 或 `GiBeanFactory` 取 Bean
- 具体容器实现放到 `core`

### cache

这一层负责缓存抽象。

核心入口：

- `GiCache`
- `GirCacheHelper`

它的价值在于：

- 上层只面向缓存抽象
- 不关心底层到底是内存缓存、Spring Cache 还是别的实现

### env

这一层负责环境与属性访问抽象。

核心入口：

- `GiEnvironmenter`
- `GiPropertier`
- `GirEnvironmentHelper`
- `GirPropertyHelper`

它的价值在于：

- 把配置读取从具体框架里抽出来
- 应用层不用直接耦合 Spring Environment

### convert

这一层负责通用转换抽象。

核心入口：

- `GiConverter`
- `GiConverterProvider`
- `GirConvertHelper`
- `GirConverterFactory`

它的意义是把“对象之间怎么转”抽成统一接口。

### data

这一层是 `geoair-base` 里体量最大的一块，主要负责模型与结果结构。

#### result

核心入口：

- `GiResult`
- `GirResult`
- `GiResultCode`
- `GiResultConfig`

这是统一结果模型层，很多模块的返回结构都会围绕它组织。

#### page
n
核心入口：

- `GiPageParam`
- `GiPager`
- `GirPageParam`
- `GirPager`

这是分页抽象层，用来统一“分页参数怎么描述、分页执行器怎么组织”。

#### model

核心入口：

- `GiModelable`
- `GiTypeModelable`
- `GiVisualModelable`
- `@GaModel`
- `@GaModelField`

这是模型抽象层，负责模型元信息和模型约束。

#### tuples

核心入口：

- `GkPair`
- `GkTriplet`
- `GkQuartet`
- 更多元组值对象

这是轻量数据结构层。

### gpa

这一层是持久化抽象层，也是统一 `gtc*` API 最重要的来源之一。

核心入口：

- `GiDao`
- `GiCreateDao`
- `GiRetrieveDao`
- `GiUpdateDao`
- `GiDeleteDao`
- `GiEntityable`
- `GiCrudEntity`
- `GiLogicCrudEntity`

它的价值在于：

- 在 `base` 里只定义统一持久化操作接口
- 让上层只接触统一的 `gtc*` 方法
- 具体是 JPA / MyBatis / MP / TK 去实现这些接口，放到 `orm` 层

### json

这一层负责 JSON 抽象。

核心入口：

- `GirJSON`
- `GirJSONException`

这意味着上层可以面向统一 JSON 接口，而不直接依赖某个 JSON 实现。

### sp

这一层负责 SPI 抽象与加载机制。

核心入口：

- `GkSP`
- `GirSpHelper`
- `GkSpLoader`

这层的作用是：

- 让实现类发现机制统一
- 让 `base` 能保持抽象，而不直接持有实现类依赖

### util

这一层放基础工具入口，例如：

- `GutilAop`
- `GutilBean`

### Gir

`Gir` 是统一门面类。

它的意义不是自己实现所有能力，而是作为：

- Bean
- 缓存
- 环境
- JSON
- 日志

这些基础能力的统一调用入口。

## GitHub 源码入口

- 模块目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-base`
- bean 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-base/src/main/java/cn/geoair/base/bean`
- cache 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-base/src/main/java/cn/geoair/base/cache`
- env 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-base/src/main/java/cn/geoair/base/env`
- page 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-base/src/main/java/cn/geoair/base/data/page`
- gpa 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-base/src/main/java/cn/geoair/base/gpa`
- result 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-base/src/main/java/cn/geoair/base/data/result`
- sp 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-base/src/main/java/cn/geoair/base/sp`

## 阅读建议

建议顺序：

1. `Gir`
2. `bean / cache / env`
3. `data/result`
4. `data/page`
5. `gpa`
6. `sp`

如果当前最关心“为什么应用层只用接触统一 `gtc*` API”，可以重点看：

- `gpa`
- `GiDao`
- `GiEntityable`
- 再到 `geoair-orm` 里看具体 ORM 是怎么把这些抽象接起来的。
