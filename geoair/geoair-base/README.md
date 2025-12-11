#  geoair-base    开发标准库

该工程作为基础在项目中应用，封装了代码约束和一些常用解耦api


## 目录说明：

* bean  容器Bean 提供从容器获取bean的工具
    * 提供容器获取bean方法，使用者不用关心具体实现是由spring容器还是其他容器提供bean服务;
    * 调用常用方法获取Bean   gtcBeanHelper.getBean(...);
* cache 缓存
    * 提供对缓存的使用。使用者不用关心具体实现是使用的何种缓存组件，由动态适配去实现。
    * 调用常用方法   gtcCacheHelper.xxx(...);
* env  提供环境和属性的读取
    * 提供对环境访问的api，由动态适配去实现。
    * 调用常用方法   gtcPropertyHelper.xxx(...);
* convert   数据转换
    * 提供通用数据转换接口
* data  数据 提供模型数据规范

* lang   基础工具

* log  日志封装

* gpa  持久化定义

* user   用户定义，包含用户会话，权限

* util   常用静态工具类

*  gtc   提供常用工具方法

## 其他：

* 对于其他可以抽象的标准实现，可以在该工程下添加更多的规范和通用接口
