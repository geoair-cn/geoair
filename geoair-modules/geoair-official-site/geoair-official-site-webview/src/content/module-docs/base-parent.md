## 模块定位

`geoair-base-parent` 是 GeoAir 的基础父 POM。它更像整个框架构建和依赖管理的总汇合点，而不是具体功能模块。

它主要负责：

- 聚合多个 BOM
- 统一构建插件配置
- 统一编码与发布参数
- 为下游标准库和业务模块提供共同继承入口

## 角色位置

从继承关系看：

- 往上承接 `geoair-framework`
- 往下服务 `geoair-standard`、`geoair-modules`、`geoair-framework-bom`

所以它的作用更偏“工程骨架”和“构建约束”。

## 核心职责

### 1. 依赖管理聚合

它会把以下依赖管理聚合进来：

- Spring Boot Dependencies BOM
- `geoair-geotools-dependencies`
- `geoair-spring-dependencies`
- `geoair-openapi-dependencies`
- `geoair-common-dependencies`

### 2. 构建配置统一

它会统一：

- Java 版本
- 编码
- 源码包 / javadoc 包生成
- 发布与签名相关插件配置

### 3. 继承入口

当某个项目或模块直接继承 `geoair-base-parent` 时，通常意味着：

- 认同这套依赖管理
- 认同这套构建约束
- 认同这套发布规范

## 使用方式

```xml
<parent>
  <groupId>cn.geoair.devkit</groupId>
  <artifactId>geoair-base-parent</artifactId>
  <version>J8.1.5</version>
</parent>
```

## 适用场景

适合：

- 直接加入 GeoAir 体系的新模块
- 需要统一依赖和构建规范的内部工程
- 想快速获得一整套基础构建约束的项目

## GitHub 源码入口

- 模块目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-base-parent`
- 根 `pom.xml`：
  - `https://github.com/geoair-cn/geoair/blob/master/geoair-framework/geoair-base-parent/pom.xml`

## 阅读建议

建议顺序：

1. 先看 `geoair-dependencies-bom`
2. 再看 `geoair-base-parent`
3. 再看 `geoair-framework-bom`

这样能从“依赖版本管理”一路看到“父工程怎么落地继承链”。
