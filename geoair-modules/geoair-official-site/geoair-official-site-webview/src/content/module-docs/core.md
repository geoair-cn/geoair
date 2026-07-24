## 模块定位

`geoair-core` 是 `geoair-base` 的默认实现层，负责把 `base` 中定义的抽象接口接到可运行的实现上。

如果把整个标准基础库分成两层来理解：

- `geoair-base`：定义抽象接口、统一门面、调用约定
- `geoair-core`：提供默认实现，并把这些实现挂到 `base` 的门面入口上

那么 `core` 的职责就非常明确：

> 不是重新定义能力，而是把 `base` 里已经定义好的能力真正实现出来，并让上层项目能“默认可用”。

## 为什么需要 core 这一层

如果没有 `core`，`base` 中虽然已经有：

- `GirBeanHelper`
- `GirPropertyHelper`
- `GirEnvironmentHelper`
- `GirLoggerFactory`
- `GirJSON`

这些统一入口，但它们本身并不直接知道：

- Bean 从哪里取
- 配置从哪里读
- 日志往哪里写
- JSON 用哪套库序列化

`core` 的作用就是把这些抽象入口接到默认实现上，并且尽量不让应用层直接依赖底层实现细节。

所以应用层往往只需要：

```java
Gir.beans.getBean(UserService.class);
Gir.property.getProperty("spring.datasource.url");
Gir.log.info("hello");
GirJSON json = GirJacksonJson.toJson(obj);
```

而不需要在业务代码里反复写：

- Spring BeanFactory
- Spring Environment
- SLF4J
- Jackson / Fastjson / Gson

## 整体设计思路

`geoair-core` 的设计不是简单地“写几个实现类”，而是包含三层：

1. **抽象入口在 base**
2. **默认实现类在 core**
3. **通过 `GkMethodHand + @GaMethodHandImpl` 挂到 helper 上**

换句话说：

- `base` 只管定义“这里应该有一个 provider”
- `core` 负责提供“默认的 provider”
- 上层项目如果不想使用默认 provider，也可以再按同一套约定替换掉它

这套设计让 `core` 既能作为默认实现存在，又不会把整个系统重新耦死在默认实现上。

## 四条最重要的 SPI 默认实现链

### 1. Bean 封装：SpringContextBean4Gir

核心类：

- `SpringContextBean4Gir`

它实现：

- `GiBeanFactory`
- `ApplicationContextAware`
- `BeanFactoryPostProcessor`

这一层的目标是把 Spring 的 Bean 容器接到 `geoair-base` 里定义的 Bean 抽象上。

关键点：

- 业务层不直接依赖 `ApplicationContext`
- 统一通过 `GirBeanHelper` / `GiBeanFactory` 取 Bean
- 默认实现由 `SpringContextBean4Gir` 提供

在类加载时，它会执行：

```java
static {
    GkMethodHand.implFromClass(SpringContextBean4Gir.class);
}
```

同时通过：

```java
@GaMethodHandImpl(
    implClass = GirBeanHelper.class,
    implMethod = "getProvider",
    type = ImplType.expectfirst
)
private static GiBeanFactory getProvider() {
    return beanProvider;
}
```

把自己的实现挂到 `GirBeanHelper.getProvider()` 对应的约定点上。

这意味着：

- `base` 里 `GirBeanHelper` 并不知道 Spring
- `core` 通过 method hand 机制，把 Spring 版 provider 接了进去
- 应用层仍然只需要调统一入口

### 2. 环境变量 / 配置封装：SpringEnvironment4Gir

核心类：

- `SpringEnvironment4Gir`

它同时实现：

- `GiPropertier`
- `GiEnvironmenter`

这条链对应的是：

- `GirPropertyHelper`
- `GirEnvironmentHelper`

默认能力来源是 Spring 的：

- `ApplicationContext`
- `Environment`

这层主要负责：

- `getProperty(...)`
- `getRequiredProperty(...)`
- `containsProperty(...)`
- `resolvePlaceholders(...)`
- `getActiveProfiles()`
- `getDefaultProfiles()`
- `containsProfile(...)`

在类加载时，它同样会先执行：

```java
static {
    GkMethodHand.implFromClass(SpringEnvironment4Gir.class);
}
```

然后通过两个实现挂载：

```java
@GaMethodHandImpl(
    implClass = GirPropertyHelper.class,
    implMethod = "getPropertier",
    type = ImplType.expectfirst
)
private static GiPropertier getPropertier() {
    return me;
}

@GaMethodHandImpl(
    implClass = GirEnvironmentHelper.class,
    implMethod = "getEnvironmenter",
    type = ImplType.expectfirst
)
private static GiEnvironmenter getEnvironmenter() {
    return me;
}
```

所以最终：

- `Gir.property.getProperty(...)`
- `GirEnvironmentHelper.getEnvironmenter()`

能够回到 Spring `Environment`。

### 3. 日志封装：Log4Gir

核心类：

- `Log4Gir`
- `GirLogger`
- `GirLoggerFactory`

这条链的亮点不是“把日志包一层”，而是：

- 默认实现不是写死某一个日志框架
- 会按当前 classpath 自动探测可用实现

在静态初始化里，`Log4Gir` 会按顺序判断：

1. Hutool Log 是否存在
2. SLF4J 是否存在
3. Apache Commons Log 是否存在
4. 如果都没有，回退到控制台日志

最终对应的实现可能是：

- `HutoolLog`
- `Slf4jLog`
- `ApacheCommonsLog`
- `GirConsoleLog`

所以 `core` 并不是强迫项目必须接某一种日志实现，而是：

- 提供统一日志接口 `GiLogger`
- 提供统一工厂 `GirLoggerFactory`
- 默认按环境选择最合适的实现

同样地，`Log4Gir` 也通过：

```java
@GaMethodHandImpl(
    implClass = GirLogger.class,
    implMethod = "getLoger",
    type = ImplType.expectfirst
)
```

和：

```java
@GaMethodHandImpl(
    implClass = GirLoggerFactory.class,
    implMethod = "getLogger",
    type = ImplType.expectfirst
)
```

把默认日志实现接回 `base` 的统一日志门面。

### 4. JSON 封装：GirJacksonJson / GirFastJson / GirGsonJson / GirHutoolJson

核心类：

- `GirJacksonJson`
- `GirFastJson`
- `GirFastJson2`
- `GirGsonJson`
- `GirHutoolJson`

这一层的设计重点不是固定某个 JSON 库，而是：

- 对外统一抽象成 `GirJSON`
- 对内允许多种实现共存
- 默认实现可以按项目依赖和环境选择

#### GirJacksonJson 的实现特点

`GirJacksonJson` 的逻辑很典型：

- 如果 Spring 容器里能拿到 `ObjectMapper`，优先使用容器中的 `ObjectMapper`
- 如果拿不到，就 new 一个默认的 `ObjectMapper`
- 然后统一实现：
  - `toJSONString()`
  - `toBean(...)`
  - `getByPath(...)`

这层带来的好处是：

- 业务层不必直接依赖某个具体 JSON 库
- 如果项目已经有自己的 `ObjectMapper` 配置，也能自然复用
- 如果没有，也不会因为缺少配置而无法使用

## GkMethodHand：默认实现为什么可被替换

这是 `core` 里非常关键、也很有辨识度的一层设计。

### 设计问题

如果 `core` 只是简单把默认实现写死，那么会有两个问题：

1. `base` 的统一门面会重新耦合回默认实现
2. 项目方一旦不想使用默认实现，就只能回头改上层调用代码

### 解决思路

GeoAir 用：

- `GkMethodHand`
- `@GaMethodHandDefine`
- `@GaMethodHandImpl`

把“默认实现”做成一种**可挂载、可替换、可礼让**的机制。

### `@GaMethodHandImpl` 的含义

例如：

```java
@GaMethodHandImpl(
    implClass = GirConvertHelper.class,
    implMethod = "getProvider",
    type = ImplType.comity
)
```

它表达的不是“给一个普通方法加注解”，而是：

- 这个方法是某个 helper 约定实现位的实现提供者
- `implClass` 指向被实现的 helper
- `implMethod` 指向 helper 中约定的方法
- `type` 指定实现挂载策略

### `ImplType` 的语义

- `expectfirst`：期待优先，默认优先使用这个实现
- `cover`：覆盖已有实现
- `comity`：礼让，如果已有实现存在就让位，否则补位

### 设计价值

这带来的核心收益是：

- `base` 保持统一调用入口
- `core` 提供默认实现
- 项目方可以不依赖默认实现，自行挂接自己的实现
- 上层业务代码仍然不需要修改

也就是说，`core` 是“默认实现层”，但并不是“唯一实现层”。

## GitHub 源码入口

- 模块目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-core`
- bean 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-core/src/main/java/cn/geoair/spi/bean`
- env 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-core/src/main/java/cn/geoair/spi/env`
- json 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-core/src/main/java/cn/geoair/spi/json`
- log 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-core/src/main/java/cn/geoair/spi/log`

## 对应测试入口

- `GirJacksonJsonExample`
- `GirFastJsonExample`
- `Log4GirExample`
- `SpringEnvironment4GirExample`

这些示例分别对应：

- Jackson JSON 实现
- FastJson 实现
- 日志选择与统一日志入口
- 环境变量与属性访问入口

## 阅读建议

建议顺序：

1. `SpringContextBean4Gir`
2. `SpringEnvironment4Gir`
3. `Log4Gir`
4. `GirJacksonJson / GirFastJson / GirGsonJson / GirHutoolJson`
5. 回到 `geoair-base` 看 `GirBeanHelper / GirPropertyHelper / GirLoggerFactory / GirJSON`
6. 再看 `GkMethodHand / @GaMethodHandImpl`

这样最容易把“base 定义抽象、core 提供默认实现、项目方仍可替换实现”这条主线读清楚。
