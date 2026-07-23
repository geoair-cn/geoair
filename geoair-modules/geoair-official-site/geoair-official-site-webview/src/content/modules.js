const dependencyModules = [
  {
    slug: 'dependencies-bom',
    route: '/standard/dependencies-bom',
    title: 'geoair-dependencies-bom',
    group: 'standard',
    summary: '依赖版本管理中心，负责按领域组织第三方依赖版本。',
    tags: ['BOM', 'Maven', '依赖管理'],
    sourceExamples: [
      {
        title: 'geoair-dependencies-bom GitHub 目录',
        path: 'https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-dependencies-bom',
        description: '直接跳到 geoair-dependencies-bom 模块目录。'
      },
      {
        title: 'geoair-geotools-dependencies',
        path: 'https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-dependencies-bom/geoair-geotools-dependencies',
        description: 'GeoTools 相关依赖定义目录。'
      },
      {
        title: 'geoair-openapi-dependencies',
        path: 'https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-dependencies-bom/geoair-openapi-dependencies',
        description: 'OpenAPI / Swagger 相关依赖定义目录。'
      }
    ],
    related: ['base-parent', 'framework-bom']
  },
  {
    slug: 'base-parent',
    route: '/standard/base-parent',
    title: 'geoair-base-parent',
    group: 'standard',
    summary: '基础父 POM，负责聚合依赖管理、构建配置与统一继承入口。',
    tags: ['Parent POM', '构建', '发布'],
    sourceExamples: [
      {
        title: 'geoair-base-parent GitHub 目录',
        path: 'https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-base-parent',
        description: '直接跳到 geoair-base-parent 模块目录。'
      },
      {
        title: 'geoair-base-parent pom.xml',
        path: 'https://github.com/geoair-cn/geoair/blob/master/geoair-framework/geoair-base-parent/pom.xml',
        description: '父 POM 本身的构建与依赖配置。'
      }
    ],
    related: ['dependencies-bom', 'framework-bom', 'base']
  },
  {
    slug: 'framework-bom',
    route: '/standard/framework-bom',
    title: 'geoair-framework-bom',
    group: 'standard',
    summary: '面向不同项目类型的父 POM 集合，负责 API / 项目 / Starter 三类模板。',
    tags: ['工程类型', 'Maven', '父 POM'],
    sourceExamples: [
      {
        title: 'geoair-framework-bom GitHub 目录',
        path: 'https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-framework-bom',
        description: '直接跳到 geoair-framework-bom 模块目录。'
      },
      {
        title: 'geoair-project-parent',
        path: 'https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-framework-bom/geoair-project-parent',
        description: '普通业务项目父工程目录。'
      },
      {
        title: 'geoair-spring-boot-starter-parent',
        path: 'https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-framework-bom/geoair-spring-boot-starter-parent',
        description: 'Starter 父工程目录。'
      }
    ],
    related: ['base-parent']
  }
]

const standardModules = [
  {
    slug: 'base',
    route: '/standard/base',
    title: 'geoair-base',
    group: 'standard',
    summary: '基础抽象层，负责定义 Bean、缓存、结果模型、环境访问和统一门面入口。',
    tags: ['SPI', '接口层', '零外部依赖'],
    sourceExamples: [
      {
        title: 'GirBeanHelper',
        path: 'https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-base/src/main/java/cn/geoair/base/bean',
        description: 'Bean 获取相关目录。'
      },
      {
        title: 'GirBeanHelperExample',
        path: 'geoair-standard/geoair-base/src/test/java/cn/geoair/base/test/GirBeanHelperExample.java',
        description: '展示 GirBeanHelper 默认入口与兜底逻辑。'
      },
      {
        title: 'GirCacheHelper',
        path: 'https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-base/src/main/java/cn/geoair/base/cache',
        description: '缓存辅助类相关目录。'
      },
      {
        title: 'GiResult / GirResultExample',
        path: 'geoair-standard/geoair-base/src/test/java/cn/geoair/base/test/GirResultExample.java',
        description: '展示统一结果模型的最小用法。'
      }
    ],
    related: ['core', 'web', 'orm']
  },
  {
    slug: 'core',
    route: '/standard/core',
    title: 'geoair-core',
    group: 'standard',
    summary: '基础抽象层的默认实现，负责把 Bean、缓存、JSON 和日志能力接到具体实现上。',
    tags: ['Spring', 'SPI 实现', '适配层'],
    sourceExamples: [
      {
        title: 'SpringContextBean4Gir',
        path: 'https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-core/src/main/java/cn/geoair/spi/bean',
        description: 'Spring 容器适配目录。'
      },
      {
        title: 'Cache4Gir',
        path: 'https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-core/src/main/java/cn/geoair/spi/cache',
        description: '缓存 SPI 实现目录。'
      },
      {
        title: 'GirJacksonJsonExample',
        path: 'geoair-standard/geoair-core/src/test/java/cn/geoair/spi/test/GirJacksonJsonExample.java',
        description: '展示 GirJacksonJson 的最小 JSON 序列化入口。'
      }
    ],
    related: ['base', 'web']
  },
  {
    slug: 'web',
    route: '/standard/web',
    title: 'geoair-web',
    group: 'standard',
    summary: 'Web 工具层，负责请求响应门面、会话管理、结果模型和分页参数提供。',
    tags: ['Web', '会话', '权限'],
    sourceExamples: [
      {
        title: 'GirWeb',
        path: 'https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-web/src/main/java/cn/geoair/web',
        description: 'Web 门面入口与核心目录。'
      },
      {
        title: 'GirCookieSession / GirTokenSession',
        path: 'https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-web/src/main/java/cn/geoair/web/session',
        description: '会话管理相关目录。'
      },
      {
        title: 'GirWebResult',
        path: 'https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-web/src/main/java/cn/geoair/web/data/result',
        description: 'Web 结果模型目录。'
      }
    ],
    related: ['base', 'core']
  },
  {
    slug: 'orm',
    route: '/standard/orm',
    title: 'geoair-orm',
    group: 'standard',
    summary: 'ORM 适配层，负责把多种 ORM 框架接回统一的持久化抽象。',
    tags: ['ORM', 'MyBatis', 'JPA'],
    sourceExamples: [
      {
        title: 'geoair-orm GitHub 目录',
        path: 'https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-orm',
        description: '直接跳到 geoair-orm 模块目录。'
      },
      {
        title: 'TkEntityHelper',
        path: 'https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-orm/geoair-orm-mybatis-tk/src/main/java/cn/geoair/orm/tkmapper/util',
        description: 'TK Mapper 工具相关目录。'
      }
    ],
    related: ['base', 'core']
  },
  {
    slug: 'sdk',
    route: '/standard/sdk',
    title: 'geoair-sdk',
    group: 'standard',
    summary: 'SDK 侧工具与配置层，负责配置、body、file 和一些面向交付的辅助能力。',
    tags: ['SDK', '对外输出'],
    sourceExamples: [
      {
        title: 'geoair-sdk GitHub 目录',
        path: 'https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-sdk',
        description: '直接跳到 geoair-sdk 模块目录。'
      },
      {
        title: 'body 目录',
        path: 'https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-sdk/src/main/java/cn/geoair/sdk/body',
        description: '请求体与 multipart 相关目录。'
      }
    ],
    related: ['base', 'tools']
  },
  {
    slug: 'tools',
    route: '/standard/tools',
    title: 'geoair-tools',
    group: 'standard',
    summary: '底层轻量工具层，提供控制台输出和一些基础辅助能力。',
    tags: ['工具类', '反射', '基础设施'],
    sourceExamples: [
      {
        title: 'geoair-tools GitHub 目录',
        path: 'https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-tools',
        description: '直接跳到 geoair-tools 模块目录。'
      },
      {
        title: 'GkConsole / GkConsoleTable',
        path: 'https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-standard/geoair-tools/src/main/java/cn/geoair/base/tool',
        description: '控制台与表格输出相关目录。'
      }
    ],
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
    summary: '直接读取 ArcGIS Compact Cache 相关缓存结构，负责 GridSet 组织与 WMTS 能力描述生成。',
    tags: ['ArcGIS', 'GeoWebCache', '缓存直读'],
    sourceExamples: [
      {
        title: 'ArcGISCompactCache / V1 / V2',
        path: 'https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-by-gwc/src/main/java/cn/geoair/map/tile/forge/core/bygwc/compact',
        description: 'ArcGIS Compact Cache 读取相关核心目录。'
      },
      {
        title: 'GridSetBuilder',
        path: 'https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-by-gwc/src/main/java/cn/geoair/map/tile/forge/core/bygwc/layer',
        description: 'GridSet 与图层网格组织相关目录。'
      },
      {
        title: 'GetCapabilitiesGenerator',
        path: 'https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-by-gwc/src/main/java/cn/geoair/map/tile/forge/core/bygwc/wmts',
        description: 'WMTS 能力描述生成相关目录。'
      }
    ],
    related: ['map-tile-forge']
  },
  {
    slug: 'jts-all',
    route: '/modules/geo/jts-all',
    title: 'geoair-jts-all',
    group: 'geo',
    summary: 'JTS 相关能力的聚合桥接模块，用于统一引入 Geometry 基础依赖。',
    tags: ['JTS', 'Geometry'],
    sourceExamples: [
      {
        title: 'geoair-jts-all GitHub 目录',
        path: 'https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-geo/geoair-jts-all',
        description: '直接跳到 geoair-jts-all 模块目录。'
      },
      {
        title: 'Test',
        path: 'geoair-geo/geoair-jts-all/src/main/java/cn/geoair/map/dynamic/jts/all/geojsonjackson/Test.java',
        description: '当前模块中可直接看到的轻量测试入口。'
      }
    ],
    related: ['geo-tools', 'message-jts-jackson', 'message-jts-mybatis']
  }
]

const businessModules = [
  {
    slug: 'apidoc',
    route: '/modules/apidoc',
    title: 'geoair-apidoc',
    group: 'business',
    summary: '接口文档配置模块，负责文档分组、主页信息和 Spring 中的文档配置组织。',
    tags: ['API 文档', 'Knife4j', 'Spring Boot Starter'],
    sourceExamples: [
      {
        title: 'GirOpenApiConfig',
        path: 'https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-apidoc/geoair-knife4j-core/src/main/java/cn/geoair/comp/knife4j/ext/core/config',
        description: '抽象配置基类目录。'
      },
      {
        title: 'DocketInfo / ApiModelInfo',
        path: 'https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-apidoc/geoair-knife4j-core/src/main/java/cn/geoair/comp/knife4j/ext/core/model',
        description: '文档分组和主页信息模型目录。'
      },
      {
        title: 'Swagger2Configuration demo',
        path: 'geoair-apidoc/geoair-knife4j-spring-boot-demo/src/main/java/cn/geoair/comp/demo/knife4j/config/Swagger2Configuration.java',
        description: '仓库里现有的 Swagger2Configuration 示例。'
      }
    ],
    related: ['code-generator', 'db-service']
  },
  {
    slug: 'code-generator',
    route: '/modules/code-generator',
    title: 'geoair-code-generator',
    group: 'business',
    summary: '代码生成模块，负责把数据库表结构组织成后端与前端的基础代码骨架。',
    tags: ['代码生成', '模板', 'Vue'],
    sourceExamples: [
      {
        title: 'geoair-code-generator GitHub 目录',
        path: 'https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-code-generator',
        description: '直接跳到 geoair-code-generator 模块目录。'
      },
      {
        title: 'geoair-code-gen-module',
        path: 'https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-code-generator/geoair-code-gen-module',
        description: '核心生成逻辑目录。'
      },
      {
        title: 'geoair-code-gen-demo',
        path: 'https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-code-generator/geoair-code-gen-demo',
        description: '代码生成 demo 目录。'
      }
    ],
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
    quickStart: `<dependency>\n  <groupId>cn.geoair.devkit</groupId>\n  <artifactId>geoair-geo-tools</artifactId>\n  <version>J17-dev-SNAPSHOT</version>\n</dependency>`,
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
    summary: '数据库服务与管理界面组合模块，覆盖数据源管理、SQL 执行与前端可视化界面。',
    tags: ['数据库管理', 'Vue2', '可视化'],
    sourceExamples: [
      {
        title: 'DsApiService',
        path: 'https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-db-service/geoair-db-service-core/src/main/java/cn/geoair/comp/db/service/core/basic/service',
        description: '数据库 API 服务层相关目录。'
      },
      {
        title: 'GirDsSQLExecutor',
        path: 'https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-db-service/geoair-db-service-core/src/main/java/cn/geoair/comp/db/service/core/basic/executor',
        description: 'SQL 执行器相关目录。'
      },
      {
        title: 'DbServiceDtoExample',
        path: 'geoair-db-service/geoair-db-service-core/src/test/java/cn/geoair/comp/db/service/core/test/DbServiceDtoExample.java',
        description: '展示 DsDataSourceApo、SQLTaskDto、ResponseDto 这几类核心 DTO 的基本使用方式。'
      },
      {
        title: 'geoair-db-service-webview',
        path: 'https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-db-service/geoair-db-service-webview',
        description: '前端管理界面目录。'
      }
    ],
    related: ['dynamic-ds', 'apidoc']
  },
  {
    slug: 'message-jts-jackson',
    route: '/modules/message-jts-jackson',
    title: 'geoair-message-jts-jackson',
    group: 'business',
    summary: 'JTS Geometry 与 Jackson 之间的序列化转换模块，主要负责 Geometry JSON 输出和自动注册。',
    tags: ['Jackson', 'JTS', 'GeoJSON'],
    sourceExamples: [
      {
        title: 'GirJtsJacksonUtils',
        path: 'https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-message-jts-jackson/src/main/java/cn/geoair/comp/message/converter/jts/jackson/utils',
        description: 'JTS 与 Jackson 的工具层目录。'
      },
      {
        title: 'JtsJacksonModuleExample',
        path: 'geoair-message-jts-jackson/src/test/java/cn/geoair/comp/message/converter/jts/jackson/test/JtsJacksonModuleExample.java',
        description: '展示 JtsExtModule 如何注册到 ObjectMapper。'
      },
      {
        title: 'JtsExtModule',
        path: 'https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-message-jts-jackson/src/main/java/cn/geoair/comp/message/converter/jts/jackson/serializer/jts',
        description: 'JTS 序列化模块相关目录。'
      },
      {
        title: 'GirJacksonJtsAutoConfiguration',
        path: 'https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-message-jts-jackson/src/main/java/cn/geoair/comp/message/converter/jts/jackson/auto',
        description: '自动装配相关目录。'
      }
    ],
    related: ['jts-all', 'message-jts-mybatis']
  },
  {
    slug: 'message-jts-mybatis',
    route: '/modules/message-jts-mybatis',
    title: 'geoair-message-jts-mybatis',
    group: 'business',
    summary: 'JTS Geometry 与 MyBatis 类型处理层的桥接模块，负责空间字段映射与配置接入。',
    tags: ['MyBatis', 'JTS', '空间字段'],
    sourceExamples: [
      {
        title: 'PgGeometryTypeHandler',
        path: 'https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-message-jts-mybatis/src/main/java/cn/geoair/comp/message/converter/jts/mybatis/typehander',
        description: 'Geometry TypeHandler 相关目录。'
      },
      {
        title: 'PgGeometryTypeHandlerExample',
        path: 'geoair-message-jts-mybatis/src/test/java/cn/geoair/comp/message/converter/jts/mybatis/test/PgGeometryTypeHandlerExample.java',
        description: '展示 PgGeometryTypeHandler 与 Geometry/JdbcType.OTHER 的基本对应关系。'
      },
      {
        title: 'GirMyBatisConfigurationCustomizer',
        path: 'https://github.com/geoair-cn/geoair/tree/master/geoair-framework/geoair-modules/geoair-message-jts-mybatis/src/main/java/cn/geoair/comp/message/converter/jts/mybatis/config',
        description: 'MyBatis 配置扩展相关目录。'
      }
    ],
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
