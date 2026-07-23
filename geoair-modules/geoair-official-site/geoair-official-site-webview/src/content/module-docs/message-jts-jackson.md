## 模块定位

`geoair-message-jts-jackson` 负责把 JTS Geometry 与 Jackson 的序列化 / 反序列化体系接起来。

如果你的需求是：

- 把 Geometry 输出成 JSON / GeoJSON
- 在接口层直接返回 Geometry 字段
- 给 Jackson ObjectMapper 注册 JTS 相关模块

这个模块就是消息转换层的入口。

## 核心类

最值得先读的类：

- `GirJtsJacksonUtils`
- `JtsExtModule`
- `GirJacksonJtsAutoConfiguration`
- `EnableGirJtsAutoRegister`

### GirJtsJacksonUtils

负责提供面向 JTS / Jackson 的工具能力。

### JtsExtModule

负责把 JTS Geometry 相关序列化能力注册为 Jackson Module。

### AutoConfiguration

- `GirJacksonJtsAutoConfiguration`
- `EnableGirJtsAutoRegister`

负责在 Spring 环境中自动装配这些能力。

## GitHub 源码入口

- 模块目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-message-jts-jackson`
- JTS serializer 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-message-jts-jackson/src/main/java/cn/geoair/comp/message/converter/jts/jackson/serializer/jts`
- auto config 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-message-jts-jackson/src/main/java/cn/geoair/comp/message/converter/jts/jackson/auto`

## 阅读建议

建议顺序：

1. `JtsExtModule`
2. `GirJtsJacksonUtils`
3. `GirJacksonJtsAutoConfiguration`
4. `EnableGirJtsAutoRegister`

先理解序列化模块本身，再看它如何被自动接到 Spring / Jackson 配置中。