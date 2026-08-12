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

## 策略对象 —— 细粒度控制 CRUD 行为

`AccessStrategy`、`UpdateStrategy`、`DeleteStrategy` 三个策略类用于在调用 opt 方法时精确控制字段映射、表名覆盖、冲突处理等行为，无需修改实体类注解。

### 三种策略概览

| 策略 | 典型场景 | 独有字段 |
|------|---------|---------|
| `AccessStrategy` | 插入 / 批量插入 / INSERT IGNORE | `conflictKeys`（冲突判定的列） |
| `UpdateStrategy` | 按主键更新 / 按条件更新 / UPSERT | `conflictKeys`（UPSERT 冲突列） |
| `DeleteStrategy` | 按主键删除 / 按条件删除 | — |

**共用字段**（三个策略都有）：

| 字段 | 默认值 | 说明 |
|------|--------|------|
| `tableName` | null（走注解） | 覆盖实体类的 `@Table` 注解 |
| `idKey` | null（走注解） | 覆盖实体类的 `@Id` 注解 |
| `toUnderlineCase` | true | 驼峰 `userName` → 下划线 `user_name` |
| `ignoreNullValue` | true | 是否跳过值为 null 的字段 |
| `ignoreFieldNames` | 空列表 | 显式排除的字段名列表 |
| `batchSize` | 1000 | 批量操作的批次大小 |

### AccessStrategy — 插入策略

**Lambda 风格**（推荐，IDE 自动补全）：

```java
// 选择性插入：自动跳过 null 值
executor.bInsertSelectiveOne(user, strategy -> strategy
    .tableName("t_user")
    .ignoreField("createTime", "updateTime"));

// 批量插入：覆盖表名 + 忽略字段
executor.bInsertBatch(users, strategy -> strategy
    .tableName("t_user")
    .batchSize(500)
    .ignoreField("role"));

// INSERT IGNORE：指定冲突列（PostgreSQL 用 ON CONFLICT，MySQL 用 INSERT IGNORE）
executor.bInsertIgnore(user, strategy -> strategy
    .tableName("t_user")
    .conflictKey("id"));

// 批量 INSERT IGNORE
executor.bInsertIgnoreBatch(users, strategy -> strategy
    .tableName("t_user")
    .conflictKey("email")
    .batchSize(200));
```

**构造对象风格**：

```java
AccessStrategy strategy = new AccessStrategy()
    .setTableName("t_user")
    .setIgnoreNullValue(false)    // 插入 null 值
    .setBatchSize(500)
    .setIgnoreFieldNames(Arrays.asList("createTime"));
executor.bInsertOne(user, strategy);
```

**内置快捷方法**（无需传策略）：

```java
// 普通插入（toUnderlineCase=true, ignoreNullValue=false）
executor.bInsertOne(user);

// 选择性插入（toUnderlineCase=true, ignoreNullValue=true）
executor.bInsertSelectiveOne(user);
```

### UpdateStrategy — 更新策略

```java
// 按主键选择性更新（只更新非 null 字段）
executor.bUpdateByPKSelective(user, strategy -> strategy
    .tableName("t_user")
    .idKey("userId")               // 覆盖主键字段名
    .ignoreField("password"));     // 排除敏感字段

// 按主键全量更新（包含 null 值）
executor.bUpdateByPK(user, strategy -> strategy
    .tableName("t_user")
    .setIgnoreNullValue(false));

// 批量按主键更新
executor.bUpdateBatchByPK(users, strategy -> strategy
    .batchSize(300));

// UPSERT：存在则更新，不存在则插入
executor.bUpsert(user, strategy -> strategy
    .tableName("t_user")
    .conflictKey("email")           // 以 email 为冲突判定
    .ignoreField("createTime"));    // 冲突时不更新创建时间

// 批量 UPSERT
executor.bUpsertBatch(users, strategy -> strategy
    .tableName("t_user")
    .conflictKey("email", "phone"));

// 按条件更新 + Lambda 条件
executor.bUpdateByWhere(user,
    strategy -> strategy.tableName("t_user").ignoreField("password"),
    where -> where.eq(User::getStatus, 1));
```

### DeleteStrategy — 删除策略

```java
// 按主键删除（实体中 @Id 字段的值作为 WHERE 条件）
executor.bDeleteByPK(user, strategy -> strategy
    .tableName("t_user")
    .idKey("userId"));

// 批量按主键删除
executor.bDeleteBatchByPK(users, strategy -> strategy
    .tableName("t_user"));

// 按条件删除 + Lambda 条件
executor.bDeleteByWhere(
    strategy -> strategy.tableName("t_user"),
    where -> where.eq(User::getStatus, 0).lt(User::getCreateTime, someDate));
```

### 策略解析优先级

策略对象中设置的值会覆盖实体类注解，未设置的值回退到默认行为：

```
Strategy 显式设置 > @Table / @Id / @Column 注解 > 驼峰转下划线默认行为
```

例如：`strategy.tableName("t_user")` 会覆盖实体类上的 `@Table(name = "user")`。

## DDL 操作 —— IAdvDDLOpt

`IAdvDDLOpt` 提供表结构管理能力，所有方法以 `d` 开头。支持建表、改表、索引、主键、Schema 管理等操作，内部已处理多数据库方言差异。

### 表操作

```java
// 创建表
List<FieldBySchemaApo> fields = Arrays.asList(
    new FieldBySchemaApo().setColumnName("id").setUdtName("INTEGER"),
    new FieldBySchemaApo().setColumnName("name").setUdtName("VARCHAR").setCharacterMaximumLength(100),
    new FieldBySchemaApo().setColumnName("geom").setUdtName("GEOMETRY")
);
executor.dCreateTable("public.cities", fields, "id");

// 判断表是否存在
if (executor.dIsTableExists("public.cities")) {
    executor.dDropTable("public.cities");
}

// 重命名表
executor.dRenameTable("public.cities_old", "public.cities_new");

// 清空表数据（比 DELETE 高效）
executor.dTruncateTable("public.temp_data");

// 查询当前 Schema 下的所有表
List<String> tables = executor.dGetTablesBySchema("public");

// 查询表与视图
List<SchemaTableApo> tableAndViews = executor.dGetTableAndViewBySchema("public");

// 获取表注释
String comment = executor.dGetTableComment("public.cities");

// 获取表大小
String size = executor.dGetTableSizeFormat("public.cities");  // "10 MB"
Long bytes = executor.dGetTableSize("public.cities");
```

### 字段操作

```java
// 查看表字段
DataFieldsApo columns = executor.dGetColumnsByTable("public.cities");
DataFieldsApo columnsBySql = executor.dGetColumnsBySQL("SELECT * FROM public.cities WHERE id > 100");

// 添加字段
executor.dAddColumn("public.cities",
    new FieldBySchemaApo().setColumnName("population").setUdtName("INTEGER"));

// 修改字段
executor.dAlterColumn("public.cities", "population",
    new FieldBySchemaApo().setColumnName("pop").setUdtName("BIGINT"));

// 删除字段
executor.dDropColumn("public.cities", "pop");
```

### 主键与索引

```java
// 添加整数自增主键（SERIAL / AUTO_INCREMENT）
executor.dAddIntAutoPrimaryKey("public.cities", "id", null);

// 添加长整数自增主键（BIGSERIAL，推荐）
executor.dAddBigIntAutoPrimaryKey("public.cities", "id", null);

// 添加字符串主键（如 file_20260811_0001）
executor.dAddStringPrimaryKey("public.files", "file_no", 50, null, "file_");

// 添加普通索引
executor.dCreateIndex("public.cities", "idx_city_name",
    Arrays.asList("name"), false);

// 添加唯一索引
executor.dCreateIndex("public.users", "uk_email",
    Arrays.asList("email"), true);

// 查询主键
List<String> pks = executor.dGetPrimaryKeys("public.cities");

// 查询索引
List<IndexApo> indexes = executor.dGetIndexes("public.cities");

// 判断索引是否存在
boolean exists = executor.dIndexesExists("public.cities", "idx_city_name");

// 删除索引
executor.dDropIndex("public.cities", "idx_city_name");

// 删除主键约束
executor.dDropPrimaryKey("public.cities", "cities_pkey");
```

### Schema 管理

```java
// 创建 Schema
executor.dCreateSchema("gis_data");

// 获取当前 Schema / Database
String schema = executor.dGetCurrentSchema();
String dbName = executor.dGetCurrentDataBase();

// 查询所有 Schema
List<String> schemas = executor.dGetAllSchemas();

// 删除 Schema（级联删除其下所有对象）
executor.dDropSchema("gis_data", true);
```

### 表复制

```java
// 复制表结构 + 数据
executor.dCopyTableByTableName("public.users_backup", "public.users", true);

// 仅复制表结构
executor.dCopyTableByTableName("public.users_empty", "public.users", false);

// 通过 SQL 查询结果复制
executor.dCopyTableBySql("public.active_users",
    "SELECT * FROM users WHERE status = 'active'", true);
```

### 通用 DDL 执行

```java
// 直接执行 DDL（不解析占位符）
executor.dExecuteDDL("ALTER TABLE cities ADD COLUMN area DOUBLE PRECISION",
    "cities", "添加面积字段");

// 带 MyBatis 风格占位符的 DDL
executor.dExecuteDDL("ALTER TABLE ${tableName} ADD COLUMN ${colName} ${colType}",
    SqlParamMap.create()
        .put("tableName", "cities")
        .put("colName", "area")
        .put("colType", "DOUBLE PRECISION"),
    "cities", "添加字段");
```

## 类型元数据系统 —— dbmeta 包

`dbmeta` 包提供了一套"数据库类型 → Java 类型"的统一映射体系，替代了早期仅支持 PostgreSQL 的硬编码类型判断。

### 核心接口：TypeMetadata

```java
public interface TypeMetadata {
    enum IgnorePolicy { NOT_SET, KEEP, IGNORE, CONDITIONAL, MUTUAL_DEPENDENT }
    enum CATEGORY_GROUP { STRING, NUMBER, BOOLEAN, BYTES, DATETIME, COLLECTION, GEOMETRY, INTERVAL, OTHER, NONE }
    enum CATEGORY {
        CHAR, TEXT, BOOLEAN, BYTES, BLOB, INT, FLOAT,
        DATE, TIME, DATETIME, TIMESTAMP, COLLECTION, GEOMETRY, INTERVAL, OTHER, NONE
    }

    CATEGORY getCategory();
    CATEGORY_GROUP getCategoryGroup();
    String getName();
    boolean support();
    Class<?> supportClass();
    int ignoreLength();    // -1=未设置, 0=保留, 1=忽略, 2=有条件, 3=互依赖
    int ignorePrecision();
    int ignoreScale();
    Config config();
}
```

每个数据库类型枚举都实现 `TypeMetadata`，统一了类型判断、Java 映射和 DDL 生成逻辑。

### 方言类型枚举

| 枚举 | 对应数据库 | 代表类型 |
|------|-----------|---------|
| `PostgreSqlType` | PostgreSQL | `int4`, `varchar`, `geometry`, `geography`, `jsonb`... |
| `MysqlType` | MySQL | `INT`, `VARCHAR`, `GEOMETRY`, `POINT`, `LINESTRING`... |
| `OracleType` | Oracle | `NUMBER`, `VARCHAR2`, `SDO_GEOMETRY`, `TIMESTAMP`... |

每种枚举的 `getByUdtName(String)` 方法通过大小写不敏感匹配，支持从 `information_schema.columns.udt_name` 或 JDBC 元数据返回值中查找对应类型。

特别是几何类型，每个枚举支持多个名称变体：

```java
// PG: geometry 可能在非 public schema 下返回 "\"public\".\"geometry\""
PostgreSqlType.GEOMETRY = new PostgreSqlType(..., "geometry", "\"public\".\"geometry\"");
PostgreSqlType.GEOGRAPHY = new PostgreSqlType(..., "geography", "\"public\".\"geography\"");

// Oracle: SDO_GEOMETRY 可能带或不带 schema 前缀
OracleType.SDO_GEOMETRY = new OracleType(..., "sdo_geometry", "SDO_GEOMETRY",
    "MDSYS.SDO_GEOMETRY", "mdsys.sdo_geometry");

// MySQL: 每种空间子类型是独立的 UDT
MysqlType.GEOMETRY = new MysqlType(..., "geometry", "GEOMETRY");
MysqlType.POINT = new MysqlType(..., "point", "POINT");
```

### IgnorePolicy —— 替代 magic number

`CATEGORY` 枚举的构造参数使用 `IgnorePolicy` 枚举，语义清晰：

```java
enum CATEGORY {
    CHAR(STRING, KEEP, IGNORE, IGNORE),    // 保留长度，忽略精度和小数位
    INT(NUMBER, KEEP, IGNORE, IGNORE),     // 同上
    FLOAT(NUMBER, IGNORE, KEEP, KEEP),     // 忽略长度，保留精度和小数位
    GEOMETRY(GEOMETRY, IGNORE, IGNORE, IGNORE), // 全忽略
    ...
}
```

### 分类体系 (CATEGORY / CATEGORY_GROUP)

`CATEGORY` 是一级分组，决定了 DDL 生成时哪些属性需要输出（长度、精度、小数位）。`CATEGORY_GROUP` 是更大的分组，供业务层快速判断：

```java
TypeMetadata dbType = field.getDbType();
if (dbType.getCategoryGroup() == CATEGORY_GROUP.GEOMETRY) { ... }
if (dbType.getCategory() == CATEGORY.CHAR) { ... }
```

---

## 字段元数据 —— FieldBySchemaApo

`FieldBySchemaApo` 是数据库列元数据的载体，从 `information_schema.columns` 或 JDBC 元数据中反序列化后，自动填充方言相关的类型信息。

### 核心字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `dialectName` | `DialectName` | Hutool 方言枚举，标识所属数据库 |
| `columnName` | `String` | 列名 |
| `originalColumnName` | `String` | 原始列名（如 `plot_geom as geom` 中的 `plot_geom`） |
| `ordinalPosition` | `Integer` | 数据库列序位置 |
| `udtName` | `String` | 数据库内部类型名（`int4` / `VARCHAR2` / `geometry`） |
| `dataType` | `String` | 类型描述（`character varying` / `USER-DEFINED`） |
| `characterMaximumLength` | `Integer` | 字符类型最大长度 |
| `numericPrecision` | `Integer` | 数值精度 |
| `numericScale` | `Integer` | 数值小数位数（非 radix） |
| `geometryFieldIs` | `boolean` | 是否为空间字段 |
| `geomType` | `AdvEnumsTypeGeom` | 空间字段子类型 |
| `srid` | `Integer` | 空间参考系 ID |
| `primaryKeyIs` | `boolean` | 是否为主键 |

### 方言感知的类型分发

```java
FieldBySchemaApo field = ...;
field.setDialectName(DialectName.POSTGRESQL);

TypeMetadata dbType = field.getDbType();  // → PostgreSqlType.getByUdtName(udtName)
String javaClass = field.getJavaClassName();  // → 如 "Integer", "String", "PGgeometry"

// 几何字段识别走类型系统而非硬编码字符串
boolean isGeom = field.isGeometryFieldIs(); // → dbType.getCategory() == CATEGORY.GEOMETRY
```

`getDbType()` 根据 `dialectName` 自动分发：
- `MYSQL` → `MysqlType.getByUdtName(udtName)`
- `ORACLE` / `DM` → `OracleType.getByUdtName(udtName)`
- `POSTGRESQL` / 其它 → `PostgreSqlType.getByUdtName(udtName)`

### 几何字段识别 —— 三层防御

`determineGeometryFieldIs()` 在字段反序列化后自动调用：

```
1. getDbType() → CATEGORY.GEOMETRY  （类型系统，覆盖所有注册类型）
2. PG JDBC 层去 "\"public\"." 前缀 （驱动 schema 降级修正）
3. udtName 原始字符串模式匹配       （兜底：PgObject/geometry/geography/sdo_geometry）
```

### 字段填充流程

`dGetColumnsByTable` 和 `getMetadataFromSql` 两个入口在创建 `FieldBySchemaApo` 时都会统一执行：

```java
field.setDialectName(getDialectName());     // 设置方言
field.setOriginalColumnName(field.getColumnName());
field.determineGeometryFieldIs();           // 计算几何标记
```

---

## 字段集合 —— DataFieldsApo

`DataFieldsApo` 封装了一组 `FieldBySchemaApo`，提供排序策略和便捷的查询方法。

### 构造即排序

```java
// 构造函数自动执行默认排序（主键 → 普通字段 → 空间字段）
DataFieldsApo fields = new DataFieldsApo(fieldList);

// 空构造用于序列化场景
DataFieldsApo empty = new DataFieldsApo();
```

### 两种排序策略

| 方法 | 排序规则 | 使用场景 |
|------|---------|---------|
| `applyDefaultSort()` | 主键在前 → 空间字段在后 | 构造函数默认执行，DDL 建表字段顺序 |
| `applyOrdinalSort()` | 按 `ordinalPosition` 升序 | 还原数据库表的自然列序 |
| `inOrdinalOrder()` | 返回已排序的新实例（不可变） | 链式调用，不改变原实例 |

### 查询方法命名对照

所有方法名都反映了实际返回的数据：

| 方法 | 返回内容 |
|------|---------|
| `filterFields(boolean includeGeom)` | 过滤后的字段列表（深拷贝） |
| `findField(Predicate)` | 按条件查找第一个匹配字段 |
| `mapFields(Function, boolean includeGeom)` | 遍历映射为自定义结果 |
| `fieldNames()` / `fieldNames(boolean)` | 字段名列表 |
| `columnNamesOf(List)` | **静态方法**，从指定列表提取列名 |
| `primaryKeyFields()` | 主键字段列表 |
| `primaryKeyFieldNames()` | 主键字段名列表 |
| `firstGeomField()` | 第一个空间字段 |
| `geomFields()` | 所有空间字段 |
| `firstGeomFieldName()` | 第一个空间字段名 |
| `geomFieldNames()` | 所有空间字段名 |
| `unresolvedGeomTypeFieldNames()` | 几何类型未知的空间字段名 |

### 典型用法

```java
DataFieldsApo fields = executor.dGetColumnsByTable("public.cities");

// 字段名列表（默认排序：PK → 其他 → 几何）
List<String> names = fields.fieldNames();

// 排除几何字段
List<String> nonGeomNames = fields.fieldNames(false);

// 按数据库自然列序
List<String> naturalOrder = fields.inOrdinalOrder().fieldNames();

// 查找主键字段
List<String> pkNames = fields.primaryKeyFieldNames();

// 空间字段探测
if (fields.firstGeomFieldName() != null) {
    String geomCol = fields.firstGeomFieldName();
    Integer srid = fields.firstGeomField().get().getSrid();
}

// 类型未知的空间字段（需要运行时探测）
List<String> unknownGeom = fields.unresolvedGeomTypeFieldNames();
```

## 空间几何操作 —— IAdvGeoOpt

`IAdvGeoOpt` 提供空间数据相关能力，所有方法以 `e` 开头。支持空间查询、空间字段管理、坐标系转换、空间索引、几何体校验与修复等。

### 空间查询

```java
// 空间相交查询：找出与指定几何体相交的记录
List<GirAdvOneRow> rows = executor.eQueryIntersects(
    "public.cities", "geom", "POINT(116.4 39.9)", 4326);

// BBOX 范围查询：找出在边界框内的记录
List<GirAdvOneRow> rows = executor.eQueryWithinBBox(
    "public.land_parcels", "geom",
    new double[]{116.0, 39.5, 117.0, 40.5}, 4326);

// 距离计算：计算每条记录到指定点的距离
List<GirAdvOneRow> rows = executor.eCalculateDistance(
    "public.stores", "geom", "POINT(116.4 39.9)", 4326, "distance_m");

// 中心点查询：返回每条记录几何字段的中心点
List<GirAdvOneRow> rows = executor.eGetCentroid(
    "public.districts", "geom", "center_point");

// BBOX 范围查询 + 空间字段处理策略
List<GirAdvOneRow> rows = executor.eSelectList(
    "SELECT id, name, ST_AsGeoJSON(geom) as geom_json FROM cities WHERE pop > 1000000",
    AdvEnumsGeomOpt.toGeoJson, "geom_json");
```

### 空间字段探测

```java
// 判断表是否包含空间字段
boolean isGeom = executor.eIsGeomByTable("public.cities");

// 获取空间字段名称
String geomCol = executor.eGetGeomColumnNameByTable("public.cities");        // 返回第一个
List<String> geomCols = executor.eGetGeomColumnNameListByTable("public.cities"); // 返回全部

// 获取空间字段元数据
FieldBySchemaApo geomMeta = executor.eGetGeomColumnByTable("public.cities");
List<FieldBySchemaApo> geomMetas = executor.eGetGeomColumnListByTable("public.cities");

// 获取空间类型
AdvEnumsTypeGeom type = executor.eGetGeoTypeByTable("public.cities");
// → Point / LineString / Polygon / MultiPoint / ...

// 判断空间类型
boolean isPoint = executor.eIsPointTable("public.cities");
boolean isLine = executor.eIsLineStringTable("public.roads");
boolean isPolygon = executor.eIsPolygonTable("public.land_parcels");

// 获取 SRID
Integer srid = executor.eGetSrid("public.cities");            // 4326
Integer srid2 = executor.eGetSrid("public.cities", "geom");   // 按字段名

// 获取所有空间图层
List<String> allLayers = executor.eGetAllGeoLayerName();
List<String> matched = executor.eGetGeoLayerNameByKeyword("city");
```

### 空间字段 DDL

```java
// 添加空间字段
executor.eAddGeomColumn("public.stores", "geom",
    AdvEnumsTypeGeom.Point, 4326);

// 删除空间字段
executor.eDropGeomColumn("public.stores", "geom");
executor.eDropGeomColumn("public.stores");  // 自动查找并删除

// 坐标系转换（如从 4326 转 3857）
executor.eTransformSrid("public.stores", "geom", 3857);
executor.eTransformSrid("public.stores", 3857);  // 自动查找空间字段

// 创建 / 删除空间索引（GIST）
executor.eCreateSpatialIndex("public.stores", "geom", "idx_stores_geom");
executor.eCreateSpatialIndex("public.stores", "idx_stores_geom");  // 自动查找空间字段
executor.eDropSpatialIndex("public.stores", "idx_stores_geom");
```

### 几何体校验与修复

```java
// 验证几何体：返回无效几何体的 ID 列表
List<Object> invalidIds = executor.eValidateGeometries("public.parcels", "geom");

// 修复无效几何体
int repaired = executor.eRepairGeometries("public.parcels", "geom");

// 获取全表几何体的边界范围
BBoxApo extent = executor.eGetExtent("public.cities", "geom");
// → {minX: 116.0, minY: 39.5, maxX: 117.0, maxY: 40.5}
```

### 空间字段结果处理策略

`AdvEnumsGeomOpt` 控制查询结果中空间字段如何处理：

```java
// 不做处理（保持 JDBC 原生返回值）
AdvEnumsGeomOpt.none

// 转为 GeoJSON 字符串
AdvEnumsGeomOpt.toGeoJson

// 转为 WKT 字符串
AdvEnumsGeomOpt.toWKT

// 转为 WKB 字节
AdvEnumsGeomOpt.toWKB

// 移除空间字段
AdvEnumsGeomOpt.remove

// 空间字段值替换为 null
AdvEnumsGeomOpt.toNull

// 空间字段值替换为空字符串
AdvEnumsGeomOpt.toEmptyStr
```

## 分页查询 —— IAdvSimplePageOpt

`IAdvSimplePageOpt` 提供分页封装能力，所有方法以 `p` 开头。自动处理不同数据库的分页语法差异（LIMIT/OFFSET vs ROWNUM vs FETCH），同时集成空间字段处理、排序和字段元数据返回。

### 基础分页

```java
// 最简分页：仅传入 SQL、页码、每页条数
PageApo<GirAdvOneRow> page = executor.pPage(
    "SELECT * FROM public.cities ORDER BY id", 1, 20);

// 访问分页结果
List<GirAdvOneRow> rows = page.getRecords();   // 当前页数据
long total = page.getTotal();                   // 总记录数
int pageSize = page.getPageSize();              // 每页条数
int currentPage = page.getCurrentPage();        // 当前页码

// 页码从 0 开始（前端常见）
PageApo<GirAdvOneRow> page = executor.pPage(
    "SELECT * FROM public.cities ORDER BY id", 0, 20, true);

// 带排序的分页
PageApo<GirAdvOneRow> page = executor.pPage(
    "SELECT * FROM public.cities",
    1, 20,
    Arrays.asList(new OrderApo("name", true), new OrderApo("id", false)));
    // name ASC, id DESC
```

### GIS 综合分页

```java
// 分页 + 空间字段处理 + 字段元数据
PageApo<GirAdvOneRow> page = executor.pPage(
    "SELECT id, name, geom FROM public.cities WHERE pop > 1000000",
    1, 20,
    false,                          // pageNumStartZero
    AdvEnumsGeomOpt.toGeoJson,      // geom 转为 GeoJSON
    true,                           // 需要字段元数据
    Arrays.asList(new OrderApo("name", true)));

// 访问字段元数据（用于动态表格表头渲染）
DataFieldsApo fields = page.getDataFields();

// 只获取总数（不查数据）
Long count = executor.pCount("SELECT * FROM cities WHERE region = 'east'");

// 带 MyBatis 占位符参数的分页
PageApo<GirAdvOneRow> page = executor.pPage(
    "SELECT * FROM ${tableName} WHERE ${whereField} > #{minValue}",
    SqlParamMap.create()
        .put("tableName", "cities")
        .put("whereField", "pop"),
    1, 20,
    false,
    AdvEnumsGeomOpt.toGeoJson,
    true,
    null);
```

### 手动构建分页 SQL

如果只需要分页 SQL 字符串而不执行查询：

```java
// 构建分页 SQL（按方言自动追加 LIMIT/OFFSET）
String pageSql = executor.pBuildPageSql(
    "SELECT * FROM cities ORDER BY id", 20, 1, false);
// MySQL:    SELECT * FROM cities ORDER BY id LIMIT 20 OFFSET 0
// PG:       SELECT * FROM cities ORDER BY id LIMIT 20 OFFSET 0
// Oracle:   SELECT * FROM (SELECT t.*, ROWNUM rn FROM (...) t WHERE ROWNUM <= 20) WHERE rn > 0

// 构建排序 SQL
String orderedSql = executor.pBuildSqlWithOrder(
    "SELECT * FROM cities",
    Arrays.asList(new OrderApo("name", true), new OrderApo("id", false)));
// → SELECT * FROM cities ORDER BY name ASC, id DESC
```

## 创建 IAdvExecutor

`IAdvExecutor` 的创建方式从上层到下分为三个层次：

| 层次 | 入口 | 适用场景 |
|------|------|---------|
| 快捷入口 | `GirAdvQuery.getIAdvExecutor(...)` | 日常编码，快速获取执行器 |
| 工厂层 | `AdvExecutorFactory.getAdvExecutorByDataSource(...)` | 需要显式控制方言路由 |
| 底层构造 | `initByDataSource(...)` / `initByConnection(...)` | 需要完全手动控制初始化 |

### 1. 快捷入口 —— GirAdvQuery

`GirAdvQuery` 提供了几个静态方法，是日常编码中最常用的入口：

```java
// 通过数据源 ID + schema 获取（Spring 环境下，走适配器）
IAdvExecutor executor = GirAdvQuery.getIAdvExecutor("master", "public");

// 直接传入 DataSource（不依赖 Spring 上下文）
IAdvExecutor executor = GirAdvQuery.getIAdvExecutor(dataSource);

// 传入 DataSource 并指定名称
IAdvExecutor executor = GirAdvQuery.getIAdvExecutor(dataSource, "myDs");

// 已知方言，跳过 JDBC 探测（性能更高）
IAdvExecutor executor = GirAdvQuery.getIAdvExecutor(DialectName.MYSQL, dataSource, "myDs");

// 指定返回类型的泛型版本
PgAdvExecutor executor = GirAdvQuery.getIAdvExecutor("master", "public", PgAdvExecutor.class);
```

`getIAdvExecutor(String dataSourceId, String schema)` 的工作流程：

1. 通过 `GirService.getPxyBeanC(IAdvExecutorAdapter.class)` 获取适配器
2. 适配器内部查找对应 `dataSourceId` 的数据源
3. 自动检测数据库方言，创建对应的 Executor
4. 设置 schema 后返回

这种方式适合 **Spring 多数据源环境**，只需传数据源 ID 即可。

`getIAdvExecutor(DataSource)` 的工作流程：

1. 直接调用 `AdvExecutorFactory.getAdvExecutorByDataSource(dataSource)`
2. 从 `DataSource` 获取 JDBC 连接
3. 通过 `DatabaseMetaData.getDatabaseProductName()` 检测数据库类型
4. 创建对应方言的 Executor（MySQL → `GirSpringMysqlAdvExecutor`，PG → `GirSpringPGAdvExecutor` 等）

这种方式适合 **非 Spring 环境**或**手动管理 DataSource** 的场景。

#### IAdvExecutorAdapter — 数据源查找的抽象层

`GirAdvQuery.getIAdvExecutor(dataSourceId, schema)` 并不是直接访问数据库的，中间隔了一个适配器接口：

```
GirAdvQuery.getIAdvExecutor("master", "public")
  └── IAdvExecutorAdapter.getIAdvExecutor(dataSourceId, schema)
        └── CommonAdvExecutorAdapter（默认实现）
              ├── AdvDynamicDataSourceStorage → 根据 ID 查找 DataSource
              ├── AdvExecutorFactory → 检测方言创建 Executor
              └── setSchemaNameGetterFunction → 设置 Schema
```

`IAdvExecutorAdapter` 的核心价值：

- **隔离数据源查找与执行器创建**：调用方不需要知道 DataSource 从哪来
- **可扩展**：如果默认的 `CommonAdvExecutorAdapter` 不满足需求（比如数据源来自不同的注册中心），可以实现自己的适配器
- **通过 SPI 暴露**：`CommonAdvExecutorAdapter` 通过 `Gir` SPI 机制注册，`GirService.getPxyBeanC(IAdvExecutorAdapter.class)` 即可获取

```java
// 默认实现：从动态数据源存储中查找
public class CommonAdvExecutorAdapter implements IAdvExecutorAdapter {
    @Override
    public IAdvExecutor getIAdvExecutor(String dataSourceId, String schema) {
        // 1. 根据 ID 获取 DataSource
        DynamicDataSourceManager instance = AdvDynamicDataSourceStorage.getInstance();
        AdvDataSourceWrapper dataSource = instance.getOrCreateDataSource(dataSourceId);
        // 2. 检测方言创建 Executor
        IAdvExecutor executor = AdvExecutorFactory.getAdvExecutorByDataSource(dataSource, dataSourceId + "_" + schema);
        // 3. 设置 Schema
        if (isNotEmpty(schema)) {
            executor.setSchemaNameGetterFunction(() -> schema);
        }
        return executor;
    }
}
```

### 2. 工厂层 —— AdvExecutorFactory

`AdvExecutorFactory` 是方言路由的核心。它提供了两种创建方式：

**方式 A：自动检测（通过 JDBC 连接探测）**

```java
// 自动从 Spring 容器获取 DataSource
IAdvExecutor executor = AdvExecutorFactory.getAdvExecutorByDataSource();

// 显式传入 DataSource
IAdvExecutor executor = AdvExecutorFactory.getAdvExecutorByDataSource(dataSource);

// 传入 DataSource 并指定名称
IAdvExecutor executor = AdvExecutorFactory.getAdvExecutorByDataSource(dataSource, "myDs");
```

方言检测逻辑：通过 `DatabaseMetaData.getDatabaseProductName()` 获取数据库产品名称并匹配：

```
DatabaseMetaData.getDatabaseProductName()
  ├── 包含 "MYSQL"              → DialectName.MYSQL      → GirSpringMysqlAdvExecutor
  ├── 包含 "POSTGRESQL" / "PG"  → DialectName.POSTGRESQL → GirSpringPGAdvExecutor
  ├── 包含 "ORACLE"             → DialectName.ORACLE     → GirSpringOracleAdvExecutor
  ├── 包含 "DAMENG" / "DM"      → DialectName.DM         → GirSpringDmAdvExecutor
  └── 其他                      → UnsupportedOperationException
```

**方式 B：直接指定方言（跳过 JDBC 探测，性能更高）**

```java
// 调用方已知数据库类型时，直接指定方言，避免额外的连接开销
IAdvExecutor executor = AdvExecutorFactory.getAdvExecutorByDialect(
        DialectName.MYSQL, dataSource, "myDs");
```

当调用方已经明确知道数据库类型时（比如从配置文件读取、或通过其他途径获取），使用 `getAdvExecutorByDialect` 可以完全跳过 JDBC 连接探测，消除 `getDbTypeFromDataSource` 中获取连接再释放的开销。

`AdvExecutorFactory` 的功能边界：

- **负责**：数据库类型检测（或接收指定）、方言 Executor 创建
- **不负责**：Spring Bean 注册（由 `AdvAutoConfiguration` 负责）、数据源生命周期管理、SQL 执行

### 3. 底层构造 —— initBy* 方法

`IAdvExecutor` 继承自 `IDataSourceGetter`，提供了四种底层初始化方式：

```java
// 通过数据源描述对象初始化
void initByDataSourceApo(DataSourceApo dataSourceApo);

// 通过数据源对象初始化
void initByDataSource(DataSource dataSource);

// 通过数据源对象初始化（指定名称）
void initByDataSource(DataSource dataSource, String dataSourceName);

// 通过数据库连接初始化
void initByConnection(Connection connection);
```

这些方法定义在 `IDataSourceGetter` 接口中，由 `AbstractPxyAdvExecutor` 实现。调用后会：

1. 初始化内部的数据源获取器
2. 触发 `initProxyObjects()`，创建各功能模块（Access / Select / Update / Delete / DDL / Geo 等）
3. 设置 Schema 和 Database 名称的获取函数

**直接使用底层构造的场景**：

```java
// 场景1：已有 Connection，不想额外管理连接池
IAdvExecutor executor = new AdvExecutorMysql(connection);

// 场景2：手动构造，传入 DataSourceApo 配置
DataSourceApo apo = new DataSourceApo();
apo.setUrl("jdbc:mysql://localhost:3306/gis");
apo.setUsername("root");
apo.setPassword("xxx");
IAdvExecutor executor = new AdvExecutorMysql(apo);

// 场景3：直接传 DataSource
IAdvExecutor executor = new AdvExecutorMysql(dataSource, "gis_db");
```

**注意**：底层构造需要自己指定方言实现类（如 `AdvExecutorMysql`），不会自动检测数据库类型。一般推荐使用 `AdvExecutorFactory` 或 `GirAdvQuery` 入口，它们会自动处理方言检测。

### 创建方式选择建议

```
需要自动检测数据库类型？
├── 是 → 用 AdvExecutorFactory 或 GirAdvQuery
│   └── 在 Spring 环境？
│       ├── 是 → GirAdvQuery.getIAdvExecutor("dsId", "schema")
│       └── 否 → GirAdvQuery.getIAdvExecutor(dataSource)
└── 否 → 直接 new 方言 Executor
    └── 已有 Connection？ → new AdvExecutorMysql(connection)
    └── 有 DataSource？   → new AdvExecutorMysql(dataSource, "name")
```

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

当前版本的核心设计决策是：

- **每个 Executor 拥有独立 Registry**：`create()` 工厂方法为每个方言执行器创建专属实例，Geometry handler 按方言自动匹配
- **保留全局注册入口**：`AdvTypeHandlerRegistry.getInstance().register(xxx)` 仍然可用，全局注册的 handler 会被所有新创建的 Executor Registry 继承
- **Geometry handler 按方言拆分**：原来一个 `JtsGeometryAdvTypeHandler` 负责所有数据库，改为每个方言一个独立实现，不再依赖 classpath 探测
- **`AdvQueryGlobalConfig` 承载 per-executor 自定义 handler**：通过 `addTypeHandler()` 注册，优先级高于全局注册和 SPI

### 三种注册方式

| 方式 | API | 作用范围 | 优先级 |
|------|-----|---------|--------|
| SPI | 无需手动调用，框架自动加载 | 全局（所有 Executor） | 最低 |
| 全局注册 | `AdvTypeHandlerRegistry.getInstance().register(handler)` | 全局（后续创建的 Executor） | 中 |
| 按 Executor 注册 | `AdvQueryGlobalConfig.of().addTypeHandler(handler)` | 当前 Executor | 高于全局 |
| 方言 Geometry | 框架自动 | 当前 Executor | 最高

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

`AdvTypeHandlerRegistry` 通过工厂方法 `create(DialectName, List<AdvTypeHandler<?>>)` 创建，加载顺序（优先级从低到高）为：

1. **SPI 加载公共 handlers**（方言无关）：
   - `BooleanAdvTypeHandler`
   - `ByteArrayAdvTypeHandler`
   - `CharacterAdvTypeHandler`
   - `EnumAdvTypeHandler`
   - `NumberAdvTypeHandler`
   - `TemporalAdvTypeHandler`

2. **全局 handlers**（通过 `AdvTypeHandlerRegistry.getInstance().register()` 注册）：优先级高于 SPI，被所有新创建的 Executor Registry 继承

3. **用户自定义 handlers**（来自 `AdvQueryGlobalConfig.typeHandlers`）：优先级高于全局注册，仅对当前 Executor 生效

4. **方言专属 Geometry handler**（优先级最高）：
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

**方式一：全局注册（影响所有后续创建的 Executor）**

```java
AdvTypeHandlerRegistry.getInstance().register(new MyCustomTypeHandler());
```

**方式二：按 Executor 注册（仅影响当前 Executor）**

```java
AdvQueryGlobalConfig config = AdvQueryGlobalConfig.of()
    .addTypeHandler(new MyCustomTypeHandler())
    .turnOnLog();
```

全局注册的 handler 会被 `create()` 工厂方法继承到每个新创建的 Registry 中，但 `AdvQueryGlobalConfig.typeHandlers` 中的 handler 优先级更高。

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

### SQL 占位符表达式 —— SqlPlaceholder

对于部分数据库（如 MySQL），简单用 `?` 传参无法正确写入空间字段：

```sql
-- MySQL 无法正确执行：? 传 WKT 字符串 → Data truncation 错误
INSERT INTO t (geom) VALUES (?)

-- 必须将 WKT 内嵌入 SQL 函数调用中
INSERT INTO t (geom) VALUES (ST_GeomFromText('LINESTRING(...)', 4326))
```

为支持这类场景，`AdvTypeHandler` 接口提供了 `getSqlPlaceholder` 方法，返回 `SqlPlaceholder` 对象：

```java
public class SqlPlaceholder {
    private final String sql;    // SQL 表达式，替代普通 `?` 的位置
    private final Object param;  // 替换后的参数值（null 表示值已内嵌在 sql 中）

    public SqlPlaceholder(String sql, Object param) { ... }
    public String getSql() { return sql; }
    public Object getParam() { return param; }
}
```

**默认行为**（`getSqlPlaceholder` 返回 null）：
```
? 占位符 + 原值放入参数列表
```

**MySQL 几何覆盖**（返回 `SqlPlaceholder`）：
```java
public class MysqlGeometryAdvTypeHandler extends JtsGeometryAdvTypeHandler {
    @Override
    public SqlPlaceholder getSqlPlaceholder(Object value) {
        if (value instanceof Geometry) {
            Geometry geom = (Geometry) value;
            String wkt = writeGeometry(geom); // 转换为 WKT 字符串
            int srid = geom.getSRID();
            // param=null → WKT 直接嵌入 SQL，不占用参数位
            return new SqlPlaceholder(
                "ST_GeomFromText('" + wkt.replace("'", "''") + "', " + srid + ")",
                null);
        }
        return null;
    }
}
```

生成的 SQL 效果：

| 列 | 占位符表达式 | 参数绑定 |
|----|------------|---------|
| 普通列 | `?` | 原值放入 params |
| MySQL 几何列 | `ST_GeomFromText('LINESTRING(...)', 4326)` | param=null → 不占位 |

`INSERT`/`UPDATE`/`UPSERT` 的 SQL 构建链路（`buildPlaceholders`、`buildSetClause`）均已接入 `getSqlPlaceholder`，几何列自动内嵌 SQL 函数，普通列保持 `?` 传参。

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
