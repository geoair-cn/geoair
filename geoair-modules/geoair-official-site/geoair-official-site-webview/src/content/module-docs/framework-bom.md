## 模块定位

`geoair-framework-bom` 不是运行时代码模块，而是一组面向不同工程形态的父 POM 集合。

它解决的问题是：

- API 项目继承什么
- 普通业务项目继承什么
- Spring Boot Starter 继承什么

如果 `geoair-base-parent` 更像统一底座，那么 `geoair-framework-bom` 更像“不同项目类型的父工程模板层”。

## 模块结构

当前可以看到的主要子模块：

- `geoair-api-parent`
- `geoair-project-parent`
- `geoair-spring-boot-starter-parent`

### geoair-api-parent

适合：

- 接口层项目
- API 服务定义层

### geoair-project-parent

适合：

- 普通业务项目
- 常规服务或应用工程

### geoair-spring-boot-starter-parent

适合：

- Spring Boot Starter 开发
- 需要作为 Starter 发布的能力模块

## 使用方式

```xml
<parent>
  <groupId>cn.geoair.devkit</groupId>
  <artifactId>geoair-project-parent</artifactId>
  <version>J8.1.5</version>
</parent>
```

## 适用场景

适合：

- 想按照项目类型选择最贴合的父 POM
- 不希望所有工程都直接继承同一个顶层 parent
- 想在组织内部统一 API / 项目 / Starter 三种项目模板

## GitHub 源码入口

- 模块目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-framework-bom`
- `geoair-api-parent`：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-framework-bom/geoair-api-parent`
- `geoair-project-parent`：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-framework-bom/geoair-project-parent`
- `geoair-spring-boot-starter-parent`：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-framework-bom/geoair-spring-boot-starter-parent`

## 阅读建议

建议顺序：

1. 先看 `geoair-base-parent`
2. 再看 `geoair-framework-bom`
3. 最后按项目类型进入 `api-parent / project-parent / starter-parent`

这样更容易理解它为什么要在 `base-parent` 之上再拆出一层项目模板。 
