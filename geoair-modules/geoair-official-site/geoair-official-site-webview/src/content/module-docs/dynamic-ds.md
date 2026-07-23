## 模块定位

`geoair-dynamic-ds` 负责动态数据源相关能力，但它关注的不只是“切库”，还包括：

- 在 Spring 中如何启用动态数据源切面
- 方法执行前后怎样维护数据源上下文
- 主从读写分离怎样通过 builder 组织
- SQL 语句怎样判断读 / 写类型

如果你在一个 GIS 服务里同时连接多个库、专题库或主从库，这个模块就是把这些访问链路组织起来的那一层。

## 核心入口

### Spring 启用入口

```java
@EnableDynamicDs
@SpringBootApplication
public class Application {
}
```

### 切面辅助入口

核心接口：

- `GirDsAspectDoAroundApiHelper`

这层负责在切面前后调用：

- `GirDynamicStackDataSource.pushDataSource(...)`
- `GirDynamicStackDataSource.popDataSource()`

### 读写分离 builder

核心入口：

- `GirReadWriteDataSourceBuilder`

### SQL 读写识别

核心入口：

- `SQLParserUtil`
- `WithStatementTest`

## 真实示例位置

当前最重要的测试示例位于：

- `geoair-dynamic-ds/src/test/java/cn/geoair/comp/dynamic/ds/readwrite/test`

重点示例类：

- `BuilderTest`
- `SQLParserUtilTest`
- `WithStatementTest`

## 核心 API 示例

### 示例1：启用动态数据源切面

```java
@EnableDynamicDs
@SpringBootApplication
public class Application {
}
```

对应测试 / 源码入口：主源码中的 `EnableDynamicDs`

### 示例2：切面前后维护数据源栈

```java
default void doBefore(Method method, ProceedingJoinPoint point) {
    String dataSourceKey = getDataSourceKey(groupName, rwType);
    if (dataSourceKey != null) {
        GirDynamicStackDataSource.pushDataSource(dataSourceKey);
    }
}

default void onFinally(Method method, ProceedingJoinPoint point) {
    GirDynamicStackDataSource.popDataSource();
}
```

对应源码入口：`GirDsAspectDoAroundApiHelper`

### 示例3：主从读写分离 builder

```java
GirReadWriteDataSource dataSource = GirReadWriteDataSourceBuilder.builder()
  .master("master_db")
  .slaves("slave1", "slave2", "slave3")
  .slaveStrategy(LoadStrategyType.ROUND_ROBIN)
  .slaveGroupName("mySlaveGroup")
  .build();
```

对应测试：`BuilderTest`

### 示例4：混合添加从库

```java
GirReadWriteDataSource dataSource = GirReadWriteDataSourceBuilder.builder()
  .master("master_db")
  .addSlave("slave1")
  .addSlave("slave2")
  .addSlave("slave3")
  .slaveStrategy(LoadStrategyType.WEIGHT)
  .build();
```

对应测试：`BuilderTest`

### 示例5：静态快速构建方式

```java
GirReadWriteDataSource dataSource = GirReadWriteDataSourceBuilder
  .build("master_db", Arrays.asList("slave1", "slave2", "slave3"));
```

对应测试：`BuilderTest`

### 示例6：SQL 读写识别

```java
SQLType type1 = SQLParserUtil.getSQLType("SELECT * FROM user");
SQLType type2 = SQLParserUtil.getSQLType("WITH temp AS (SELECT id FROM user) SELECT * FROM temp");
boolean isRead = SQLParserUtil.isReadOperation("SELECT * FROM user");
boolean isWrite = SQLParserUtil.isWriteOperation("UPDATE user SET name = 'new' WHERE id = 1");
```

对应测试：`SQLParserUtilTest`、`WithStatementTest`

## GitHub 源码入口

- 模块目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-dynamic-ds`
- 测试目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-dynamic-ds/src/test/java/cn/geoair/comp/dynamic/ds/readwrite/test`

## 阅读建议

建议顺序：

1. `BuilderTest`
2. `SQLParserUtilTest`
3. `WithStatementTest`
4. 主源码里的 `EnableDynamicDs` / `GirDsAspectDoAroundApiHelper` / `GirDynamicStackDataSource`

这样可以先理解主从 builder，再看 SQL 识别，再回到 Spring 切面上下文如何把这些能力串起来。
