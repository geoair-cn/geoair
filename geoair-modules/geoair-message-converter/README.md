# GeoAir Message Converter 模块使用指南

## 模块介绍

GeoAir Message Converter 模块提供了消息转换功能，支持 JTS 几何对象与 JSON、数据库类型的转换，为地理空间数据的序列化和反序列化提供了便捷的解决方案。

## 目录结构

```
goair-message-converter/
├── geoair-message-jts-jackson/   # JTS Jackson 消息转换器
└── geoair-message-jts-mybatis/   # JTS MyBatis 消息转换器
```

## 模块说明

### 1. geoair-message-jts-jackson

JTS Jackson 消息转换器，支持 JTS 几何对象与 JSON 的转换。

### 2. geoair-message-jts-mybatis

JTS MyBatis 消息转换器，支持 JTS 几何对象与数据库类型的转换。

## 快速开始

### 1. 引入依赖

在 Maven 项目中，添加以下依赖：

```xml
<!-- JTS Jackson 消息转换器 -->
<dependency>
    <groupId>cn.geoair.devkit</groupId>
    <artifactId>geoair-message-jts-jackson</artifactId>
    <version>J8.1.0-SNAPSHOT</version>
</dependency>

<!-- JTS MyBatis 消息转换器 -->
<dependency>
    <groupId>cn.geoair.devkit</groupId>
    <artifactId>geoair-message-jts-mybatis</artifactId>
    <version>J8.1.0-SNAPSHOT</version>
</dependency>
```

### 2. 配置

模块会自动配置，无需额外配置。

### 3. 使用

使用消息转换功能：

```java
// JTS 几何对象转 JSON
Geometry geometry = new Point(0, 0);
ObjectMapper mapper = new ObjectMapper();
String json = mapper.writeValueAsString(geometry);

// JSON 转 JTS 几何对象
Geometry geometry = mapper.readValue(json, Geometry.class);

// MyBatis 中使用
@Select("SELECT id, geom FROM spatial_table")
List<SpatialEntity> selectAll();
```

## 功能特性

- 支持 JTS 几何对象与 JSON 的转换
- 支持 JTS 几何对象与数据库类型的转换
- 自动配置，使用简单
- 支持多种几何类型

## 依赖关系

- **geoair-message-jts-jackson**：JTS Jackson 消息转换器
- **geoair-message-jts-mybatis**：JTS MyBatis 消息转换器

## 版本历史

- J8.1.0-SNAPSHOT：当前开发版本

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
- 邮箱：1159856928@qq.com
- 组织：geoair
- 官网：https://xmt.geoair.cn/
 