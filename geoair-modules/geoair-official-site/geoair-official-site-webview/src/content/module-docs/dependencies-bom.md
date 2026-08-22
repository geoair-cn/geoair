## 我为什么把依赖治理单独做成一层

GeoAir 的模块很多：Spring、GeoTools、数据库驱动、MyBatis、OpenAPI 都会间接带入自己的版本。若每个业务模块各自声明版本，短期看很方便，长期一定会出现同一依赖在不同模块漂移的问题。

因此我把职责收敛成三层：

```
geoair-dependencies-bom  →  geoair-base-parent  →  geoair-framework-bom
第三方版本归属              通用构建与最终仲裁          面向项目的对外入口
```

`geoair-dependencies-bom` 只回答一件事：**某一类第三方依赖由谁、以什么版本管理**。它不提供运行时代码，也不承担业务模块的依赖聚合。

## 当前的领域划分

- `geoair-geotools-dependencies`：GeoTools、JTS、空间数据与其兼容依赖。
- `geoair-spring-dependencies`：Spring 生态相关依赖。
- `geoair-openapi-dependencies`：OpenAPI、Swagger、Knife4j 等接口文档依赖。
- `geoair-common-dependencies`：数据库连接池、ORM、工具库等其余公共依赖。

已删除没有实际依赖内容的 `geoair-template-dependencies`。空 BOM 只会增加继承路径，并不能提供版本治理价值。

## 如何使用

大多数应用不应逐个导入领域 BOM，而应使用后两层入口。只有在独立工程确实只需要某个领域约束时，才直接导入对应 BOM：

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>cn.geoair.devkit</groupId>
      <artifactId>geoair-geotools-dependencies</artifactId>
      <version>${geoair.version}</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

导入后，模块使用 GeoTools、PostgreSQL 驱动等依赖时通常不再写版本号；版本由 BOM 统一给出。

## 设计约束

我把“版本的权威来源”限定在 BOM 与顶层版本属性中。业务模块只声明自己真正需要的坐标，不重新声明第三方版本。发生生态兼容冲突时，由 `geoair-base-parent` 在所有导入 BOM 之后给出最终仲裁版本，避免 Maven 的导入顺序变成隐性规则。

## 阅读顺序

1. 先看本模块的四个领域 BOM，理解版本按什么边界归属。
2. 再看 `geoair-base-parent`，理解它如何导入并最终仲裁版本。
3. 最后看 `geoair-framework-bom` 与三种项目父 POM，选择项目的使用入口。
