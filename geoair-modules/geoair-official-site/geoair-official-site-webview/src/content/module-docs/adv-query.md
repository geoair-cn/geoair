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

与旧版不同，当前版本的核心设计决策是：

- **`AdvTypeHandlerRegistry` 不再是全局单例**：每个数据库方言执行器拥有独立的 Registry 实例
- **Geometry handler 按方言拆分**：原来一个 `JtsGeometryAdvTypeHandler` 负责所有数据库，改为每个方言一个独立实现
- **`AdvQueryGlobalConfig` 承载用户自定义 handler**：通过 `addTypeHandler()` 注册，优先级高于 SPI 默认处理器

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

`AdvTypeHandlerRegistry` 通过工厂方法 `create(DialectName, List<AdvTypeHandler<?>>)` 创建，加载顺序为：

1. **SPI 加载公共 handlers**（方言无关）：
   - `BooleanAdvTypeHandler`
   - `ByteArrayAdvTypeHandler`
   - `CharacterAdvTypeHandler`
   - `EnumAdvTypeHandler`
   - `NumberAdvTypeHandler`
   - `TemporalAdvTypeHandler`

2. **用户自定义 handlers**（来自 `AdvQueryGlobalConfig.typeHandlers`）：优先级高于 SPI

3. **方言专属 Geometry handler**（优先级最高）：
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

在 `AdvQueryGlobalConfig` 中添加自定义处理器：

```java
AdvQueryGlobalConfig config = AdvQueryGlobalConfig.of()
    .addTypeHandler(new MyCustomTypeHandler())
    .turnOnLog();
```

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
