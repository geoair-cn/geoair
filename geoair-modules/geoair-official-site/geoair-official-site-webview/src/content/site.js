export const siteMeta = {
  name: 'GeoAir Framework',
  shortName: 'GeoAir',
  version: 'J8-dev-SNAPSHOT',
  tagline: 'GIS Java 工具与模块集合',
  description: 'GeoAir Framework 基于 Maven 多模块组织方式，聚焦 GIS 开发中常见的空间处理、动态数据源、瓦片服务、矢量瓦片和数据库相关能力。',
  heroActions: [
    { label: '快速开始', href: '#quick-start', type: 'primary' },
    { label: '查看模块', to: '/modules', type: 'default' },
    { label: '仓库地址', href: 'https://github.com/geoair-cn/geoair', type: 'ghost' }
  ],
  highlights: [
    { value: 'SPI', label: '统一解耦机制', detail: '门面、策略与 SPI 插件协同工作' },
    { value: 'GIS', label: '空间处理能力', detail: '坐标转换、格式互转、查询、瓦片与 GeoServer 集成' },
    { value: 'Vue2', label: '现成前端技术栈', detail: '与仓库现有 Vue2 工程风格保持一致' },
    { value: 'Maven', label: '模块化构建', detail: '依赖管理、父 POM 与业务模块分层清晰' }
  ],
  principles: [
    {
      title: '先统一抽象，再落具体实现',
      description: '以 geoair-base、geoair-core、geoair-web 和 geoair-orm 为核心，把日志、配置、JSON、ORM 与 Web 能力收敛成统一入口。'
    },
    {
      title: '面向 GIS 日常开发，而不是泛化叙事',
      description: '模块说明优先落在真实代码、真实数据流和常见 GIS 场景上，尽量让读者能直接对照源码和测试。'
    },
    {
      title: '模块可拆分，接入可以渐进',
      description: '项目既可以直接继承父 POM，也可以只按需引入 geoair-geo-tools、geoair-dynamic-ds 或 apidoc 等单个模块。'
    }
  ],
  quickStart: {
    title: '五分钟接入',
    summary: '继承父 POM 或导入依赖管理后，按需添加模块依赖即可开始使用。',
    code: `<parent>\n    <groupId>cn.geoair.devkit</groupId>\n    <artifactId>geoair-base-parent</artifactId>\n    <version>J8-dev-SNAPSHOT</version>\n</parent>\n\n<dependency>\n    <groupId>cn.geoair.devkit</groupId>\n    <artifactId>geoair-geo-tools</artifactId>\n    <version>J8-dev-SNAPSHOT</version>\n</dependency>`
  },
  sampleCode: {
    title: '统一入口示例',
    code: `Gir.log.info(\"Hello GeoAir!\");\n\nString dbUrl = Gir.property.getProperty(\"spring.datasource.url\");\n\nGiResult<String> result = GiResult.successMsg(\"操作成功\").andValue(\"data\");\n\nGirGeoTools tools = GirGeoTools.defaultInstance();\ndouble[] gcj02 = tools.getCoordinateOpt().wgs84ToGcj02(116.40, 39.90);`
  },
  docLinks: [
    { title: '根 README', href: 'https://github.com/geoair-cn/geoair/blob/master/README.md', description: '项目总览、能力地图与技术栈。' },
    { title: 'Framework README', href: 'https://github.com/geoair-cn/geoair/blob/master/geoair-framework/README.md', description: '框架结构、标准基础库与核心功能说明。' },
    { title: 'Modules README', href: 'https://github.com/geoair-cn/geoair/blob/master/geoair-framework/geoair-modules/README.md', description: '业务组件清单与 GIS 子模块目录。' },
    { title: '组织官网', href: 'https://xmt.geoair.cn/', description: 'GeoAir 对外官网入口。' }
  ],
  footer: {
    organization: 'GeoAir',
    maintainer: '张逢吉',
    email: '1159856928@qq.com',
    license: 'Apache License 2.0',
    repo: 'https://github.com/geoair-cn/geoair'
  }
}

export const topNav = [
  { label: '首页', to: '/' },
  { label: '标准基础库', to: '/standard' },
  { label: '业务模块', to: '/modules' },
  { label: '文档索引', to: '/docs' }
]
