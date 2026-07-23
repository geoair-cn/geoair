## 模块定位

`geoair-code-generator` 负责把数据库表结构转换成一组可直接接入项目的代码骨架。它更像一个代码产出层，而不是运行时业务模块。

如果你的需求是：

- 生成 Entity
- 生成 Mapper
- 生成 Service / Controller
- 生成前端页面骨架

那么这个模块就是入口。

## 模块结构

当前可以直接看到的主要子模块：

- `geoair-code-gen-module`
- `geoair-code-gen-demo`

### geoair-code-gen-module

负责生成逻辑本身，也就是模板、输出结构和代码装配。

### geoair-code-gen-demo

负责演示怎么使用这一套生成逻辑。

## GitHub 源码入口

- 模块目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-code-generator`
- 核心生成目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-code-generator/geoair-code-gen-module`
- demo 目录：
  - `https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-code-generator/geoair-code-gen-demo`

## 适用场景

适合：

- 新建数据库驱动的后台模块
- 快速生成 CRUD 骨架
- 给标准工程补齐初始目录和基础类
- 生成前端列表 / 表单页面初版

## 阅读建议

建议顺序：

1. 先看 `geoair-code-gen-demo`
2. 再看 `geoair-code-gen-module`
3. 最后对照你自己的表结构和生成模板做改造

如果你只是想快速确认“这个模块到底生成什么”，先看 demo 通常最省时间。
