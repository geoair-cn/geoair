## 模块定位

`geoair-apidoc` 负责把接口文档配置能力组织成一套可复用的扩展层。它的重点不是只生成 Swagger 页面，而是：

- 怎么组织文档分组
- 怎么定义主页信息
- 怎么把这套配置接到 Spring 容器里

适用场景包括：

## 核心类

最值得先读的类：

- `GirOpenApiConfig`
- `DocketInfo`
- `ApiModelInfo`
- `Swagger2Configuration`

### GirOpenApiConfig

这是抽象配置基类。通常项目会：

- 继承它
- 覆盖 `getDocketInfos()`
- 覆盖 `getApiModelInfo()`

### DocketInfo

`DocketInfo` 负责定义每一个分组的扫描规则，最常见的是：

- `groupName`
- `basePackage`
- 可选的 `specifyScan`
- 可选的 `modelscan` / `modelClassList`

### ApiModelInfo

`ApiModelInfo` 负责定义首页信息，例如：

- 标题
- 描述
- 作者
- 版本

## 项目示例

下面这段配置可以作为项目中的典型写法：

```java
@Configuration
@Import(SpringContextBean4Gtc.class)
public class Swagger2Configuration extends GtcOpenApiConfig {
    @Override
    public List<DocketInfo> getDocketInfos() {
        return ListUtil.of(
                new DocketInfo("user", "com.gtc.gishubteam.my-service"),
                new DocketInfo("admin", "com.gtc.gishubteam.admin.auth.controller"));
    }

    @Override
    public ApiModelInfo getApiModelInfo() {
        return new ApiModelInfo("my-service在线文档", "my-service在线文档", "my-service", "1.0");
    }
}
```

这段配置说明了两件事：

1. 通过 `getDocketInfos()` 把接口文档拆成多个分组
2. 通过 `getApiModelInfo()` 定义整份在线文档的主页信息

## 仓库中现有 demo 示例

当前仓库里已经有一个同类型的 demo：

- `geoair-knife4j-spring-boot-demo/src/main/java/cn/geoair/comp/demo/knife4j/config/Swagger2Configuration.java`

它的写法是：

```java
@Component
public class Swagger2Configuration extends GirOpenApiConfig {

    @Override
    public List<DocketInfo> getDocketInfos() {
        return ListUtil.of(
                new DocketInfo("demo2", "cn.geoair.comp.demo.knife4j.controller.group2"),
                new DocketInfo("demo1", "cn.geoair.comp.demo.knife4j.controller.group1"));
    }

    @Override
    public ApiModelInfo getApiModelInfo() {
        return new ApiModelInfo("demo 在线文档", "demo在线文档", "demo", "666666.0");
    }
}
```

## 典型使用方式

### 示例1：最小分组配置

```java
@Override
public List<DocketInfo> getDocketInfos() {
    return ListUtil.of(
        new DocketInfo("user", "com.example.user.controller"),
        new DocketInfo("admin", "com.example.admin.controller"));
}
```

### 示例2：主页信息配置

```java
@Override
public ApiModelInfo getApiModelInfo() {
    return new ApiModelInfo("在线文档", "服务接口在线文档", "team", "1.0");
}
```

## GitHub 源码入口

- 模块目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-apidoc`
- 核心配置目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-apidoc/geoair-knife4j-core/src/main/java/cn/geoair/comp/knife4j/ext/core/config`
- 模型目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-apidoc/geoair-knife4j-core/src/main/java/cn/geoair/comp/knife4j/ext/core/model`
- demo 配置目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-apidoc/geoair-knife4j-spring-boot-demo/src/main/java/cn/geoair/comp/demo/knife4j/config`

## 阅读建议

建议顺序：

1. `GirOpenApiConfig`
2. `DocketInfo`
3. `ApiModelInfo`
4. `Swagger2Configuration` demo
5. 再对照你项目里的实际配置类

这样会先理解配置抽象，再看具体项目如何落地。
