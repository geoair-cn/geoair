## 模块定位

`geoair-tools` 是标准基础库里更偏底层的工具层，负责提供一些不依赖重型框架的通用工具和控制台辅助能力。

它不像 `geoair-base` 那样负责抽象，也不像 `geoair-core` 那样负责实现接入，而更偏“随手可用的小工具层”。

## 主要能力分组

从当前源码结构看，这一层至少可以分成几组：

### 1. 控制台与输出工具

典型类：

- `GkConsole`
- `GkConsoleTable`

这一层负责：

- 控制台输出
- 表格化输出
- 调试阶段的快速打印

### 2. 常用 util 工具组

当前可以看到大量 `Gutil*`：

- `GutilArray`
- `GutilCollection`
- `GutilDate`
- `GutilObject`
- `GutilNumber`
- `GutilStr`
- `GutilReflection`
- `GutilDigest`
- `GutilLambda`
- `GutilAssert`

这一层负责的是底层静态工具支撑。

### 3. 日志工具

典型类：

- `GirConsoleLog`
- `GirLogger`
- `GirLoggerFactory`
- `GirLogWrapper`
- `MessageFormatter`

这一层负责更轻量的日志包装和格式化。

### 4. 并发工具

典型类：

- `GirPxyExecutorService`
- `GirScheduledPxyExecutorService`
- `GirTaskInterceptor`
- `DefaultLogTaskInterceptor`

这一层负责执行器包装与任务拦截。

### 5. 方法分派与 Lambda 元编程

典型类：

- `GkMethodHand`
- `GaMethodHandDefine`
- `GaMethodHandImpl`
- `GkSerializableFunction`
- `GkReflectLambdaMeta`
- `GkShadowLambdaMeta`

这一层和 `geoair-base`、`geoair-core` 的门面与 SPI 机制结合得更紧。

## GitHub 源码入口

- 模块目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-tools`
- tool 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-tools/src/main/java/cn/geoair/base/tool`
- util 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-tools/src/main/java/cn/geoair/base/util`
- log 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-tools/src/main/java/cn/geoair/base/log`
- concurrent 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-tools/src/main/java/cn/geoair/base/concurrent`
- invoke / lambda 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-tools/src/main/java/cn/geoair/base/lang`

## 阅读建议

建议顺序：

1. `GkConsole` / `GkConsoleTable`
2. `GutilObject` / `GutilCollection` / `GutilStr`
3. `GirLoggerFactory`
4. `GirPxyExecutorService`
5. `GkMethodHand`

先从最直接的控制台和 util 开始，再往下看日志、并发和方法分派，会更容易把这层吃透。
