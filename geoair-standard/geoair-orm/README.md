# GeoAir ORM 模块使用指南

## 模块介绍

GeoAir ORM 模块提供了多种 ORM 框架的集成，为数据持久化提供了统一的接口和工具，支持 MyBatis、MyBatis-Plus 等 ORM 框架。

## 目录结构

```
goair-orm/
├── geoair-orm-base/         # ORM 基础模块
├── geoair-orm-mybatis/      # MyBatis 集成模块
├── geoair-orm-mybatis-tk/   # MyBatis-Plus 集成模块
├── geoair-orm-springjpa/    # Spring JPA 集成模块
└── geoair-orm-spi/          # ORM SPI 模块
```

## 模块说明

### 1. geoair-orm-base

ORM 基础模块，提供了 ORM 框架的基础接口和工具。

### 2. geoair-orm-mybatis

MyBatis 集成模块，提供了 MyBatis 框架的集成。

### 3. geoair-orm-mybatis-tk
#  geoair-orm  持久层工程

MyBatis-Plus 集成模块，提供了 MyBatis-Plus 框架的集成，包含以下功能：

- **impls**：提供了各种 Mapper 实现
  - `TkDeleteMapper`：删除操作 Mapper
  - `TkEntityMapper`：实体操作 Mapper
  - `TkInsertMapper`：插入操作 Mapper
  - `TkPagerMapper`：分页操作 Mapper
  - `TkRetrieveMapper`：查询操作 Mapper
  - `TkUpdateMapper`：更新操作 Mapper
  - `TkVisualSelectMapper`：可视化查询 Mapper
- **page**：分页相关功能
  - `TkGithubPageHelper`：分页助手
- **support**：支持功能
  - `insert`：插入相关支持
  - `update`：更新相关支持
- **util**：工具类
  - `TkEntityHelper`：实体助手

### 4. geoair-orm-mybatis-plus
## 工程结构

MyBatis-Plus 集成模块，提供了 MyBatis-Plus 框架的集成，包含以下功能：
*  geoair-orm-base  可以在该工程下添加持久层的公共工具和封装实现。

- **impls**：提供了各种 Mapper 实现
  - `PlusDeleteMapper`：删除操作 Mapper
  - `PlusEntityMapper`：实体操作 Mapper
  - `PlusInsertMapper`：插入操作 Mapper
  - `PlusPagerMapper`：分页操作 Mapper
  - `PlusRetrieveMapper`：查询操作 Mapper
  - `PlusUpdateMapper`：更新操作 Mapper
  - `PlusVisualSelectMapper`：可视化查询 Mapper
- **page**：分页相关功能
  - `PlusGithubPageHelper`：分页助手
*  geoair-mybatis    使用mybatis

### 5. geoair-orm-springjpa
*  geoair-mybatis-plus    使用mybatis + mybatisplus

Spring JPA 集成模块，提供了 Spring JPA 框架的集成。
*  geoair-mybatis-tk    使用mybatis + tkMapper

### 6. geoair-orm-spi

ORM SPI 模块，提供了 ORM 框架的 SPI 接口。

## 快速开始

### 1. 引入依赖

在 Maven 项目中，添加以下依赖：

```xml
<!-- MyBatis-Plus 集成 -->
<dependency>
    <groupId>cn.geoair.devkit</groupId>
    <artifactId>geoair-orm-mybatis-plus</artifactId>
    <version>J17.1.5</version>
</dependency>

<!-- 或 MyBatis 集成 -->
<dependency>
    <groupId>cn.geoair.devkit</groupId>
    <artifactId>geoair-orm-mybatis</artifactId>
    <version>J17.1.5</version>
</dependency>
```

### 2. 使用

使用 MyBatis-Plus 集成：

```java
// 继承 PlusEntityMapper
public interface UserMapper extends PlusEntityMapper<User> {
    // 自定义方法
}

// 使用 Mapper
@Autowired
private UserMapper userMapper;

public void test() {
    // 查询所有用户
    List<User> users = userMapper.selectList(null);
    
    // 分页查询
    Page<User> page = new Page<>(1, 10);
    IPage<User> userPage = userMapper.selectPage(page, null);
}
```

## 功能特性

- 提供了统一的 ORM 接口
- 支持多种 ORM 框架（MyBatis、MyBatis-Plus、Spring JPA）
- 提供了丰富的 Mapper 实现
- 支持分页查询
- 支持可视化查询

## 依赖关系

- **geoair-orm** 依赖于 **geoair-base**
- 各个子模块之间相互独立，可以根据需要单独引入

## 版本历史

- J17.1.5：当前开发版本

## 贡献指南

1. Fork 本项目
2. 创建 feature 分支
3. 提交代码
4. 推送到远程分支
5. 创建 Pull Request

## 许可证

本项目采用 Apache License 2.0 许可证，详见 [LICENSE](LICENSE) 文件。

## 联系方式

- 开发者：张逢吉
- 邮箱：zhangjun7570@qq.com
- 组织：geoair
- 官网：https://xmt.geoair.cn/

*  geoair-springjpa    使用springjpa
