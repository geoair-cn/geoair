## 模块定位

`geoair-db-service` 更像一个数据库服务与数据库管理界面的组合模块。它不是单纯的 JDBC 工具，也不是单纯的前端页面，而是把：

- 数据源管理
- SQL 执行
- API 配置
- 表结构与数据浏览
- Web 管理界面

放到了一组前后端模块里。

## 模块结构

### 核心服务层

重点类包括：

- `DsApiService`
- `DsDataSourceService`
- `GirDsSQLExecutor`

这些类负责：

- 数据源管理
- SQL 执行
- API 配置与数据访问抽象

### 控制层

重点类包括：

- `GirDsDataSourceController`
- `GirDsTableController`
- `GirDsApiConfigController`
- `GirDsSystemController`

这些类负责把数据库服务能力暴露成管理接口。

### 前端层

前端部分位于：

- `geoair-db-service-webview`

也就是你当前这个官网项目最初参考过的那套 Vue2 + Element UI 工程基础。

## GitHub 源码入口

- 模块目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-db-service`
- 核心服务目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-db-service/geoair-db-service-core/src/main/java/cn/geoair/comp/db/service/core`
- 前端目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-db-service/geoair-db-service-webview`

## 阅读建议

建议顺序：

1. `DsDataSourceService`
2. `DsApiService`
3. `GirDsSQLExecutor`
4. 各个 Controller
5. 最后再看 `geoair-db-service-webview`

先理解服务端的数据源与 SQL 组织方式，再回头看前端管理界面会更顺。