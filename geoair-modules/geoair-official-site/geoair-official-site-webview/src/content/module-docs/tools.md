## 模块定位

`geoair-tools` 是标准基础库里更偏底层的工具层，负责提供一些不依赖重型框架的通用工具和控制台辅助能力。

它不像 `geoair-base` 那样负责抽象，也不像 `geoair-core` 那样负责实现接入，而更偏“随手可用的小工具层”。

## 核心类

目前直接能看到的典型入口包括：

- `GkConsole`
- `GkConsoleTable`

这说明这一层当前更明显的能力是：

- 控制台输出
- 表格化输出
- 轻量工具支撑

## GitHub 源码入口

- 模块目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-tools`
- tool 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-tools/src/main/java/cn/geoair/base/tool`

## 阅读建议

建议顺序：

1. `GkConsole`
2. `GkConsoleTable`
3. 再回到 `geoair-base` 看这些工具如何与基础抽象协同使用

如果你只是在找一些底层可复用工具，这一层通常会比上层模块更直接。
