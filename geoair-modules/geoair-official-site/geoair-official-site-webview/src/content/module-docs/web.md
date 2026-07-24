## 模块定位

`geoair-web` 负责把 Web 相关能力组织成统一工具层，覆盖请求 / 响应访问、会话管理、结果模型和页面参数处理。

在 Web 场景中，这一层负责连接基础库与 Servlet / Spring Web。

如果把标准基础库按职责拆开：

- `geoair-base`：定义结果模型、配置读取、Bean 抽象
- `geoair-core`：提供默认实现
- `geoair-web`：把这些能力组织到 Web 场景下

那么 `web` 的定位就不是“额外再造一套框架”，而是把基础层能力带入 HTTP / Session / Web Result 这一类典型场景里。

## 设计重点

### 1. Web 门面不是重新定义一套基础能力

`geoair-web` 里的很多类不是完全独立存在的，它们是在 `base` 和 `core` 之上继续向 Web 靠拢。

例如：

- `GirWebResult` 仍然是围绕统一结果模型在做 Web 场景包装
- `GirCookieSession` / `GirTokenSession` 是在会话场景下组织状态
- `GirWebPageParamProvider` 是在分页场景下把 Web 请求参数映射到统一分页模型

所以这层更适合理解为：

> Web 场景适配层，而不是完全独立的业务层。

### 2. 会话策略是可切换的

`geoair-web` 没有把会话写死成某一种实现，而是提供了至少两种典型方式：

- `GirCookieSession`
- `GirTokenSession`

这意味着：

- 如果系统更偏传统浏览器会话，可以走 Cookie 风格
- 如果系统更偏接口调用或前后端分离，可以走 Token 风格

也就是说，这层仍然保留了“统一接口 + 不同实现策略”的思路。

### 3. 结果模型面向 Web 输出而不是面向底层执行

`GirWebResult` 和 `GirWebResultConfig` 的价值不在“重新发明结果对象”，而在于：

- 把统一结果结构带进 Web 接口输出
- 让接口层输出保持一致
- 让分页和结果对象在 Web 层能统一消费

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

## 对应测试入口

当前标准层测试类较少，因此这里优先建议从源码目录顺着入口类阅读；如果后续补充最小 test，建议优先围绕：

- `GirWebResult`
- `GirCookieSession`
- `GirTokenSession`
- `GirWebPageParamProvider`

## 阅读建议

建议顺序：

1. `GirWeb`
2. `GirWebResult`
3. `GirCookieSession` / `GirTokenSession`
4. `GirWebPageParamProvider`

先理解请求/响应门面，再看结果模型和会话机制，会更容易把 `base -> core -> web` 这一条线串起来。
