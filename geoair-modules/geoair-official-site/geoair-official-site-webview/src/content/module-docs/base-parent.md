## 模块定位

`geoair-base-parent` 是我放置**通用工程约定**的地方：它把 GeoAir 的领域 BOM、Spring Boot BOM 和构建约定收敛成一个稳定底座。它不是业务功能包，也不是让应用直接依赖的运行时库。

在三层设计中，它位于中间：

```
geoair-dependencies-bom  →  geoair-base-parent  →  geoair-framework-bom
```

## 它负责什么

- 统一 Java、编码、源码与 Javadoc 等构建约定。
- 导入 Spring、GeoTools、公共组件和 OpenAPI 等领域 BOM。
- 对多个 BOM 都会管理的关键依赖给出最终版本，例如 Jackson、PostgreSQL JDBC、SQLite JDBC、Guava 与 Commons Lang。

最后一点很重要：我不把“哪个 BOM 后导入”当作版本策略。冲突坐标在所有 BOM 之后显式声明，后续升级 Spring Boot 或 GeoTools 时，版本归属仍然清晰可查。

## 怎么用

GeoAir 内部模块通常直接以它为父 POM：

```xml
<parent>
  <groupId>cn.geoair.devkit</groupId>
  <artifactId>geoair-base-parent</artifactId>
  <version>J17-dev-SNAPSHOT</version>
</parent>
```

如果是在对外业务项目中，我更建议选择 `geoair-framework-bom` 下的项目父 POM；它会在此基础上表达“这是 API、普通项目还是 Starter”。

## 我期望模块作者遵守的规则

1. 业务模块只声明实际使用的依赖，不重复写由 BOM 管理的第三方版本。
2. 新的第三方依赖先判断属于哪个领域 BOM，再增加版本属性和约束。
3. 只有确实需要覆盖生态版本时，才在本层做显式最终仲裁，并说明兼容原因。

这样，模块代码关注功能，依赖升级则能沿着唯一的版本治理路径完成。
