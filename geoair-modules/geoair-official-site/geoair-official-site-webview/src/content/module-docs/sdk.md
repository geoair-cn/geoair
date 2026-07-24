## 模块定位

`geoair-sdk` 更偏一组面向上层交付的 SDK 辅助能力，而不是通用基础抽象层。它主要包含：

- SDK 配置
- SDK 工具方法
- Multipart / Body 组织
- Secret Provider 等封装

如果需要把 GeoAir 的某些能力对外整理成更稳定的调用入口，这一层值得优先阅读。

## 核心类

最值得先读的类：

- `GirSdkUtil`
- `GirSdkProfileConfig`
- `GirSdkSecretProvider`
- `GirMultipartBody`
- `GiRequestBody`

### GirSdkUtil

负责提供 SDK 级别的工具能力。

### GirSdkProfileConfig

负责组织 SDK 侧的配置。

### GirSdkSecretProvider

负责和密钥、签名或认证相关的能力入口。

### Multipart / Body

- `GiRequestBody`
- `GirMultipartBody`
- `StringRequestBody`
- `GiSdkMultipartFile`

这层负责组织 SDK 中常见的请求体结构。

## GitHub 源码入口

- 模块目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-sdk`
- body 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-sdk/src/main/java/cn/geoair/sdk/body`
- file 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-sdk/src/main/java/cn/geoair/sdk/file`

## 阅读建议

建议顺序：

1. `GirSdkUtil`
2. `GirSdkProfileConfig`
3. `GirSdkSecretProvider`
4. `body` 与 `file` 目录

先理解 SDK 工具和配置，再看请求体与文件相关的封装。
