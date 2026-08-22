## 我为什么新增这个模块

在动态数据源、数据库服务和空间查询中，过去多处都需要从 JDBC URL 读取主机、端口、库名或 schema，也需要替换 schema。直接用 `split("?")`、`substring(...)` 处理连接串，看似简单，却很容易把 SQL Server 的分号参数、Oracle 的 SID/服务名、H2 文件路径或驱动私有参数处理坏。

`geoair-jdbc-url` 将这些差异收敛为一个独立组件：**解析为结构化对象，再由对应方言格式化或重写**。

## 核心设计

```
GirJdbcUrlCodecs
    ↓ defaultCodec()
JdbcUrlCodec
    ├── parse：连接串 → JdbcUrl
    ├── create：结构化字段 → JdbcUrl
    ├── format：JdbcUrl → 连接串
    ├── withProperty：新增或替换驱动参数
    └── rewriteSchema / getSchema：方言化 schema 语义
          ↓
JdbcUrlDialect 的独立实现（PostgreSQL、MySQL、Oracle、SQL Server、H2、SQLite 等）
```

`JdbcUrl` 会保留原始主体、端点列表、数据库名、参数顺序和参数风格。未知驱动按不透明 URL 返回，不猜测、不重组，避免一次“修复”破坏驱动私有格式。

## 最小使用方式

```java
JdbcUrlCodec codec = GirJdbcUrlCodecs.defaultCodec();

JdbcUrl url = codec.parse(
    "jdbc:postgresql://127.0.0.1:5432/gis?currentSchema=public&sslmode=disable");

String schema = codec.getSchema(url.getRawUrl());
String tenantUrl = codec.rewriteSchema(url.getRawUrl(), "tenant_gis");

JdbcUrl mysql = codec.create(DatabaseType.MYSQL, "db.example.com", 3306, "geoair");
mysql = codec.withProperty(mysql, "characterEncoding", "utf8");
String jdbcUrl = codec.format(mysql);
```

对 PostgreSQL，`rewriteSchema` 会改写 `currentSchema`；对 SQL Server、Oracle、H2 则使用各自定义的参数语义。不支持 URL schema 语义的数据库会明确拒绝重写，而不是静默生成不可用连接串。

## 在项目中怎么接入

```xml
<dependency>
  <groupId>cn.geoair.devkit</groupId>
  <artifactId>geoair-jdbc-url</artifactId>
</dependency>
```

如果项目已使用 GeoAir 的项目父 POM，版本由 BOM 管理，无需在这里单独写版本号。

## 与旧 API 的关系

`geoair-dynamic-ds` 中原有的 `AdvJdbcUrlUtil` 与 `JdbcUrlSplitter` 已保留以兼容既有代码，但标记为过时，并内部委托给新编解码器。新代码应从 `GirJdbcUrlCodecs.defaultCodec()` 获取 `JdbcUrlCodec`，不要再自行按字符位置解析 JDBC URL。

## 扩展自己的数据库

方言实现都位于 `impl` 包，并通过 `JdbcUrlDialect` 表达匹配、解析、构建与 schema 参数规则。业务方如有私有驱动，可以实现该接口并注册到 `DefaultJdbcUrlCodec`；后注册的方言优先匹配，因此能安全覆盖内置处理。
