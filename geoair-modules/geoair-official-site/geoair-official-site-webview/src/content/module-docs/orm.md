## 模块定位

`geoair-orm` 负责把不同 ORM 框架接到统一的持久化接口层上。它本身不是单一 ORM 的封装，而是一组适配模块的组合。

如果你在不同项目里会用到：

- MyBatis
- MyBatis-Plus
- TK Mapper
- Spring JPA

那么 `geoair-orm` 的价值在于：把这些能力拉回到统一抽象层，而不是让上层代码直接散落在不同 ORM API 上。

## 模块结构

当前可以直接看到的子模块包括：

- `geoair-orm-base`
- `geoair-orm-mybatis`
- `geoair-orm-mybatis-plus`
- `geoair-orm-mybatis-tk`
- `geoair-orm-springjpa`
- `geoair-orm-spi`

### 你应该先关注什么

从源码入口上，当前最值得先读的是：

- `TkEntityHelper`
- ORM 相关的 Mapper 实现目录
- `GiDao` / `GiEntityable` 这些基础抽象（实际定义在 base 层）

## GitHub 源码入口

- 模块目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-orm`
- TK Mapper 工具目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-orm/geoair-orm-mybatis-tk/src/main/java/cn/geoair/orm/tkmapper/util`
- MyBatis-Plus 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-orm/geoair-orm-mybatis-plus`
- Spring JPA 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-orm/geoair-orm-springjpa`

## 阅读建议

建议顺序：

1. 先看 `geoair-base` 中的 `GiDao` / `GiEntityable`
2. 再看 `geoair-orm` 的子模块目录划分
3. 然后按实际项目使用的 ORM 子模块深入
4. 如果使用 TK Mapper，可以先看 `TkEntityHelper`

这样更容易从抽象层过渡到具体 ORM 实现层。
