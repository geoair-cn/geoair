## 模块定位

`geoair-base` 是标准基础库中最底层的一层，负责定义抽象接口、结果模型、Bean / Cache / 环境访问辅助类，以及一些约束性的基础数据结构。

它更偏“抽象与约束”，而不是具体实现。

## 关键入口

最值得先读的类与接口：

- `Gir`
- `GirBeanHelper`
- `GirCacheHelper`
- `GiResult`

### Gir

`Gir` 是统一门面入口。很多基础能力最终都会通过它暴露给调用方。

### GirBeanHelper

负责统一从容器中获取 Bean，不让上层直接耦合具体容器实现。

### GirCacheHelper

负责统一使用缓存入口，不要求上层知道底层到底是哪种缓存实现。

### GiResult

负责统一结果模型，是服务返回结果的一层抽象。

## GitHub 源码入口

- 模块目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-base`
- Bean 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-base/src/main/java/cn/geoair/base/bean`
- Cache 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-base/src/main/java/cn/geoair/base/cache`
- Result 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-base/src/main/java/cn/geoair/base/data/result`

## 阅读建议

建议顺序：

1. `Gir`
2. `GirBeanHelper`
3. `GirCacheHelper`
4. `GiResult`

先理解统一入口，再理解结果模型和基础辅助类。
