## 模块定位

`geoair-framework-bom` 是 GeoAir 面向项目使用者的入口层。它继承 `geoair-base-parent` 的版本治理，再按工程类型提供三种父 POM：

- `geoair-api-parent`：接口定义或 API 契约项目。
- `geoair-project-parent`：常规业务服务与应用项目。
- `geoair-spring-boot-starter-parent`：需要发布为 Spring Boot Starter 的组件。

它解决的不是“再管理一遍第三方版本”，而是**让不同类型项目从合适的工程语义开始**。

## 推荐用法

普通业务服务从 `geoair-project-parent` 开始：

```xml
<parent>
  <groupId>cn.geoair.devkit</groupId>
  <artifactId>geoair-project-parent</artifactId>
  <version>${geoair.version}</version>
</parent>
```

随后只按需声明 GeoAir 模块，例如 `geoair-dynamic-ds`、`geoair-geo-tools` 或 `geoair-jdbc-url`；不需要为它们逐个维护第三方版本。

## 三层如何配合

```
领域 BOM：定义第三方版本属于哪里
    ↓
基础父 POM：导入领域 BOM，并处理跨生态的最终版本仲裁
    ↓
项目父 POM：按 API / 应用 / Starter 选择项目入口
```

这也是我保留 `geoair-framework-bom` 的原因：版本治理与项目模板是两个变化频率不同的职责，不应混在同一个 POM 中。

## 选择建议

- 新建普通 GIS 或数据服务：选 `geoair-project-parent`。
- 只提供 API 模型、客户端契约或公共接口：选 `geoair-api-parent`。
- 需要自动装配并供其他应用引入：选 `geoair-spring-boot-starter-parent`。

如果项目只是临时验证某个组件，也可以直接导入单个 BOM；但正式项目应优先沿这条三层继承链接入，避免重新发明版本管理。
