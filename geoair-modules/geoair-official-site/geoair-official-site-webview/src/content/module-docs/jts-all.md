## 模块定位

`geoair-jts-all` 更像一个聚合桥接模块，它本身不是一组庞大的业务 API，而是把 JTS 相关能力汇总到一个模块下，方便上层统一引入。

适用关注点包括：

- JTS Geometry 统一依赖
- 与其他消息转换模块组合使用
- 作为几何处理基础依赖入口

那么这个模块的重点在“聚合”，不在“复杂业务逻辑”。

## 目前可见代码

当前在源码里能直接看到的类较少，典型入口是：

- `Test`

这也说明这个模块更偏依赖收口和桥接，而不是像 `geo-tools` 那样提供大量独立 API 面。

## 适用场景

适合：

- 需要统一引入 JTS 相关能力的上层模块
- 与 `geoair-message-jts-jackson` 或 `geoair-message-jts-mybatis` 组合使用
- 作为 Geometry 处理基础层的聚合依赖

## GitHub 源码入口

- 模块目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-jts-all`

## 阅读建议

建议顺序：

1. 先看 `geoair-jts-all` 本身的目录结构
2. 再看 `geoair-message-jts-jackson`
3. 再看 `geoair-message-jts-mybatis`

因为这个模块更像桥接层，真正的序列化和数据库映射能力更多体现在这两个消息转换模块里。
