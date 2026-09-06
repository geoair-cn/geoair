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
Oracle 与达梦。

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

SQLite/SpatiaLite 仍需独立的原生执行器，不能复用 MySQL 或 PostgreSQL 的空间 DDL 和函数语法。
