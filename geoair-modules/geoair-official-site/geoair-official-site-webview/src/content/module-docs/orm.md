## 模块定位

`geoair-orm` 负责把不同 ORM 框架接到统一的持久化接口层上。它本身不是单一 ORM 的封装，而是一组适配模块的组合。

如果在不同项目里会用到：

- Spring JPA
- MyBatis-Plus
- TK Mapper

那么 `geoair-orm` 的价值就在于：

- 上层只接触统一的 `gtc*` 风格 API
- 下层再分别落到不同 ORM 实现
- 应用层不需要感知底层 ORM 的差异

## 设计核心：上层 API 脱耦

`gtcUpdateByPK` / `gtcUpdateByPKSelective` 这一类统一方法正体现了这一层的设计重点：

- 在基础抽象层先定义统一方法名
- 在各 ORM 适配模块里分别桥接到自身实现
- 应用层只面向 `gtc*` 方法，不直接面向 `updateByPrimaryKeySelective`、`saveAll`、`batchUpdateByPKSelective` 这些框架专用 API

这样做的目的不是“隐藏 ORM”，而是把常用软件开发 API 先收束成统一的接口语义。

## 模块结构

当前可以直接看到的主要子模块包括：

- `geoair-orm-base`
- `geoair-orm-mybatis`
- `geoair-orm-mybatis-plus`
- `geoair-orm-mybatis-tk`
- `geoair-orm-springjpa`
- `geoair-orm-spi`

其中：

- `spi` 负责抽象桥接
- `mybatis-plus` / `tk` / `springjpa` 负责各自框架落地

## 三条重要适配线

### 1. Spring JPA 适配线

关键目录：

- `geoair-orm-springjpa/impls`
- `geoair-orm-springjpa/extra/BatchRepository`

这一层的特点是：

- Repository 接口按能力拆分成 `InsertRepository / UpdateRepository / DeleteRepository / RetrieveRepository / PagerRepository / VisualSelectRepository`
- 如果需要批量更新，还要额外依赖 `BatchRepository`

这类逻辑正体现了这种设计：

- 上层调用 `gtcUpdateByPKSelective(List<T>)`
- 如果当前 Repository 实现了 `BatchRepository`
- 就调用 `batchUpdateSelective(records)`
- 否则抛出异常，提示该 Repository 需要继承 `BatchRepository`

这意味着：

- JPA 这一层不会假定所有 Repository 都天然支持批量更新
- 批量能力通过额外接口显式补进来
- 上层 `gtc*` API 保持不变

### 2. MyBatis-Plus 适配线

关键目录：

- `geoair-orm-mybatis-plus/impls`

关键类：

- `PlusEntityMapper`
- `PlusInsertMapper`
- `PlusRetrieveMapper`
- `PlusUpdateMapper`
- `PlusDeleteMapper`
- `PlusPagerMapper`
- `PlusVisualSelectMapper`

这一层的特点是：

- 直接把统一的 `gtc*` API 映射到 MyBatis-Plus 的 Mapper 能力
- 适合偏通用 CRUD、分页和查询扩展的场景
- 比 JPA 这一层更接近 Mapper 风格而不是 Repository 风格

### 3. TK Mapper 适配线

关键目录：

- `geoair-orm-mybatis-tk/impls`
- `geoair-orm-mybatis-tk/support/update`
- `geoair-orm-mybatis-tk/util/TkEntityHelper`

关键类：

- `TkEntityMapper`
- `TkInsertMapper`
- `TkRetrieveMapper`
- `TkUpdateMapper`
- `TkDeleteMapper`
- `TkPagerMapper`
- `TkVisualSelectMapper`
- `UpdateBatchMapper`
- `TkEntityHelper`

这一层的特点是：

- 更明显地走 Mapper + Helper + Provider 组合方式
- 批量更新能力通过专门的 `UpdateBatchMapper` 等扩展补进来
- `TkEntityHelper` 这类工具类在这一层很关键，用来辅助实体与元信息处理

## `gtc*` API 是怎么接回不同 ORM 的

核心思路可以概括成：

1. 在 `geoair-base` 的 GPA / DAO / Entity 抽象层里先定义统一语义
2. 在 `geoair-orm` 里让每个 ORM 子模块去实现这一套语义
3. 例如：
   - `gtcUpdateByPKSelective(record)`
   - 在 TK 层映射到 `updateByPrimaryKeySelective(record)`
   - 在 MP 层映射到自己的 Update Mapper 逻辑
   - 在 JPA 层可能映射到 Repository 实现或 `BatchRepository` 扩展

所以统一的并不是“底层方法名”，而是“上层业务动作”。

## 为什么这套设计有价值

### 对应用层

应用层只需要记住：

- `gtcInsert*`
- `gtcRetrieve*`
- `gtcUpdate*`
- `gtcDelete*`

而不需要分别记住：

- JPA 的 Repository 风格
- MP 的 Mapper 风格
- TK 的 Mapper + Helper 风格

### 对框架层

框架层仍然保留每种 ORM 的特性差异：

- JPA 批量更新要额外实现 `BatchRepository`
- TK 需要自己的 batch mapper 支撑
- MP 有自己的 mapper 语义

所以这不是“完全抹平差异”，而是“给上层一个统一表面”。

## GitHub 源码入口

- 模块目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-orm`
- MyBatis-Plus 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-orm/geoair-orm-mybatis-plus`
- TK Mapper 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-orm/geoair-orm-mybatis-tk`
- Spring JPA 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-orm/geoair-orm-springjpa`
- SPI 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-orm/geoair-orm-spi`

## 阅读建议

建议顺序：

1. 先看 `geoair-base` 中的 GPA 抽象：`GiDao` / `GiEntityable`
2. 再看 `geoair-orm-spi`
3. 然后按你实际使用的 ORM 进入：
   - `springjpa`
   - `mybatis-plus`
   - `tk`
4. 如果重点是批量更新，就优先看：
   - `BatchRepository`
   - `PlusUpdateMapper`
   - `TkUpdateMapper / UpdateBatchMapper`

这样最容易把“上层统一 API”和“下层 ORM 具体落地”对应起来。
