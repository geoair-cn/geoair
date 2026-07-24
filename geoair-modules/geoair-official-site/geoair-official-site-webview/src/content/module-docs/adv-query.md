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

所以当你看到实现类同时具备查、增、改、删能力时，本质上就是通过 `IAdvBaseOpt` 把这四组基础接口拼起来了。

## typehandler 使用与注册逻辑

`adv-query` 自己内部就有一套类型处理链，不是完全依赖外部 ORM。

### 入口对象

最关键的三个类是：

- `AdvTypeHandlerRegistry`
- `AdvPreparedStatementBinder`
- `JtsGeometryAdvTypeHandler`

### 注册逻辑

`AdvTypeHandlerRegistry` 在构造时会默认注册一组处理器：

- `JtsGeometryAdvTypeHandler`
- `StringAdvTypeHandler`
- `CharacterAdvTypeHandler`
- `BooleanAdvTypeHandler`
- `NumberAdvTypeHandler`
- `TemporalAdvTypeHandler`
- `ByteArrayAdvTypeHandler`
- `EnumAdvTypeHandler`

也就是说，这个模块本身已经预置了：

- 空间类型处理
- 字符串处理
- 数值处理
- 时间类型处理
- 布尔类型处理
- 枚举处理
- 字节数组处理

如果没有匹配到任何具体 handler，则会回退到：

- `ObjectAdvTypeHandler`

### 写入绑定逻辑

`AdvPreparedStatementBinder` 会在绑定参数时调用：

```java
Object jdbcValue = typeHandlerRegistry.convertForWrite(
    value,
    value == null ? Object.class : value.getClass(),
    AdvTypeHandlerContext.simple(null));
preparedStatement.setObject(index, jdbcValue);
```

也就是说，参数在真正进入 JDBC 之前，会先通过注册表做一次“Java 类型 -> JDBC 可写值”的转换。

### 空间类型处理逻辑

`JtsGeometryAdvTypeHandler` 负责 Geometry 类型，它的读写逻辑大致是：

#### 读取时
尝试把以下对象还原成 `Geometry`：

- `Geometry`
- `String`（WKT / WKB / GeoJSON）
- `PGobject`
- PostGIS 不同驱动返回对象
- MySQL Geometry 二进制
- Oracle Spatial SDO Geometry

#### 写入时
优先把 Geometry 转成：

- PostGIS Net 驱动对象
- PostGIS Org 驱动对象
- Oracle Spatial 兼容值
- 如果都不适用，则回退成 WKT 字符串

这意味着 `adv-query` 本身就已经把“空间对象参数如何进 JDBC”这件事抽象掉了。

## 适用场景

适合：

- 地图框选查询
- 专题图筛选
- 多表字段组合条件查询
- 分页列表与排序
- 自定义 SQL + 统一分页封装
- 需要把查询能力抽成通用层的 GIS 服务
- 需要在 JDBC 写入 / 查询过程中自动处理 Geometry 参数与结果

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
- typehandler 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-adv-query/src/main/java/cn/geoair/map/dynamic/adv/query/typehandler`
- 参数绑定目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-adv-query/src/main/java/cn/geoair/map/dynamic/adv/query/mapping`

## 阅读建议

建议顺序：

1. `WhereQueryExample`
2. `LambdaFilterExample`
3. `GirAdvQueryRequestExample`
4. `GirAdvQueryRequest1Example`
5. `AdvTypeHandlerRegistry`
6. `AdvPreparedStatementBinder`
7. `JtsGeometryAdvTypeHandler`

先看查询请求怎么组织，再看类型参数如何进入 JDBC，会更容易把这套 API 吃透。
