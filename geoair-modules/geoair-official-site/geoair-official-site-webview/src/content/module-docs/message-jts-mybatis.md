## 模块定位

`geoair-message-jts-mybatis` 负责把 JTS Geometry 与 MyBatis 的类型处理层接起来。

适用场景包括：

- 在 MyBatis 中直接映射 Geometry 字段
- 对接不同 PostGIS JDBC 版本
- 统一注册 Geometry TypeHandler

这个模块就是数据库映射层的入口。

## 核心类

最值得先读的类：

- `PgGeometryTypeHandler`
- `OrgPgGeometryTypeHandler`
- `NetPgGeometryTypeHandler`
- `GirMyBatisConfigurationCustomizer`
- `GirMybatisJtsAutoConfiguration`

### TypeHandler

这一层负责：

- PostgreSQL / PostGIS Geometry 类型映射
- 不同驱动实现的兼容
- 把数据库字段转换成 JTS Geometry

`PgGeometryTypeHandler` 本身是一个总入口，它会根据：

- `GirPostGisTran.isNetConvert()`
- `GirPostGisTran.isOrgConvert()`

决定委托给：

- `NetPgGeometryTypeHandler`
- `OrgPgGeometryTypeHandler`

这意味着：

- 使用者面对的是统一的 `PgGeometryTypeHandler`
- 底层驱动差异由模块内部再做分发

### MyBatis 配置扩展

- `GirMyBatisConfigurationCustomizer`
- `GirMybatisJtsAutoConfiguration`

这一层负责把 Geometry TypeHandler 接入 MyBatis 配置链。

## 最小示例

```java
PgGeometryTypeHandler handler = new PgGeometryTypeHandler();
Geometry geometry = new WKTReader().read("POINT (116.40 39.90)");

System.out.println(handler.getClass().getSimpleName());
System.out.println(geometry);
System.out.println(JdbcType.OTHER);
```

对应测试：`PgGeometryTypeHandlerExample`

## 和上层模块的关系

这一层通常会和：

- `geoair-jts-all`
- `geoair-geo-tools`
- `geoair-adv-query`
- MyBatis DAO / Mapper 层

配合使用。

典型关系是：

1. `geo-tools` 负责生成或处理 Geometry
2. `message-jts-mybatis` 负责把 Geometry 映射到数据库字段和查询结果
3. `adv-query` 或 Mapper 层负责组织查询与更新逻辑

## GitHub 源码入口

- 模块目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-message-jts-mybatis`
- TypeHandler 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-message-jts-mybatis/src/main/java/cn/geoair/comp/message/converter/jts/mybatis/typehander`
- config 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-message-jts-mybatis/src/main/java/cn/geoair/comp/message/converter/jts/mybatis/config`
- 测试目录：
  - `geoair-message-jts-mybatis/src/test/java/cn/geoair/comp/message/converter/jts/mybatis/test/PgGeometryTypeHandlerExample.java`

## 阅读建议

建议顺序：

1. `PgGeometryTypeHandler`
2. `PgGeometryTypeHandlerExample`
3. `OrgPgGeometryTypeHandler` / `NetPgGeometryTypeHandler`
4. `GirMyBatisConfigurationCustomizer`
5. `GirMybatisJtsAutoConfiguration`

先理解字段映射和最小示例，再回头看配置接入链会更顺。
