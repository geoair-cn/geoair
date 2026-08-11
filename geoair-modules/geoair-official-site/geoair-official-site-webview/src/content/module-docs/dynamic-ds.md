## 模块定位

`geoair-dynamic-ds` 负责动态数据源相关能力，但它关注的不只是“切库”，还包括：

- 在 Spring 中如何启用动态数据源切面
- 方法执行前后怎样维护数据源上下文
- 主从读写分离怎样通过 builder 组织
- SQL 语句怎样判断读 / 写类型

在需要同时连接多个库、专题库或主从库的 GIS 服务中，这个模块负责把这些访问链路组织起来。

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

## 动态数据源存储管理

`AdvDynamicDataSourceStorage` 是动态数据源的中央存储器，全局单例，负责数据源的查找、创建、缓存和生命周期管理。

### 核心组件协作

```
AdvDynamicDataSourceStorage (全局单例)
  ├── IAdvDataSourceHelper     → 根据 ID 查找 DataSourceApo 配置
  ├── IAdvDataSourceInitHelper → 将 DataSourceApo 转为物理 DataSource
  └── ConcurrentHashMap         → 缓存 dataSourceId → AdvDataSourceWrapper
```

### 核心 API

```java
// 获取全局实例
DynamicDataSourceManager manager = AdvDynamicDataSourceStorage.getInstance();

// 注入 helper（非 Spring 环境推荐，避免自动查找 Bean 失败）
DynamicDataSourceManager manager = AdvDynamicDataSourceStorage.getInstance(myHelper);

// 按 ID 获取或创建数据源（不存在则触发创建）
AdvDataSourceWrapper ds = manager.getOrCreateDataSource("master");

// 只读查询（不存在返回 null，不触发创建）
AdvDataSourceWrapper ds = manager.getDataSourceById("extra");

// 手动注册数据源
manager.registerDataSource("custom", someDataSource);

// 移除并关闭数据源
manager.removeDataSource("obsolete");
```

### 创建流程

`getOrCreateDataSource(dataSourceId)` 内部执行：

1. 检查缓存 `ConcurrentHashMap`，命中直接返回
2. 未命中 → 加同步锁，双重检查避免并发重复创建
3. 调用 `IAdvDataSourceHelper.getDataSourceApoById(id)` 获取配置
4. 调用 `IAdvDataSourceInitHelper.getDbDataSourceByApo(apo)` 创建物理连接池
5. 包装为 `AdvDataSourceWrapper` 并缓存
6. **创建失败则直接抛出异常**（不返回 null，调用方无需 null 检查）

### 线程安全设计

| 维度 | 方案 |
|------|------|
| 单例创建 | `volatile` + DCL（双重检查锁定） |
| 数据源缓存 | `ConcurrentHashMap` |
| 数据源创建 | `synchronized` + DCL（同一 ID 只创建一次） |
| Helper 懒加载 | `synchronized` + DCL（仅首次获取时触发 Spring 查找） |

### Spring 集成与手动注入

`IAdvDataSourceHelper` 和 `IAdvDataSourceInitHelper` 的获取策略：

1. **优先使用手动注入**（通过 setter 或 `getInstance(helper)`），避免依赖 Spring 容器
2. **若未注入**，自动从 `Gir.beans`（Spring 容器）获取
3. **若两者都不可用**，抛出 `RuntimeException` 并提示调用方手动注入

```java
// 方式一：通过 getInstance 注入
DynamicDataSourceManager manager = AdvDynamicDataSourceStorage.getInstance(helper);

// 方式二：通过 setter 注入
AdvDynamicDataSourceStorage storage = (AdvDynamicDataSourceStorage) AdvDynamicDataSourceStorage.getInstance();
storage.setIAdvDataSourceHelper(helper);
storage.setAdvDataSourceInitHelper(initHelper);
```

### JVM 关闭清理

私有构造中注册了 `ShutdownHook`，JVM 关闭时自动：
- 遍历所有缓存的 `AdvDataSourceWrapper`
- 逐一调用 `close()` 释放连接池
- 清空缓存 Map

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
3. `AdvDynamicDataSourceStorage`（数据源存储的源头，理解数据源如何创建与缓存）
4. `WithStatementTest`
5. 主源码里的 `EnableDynamicDs` / `GirDsAspectDoAroundApiHelper` / `GirDynamicStackDataSource`

可以先理解主从 builder，再看静态存储管理，再回到 Spring 切面上下文如何把这些能力串起来。
