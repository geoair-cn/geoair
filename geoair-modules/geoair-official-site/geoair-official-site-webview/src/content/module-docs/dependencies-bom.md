## 模块定位

`geoair-dependencies-bom` 是 GeoAir 的依赖版本管理中心。它的职责不是提供运行时代码，而是把第三方依赖按领域拆开管理，避免版本散乱和冲突。

适用于需要统一以下依赖版本的项目：

- GeoTools 相关依赖版本
- Spring 相关依赖版本
- OpenAPI / Swagger 相关依赖版本
- 其他公共第三方依赖版本

那么这一层就是依赖管理入口。

## 模块结构

当前 BOM 主要分为这些子模块：

- `geoair-geotools-dependencies`
- `geoair-spring-dependencies`
- `geoair-openapi-dependencies`
- `geoair-common-dependencies`
- `geoair-template-dependencies`

这意味着它的重点不是“类”，而是“依赖版本组织方式”。

## 使用方式

### 方式1：通过 `geoair-base-parent` 间接引入

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>cn.geoair.devkit</groupId>
      <artifactId>geoair-base-parent</artifactId>
      <version>J8.1.5</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

### 方式2：单独引入某个领域 BOM

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>cn.geoair.devkit</groupId>
      <artifactId>geoair-geotools-dependencies</artifactId>
      <version>J8.1.5</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

## 适用场景

适合：

- 需要统一第三方依赖版本的多模块工程
- 只想按领域引入依赖，而不是直接全量继承
- 想避免 GeoTools / Spring / OpenAPI 相关依赖冲突

## GitHub 源码入口

- 模块目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-dependencies-bom`
- geotools dependencies：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-dependencies-bom/geoair-geotools-dependencies`
- spring dependencies：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-dependencies-bom/geoair-spring-dependencies`
- openapi dependencies：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-dependencies-bom/geoair-openapi-dependencies`

## 阅读建议

建议顺序：

1. 先看 `geoair-dependencies-bom` 根 README
2. 再看 geotools / spring / openapi 这几个子 BOM
3. 最后再回到 `geoair-base-parent` 看它是怎么把这些 BOM 聚合起来的
