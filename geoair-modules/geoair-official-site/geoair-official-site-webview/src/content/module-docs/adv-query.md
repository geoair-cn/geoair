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

## 适用场景

适合：

- 地图框选查询
- 专题图筛选
- 多表字段组合条件查询
- 分页列表与排序
- 自定义 SQL + 统一分页封装
- 需要把查询能力抽成通用层的 GIS 服务

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

## 阅读建议

建议顺序：

1. `WhereQueryExample`
2. `LambdaFilterExample`
3. `GirAdvQueryRequestExample`
4. `GirAdvQueryRequest1Example`

先看基础串式条件，再看 Lambda 风格，再看复杂查询请求对象的组织方式，会更容易把这套 API 吃透。
