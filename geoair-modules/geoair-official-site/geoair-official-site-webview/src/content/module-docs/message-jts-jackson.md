## 模块定位

`geoair-message-jts-jackson` 负责把 JTS Geometry 与 Jackson 的序列化 / 反序列化体系接起来。

适用场景包括：

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

## 最小示例

```java
ObjectMapper objectMapper = new ObjectMapper();
objectMapper.registerModule(new JtsExtModule());

Envelope envelope = new Envelope(116.35, 116.55, 39.85, 40.05);
String json = objectMapper.writeValueAsString(envelope);
```

对应测试：`JtsJacksonModuleExample`

## 和上层模块的关系

这一层通常会和：

- `geoair-jts-all`
- `geoair-geo-tools`
- Web 接口层

配合使用。

典型关系是：

1. `geo-tools` 负责生成或处理 `Geometry`
2. `message-jts-jackson` 负责把 `Geometry` 输出成 JSON / GeoJSON
3. Web 层把这些对象直接返回给前端

## GitHub 源码入口

- 模块目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-message-jts-jackson`
- JTS serializer 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-message-jts-jackson/src/main/java/cn/geoair/comp/message/converter/jts/jackson/serializer/jts`
- auto config 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-message-jts-jackson/src/main/java/cn/geoair/comp/message/converter/jts/jackson/auto`
- 测试目录：
  - `geoair-message-jts-jackson/src/test/java/cn/geoair/comp/message/converter/jts/jackson/test/JtsJacksonModuleExample.java`

## 阅读建议

建议顺序：

1. `JtsExtModule`
2. `JtsJacksonModuleExample`
3. `GirJtsJacksonUtils`
4. `GirJacksonJtsAutoConfiguration`
5. `EnableGirJtsAutoRegister`

先理解序列化模块和最小 test，再回头看自动装配会更顺。
