# GeoAir File Transfer

`geoair-file-tran` 用于在空间文件与空间数据库之间传输矢量要素。原有的 `GeoFileTran`、
CSV、Shapefile、GeoJSON 与 PostGIS API 均保持可用；新增的 Core V2 将一次传输收敛为一个
请求对象，避免旧实现通过多个可变 setter 维护运行时状态。

## 模块分层

| 模块 | 职责 |
| --- | --- |
| `geoair-file-core` | 文件读写抽象、原有 `GeoFileTran` 和无状态的 Core V2 传输引擎。 |
| `geoair-file-jdbc` | JDBC 空间读取/写入公共实现，统一 JDBC URL 解析、分页排序与资源管理。 |
| `geoair-file-postgis` | 保留原有 PostGIS 实现，同时提供基于 JDBC 公共层的 V2 适配器。 |
| `geoair-file-oracle` | Oracle Spatial（`SDO_GEOMETRY`）适配器。 |
| `geoair-file-mysql` | MySQL 8 Spatial 适配器；明确不支持 MySQL 5.7。 |
| `geoair-file-shp`、`geoair-file-geojson`、`geoair-file-csv` | 文件格式读写实现。 |

## Core V2 的使用方式

读取器和写入器先各自完成连接配置，再提交给无状态引擎。一个 `GeoTransferEngine` 可以在
多个任务间复用；每一次调用都拥有独立的 `GeoTransferRequest` 和结果对象。

```java
MysqlJdbcGeoFileReader reader = new MysqlJdbcGeoFileReader();
reader.setLinkInfo(new MysqlJdbcReadLinkInfo()
        .setQuerySql("select id, name, geom from source_feature")
        .setOrderBy("id")
        .setJdbcUrl("jdbc:mysql://127.0.0.1:3306/source_db")
        .setUsername("reader")
        .setPassword("***")
        .setSrid(4326));

MysqlJdbcGeoFileWriter writer = new MysqlJdbcGeoFileWriter();
writer.setLinkInfo(new MysqlJdbcWriterLinkInfo()
        .setTableName("target_feature")
        .setJdbcUrl("jdbc:mysql://127.0.0.1:3306/target_db")
        .setUsername("writer")
        .setPassword("***")
        .setSrid(4326));

GeoTransferResult result = GeoTransferEngine.create().execute(new GeoTransferRequest()
        .setReader(reader)
        .setWriter(writer)
        .setWriteConfig(new WriteConfig().setOverwrite(true).setOutPutSrid(4326))
        .setOptions(new GeoTransferOptions()
                .setBatchSize(3000)
                .setErrorPolicy(ErrorPolicy.SKIP_RECORD)));
```

`orderBy` 是 JDBC 读取配置的必填项。V2 使用 `offset/limit` 分页时必须依赖唯一、稳定的排序，
例如主键 `id` 或 `id, version`；否则数据库在数据变化或执行计划变化时可能出现漏读、重读。

## 选择错误策略

- `FAIL_FAST`：任意批次写入失败后终止任务，适用于要求原子性或由外部事务控制的任务。
- `SKIP_BATCH`：记录该批次失败并继续下一批。
- `SKIP_RECORD`：批量失败后逐条重试，保留成功记录并统计失败记录。

## 数据库注意事项

- PostGIS、Oracle Spatial、MySQL 8 都通过 `geoair-jdbc-url` 统一解析 JDBC URL，业务代码不再自行拆分主机、端口和库名。
- Oracle 的 `schema` 通常对应数据库用户；请为目标用户授予建表、建索引及空间元数据相关权限。
- MySQL 适配器按 MySQL 8 的 JDBC 驱动和 Spatial 行为实现，表建议使用 InnoDB，并为稳定排序字段建立索引。
- V2 写入端通过 `GirAdvQuery` 处理数据库方言的几何 DDL 和几何参数，不应再手工拼接几何 SQL。
