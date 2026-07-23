# geoair-web  Web库

## 项目介绍

geoair-web 是一个轻量级的 Web 工具库，为 Java Web 应用提供了一系列便捷的工具类和功能组件，简化 Web 开发过程。

## 主要功能

- **Web 工具类**：提供 HttpServletRequest、HttpServletResponse 等 Servlet API 的便捷访问
- **结果处理**：标准化的 Web 响应结果封装
- **会话管理**：支持多种会话存储方式（Cookie、Token、HttpSession、Spring Session）
- **页面参数处理**：统一的页面参数获取和处理机制
- **权限管理**：用户权限相关的工具类

## 核心组件

### 1. Web 工具类

- `GirWeb`：提供静态方法快速获取 Servlet API 对象
- `GirHttpServletHelper`：Servlet 相关的辅助方法
- `GutilCookie`：Cookie 操作工具类

### 2. 结果处理

- `GirWebResult`：标准化的 Web 响应结果，支持数据封装和跳转地址设置
- `GirWebResultConfig`：结果配置类

### 3. 会话管理

- `GirSessionAn`：会话管理注解，支持配置多种会话参数
- `GirCookieSession`：基于 Cookie 的会话实现
- `GirHttpSession`：基于 HttpSession 的会话实现
- `GirTokenSession`：基于 Token 的会话实现
- `GirSpringSession`：基于 Spring Session 的会话实现
- `GirWebUserSession`：用户会话管理

### 4. 页面参数处理

- `GirWebPageParamProvider`：页面参数提供者，统一处理分页等参数

### 5. 模块管理

- `GiModule`：模块接口
- `GiModuleProvider`：模块提供者

## 技术依赖

- Java 8+
- Servlet API 3.0+
- geoair-base

## 安装使用

### Maven 依赖

```xml
<dependency>
    <groupId>cn.geoair.devkit</groupId>
    <artifactId>geoair-web</artifactId>
    <version>J8-dev-SNAPSHOT</version>
</dependency>
```

### 基本使用

#### 1. 获取 Servlet API 对象

```java
// 获取 HttpServletRequest
HttpServletRequest request = GirWeb.getRequest();

// 获取 HttpServletResponse
HttpServletResponse response = GirWeb.getResponse();

// 获取 ServletContext
ServletContext servletContext = GirWeb.getServletContext();
```

#### 2. 使用 Web 响应结果

```java
// 创建响应结果
GirWebResult<User> result = new GirWebResult<>();
result.setData(user);
result.setMessage("操作成功");
result.setCode(200);

// 设置跳转地址
result.setLocation("/home");
// 链式调用
result.andLocation("/home");
```

#### 3. 会话管理

使用注解配置会话：

```java
@GirSessionAn(
    cookieKey = "sessionId",
    tokenKey = "token",
    httpTimeout = 3600000,
    cookieTimeout = 3600,
    tokenTimeout = 3600000,
    useCache = true,
    tokenInHeader = true,
    catalog = "userSession",
    cacheName = "gtcSessionCache"
)
public class UserController {
    // 控制器方法
}
```

## 项目结构

```
geoair-web/
├── src/main/java/cn/geoair/web/
│   ├── GirWeb.java                # 核心工具类
│   ├── data/                      # 数据相关
│   │   ├── page/                  # 页面参数
│   │   │   └── GirWebPageParamProvider.java
│   │   └── result/                # 结果处理
│   │       ├── GiWebResult.java
│   │       ├── GirWebResult.java
│   │       └── GirWebResultConfig.java
│   ├── module/                    # 模块管理
│   │   ├── GiModule.java
│   │   └── GiModuleProvider.java
│   ├── permission/                # 权限管理
│   │   └── GiWebPermissionUser.java
│   ├── session/                   # 会话管理
│   │   ├── GirCookieSession.java
│   │   ├── GirHttpSession.java
│   │   ├── GirSessionAn.java
│   │   ├── GirSessionConfig.java
│   │   ├── GirSpringSession.java
│   │   ├── GirTokenSession.java
│   │   ├── GirWebUserSession.java
│   │   └── gtcWebUserSessionProvider.java
│   └── util/                      # 工具类
│       ├── GirHttpServletHelper.java
│       └── GutilCookie.java
├── pom.xml                        # Maven 配置
└── README.md                      # 项目说明
```

## 许可证

Apache License 2.0

## 开发团队

- **开发者**：zhangfengji
- **邮箱**：zhangjun7570@qq.com
- **组织**：geoair
- **官网**：https://xmt.geoair.cn/

## 项目地址

- **Gitee**：https://github.com/geoair-cn/geoair

## 版本信息

当前版本：J8-dev-SNAPSHOT

## 贡献指南

欢迎提交 Issue 和 Pull Request 来帮助改进这个项目！

## 更新日志

- J8-dev-SNAPSHOT：初始版本，提供基础 Web 工具功能
