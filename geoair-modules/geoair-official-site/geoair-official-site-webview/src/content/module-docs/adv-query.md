## 模块定位

`geoair-adv-query` 更像一层高级空间查询执行器。它关注的不是简单的 CRUD，而是：

- 多数据库方言下的空间查询组织方式
- WHERE 条件和表达式的组合
- 分页、排序、分组等查询结构
- 把空间数据访问写成可复用、可维护的代码

如果 `geoair-geo-tools` 更偏 Geometry 与坐标处理，那么 `geoair-adv-query` 更偏“如何查 Geometry”。

## 核心接口组成

在这个模块里，最重要的几个对象是：

- `GirAdvQueryRequest`
- `GirAdvWhereFilter`
- `GirAdvWhereLambdaFilter`
- `GirAdvSqlComposer`

它们之间的关系可以理解为：

1. `GirAdvQueryRequest` 负责描述“我要查什么”
2. `GirAdvWhereFilter` / `GirAdvWhereLambdaFilter` 负责描述“查询条件怎么组织”
3. `GirAdvSqlComposer` 负责把请求对象编译成 SQL 与参数

## IAdvBase*Opt 分层说明

`adv-query` 并不是把所有基础能力堆在一个接口里，而是先拆成几组基础操作，再由 `IAdvBaseOpt` 聚合起来。

### IAdvBaseAccessOpt

这一层负责“写入 / 插入”相关能力，重点包括：

- `bInsertBySql(...)`
- `bInsertOne(...)`
- `bInsertSelectiveOne(...)`
- `bInsertBatch(...)`
- `bInsertIgnore(...)`
- `bInsertIgnoreBatch(...)`

在需要新增数据、批量导入数据或插入冲突忽略的场景下，这一层是基础入口。

### IAdvBaseSelectOpt

这一层负责“查询 / 映射”相关能力，重点包括：

- `bSelectOne(...)`
- `bSelectList(...)`
- `bSelectListStream(...)`
- `bSelectListToValueList(...)`
- `bSelectNumber(...)`
- `bSelectRecordRowCount(...)`
- `bSelectObjOne(...)`
- `bSelectObjList(...)`
- `bSelectObjListStream(...)`

这一层是整个 `adv-query` 最常用的一组基础查询能力，既支持：

- 直接查 `GirAdvOneRow`
- 查纯值列表
- 查对象映射
- 流式查询

### IAdvBaseUpdateOpt

这一层负责“更新 / upsert”相关能力，重点包括：

- `bUpdateBySql(...)`
- `bUpdateByPK(...)`
- `bUpdateBatchByPK(...)`
- `bUpdateByWhere(...)`
- `bUpsert(...)`
- `bUpsertBatch(...)`

在需要按主键更新、按条件更新、批量更新或 upsert 的场景下，这一层是基础入口。

### IAdvBaseDeleteOpt

这一层负责“删除”相关能力，重点包括：

- `bDeleteBySql(...)`
- `bDeleteByPK(...)`
- `bDeleteByPKs(...)`
- `bDeleteByMap(...)`
- `bDeleteByWhere(...)`

适合处理：

- 自定义 SQL 删除
- 按主键删除
- 批量删除
- Lambda / Filter 条件删除

### IAdvBaseOpt

`IAdvBaseOpt` 本身不新增方法，而是把：

- `IAdvBaseSelectOpt`
- `IAdvBaseDeleteOpt`
- `IAdvBaseAccessOpt`
- `IAdvBaseUpdateOpt`

统一聚合成一个基础操作总接口。

所以当实现类同时具备查、增、改、删能力时，本质上就是通过 `IAdvBaseOpt` 把这四组基础接口拼起来了。

## 创建 IAdvExecutor

`IAdvExecutor` 的创建方式从上层到下分为三个层次：

| 层次 | 入口 | 适用场景 |
|------|------|---------|
| 快捷入口 | `GirAdvQuery.getIAdvExecutor(...)` | 日常编码，快速获取执行器 |
| 工厂层 | `AdvExecutorFactory.getAdvExecutorByDataSource(...)` | 需要显式控制方言路由 |
| 底层构造 | `initByDataSource(...)` / `initByConnection(...)` | 需要完全手动控制初始化 |

### 1. 快捷入口 —— GirAdvQuery

`GirAdvQuery` 提供了几个静态方法，是日常编码中最常用的入口：

```java
// 通过数据源 ID + schema 获取（Spring 环境下，走适配器）
IAdvExecutor executor = GirAdvQuery.getIAdvExecutor("master", "public");

// 直接传入 DataSource（不依赖 Spring 上下文）
IAdvExecutor executor = GirAdvQuery.getIAdvExecutor(dataSource);

// 传入 DataSource 并指定名称
IAdvExecutor executor = GirAdvQuery.getIAdvExecutor(dataSource, "myDs");

// 已知方言，跳过 JDBC 探测（性能更高）
IAdvExecutor executor = GirAdvQuery.getIAdvExecutor(DialectName.MYSQL, dataSource, "myDs");

// 指定返回类型的泛型版本
PgAdvExecutor executor = GirAdvQuery.getIAdvExecutor("master", "public", PgAdvExecutor.class);
```

`getIAdvExecutor(String dataSourceId, String schema)` 的工作流程：

1. 通过 `GirService.getPxyBeanC(IAdvExecutorAdapter.class)` 获取适配器
2. 适配器内部查找对应 `dataSourceId` 的数据源
3. 自动检测数据库方言，创建对应的 Executor
4. 设置 schema 后返回

这种方式适合 **Spring 多数据源环境**，只需传数据源 ID 即可。

`getIAdvExecutor(DataSource)` 的工作流程：

1. 直接调用 `AdvExecutorFactory.getAdvExecutorByDataSource(dataSource)`
2. 从 `DataSource` 获取 JDBC 连接
3. 通过 `DatabaseMetaData.getDatabaseProductName()` 检测数据库类型
4. 创建对应方言的 Executor（MySQL → `GirSpringMysqlAdvExecutor`，PG → `GirSpringPGAdvExecutor` 等）

这种方式适合 **非 Spring 环境**或**手动管理 DataSource** 的场景。

#### IAdvExecutorAdapter — 数据源查找的抽象层

`GirAdvQuery.getIAdvExecutor(dataSourceId, schema)` 并不是直接访问数据库的，中间隔了一个适配器接口：

```
GirAdvQuery.getIAdvExecutor("master", "public")
  └── IAdvExecutorAdapter.getIAdvExecutor(dataSourceId, schema)
        └── CommonAdvExecutorAdapter（默认实现）
              ├── AdvDynamicDataSourceStorage → 根据 ID 查找 DataSource
              ├── AdvExecutorFactory → 检测方言创建 Executor
              └── setSchemaNameGetterFunction → 设置 Schema
```

`IAdvExecutorAdapter` 的核心价值：

- **隔离数据源查找与执行器创建**：调用方不需要知道 DataSource 从哪来
- **可扩展**：如果默认的 `CommonAdvExecutorAdapter` 不满足需求（比如数据源来自不同的注册中心），可以实现自己的适配器
- **通过 SPI 暴露**：`CommonAdvExecutorAdapter` 通过 `Gir` SPI 机制注册，`GirService.getPxyBeanC(IAdvExecutorAdapter.class)` 即可获取

```java
// 默认实现：从动态数据源存储中查找
public class CommonAdvExecutorAdapter implements IAdvExecutorAdapter {
    @Override
    public IAdvExecutor getIAdvExecutor(String dataSourceId, String schema) {
        // 1. 根据 ID 获取 DataSource
        DynamicDataSourceManager instance = AdvDynamicDataSourceStorage.getInstance();
        AdvDataSourceWrapper dataSource = instance.getOrCreateDataSource(dataSourceId);
        // 2. 检测方言创建 Executor
        IAdvExecutor executor = AdvExecutorFactory.getAdvExecutorByDataSource(dataSource, dataSourceId + "_" + schema);
        // 3. 设置 Schema
        if (isNotEmpty(schema)) {
            executor.setSchemaNameGetterFunction(() -> schema);
        }
        return executor;
    }
}
```

### 2. 工厂层 —— AdvExecutorFactory

`AdvExecutorFactory` 是方言路由的核心。它提供了两种创建方式：

**方式 A：自动检测（通过 JDBC 连接探测）**

```java
// 自动从 Spring 容器获取 DataSource
IAdvExecutor executor = AdvExecutorFactory.getAdvExecutorByDataSource();

// 显式传入 DataSource
IAdvExecutor executor = AdvExecutorFactory.getAdvExecutorByDataSource(dataSource);

// 传入 DataSource 并指定名称
IAdvExecutor executor = AdvExecutorFactory.getAdvExecutorByDataSource(dataSource, "myDs");
```

方言检测逻辑：通过 `DatabaseMetaData.getDatabaseProductName()` 获取数据库产品名称并匹配：

```
DatabaseMetaData.getDatabaseProductName()
  ├── 包含 "MYSQL"              → DialectName.MYSQL      → GirSpringMysqlAdvExecutor
  ├── 包含 "POSTGRESQL" / "PG"  → DialectName.POSTGRESQL → GirSpringPGAdvExecutor
  ├── 包含 "ORACLE"             → DialectName.ORACLE     → GirSpringOracleAdvExecutor
  ├── 包含 "DAMENG" / "DM"      → DialectName.DM         → GirSpringDmAdvExecutor
  └── 其他                      → UnsupportedOperationException
```

**方式 B：直接指定方言（跳过 JDBC 探测，性能更高）**

```java
// 调用方已知数据库类型时，直接指定方言，避免额外的连接开销
IAdvExecutor executor = AdvExecutorFactory.getAdvExecutorByDialect(
        DialectName.MYSQL, dataSource, "myDs");
```

当调用方已经明确知道数据库类型时（比如从配置文件读取、或通过其他途径获取），使用 `getAdvExecutorByDialect` 可以完全跳过 JDBC 连接探测，消除 `getDbTypeFromDataSource` 中获取连接再释放的开销。

`AdvExecutorFactory` 的功能边界：

- **负责**：数据库类型检测（或接收指定）、方言 Executor 创建
- **不负责**：Spring Bean 注册（由 `AdvAutoConfiguration` 负责）、数据源生命周期管理、SQL 执行

### 3. 底层构造 —— initBy* 方法

`IAdvExecutor` 继承自 `IDataSourceGetter`，提供了四种底层初始化方式：

```java
// 通过数据源描述对象初始化
void initByDataSourceApo(DataSourceApo dataSourceApo);

// 通过数据源对象初始化
void initByDataSource(DataSource dataSource);

// 通过数据源对象初始化（指定名称）
void initByDataSource(DataSource dataSource, String dataSourceName);

// 通过数据库连接初始化
void initByConnection(Connection connection);
```

这些方法定义在 `IDataSourceGetter` 接口中，由 `AbstractPxyAdvExecutor` 实现。调用后会：

1. 初始化内部的数据源获取器
2. 触发 `initProxyObjects()`，创建各功能模块（Access / Select / Update / Delete / DDL / Geo 等）
3. 设置 Schema 和 Database 名称的获取函数

**直接使用底层构造的场景**：

```java
// 场景1：已有 Connection，不想额外管理连接池
IAdvExecutor executor = new AdvExecutorMysql(connection);

// 场景2：手动构造，传入 DataSourceApo 配置
DataSourceApo apo = new DataSourceApo();
apo.setUrl("jdbc:mysql://localhost:3306/gis");
apo.setUsername("root");
apo.setPassword("xxx");
IAdvExecutor executor = new AdvExecutorMysql(apo);

// 场景3：直接传 DataSource
IAdvExecutor executor = new AdvExecutorMysql(dataSource, "gis_db");
```

**注意**：底层构造需要自己指定方言实现类（如 `AdvExecutorMysql`），不会自动检测数据库类型。一般推荐使用 `AdvExecutorFactory` 或 `GirAdvQuery` 入口，它们会自动处理方言检测。

### 创建方式选择建议

```
需要自动检测数据库类型？
├── 是 → 用 AdvExecutorFactory 或 GirAdvQuery
│   └── 在 Spring 环境？
│       ├── 是 → GirAdvQuery.getIAdvExecutor("dsId", "schema")
│       └── 否 → GirAdvQuery.getIAdvExecutor(dataSource)
└── 否 → 直接 new 方言 Executor
    └── 已有 Connection？ → new AdvExecutorMysql(connection)
    └── 有 DataSource？   → new AdvExecutorMysql(dataSource, "name")
```

## Spring 集成方式

`adv-query` 并不只是工具层或手动构造执行器，在 Spring 环境中也提供了一整套自动装配链。

### 启用入口

核心注解：

- `EnableGirAdvDynamic`

通过启用这个注解，可以触发：

- `AdvAutoConfiguration`

### 自动装配逻辑

`AdvAutoConfiguration` 的作用是：

1. 检查 Spring 环境中是否已经有 `DataSource`
2. 读取当前数据源
3. 根据数据源类型创建默认 `IAdvExecutor`
4. 再把它包装成 `GirSpringAdvExecutor`
5. 最终让 Spring 容器里可以直接获取：
   - `IAdvExecutor`
   - `GirSpringAdvExecutor`

也就是说，在 Spring 环境中：

- 不一定每次都要手动 `AdvExecutorFactory.getAdvExecutorByDataSource(...)`
- 默认情况下可以依赖自动装配

### Spring 环境下的快速使用方式

如果项目里已经启用了自动装配，那么业务代码里可以直接使用：

```java
GirSpringAdvExecutor.getInstance().bSelectList(...)
```

或者直接获取：

```java
IAdvExecutor executor = GirSpringAdvExecutor.getExecutorInstance();
```

这样就能在 Spring 环境下任意位置直接访问当前数据源对应的高级查询能力。

### Spring 配置示例

下面这段写法就是一个典型的 Spring 自动装配入口：

```java
@Configuration
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
public class AdvAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(IAdvExecutor.class)
    public IAdvExecutor springAdvExecutor(ObjectProvider<DataSource> dataSourceProvider) {
        DataSource dataSource = dataSourceProvider.getIfAvailable();
        IAdvExecutor advExecutorByDataSource =
                AdvExecutorFactory.getAdvExecutorByDataSource(dataSource, "master_by_spring");
        return new GirSpringAdvExecutor(advExecutorByDataSource);
    }
}
```

它的核心意义是：

- 让 `adv-query` 能自然接进 Spring Boot 的默认数据源体系
- 让上层业务代码不需要反复自己 new 执行器

## typehandler 使用与注册逻辑

`adv-query` 自己内部就有一套类型处理链，不是完全依赖外部 ORM。

### 设计要点

当前版本的核心设计决策是：

- **每个 Executor 拥有独立 Registry**：`create()` 工厂方法为每个方言执行器创建专属实例，Geometry handler 按方言自动匹配
- **保留全局注册入口**：`AdvTypeHandlerRegistry.getInstance().register(xxx)` 仍然可用，全局注册的 handler 会被所有新创建的 Executor Registry 继承
- **Geometry handler 按方言拆分**：原来一个 `JtsGeometryAdvTypeHandler` 负责所有数据库，改为每个方言一个独立实现，不再依赖 classpath 探测
- **`AdvQueryGlobalConfig` 承载 per-executor 自定义 handler**：通过 `addTypeHandler()` 注册，优先级高于全局注册和 SPI

### 三种注册方式

| 方式 | API | 作用范围 | 优先级 |
|------|-----|---------|--------|
| SPI | 无需手动调用，框架自动加载 | 全局（所有 Executor） | 最低 |
| 全局注册 | `AdvTypeHandlerRegistry.getInstance().register(handler)` | 全局（后续创建的 Executor） | 中 |
| 按 Executor 注册 | `AdvQueryGlobalConfig.of().addTypeHandler(handler)` | 当前 Executor | 高于全局 |
| 方言 Geometry | 框架自动 | 当前 Executor | 最高

### 入口对象

最关键的几个类是：

- `AdvTypeHandlerRegistry` — 类型处理注册表（每个 Executor 一个实例）
- `AdvPreparedStatementBinder` — 参数绑定器
- `JtsGeometryAdvTypeHandler` — JTS Geometry 类型处理抽象基类
- `PostGisGeometryAdvTypeHandler` — PostgreSQL/PostGIS 方言实现
- `MysqlGeometryAdvTypeHandler` — MySQL 方言实现
- `OracleGeometryAdvTypeHandler` — Oracle Spatial 方言实现
- `WktGeometryAdvTypeHandler` — 达梦 / 通用 WKT 方言实现

### Registry 创建流程

`AdvTypeHandlerRegistry` 通过工厂方法 `create(DialectName, List<AdvTypeHandler<?>>)` 创建，加载顺序（优先级从低到高）为：

1. **SPI 加载公共 handlers**（方言无关）：
   - `BooleanAdvTypeHandler`
   - `ByteArrayAdvTypeHandler`
   - `CharacterAdvTypeHandler`
   - `EnumAdvTypeHandler`
   - `NumberAdvTypeHandler`
   - `TemporalAdvTypeHandler`

2. **全局 handlers**（通过 `AdvTypeHandlerRegistry.getInstance().register()` 注册）：优先级高于 SPI，被所有新创建的 Executor Registry 继承

3. **用户自定义 handlers**（来自 `AdvQueryGlobalConfig.typeHandlers`）：优先级高于全局注册，仅对当前 Executor 生效

4. **方言专属 Geometry handler**（优先级最高）：
   - PostgreSQL → `PostGisGeometryAdvTypeHandler`
   - MySQL → `MysqlGeometryAdvTypeHandler`
   - Oracle → `OracleGeometryAdvTypeHandler`
   - 达梦 → `WktGeometryAdvTypeHandler`

如果没有匹配到任何具体 handler，则回退到 `ObjectAdvTypeHandler`。

### Registry 创建位置

`AdvTypeHandlerRegistry` 的创建不在 `AbstractPxyAdvExecutor`（纯代理层），而在各方言的 `*AdvBaseOpt`（方言工厂）构造函数中：

```java
public class MysqlAdvBaseOpt extends AbstractPxyAdvBaseOpt {
    private final AdvTypeHandlerRegistry typeHandlerRegistry;

    public MysqlAdvBaseOpt(IDataSourceGetter dsGetter, Supplier<AdvQueryGlobalConfig> configGetter) {
        super(dsGetter, configGetter);
        this.typeHandlerRegistry = AdvTypeHandlerRegistry.create(
                DialectName.MYSQL,
                configGetter.get().getTypeHandlers());
    }
    // ... Registry 通过构造注入传递给 PgAdvBaseAccessOpt / SelectOpt / UpdateOpt / DeleteOpt
}
```

这样设计保证了：
- `AbstractPxyAdvExecutor` 保持纯代理职责
- 每个 Executor 的 Registry 天生知道自己的方言
- Geometry handler 不再需要猜测 classpath 上有什么驱动

### 配置用户自定义 TypeHandler

**方式一：全局注册（影响所有后续创建的 Executor）**

```java
AdvTypeHandlerRegistry.getInstance().register(new MyCustomTypeHandler());
```

**方式二：按 Executor 注册（仅影响当前 Executor）**

```java
AdvQueryGlobalConfig config = AdvQueryGlobalConfig.of()
    .addTypeHandler(new MyCustomTypeHandler())
    .turnOnLog();
```

全局注册的 handler 会被 `create()` 工厂方法继承到每个新创建的 Registry 中，但 `AdvQueryGlobalConfig.typeHandlers` 中的 handler 优先级更高。

### 写入绑定逻辑

`AdvPreparedStatementBinder` 通过构造注入持有 `AdvTypeHandlerRegistry`，在绑定参数时调用：

```java
Object jdbcValue = typeHandlerRegistry.convertForWrite(
    value,
    value == null ? Object.class : value.getClass(),
    AdvTypeHandlerContext.simple(null));
preparedStatement.setObject(index, jdbcValue);
```

参数在真正进入 JDBC 之前，会先通过注册表做一次”Java 类型 → JDBC 可写值”的转换。

### 空间类型处理逻辑（按方言拆分）

#### PostGisGeometryAdvTypeHandler（PostgreSQL）

**读取时**：
- `PGobject` → 通过 `GirPostGisJdbcTran` 还原
- PostGIS org 驱动对象 → 通过 `GirPostGisOrgTran` 还原
- PostGIS net 驱动对象 → 通过 `GirPostGisNetTran` 还原
- String（WKT / WKB / GeoJSON）→ 兜底解析

**写入时**：
- 优先转为 PostGIS net 驱动 `PGgeometry` 对象
- 其次转为 PostGIS org 驱动对象
- 兜底回退为 WKT 字符串

#### MysqlGeometryAdvTypeHandler（MySQL）

**读取时**：
- MySQL 二进制几何格式 → 通过 `GirMysqlTran` 还原
- String（WKT / WKB / GeoJSON）→ 兜底解析

**写入时**：
- 直接转为 WKT 字符串（MySQL JDBC 驱动原生支持）

#### OracleGeometryAdvTypeHandler（Oracle）

**读取时**：
- Oracle `SDO_GEOMETRY` 对象 → 通过 `GirOracleSpatialTran` 还原
- String（WKT / WKB / GeoJSON）→ 兜底解析

**写入时**：
- 转为 Oracle Spatial 兼容值
- 兜底回退为 WKT 字符串

#### WktGeometryAdvTypeHandler（达梦 / 通用）

**读取时**：
- String 格式（WKT / WKB / GeoJSON）→ 解析还原

**写入时**：
- 直接转为 WKT 字符串（JDBC 原生兼容）

### 与 @GirAdvTypeHandler 注解的关系

`@GirAdvTypeHandler` 是字段级注解，为特定实体字段指定自定义类型处理器。它与 Registry 是**并行互补**的关系：

- 字段标注了 `@GirAdvTypeHandler` → 优先级最高，直接调用注解指定的 handler
- 字段无注解 → 走 `AdvTypeHandlerRegistry` 全局匹配

方案 D 的改动不影响注解机制，两者完全独立。

### 与 SPI 的关系

SPI 仍然用于加载 6 个**方言无关**的公共 handler。`JtsGeometryAdvTypeHandler` 已从 SPI 中移除，改由各方言 Executor 在 `*AdvBaseOpt` 中按需注册对应的 Geometry handler 实现。

### 适用场景

适合：

- 地图框选查询
- 专题图筛选
- 多表字段组合条件查询
- 分页列表与排序
- 自定义 SQL + 统一分页封装
- 需要把查询能力抽成通用层的 GIS 服务
- 需要在 JDBC 写入 / 查询过程中自动处理 Geometry 参数与结果
- 需要在 Spring 环境中直接把当前数据源自动挂成 `IAdvExecutor`
- 多数据源场景下区分 MySQL / PostgreSQL / Oracle / 达梦的空间类型转换

## 真实示例位置

源码中的主要示例已经在 test 包：

- `WhereQueryExample`
- `LambdaFilterExample`
- `GirAdvQueryRequestExample`
- `GirAdvQueryRequest1Example`

对应目录：

- `geoair-geo/geoair-adv-query/src/test/java/cn/geoair/map/dynamic/adv/query/wherequery/test`

## 核心 API 示例

### 示例1：最基础的查询请求

```java
GirAdvQueryRequest query = GirAdvQueryRequest.builder()
  .table("user")
  .fields("id", "name", "status")
  .where(GirAdvWhereFilter.of()
    .eq("name", "张三")
    .eq("status", 1))
  .build();

GirAdvSqlComposer.SqlBuildResult result = sqlBuilder.buildSelectSql(query);
```

对应测试：`WhereQueryExample`、`GirAdvQueryRequestExample`

### 示例2：比较与范围条件

```java
GirAdvQueryRequest query = GirAdvQueryRequest.builder()
  .table("user")
  .fields("id", "name", "age")
  .where(GirAdvWhereFilter.of()
    .gt("age", 18)
    .in("id", Arrays.asList(1, 2, 3, 4, 5))
    .between("age", 18, 30))
  .build();
```

对应测试：`WhereQueryExample`

### 示例3：复杂嵌套条件

```java
GirAdvQueryRequest query = GirAdvQueryRequest.builder()
  .table("user")
  .fields("id", "name", "age", "status", "dept_id", "role")
  .where(GirAdvWhereFilter.of()
    .like("name", "张")
    .group(group -> group
      .gt("age", 18)
      .or()
      .eq("status", 1))
    .group(group -> group
      .eq("dept_id", 100)
      .or()
      .eq("role", "admin")))
  .build();
```

对应测试：`WhereQueryExample`

### 示例4：分页与排序

```java
GirAdvQueryRequest query = GirAdvQueryRequest.builder()
  .table("user")
  .fields("id", "name", "status", "create_time")
  .where(GirAdvWhereFilter.of().eq("status", 1))
  .orderByDesc("create_time")
  .orderByAsc("id")
  .page(2, 10)
  .build();

GirAdvSqlComposer.SqlBuildResult result = sqlBuilder.buildPageSql(query);
```

对应测试：`WhereQueryExample`、`GirAdvQueryRequest1Example`

### 示例5：Lambda 风格条件

```java
GirAdvWhereLambdaFilter<User> wrapper = GirAdvWhereLambdaFilter.of(User.class)
  .eq(User::getName, "张三")
  .ge(User::getAge, 18)
  .eq(User::getStatus, 1);

GirAdvWhereFilter whereFilter = wrapper.toWhereFilter();
```

对应测试：`LambdaFilterExample`、`GirAdvQueryRequest1Example`

### 示例6：表达式与函数查询

```java
GirAdvWhereLambdaFilter<User> wrapper = GirAdvWhereLambdaFilter.of(User.class)
  .exprEq("YEAR(create_time)", 2024)
  .exprGt("salary * 1.1", new BigDecimal("10000"))
  .exprLike("CONCAT(first_name, ' ', last_name)", "张%");
```

对应测试：`LambdaFilterExample`、`GirAdvQueryRequestExample`

## GitHub 源码入口

- 模块目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-adv-query`
- 示例目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-adv-query/src/test/java/cn/geoair/map/dynamic/adv/query/wherequery/test`
- Spring 集成目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-adv-query/src/main/java/cn/geoair/map/dynamic/adv/spring`
- typehandler 目录（抽象基类 + 方言实现）：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-adv-query/src/main/java/cn/geoair/map/dynamic/adv/query/typehandler`
- 参数绑定目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-adv-query/src/main/java/cn/geoair/map/dynamic/adv/query/mapping`

## 阅读建议

建议顺序：

1. `WhereQueryExample`
2. `LambdaFilterExample`
3. `GirAdvQueryRequestExample`
4. `GirAdvQueryRequest1Example`
5. `AdvAutoConfiguration`
6. `GirSpringAdvExecutor`
7. `AdvTypeHandlerRegistry`
8. `AdvPreparedStatementBinder`
9. `JtsGeometryAdvTypeHandler`（抽象基类）
10. `PostGisGeometryAdvTypeHandler`（PG 方言实现）
11. `MysqlGeometryAdvTypeHandler`（MySQL 方言实现）
12. `OracleGeometryAdvTypeHandler`（Oracle 方言实现）
13. `WktGeometryAdvTypeHandler`（达梦/通用实现）
14. `AdvQueryGlobalConfig`（配置与自定义 handler 入口）
15. `PgAdvBaseOpt` / `MysqlAdvBaseOpt`（方言 Registry 创建入口）

先看查询请求怎么组织，再看 Spring 集成与自动装配，然后看 typehandler 的抽象与方言分离设计，最后再看配置入口，会更容易把整套 API 吃透。
