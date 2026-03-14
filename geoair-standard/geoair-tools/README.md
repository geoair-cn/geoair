# GeoAir Tools 模块使用指南

## 模块介绍

GeoAir Tools 是 GeoAir 框架的工具类模块，提供了丰富的工具类，包括 Bean 操作、定义工具、异常处理、语言工具、日志工具、文本工具和通用工具等，为开发提供了便捷的工具支持。

## 目录结构

```
goair-tools/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── cn/geoair/base/ # 工具类代码
│   │   │       ├── bean/       # Bean 相关工具
│   │   │       ├── def/        # 定义相关工具
│   │   │       ├── exception/  # 异常相关工具
│   │   │       ├── lang/       # 语言相关工具
│   │   │       ├── log/        # 日志相关工具
│   │   │       ├── text/       # 文本相关工具
│   │   │       ├── tool/       # 工具类
│   │   │       └── util/       # 通用工具类
│   │   └── resources/ # 资源文件
│   └── test/ # 测试代码
├── target/ # 构建输出
├── README.md
└── pom.xml # Maven 配置
```

## 模块说明

### 1. bean 包

- **GkBeanPath**：Bean 路径工具，用于处理 Bean 的属性路径
- **GkNullWrapperBean**：空包装 Bean，用于处理空值情况

### 2. def 包

#### 2.1 annotation
- **GaClass**：类注解
- **GaParameter**：参数注解
- **GaType**：类型注解

#### 2.2 核心类
- **GkClazz**：类工具
- **GkEditor**：编辑器接口
- **GkFilter**：过滤器接口
- **GkMatcher**：匹配器接口
- **GkOffice**：办公室接口
- **GkOperater**：操作器接口
- **GkPackager**：打包器接口

### 3. exception 包

- **GirEBizException**：业务异常类
- **GirEServerException**：服务器异常类
- **GirException**：基础异常类

### 4. lang 包

#### 4.1 caller
- **GkCaller**：调用者接口
- **GkCallerUtil**：调用者工具类
- **GkSecurityManagerCaller**：安全管理器调用者
- **GkStackTraceCaller**：堆栈跟踪调用者

#### 4.2 invoke
- **GaMethodHandDefine**：方法句柄定义
- **GaMethodHandImpl**：方法句柄实现
- **GkMethodHand**：方法句柄接口

#### 4.3 lambda
- **GkIdeaProxyLambdaMeta**：Idea 代理 Lambda 元数据
- **GkReflectLambdaMeta**：反射 Lambda 元数据
- **GkSerializableFunction**：可序列化函数
- **GkSerializedLambda**：序列化 Lambda
- **GkShadowLambdaMeta**：影子 Lambda 元数据
- **GkfLambdaMeta**：Lambda 元数据接口

#### 4.4 核心类
- **GkActualTypeMapperPool**：实际类型映射池
- **GkBasicType**：基本类型工具
- **GkParameterizedTypeImpl**：参数化类型实现
- **GkSetAccessibleAction**：设置可访问操作
- **GkTypeReference**：类型引用

### 5. log 包

- **GemLogLevel**：日志级别枚举
- **GiLogger**：日志接口
- **GirConsoleLog**：控制台日志实现
- **GirLogger**：日志实现

### 6. text 包

- **GuFastDateFormat**：快速日期格式化
- **GuStrFormatter**：字符串格式化

### 7. tool 包

- **GkConcurrentReferenceHashMap**：并发引用哈希表
- **GkConsole**：控制台工具
- **GkConsoleTable**：控制台表格
- **GkSnowflake**：雪花算法 ID 生成器
- **GkSystemClock**：系统时钟

### 8. util 包

- **GutilArray**：数组工具类
- **GutilAssert**：断言工具类
- **GutilChar**：字符工具类
- **GutilClass**：类工具类
- **GutilCollection**：集合工具类
- **GutilCompare**：比较工具类
- **GutilDate**：日期工具类
- **GutilDigest**：摘要工具类
- **GutilGenericType**：泛型类型工具类
- **GutilLambda**：Lambda 工具类
- **GutilNumber**：数字工具类
- **GutilObject**：对象工具类
- **GutilReflection**：反射工具类
- **GutilStr**：字符串工具类
- **GutilType**：类型工具类
- **PrimitiveArrayUtil**：原始数组工具类

## 快速开始

### 1. 引入依赖

在 Maven 项目中，添加以下依赖：

```xml
<dependency>
    <groupId>cn.geoair.devkit</groupId>
    <artifactId>geoair-tools</artifactId>
    <version>J8.1.0-SNAPSHOT</version>
</dependency>
```

### 2. 使用

#### 2.1 使用字符串工具

```java
// 字符串工具
String str = "Hello, GeoAir!";
boolean isEmpty = GutilStr.isEmpty(str);
String upperCase = GutilStr.toUpperCase(str);
String lowerCase = GutilStr.toLowerCase(str);
```

#### 2.2 使用日期工具

```java
// 日期工具
Date date = new Date();
String dateStr = GutilDate.format(date, "yyyy-MM-dd HH:mm:ss");
Date parseDate = GutilDate.parse("2023-01-01", "yyyy-MM-dd");
```

#### 2.3 使用集合工具

```java
// 集合工具
List<String> list = new ArrayList<>();
list.add("a");
list.add("b");
list.add("c");
boolean contains = GutilCollection.contains(list, "a");
List<String> subList = GutilCollection.subList(list, 0, 2);
```

#### 2.4 使用反射工具

```java
// 反射工具
User user = new User();
GutilReflection.setFieldValue(user, "name", "GeoAir");
String name = (String) GutilReflection.getFieldValue(user, "name");
```

#### 2.5 使用雪花算法

```java
// 雪花算法 ID 生成器
GkSnowflake snowflake = new GkSnowflake(1, 1);
long id = snowflake.nextId();
```

## 功能特性

- 提供了丰富的工具类
- 支持 Bean 操作
- 支持异常处理
- 支持语言工具（调用者、方法句柄、Lambda）
- 支持日志工具
- 支持文本工具
- 支持各种通用工具（数组、断言、字符、类、集合、比较、日期、摘要、泛型类型、Lambda、数字、对象、反射、字符串、类型等）
- 提供了雪花算法 ID 生成器
- 提供了控制台工具和表格

## 依赖关系

- **geoair-tools** 依赖于 **geoair-base**

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
