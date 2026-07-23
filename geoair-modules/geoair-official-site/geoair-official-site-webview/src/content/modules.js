const dependencyModules = [
  {
    slug: 'dependencies-bom',
    route: '/standard/dependencies-bom',
    title: 'geoair-dependencies-bom',
    group: 'standard',
    summary: '集中管理 GeoTools、Spring、OpenAPI 与通用第三方依赖版本，保证多模块项目的一致性。',
    tags: ['BOM', 'Maven', '依赖管理'],
    capabilities: [
      '统一管理 5 个领域 BOM 与第三方版本。',
      '降低多模块工程中的依赖冲突与升级成本。',
      '为父 POM 与下游业务模块提供稳定基线。'
    ],
    quickStart: `<dependencyManagement>\n  <dependencies>\n    <dependency>\n      <groupId>cn.geoair.devkit</groupId>\n      <artifactId>geoair-base-parent</artifactId>\n      <version>J8-dev-SNAPSHOT</version>\n      <type>pom</type>\n      <scope>import</scope>\n    </dependency>\n  </dependencies>\n</dependencyManagement>`,
    example: '适合希望统一 GeoTools、Spring Boot 与 API 文档组件版本的 Maven 多模块项目。',
    related: ['base-parent', 'framework-bom']
  },
  {
    slug: 'base-parent',
    route: '/standard/base-parent',
    title: 'geoair-base-parent',
    group: 'standard',
    summary: 'GeoAir 的核心父 POM，聚合依赖管理、构建配置与发布能力。',
    tags: ['Parent POM', '构建', '发布'],
    capabilities: [
      '导入 GeoAir 所需的依赖版本管理。',
      '统一编码、测试、发布与插件配置。',
      '作为标准库与业务模块的共同继承入口。'
    ],
    quickStart: `<parent>\n  <groupId>cn.geoair.devkit</groupId>\n  <artifactId>geoair-base-parent</artifactId>\n  <version>J8-dev-SNAPSHOT</version>\n</parent>`,
    example: '适合新建业务系统、Starter 或空间服务时直接继承，减少重复 Maven 配置。',
    related: ['dependencies-bom', 'framework-bom', 'base']
  },
  {
    slug: 'framework-bom',
    route: '/standard/framework-bom',
    title: 'geoair-framework-bom',
    group: 'standard',
    summary: '按工程类型拆分父 POM，分别服务于 API 项目、普通业务项目与 Spring Boot Starter。',
    tags: ['工程类型', 'Maven', '父 POM'],
    capabilities: [
      '提供不同项目形态的父工程模板。',
      '为 API、业务项目与 Starter 保持清晰边界。',
      '让组织内部工程继承方式更统一。'
    ],
    quickStart: `<parent>\n  <groupId>cn.geoair.devkit</groupId>\n  <artifactId>geoair-project-parent</artifactId>\n  <version>J8-dev-SNAPSHOT</version>\n</parent>`,
    example: '当项目已经确定是标准业务工程或 Starter 时，可以直接选择对应父 POM。',
    related: ['base-parent']
  }
]

const standardModules = [
  {
    slug: 'base',
    route: '/standard/base',
    title: 'geoair-base',
    group: 'standard',
    summary: '接口与抽象定义层，提供 GiResult、GiDao、GiCache、Gir 等核心抽象。',
    tags: ['SPI', '接口层', '零外部依赖'],
    capabilities: [
      '定义 Bean、缓存、结果、分页、实体与环境读取等统一接口。',
      '通过 Gir 门面为日志、配置、Bean 和 JSON 提供统一入口。',
      '为 geoair-core 与业务模块提供稳定的抽象边界。'
    ],
    quickStart: `<dependency>\n  <groupId>cn.geoair.devkit</groupId>\n  <artifactId>geoair-base</artifactId>\n  <version>J8-dev-SNAPSHOT</version>\n</dependency>`,
    example: `GiResult<String> result = GiResult.successMsg("操作成功").andValue("data");`,
    related: ['core', 'web', 'orm']
  },
  {
    slug: 'core',
    route: '/standard/core',
    title: 'geoair-core',
    group: 'standard',
    summary: '基于 Spring 的 SPI 默认实现层，把抽象接口落到 Bean、日志、JSON 与环境等具体实现。',
    tags: ['Spring', 'SPI 实现', '适配层'],
    capabilities: [
      '适配 Spring Context、Spring Environment 与 Servlet API。',
      '提供 Jackson、FastJSON、Gson、Hutool JSON 等多实现选择。',
      '为日志、缓存和类型转换提供默认实现。'
    ],
    quickStart: `<dependency>\n  <groupId>cn.geoair.devkit</groupId>\n  <artifactId>geoair-core</artifactId>\n  <version>J8-dev-SNAPSHOT</version>\n</dependency>`,
    example: '当工程运行在 Spring 环境中时，geoair-core 会把 Gir 门面对接到底层容器和配置系统。',
    related: ['base', 'web']
  },
  {
    slug: 'web',
    route: '/standard/web',
    title: 'geoair-web',
    group: 'standard',
    summary: 'Web 公共组件集合，覆盖会话、权限、请求日志、CORS、Cookie 与统一 Web 结果。',
    tags: ['Web', '会话', '权限'],
    capabilities: [
      '支持 Session、Cookie、Token 与 Spring Session 等多种会话策略。',
      '提供 GiWebResult 与 Web 分页模型。',
      '封装请求日志采集、跨域处理和 MIME 类型解析。'
    ],
    quickStart: `<dependency>\n  <groupId>cn.geoair.devkit</groupId>\n  <artifactId>geoair-web</artifactId>\n  <version>J8-dev-SNAPSHOT</version>\n</dependency>`,
    example: '适合在需要统一 Web 入口、登录态与 API 输出格式的服务端项目中直接接入。',
    related: ['base', 'core']
  },
  {
    slug: 'orm',
    route: '/standard/orm',
    title: 'geoair-orm',
    group: 'standard',
    summary: '通过 SPI 抽象对接 MyBatis、MyBatis-Plus、TK Mapper 与 Spring Data JPA。',
    tags: ['ORM', 'MyBatis', 'JPA'],
    capabilities: [
      '统一 Entity 解析、Example 构造与 CRUD 执行入口。',
      '兼容不同 ORM 实现，降低框架切换成本。',
      '适合配合 GPA 架构和实体模型一起使用。'
    ],
    quickStart: `<dependency>\n  <groupId>cn.geoair.devkit</groupId>\n  <artifactId>geoair-orm</artifactId>\n  <version>J8-dev-SNAPSHOT</version>\n  <type>pom</type>\n</dependency>`,
    example: '在同一组织存在不同 ORM 技术栈时，可以用 geoair-orm 维持统一上层编码方式。',
    related: ['base', 'core']
  },
  {
    slug: 'sdk',
    route: '/standard/sdk',
    title: 'geoair-sdk',
    group: 'standard',
    summary: '统一 SDK 输出层，用于对外封装与交付 GeoAir 的公共能力。',
    tags: ['SDK', '对外输出'],
    capabilities: [
      '统一对外暴露框架能力。',
      '帮助上层项目减少直接依赖内部细节。',
      '作为能力聚合入口，适合二次封装与分发。'
    ],
    quickStart: `<dependency>\n  <groupId>cn.geoair.devkit</groupId>\n  <artifactId>geoair-sdk</artifactId>\n  <version>J8-dev-SNAPSHOT</version>\n</dependency>`,
    example: '当项目要把 GeoAir 能力打包成对外组件时，可以从 geoair-sdk 开始构建。',
    related: ['base', 'tools']
  },
  {
    slug: 'tools',
    route: '/standard/tools',
    title: 'geoair-tools',
    group: 'standard',
    summary: '底层工具库，提供方法分派、集合、反射、控制台输出等基础能力。',
    tags: ['工具类', '反射', '基础设施'],
    capabilities: [
      '支撑 Gir 门面与方法句柄分派机制。',
      '提供控制台、表格输出和常见工具方法。',
      '为标准基础库和业务模块复用底层能力。'
    ],
    quickStart: `<dependency>\n  <groupId>cn.geoair.devkit</groupId>\n  <artifactId>geoair-tools</artifactId>\n  <version>J8-dev-SNAPSHOT</version>\n</dependency>`,
    example: '适合需要在不引入更重依赖的前提下，复用底层工具方法的内部模块。',
    related: ['base', 'sdk']
  }
]

const geoModules = [
  {
    slug: 'geo-tools',
    route: '/modules/geo/tools',
    title: 'geoair-geo-tools',
    group: 'geo',
    summary: 'GIS 工具集总入口，统一封装坐标转换、格式互转、空间测量、几何合并、SRID 转换与瓦片计算。',
    tags: ['GIS', 'GeoTools API', '统一入口'],
    sourceExamples: [
      {
        title: 'GirGeoToolsOverviewExample',
        path: 'geoair-geo/geoair-geo-tools/src/test/java/cn/geoair/map/dynamic/tools/test/GirGeoToolsOverviewExample.java',
        description: '统一入口示例，展示如何从 GirGeoTools 获取各类 API。'
      },
      {
        title: 'GirGeoToolsCoordinateExample',
        path: 'geoair-geo/geoair-geo-tools/src/test/java/cn/geoair/map/dynamic/tools/test/GirGeoToolsCoordinateExample.java',
        description: '坐标转换示例，覆盖单点、Point、批量、Geometry、DMS 和墨卡托转换。'
      },
      {
        title: 'GirGeoToolsFormatExample',
        path: 'geoair-geo/geoair-geo-tools/src/test/java/cn/geoair/map/dynamic/tools/test/GirGeoToolsFormatExample.java',
        description: '格式转换示例，覆盖 GeoJSON、WKT、WKB 和 Point 构造。'
      },
      {
        title: 'GirGeoToolsMeasureExample',
        path: 'geoair-geo/geoair-geo-tools/src/test/java/cn/geoair/map/dynamic/tools/test/GirGeoToolsMeasureExample.java',
        description: '测量示例，覆盖面积、长度、点点距离、点线距离和单位换算。'
      },
      {
        title: 'GirGeoToolsMergeExample',
        path: 'geoair-geo/geoair-geo-tools/src/test/java/cn/geoair/map/dynamic/tools/test/GirGeoToolsMergeExample.java',
        description: '几何合并示例，覆盖点、线、面合并到 MultiGeometry 和单 Geometry。'
      },
      {
        title: 'GirGeoToolsTileExample',
        path: 'geoair-geo/geoair-geo-tools/src/test/java/cn/geoair/map/dynamic/tools/test/GirGeoToolsTileExample.java',
        description: '瓦片与 QuadKey 示例，覆盖 xyzToTileBox、tileRangeByBox 和 xyzToQuadKey。'
      },
      {
        title: 'GirGeoToolsSridExample',
        path: 'geoair-geo/geoair-geo-tools/src/test/java/cn/geoair/map/dynamic/tools/test/GirGeoToolsSridExample.java',
        description: 'SRID 转换示例，覆盖 Geometry、Envelope 和单点坐标转换。'
      },
      {
        title: 'geoair-geo-tools GitHub 目录',
        path: 'https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-geo-tools',
        description: '直接跳到 geoair-geo-tools 模块目录。'
      }
    ],
    related: ['adv-query', 'file-tran', 'mvt', 'map-tile-forge', 'map-tile-fuser']
  },
  {
    slug: 'adv-query',
    route: '/modules/geo/adv-query',
    title: 'geoair-adv-query',
    group: 'geo',
    summary: '空间查询执行器模块，负责多数据库方言下的查询请求组织、条件构造与 SQL 生成。',
    tags: ['空间查询', 'PostGIS', 'SQL', 'BBox', '动态数据源'],
    sourceExamples: [
      {
        title: 'WhereQueryExample',
        path: 'geoair-geo/geoair-adv-query/src/test/java/cn/geoair/map/dynamic/adv/query/wherequery/test/WhereQueryExample.java',
        description: '覆盖基础条件、比较、范围、模糊、NULL、条件组、嵌套、分页、自定义 SQL、排序等串式查询写法。'
      },
      {
        title: 'LambdaFilterExample',
        path: 'geoair-geo/geoair-adv-query/src/test/java/cn/geoair/map/dynamic/adv/query/wherequery/test/LambdaFilterExample.java',
        description: '覆盖 Lambda 条件构造、模糊、IN、BETWEEN、NULL 判断和表达式条件。'
      },
      {
        title: 'GirAdvQueryRequestExample',
        path: 'geoair-geo/geoair-adv-query/src/test/java/cn/geoair/map/dynamic/adv/query/wherequery/test/GirAdvQueryRequestExample.java',
        description: '覆盖 GROUP BY、HAVING、DISTINCT、自定义 SQL、分页、SQL 视图和复杂业务场景。'
      },
      {
        title: 'GirAdvQueryRequest1Example',
        path: 'geoair-geo/geoair-adv-query/src/test/java/cn/geoair/map/dynamic/adv/query/wherequery/test/GirAdvQueryRequest1Example.java',
        description: '覆盖 Lambda builder、排序 API、动态条件构建、字段别名和业务型查询组织方式。'
      },
      {
        title: 'geoair-adv-query GitHub 目录',
        path: 'https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-adv-query',
        description: '直接跳到 geoair-adv-query 模块目录。'
      }
    ],
    related: ['geo-tools', 'dynamic-ds', 'db-service']
  },
  {
    slug: 'mvt',
    route: '/modules/geo/mvt',
    title: 'geoair-mvt',
    group: 'geo',
    summary: '矢量瓦片相关模块集合，覆盖实时 MVT、离线 Spark 生成和工具层处理。',
    tags: ['MVT', '矢量瓦片', 'Spark'],
    related: ['geo-tools', 'map-tile-forge', 'map-tile-fuser'],
    sourceExamples: [
      {
        title: 'AdvMvtTileUtilsExample',
        path: 'geoair-geo/geoair-mvt/geoair-mvt-tools/src/test/java/cn/geoair/map/dynamic/mvt/tools/test/AdvMvtTileUtilsExample.java',
        description: '展示 geoair-mvt-tools 中的瓦片范围计算与 TileExecParams 构造。'
      },
      {
        title: 'PipelineBuilderExample',
        path: 'geoair-geo/geoair-mvt/geoair-mvt-tools/src/test/java/cn/geoair/map/dynamic/mvt/tools/test/PipelineBuilderExample.java',
        description: '展示 PipelineBuilder 的坐标转换与几何简化流程。'
      },
      {
        title: 'GirRealMvtEntryExample',
        path: 'geoair-geo/geoair-mvt/geoair-real-mvt/src/test/java/cn/geoair/map/dynamic/mvt/test/GirRealMvtEntryExample.java',
        description: '展示 geoair-real-mvt 中的 GirRealMvtHelper、TileRequestParams 和 VectorTileExecutorV2 入口。'
      },
      {
        title: 'TileRequestParamsExample',
        path: 'geoair-geo/geoair-mvt/geoair-real-mvt/src/test/java/cn/geoair/map/dynamic/mvt/test/TileRequestParamsExample.java',
        description: '展示 TileRequestParams 的参数组织与 Base32 编解码。'
      },
      {
        title: 'TileExecutorConfigExample',
        path: 'geoair-geo/geoair-mvt/geoair-real-mvt/src/test/java/cn/geoair/map/dynamic/mvt/test/TileExecutorConfigExample.java',
        description: '展示 TileExecutorConfig 的低级别优化策略和密度优化策略配置。'
      },
      {
        title: 'TileGlobalConfigExample',
        path: 'geoair-geo/geoair-mvt/geoair-real-mvt/src/test/java/cn/geoair/map/dynamic/mvt/test/TileGlobalConfigExample.java',
        description: '展示 TileGlobalConfig 如何组合 TileRequestParams、TileExecParams 和 TileExecutorConfig。'
      },
      {
        title: 'TileSliceParameterExample',
        path: 'geoair-geo/geoair-mvt/geoair-static-mvt-spark/src/test/java/cn/geoair/map/dynamic/statics/mvt/spark/vectile/test/TileSliceParameterExample.java',
        description: '展示 TileSliceParameter 的离线切片参数组织与 Base32 编解码。'
      },
      {
        title: 'geoair-mvt GitHub 目录',
        path: 'https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-mvt',
        description: '直接跳到 geoair-mvt 模块目录。'
      }
    ]
  },
  {
    slug: 'map-tile-forge',
    route: '/modules/geo/map-tile-forge',
    title: 'geoair-map-tile-forge',
    group: 'geo',
    summary: '瓦片读取与服务适配层，负责统一图层配置、存储类型适配与瓦片输出。',
    tags: ['瓦片服务', 'S3', '栅格地图'],
    related: ['mvt', 'map-tile-fuser', 'by-gwc'],
    sourceExamples: [
      {
        title: 'GirMapTileForgeExample',
        path: 'geoair-geo/geoair-map-tile-forge/src/test/java/cn/geoair/map/tile/forge/core/test/GirMapTileForgeExample.java',
        description: '展示 GirMapTileService、TileStorageSupportAdapter 和 ITileStorageSupport 的主入口。'
      },
      {
        title: 'GirLayerConfigContextExample',
        path: 'geoair-geo/geoair-map-tile-forge/src/test/java/cn/geoair/map/tile/forge/core/test/GirLayerConfigContextExample.java',
        description: '展示 GirLayerConfigContext 的核心字段组织方式。'
      },
      {
        title: 'TileForgeEnumExample',
        path: 'geoair-geo/geoair-map-tile-forge/src/test/java/cn/geoair/map/tile/forge/core/test/TileForgeEnumExample.java',
        description: '展示 GirStorageType 与 GirMapTileType 的枚举值和取值范围。'
      },
      {
        title: 'TileRequestExample',
        path: 'geoair-geo/geoair-map-tile-forge/src/test/java/cn/geoair/map/tile/forge/core/test/TileRequestExample.java',
        description: '展示 TileRequest 返回对象的基本结构和 emptyByContext 用法。'
      },
      {
        title: 'XyzTest',
        path: 'geoair-geo/geoair-map-tile-forge/src/test/java/test/XyzTest.java',
        description: '展示基于 GirLayerConfigContext 的本地 ZIP XYZ 预缓存流程。'
      },
      {
        title: 'geoair-map-tile-forge GitHub 目录',
        path: 'https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-map-tile-forge',
        description: '直接跳到 geoair-map-tile-forge 模块目录。'
      }
    ]
  },
  {
    slug: 'map-tile-fuser',
    route: '/modules/geo/map-tile-fuser',
    title: 'geoair-map-tile-fuser',
    group: 'geo',
    summary: '多源瓦片融合层，负责根据图层配置创建 getter、接入缓存并输出融合结果。',
    tags: ['瓦片融合', '缓存', '影像拼接'],
    related: ['map-tile-forge', 'mvt'],
    sourceExamples: [
      {
        title: 'GirMapTileFuserExample',
        path: 'geoair-geo/geoair-map-tile-fuser/src/test/java/cn/geoair/map/tile/forge/fuser/test/GirMapTileFuserExample.java',
        description: '展示 GirFuser、PxyLayerInfo、TileGetterFactory 的入口关系。'
      },
      {
        title: 'TileFuserConfigExample',
        path: 'geoair-geo/geoair-map-tile-fuser/src/test/java/cn/geoair/map/tile/forge/fuser/test/TileFuserConfigExample.java',
        description: '展示 SrcType、OriginType、PxyLayerInfo 这些配置模型的组合方式。'
      },
      {
        title: 'LayerTileGetterRouteExample',
        path: 'geoair-geo/geoair-map-tile-fuser/src/test/java/cn/geoair/map/tile/forge/fuser/test/LayerTileGetterRouteExample.java',
        description: '展示不同 SrcType / gridSrid 如何路由到不同的 LayerTileGetter 实现。'
      },
      {
        title: 'FuserExecContractExample',
        path: 'geoair-geo/geoair-map-tile-fuser/src/test/java/cn/geoair/map/tile/forge/fuser/test/FuserExecContractExample.java',
        description: '展示 FuserExec 这一层输出契约：图像字节、输出格式、源格式和源范围。'
      },
      {
        title: 'geoair-map-tile-fuser GitHub 目录',
        path: 'https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-map-tile-fuser',
        description: '直接跳到 geoair-map-tile-fuser 模块目录。'
      }
    ]
  },
  {
    slug: 'by-gwc',
    route: '/modules/geo/by-gwc',
    title: 'geoair-by-gwc',
    group: 'geo',
    summary: '直接读取 ArcGIS Compact Cache 格式的瓦片缓存，无需依赖 ArcGIS Server。',
    tags: ['ArcGIS', 'GeoWebCache', '缓存直读'],
    capabilities: [
      '支持 ArcGIS Compact Cache V1/V2 格式。',
      '解析 bundlx 和 bundle 文件，按偏移量读取切片。',
      '自动生成 WMTS 能力文档。'
    ],
    quickStart: '适合复用历史 ArcGIS 缓存资产，而不重建原始服务。',
    example: '对于已有大规模 ArcGIS 缓存的组织，可以低成本接入现有瓦片。',
    related: ['map-tile-forge']
  },
  {
    slug: 'jts-all',
    route: '/modules/geo/jts-all',
    title: 'geoair-jts-all',
    group: 'geo',
    summary: 'JTS 相关能力的聚合打包模块，便于统一引入几何处理基础设施。',
    tags: ['JTS', 'Geometry'],
    capabilities: [
      '统一汇总 JTS 相关依赖与扩展能力。',
      '适合作为空间几何处理的基础依赖入口。',
      '便于下游模块减少零散依赖声明。'
    ],
    quickStart: `<dependency>\n  <groupId>cn.geoair.devkit</groupId>\n  <artifactId>geoair-jts-all</artifactId>\n  <version>J8-dev-SNAPSHOT</version>\n</dependency>`,
    example: '适合做 Geometry 序列化、空间分析和空间数据库映射的通用基础。',
    related: ['geo-tools', 'message-jts-jackson', 'message-jts-mybatis']
  }
]

const businessModules = [
  {
    slug: 'apidoc',
    route: '/modules/apidoc',
    title: 'geoair-apidoc',
    group: 'business',
    summary: '基于 Knife4j 的 API 文档模块，同时支持 OpenAPI 3 和 Swagger 2。',
    tags: ['API 文档', 'Knife4j', 'Spring Boot Starter'],
    capabilities: [
      '自动扫描 Controller 包路径并按包分组。',
      '支持文档导出和零配置启动。',
      '兼容 OpenAPI 3 与 Swagger 2 两条规范。'
    ],
    quickStart: `<dependency>\n  <groupId>cn.geoair.devkit</groupId>\n  <artifactId>geoair-knife4j-springdoc-spring-boot-starter</artifactId>\n  <version>J8-dev-SNAPSHOT</version>\n</dependency>`,
    example: '访问 /doc.html 即可查看文档页面，适合 Spring Boot 服务快速接入。',
    related: ['code-generator', 'db-service']
  },
  {
    slug: 'code-generator',
    route: '/modules/code-generator',
    title: 'geoair-code-generator',
    group: 'business',
    summary: '从数据库表结构生成 Entity、Mapper、Service、Controller 以及 Vue 组件代码。',
    tags: ['代码生成', '模板', 'Vue'],
    capabilities: [
      '支持生成后端与前端基础代码。',
      '支持按项目规范自定义模板。',
      '减少重复样板代码，提升交付速度。'
    ],
    quickStart: '适合新建 CRUD 业务模块时快速拉起基础代码。',
    example: '从库表直接生成 Entity、Mapper 和管理页面，缩短业务起步时间。',
    related: ['apidoc', 'db-service']
  },
  {
    slug: 'geo',
    route: '/modules/geo',
    title: 'geoair-geo',
    group: 'business',
    summary: 'GeoAir 最核心的 GIS 能力集合，覆盖坐标转换、空间查询、文件互转、瓦片、GeoServer 与缓存直读。',
    tags: ['GIS 核心', 'GeoTools', '空间处理'],
    capabilities: [
      '提供统一的空间处理与空间服务能力。',
      '下含多个可独立接入的 GIS 子模块。',
      '是构建地图平台、空间分析和空间服务的核心基础。'
    ],
    quickStart: `<dependency>\n  <groupId>cn.geoair.devkit</groupId>\n  <artifactId>geoair-geo-tools</artifactId>\n  <version>J8-dev-SNAPSHOT</version>\n</dependency>`,
    example: '如果你的项目要处理坐标、Geometry、空间查询或瓦片服务，geoair-geo 是最先需要了解的模块组。',
    related: geoModules.map(item => item.slug),
    children: geoModules.map(item => item.slug)
  },
  {
    slug: 'dynamic-ds',
    route: '/modules/dynamic-ds',
    title: 'geoair-dynamic-ds',
    group: 'business',
    summary: '动态数据源相关模块，围绕 Spring 切面切库、线程上下文和主从读写分离 builder 展开。',
    tags: ['动态数据源', 'AOP', '读写分离', 'Spring', 'SQLParser'],
    sourceExamples: [
      {
        title: 'BuilderTest',
        path: 'geoair-dynamic-ds/src/test/java/cn/geoair/comp/dynamic/ds/readwrite/test/BuilderTest.java',
        description: '覆盖主从 builder、批量 slaves、addSlave、权重策略和静态快速 build 方式。'
      },
      {
        title: 'SQLParserUtilTest',
        path: 'geoair-dynamic-ds/src/test/java/cn/geoair/comp/dynamic/ds/readwrite/test/SQLParserUtilTest.java',
        description: '覆盖 SELECT、INSERT、UPDATE、DELETE、SHOW、DESC、EXPLAIN 和复杂 SQL 的读写识别。'
      },
      {
        title: 'WithStatementTest',
        path: 'geoair-dynamic-ds/src/test/java/cn/geoair/comp/dynamic/ds/readwrite/test/WithStatementTest.java',
        description: '专门覆盖 WITH / RECURSIVE / INSERT ... RETURNING / UPDATE / DELETE 等 CTE 语句识别。'
      },
      {
        title: 'EnableDynamicDs / GirDsAspectDoAroundApiHelper',
        path: 'geoair-dynamic-ds/src/main/java/cn/geoair/comp/dynamic/ds/datasource/EnableDynamicDs.java 等主源码类',
        description: '说明 Spring 场景下如何启用切面，以及切面前后如何调用 pushDataSource / popDataSource。'
      },
      {
        title: 'geoair-dynamic-ds GitHub 目录',
        path: 'https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-dynamic-ds',
        description: '直接跳到 geoair-dynamic-ds 模块目录。'
      }
    ],
    related: ['adv-query', 'db-service']
  },
  {
    slug: 'db-service',
    route: '/modules/db-service',
    title: 'geoair-db-service',
    group: 'business',
    summary: '数据库可视化服务模块，前后端配合提供 SQL 编辑、数据浏览与表结构管理。',
    tags: ['数据库管理', 'Vue2', '可视化'],
    capabilities: [
      '提供数据库访问抽象层与可视化 Web 管理界面。',
      '前端技术栈为 Vue2 + Element UI + ECharts。',
      '适合作为数据库运维和数据服务配置入口。'
    ],
    quickStart: '包含 geoair-db-service-core、starter 与 webview 三部分。',
    example: '可作为数据库 Web 管理入口，也能为其他模块提供数据访问支持。',
    related: ['dynamic-ds', 'apidoc']
  },
  {
    slug: 'message-jts-jackson',
    route: '/modules/message-jts-jackson',
    title: 'geoair-message-jts-jackson',
    group: 'business',
    summary: 'JTS Geometry 与 JSON 之间的序列化转换模块，支持 GeoJSON 输出。',
    tags: ['Jackson', 'JTS', 'GeoJSON'],
    capabilities: [
      '自动注册 GeometrySerializer 与 GeometryDeserializer。',
      '支持 Point、Polygon、Multi* 与 GeometryCollection。',
      '适合把空间对象直接暴露为 API 返回值。'
    ],
    quickStart: `<dependency>\n  <groupId>cn.geoair.devkit</groupId>\n  <artifactId>geoair-message-jts-jackson</artifactId>\n</dependency>`,
    example: '在 REST API 中返回空间对象时，可以直接输出 GeoJSON 风格的结构。',
    related: ['jts-all', 'message-jts-mybatis']
  },
  {
    slug: 'message-jts-mybatis',
    route: '/modules/message-jts-mybatis',
    title: 'geoair-message-jts-mybatis',
    group: 'business',
    summary: 'JTS Geometry 与数据库空间字段之间的 MyBatis TypeHandler 映射模块。',
    tags: ['MyBatis', 'JTS', '空间字段'],
    capabilities: [
      '自动把数据库空间字段映射为 JTS Geometry。',
      '支持 PostGIS、Oracle Spatial 等空间数据库。',
      '让业务层直接面向 Geometry 编程。'
    ],
    quickStart: '适合 MyBatis 项目直接读写空间字段。',
    example: '查询结果可以直接得到 Geometry 对象，无需手工解析数据库二进制或文本格式。',
    related: ['message-jts-jackson', 'jts-all']
  }
]

export const sectionCatalog = {
  standard: {
    key: 'standard',
    title: '标准基础库与工程管理',
    description: '从依赖管理、父 POM 到抽象接口、SPI 实现、Web 和 ORM 能力，构成 GeoAir 的基础底座。',
    route: '/standard',
    modules: [...dependencyModules, ...standardModules]
  },
  business: {
    key: 'business',
    title: '业务模块',
    description: '面向开发效率、空间业务与数据服务的模块集合，是搭建真实 GIS 业务系统的主要能力来源。',
    route: '/modules',
    modules: businessModules
  },
  geo: {
    key: 'geo',
    title: 'GIS 子模块',
    description: 'GeoAir 最具差异化的一层，围绕坐标、Geometry、查询、瓦片、GeoServer 与缓存读写展开。',
    route: '/modules/geo',
    modules: geoModules
  }
}

export const homeSections = [
  {
    title: '标准基础库与工程管理',
    description: '用统一抽象和父工程把框架能力收束到稳定基础层。',
    route: '/standard',
    items: [...dependencyModules.slice(0, 3), ...standardModules.slice(0, 3)]
  },
  {
    title: '业务模块',
    description: '围绕文档、代码生成、数据服务与空间组件，形成可直接接入的模块目录。',
    route: '/modules',
    items: businessModules
  },
  {
    title: 'GIS 子模块',
    description: '从坐标到瓦片，从文件互转到 GeoServer，按场景拆成独立子模块。',
    route: '/modules/geo',
    items: geoModules
  }
]

export const allModules = [...dependencyModules, ...standardModules, ...businessModules, ...geoModules]

export const moduleMap = allModules.reduce((accumulator, item) => {
  accumulator[item.slug] = item
  return accumulator
}, {})

export function getModuleBySlug(slug) {
  return moduleMap[slug] || null
}

export function getSectionByKey(key) {
  return sectionCatalog[key] || null
}
