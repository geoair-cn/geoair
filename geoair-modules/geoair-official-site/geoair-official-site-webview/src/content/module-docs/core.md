## 模块定位

`geoair-core` 是对 `geoair-base` 抽象层的主要实现层，重点是把 Bean、缓存、JSON、日志、环境等能力接到具体实现上。

如果说 `geoair-base` 解决的是“接口怎么定义”，那么 `geoair-core` 解决的是“这些接口默认怎么落地”。

## 关键入口

最值得先读的类：

- `SpringContextBean4Gir`
- `Cache4Gir`
- `GirJacksonJson`
- `Log4Gir`

### SpringContextBean4Gir

负责把 Bean 获取能力接到 Spring 容器。

### Cache4Gir

负责把缓存能力接到具体缓存实现。

### GirJacksonJson

负责把 JSON 能力落到 Jackson 实现上。

### Log4Gir

负责把日志能力落到具体日志实现上。

## GitHub 源码入口

- 模块目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-core`
- Bean SPI 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-core/src/main/java/cn/geoair/spi/bean`
- Cache SPI 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-core/src/main/java/cn/geoair/spi/cache`
- JSON SPI 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-core/src/main/java/cn/geoair/spi/json`
- Log SPI 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-core/src/main/java/cn/geoair/spi/log`

## 阅读建议

建议顺序：

1. `SpringContextBean4Gir`
2. `Cache4Gir`
3. `GirJacksonJson`
4. `Log4Gir`

先看容器和缓存，再看 JSON 与日志，会更容易理解 `geoair-base -> geoair-core` 的关系。
