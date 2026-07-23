## 模块定位

`geoair-web` 负责把 Web 相关能力组织成统一工具层，覆盖请求 / 响应访问、会话管理、结果模型和页面参数处理。

如果你的项目运行在 Web 场景中，这一层是连接基础库和 Servlet / Spring Web 的主要位置。

## 关键入口

最值得先读的类：

- `GirWeb`
- `GirWebResult`
- `GirCookieSession`
- `GirTokenSession`
- `GirWebPageParamProvider`

### GirWeb

负责快速获取请求、响应和上下文对象，是最直接的 Web 门面工具。

### GirWebResult

负责统一 Web 响应结果。

### GirCookieSession / GirTokenSession

负责不同会话模式下的 Session 组织方式。

### GirWebPageParamProvider

负责页面参数和分页参数的统一提供。

## GitHub 源码入口

- 模块目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-web`
- Session 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-web/src/main/java/cn/geoair/web/session`
- Result 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-web/src/main/java/cn/geoair/web/data/result`
- Page 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-web/src/main/java/cn/geoair/web/data/page`

## 阅读建议

建议顺序：

1. `GirWeb`
2. `GirWebResult`
3. `GirCookieSession` / `GirTokenSession`
4. `GirWebPageParamProvider`

先理解请求/响应门面，再看结果模型和会话机制。
