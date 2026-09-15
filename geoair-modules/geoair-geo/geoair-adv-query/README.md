# GeoAir AdvQuery

`geoair-adv-query` 是 GeoAir 的动态数据库访问层，统一提供 CRUD、DDL、分页、动态条件、
事务和空间几何处理能力。每个数据库方言由独立的 `IAdvExecutor` 实现负责，因此不会把一种
数据库的空间 SQL 直接用于另一种数据库。

## 已支持的方言

| 方言标识 | JDBC 产品名识别 | 执行器 |
| --- | --- | --- |
| `mysql` | MySQL | MySQL Spatial 原生实现 |
| `mariadb` | MariaDB | MySQL 兼容实现；空间函数以目标 MariaDB 版本为准 |
| `postgresql` | PostgreSQL | PostgreSQL/PostGIS 原生实现 |
| `kingbase` | KingbaseES | PostgreSQL/PostGIS 兼容实现；要求部署开启相应兼容能力 |
| `opengauss` | openGauss | PostgreSQL 兼容实现；空间能力以部署扩展为准。GaussDB 产品线请显式指定方言或注册专用 Provider。 |
| `sqlserver` | Microsoft SQL Server | SQL Server `geometry` 原生实现 |
| `oracle` | Oracle | Oracle Spatial 原生实现 |
| `dm` | 达梦 | 达梦原生实现 |
| `sqlite` | SQLite | SQLite Core；不包含 SpatiaLite 空间能力 |

普通场景保持原有调用方式，工厂会根据 JDBC 元数据选择注册方言：

```java
IAdvExecutor executor = GirAdvQuery.getIAdvExecutor(dataSource);
```

已明确数据库类型时，推荐跳过一次 JDBC 元数据探测：

```java
IAdvExecutor executor = AdvExecutorFactory.getAdvExecutorByDialectId(
        "kingbase", dataSource, "gis-kingbase");
```

旧的 `getAdvExecutorByDialect(DialectName, ...)` API 仍然可用，兼容 MySQL、PostgreSQL、
Oracle、达梦与 SQLite（`DialectName.SQLITE3`）。

## SQLite Core

应用需要显式引入 Xerial JDBC 驱动（本模块将它声明为 optional，避免强制传递给非 SQLite 项目）：

```xml
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>${sqlite-jdbc.version}</version>
</dependency>
```

数据源 URL 示例：`jdbc:sqlite:D:/data/app.db`，驱动类为 `org.sqlite.JDBC`，无需用户名和密码。
本实现已使用项目当前的 Xerial `3.41.2.2` 验证；若业务替换为更旧驱动，至少需要 SQLite
`3.35.0` 才能使用本文列出的 `ALTER TABLE ... DROP COLUMN` 能力。

当前 SQLite Core 支持：

- 原始 SQL、动态参数 SQL、普通条件查询、CRUD、批量操作；
- `INSERT ... ON CONFLICT` UPSERT，以及指定冲突列的 `DO NOTHING`；
- `LIMIT ... OFFSET ...` 分页和事务提交/回滚；
- `main` 库的表、视图、字段、主键、普通索引和函数元数据；
- 清空数据、删表、表重命名、删除普通列、创建/删除普通索引；
- 通过 CTAS 复制查询结果或表的数据形状。

边界说明：

- 不支持 Geometry/SRID、空间查询、空间索引和 `ST_*` 函数；所有空间 API 会明确提示需要后续 SpatiaLite 方言。普通 TEXT/BLOB 中可以保存 WKT/WKB，但只按普通值处理。
- 不支持 `CREATE/DROP SCHEMA`。SQLite 的 `main`、`temp` 和 `ATTACH` 是连接级命名空间；在多连接数据源中，不能把一次连接上的 `ATTACH` 当成全局 schema。
- 不支持直接修改列定义或事后增删主键。此类变更应使用“建新表、复制数据、替换旧表”的显式迁移。
- `dTruncateTable` 实际执行 `DELETE`，不会承诺重置 ROWID 或 `sqlite_sequence`。
- CTAS 只复制结果列和数据，不保留原表的主键、唯一约束、外键、默认值、索引及触发器。
- SQLite 没有稳定、通用的逐表物理大小统计，`dGetTableSize` 返回 `null`。
- `EQUAL_NULL_SAFE` 映射为 SQLite 的 `IS`；`ILIKE`、`ANY`、`ALL` 会明确拒绝。大小写不敏感查询请由业务 SQL 显式指定合适的 `COLLATE`。
- 字段类型按 SQLite 的 INTEGER/TEXT/BLOB/REAL/NUMERIC 五类亲和性识别，不套用 MySQL/PostgreSQL 类型系统。
- 文件库支持多连接读取，但 SQLite 同一时刻通常只有一个写事务。业务侧应按负载配置 busy timeout/WAL。分页会并行执行 count 和 data 查询，多连接场景不要使用彼此隔离的普通 `jdbc:sqlite::memory:`。

## 扩展新数据库

新增数据库应实现 `AdvDialectProvider`，并由提供者创建完整的原生 `IAdvExecutor`。一个原生执行器至少需要校验：

1. 标识符引用、分页、CRUD 与 UPSERT；
2. 建表、字段/索引/主键等 DDL；
3. Geometry/Geography 参数读写、SRID 和空间索引；
4. 相交、范围、距离、面积等空间 SQL；
5. 真实数据库集成测试。

```java
AdvExecutorFactory.registerProvider(new SqlServerAdvDialectProvider());
```

SQL Server 默认使用平面 `geometry`。其没有服务端投影转换函数，`eTransformSrid` 会明确拒绝调用；
请先通过 `geoair-geo-tools` 转换坐标后写回，避免把仅修改 SRID 标签误认为真正的投影转换。

SpatiaLite 后续仍需独立方言，不能复用 MySQL、PostgreSQL 或 SQLite Core 的空间 DDL 和函数语法。
