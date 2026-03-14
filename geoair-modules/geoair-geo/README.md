# GeoAir Geo 模块使用指南

## 模块介绍

GeoAir Geo 模块提供了地理空间相关功能，包括高级查询、空间分析等，为地理信息系统（GIS）应用提供了强大的支持。

## 目录结构

```
goair-geo/
└── geoair-adv-query/    # 高级查询模块
```

## 模块说明

### 1. geoair-adv-query

高级查询模块，提供了地理空间数据的高级查询功能，支持空间索引、空间分析等。

## 快速开始

### 1. 引入依赖

在 Maven 项目中，添加以下依赖：

```xml
<dependency>
    <groupId>cn.geoair.devkit</groupId>
    <artifactId>geoair-adv-query</artifactId>
    <version>J8.1.0-SNAPSHOT</version>
</dependency>
```

### 2. 配置

在 application.yml 或 application.properties 中添加配置：

```yaml
geoair:
  adv-query:
    enabled: true
```

### 3. 使用

使用高级查询功能：

```java
@Autowired
private GirAdvQuery girAdvQuery;

public void query() {
    // 创建查询条件
    Map<String, Object> params = new HashMap<>();
    params.put("name", "test");
    
    // 执行查询
    List<Map<String, Object>> result = girAdvQuery.select("table_name", params);
    
    // 处理结果
    for (Map<String, Object> row : result) {
        System.out.println(row);
    }
}
```

## 功能特性

- 支持空间索引查询
- 支持空间分析
- 支持动态 SQL 生成
- 支持多种数据库方言
- 支持分页查询

## 依赖关系

- **geoair-adv-query**：核心功能模块

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
 