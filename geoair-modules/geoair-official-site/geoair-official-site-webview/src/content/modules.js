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
    summary: 'GIS 工具集，统一封装坐标转换、格式互转、空间测量、几何合并与瓦片坐标计算。',
    tags: ['GIS', '坐标转换', 'GeoTools'],
    capabilities: [
      '支持 WGS84、GCJ02、BD09、EPSG:3857、EPSG:4490 等常见坐标体系。',
      '支持 GeoJSON、WKT、WKB、JTS Geometry 与 PGGeometry 互转。',
      '提供面积、长度、距离、Geometry 合并与瓦片索引计算。'
    ],
    quickStart: `GirGeoTools tools = GirGeoTools.defaultInstance();\ndouble[] gcj02 = tools.getCoordinateOpt().wgs84ToGcj02(116.40, 39.90);`,
    example: '适合地图服务、空间分析和地理数据导入导出等基础场景。',
    related: ['geo', 'adv-query', 'file-tran']
  },
  {
    slug: 'adv-query',
    route: '/modules/geo/adv-query',
    title: 'geoair-adv-query',
    group: 'geo',
    summary: 'GeoAir 的高级空间查询执行器，围绕多数据库方言、空间过滤、动态 SQL、分页与 Lambda 条件构造，提供一套更贴近 GIS 业务的数据库访问方式。',
    tags: ['空间查询', 'PostGIS', 'SQL', 'BBox', '动态数据源'],
    capabilities: [
      '统一聚合 CRUD、DDL、空间查询、分页和事务相关能力，适合作为 GIS 数据访问层的核心执行入口。',
      '适配 PostgreSQL + PostGIS、MySQL 与 Oracle Spatial，适合存在多数据库方言差异的空间项目。',
      '支持 BBox、距离、相交、质心、几何修复等常见空间操作，并与 Geometry / WKT / GeoJSON 等对象模型衔接。',
      '内置 Fluent Lambda 条件构造器与动态 SQL 标签能力，便于把复杂 GIS 查询组织成可维护代码。'
    ],
    quickStart: `<dependency>\n  <groupId>cn.geoair.devkit</groupId>\n  <artifactId>geoair-adv-query</artifactId>\n  <version>J8-dev-SNAPSHOT</version>\n</dependency>\n\n@Resource\nprivate IAdvExecutor executor;\n\nList<User> users = executor.wSelectList(User.class, builder -> builder\n  .select("id", "name", "geom")\n  .where(w -> w.eq(User::getStatus, 1))\n  .orderBy(o -> o.desc(User::getId)));`,
    example: '适合空间检索、专题图查询、多租户 GIS 服务和跨数据库空间访问场景。',
    detailSections: [
      {
        title: '模块定位',
        items: [
          '它不是单纯的 SQL 拼接工具，而是把空间查询、数据库方言差异和分页能力统一到同一执行入口。',
          '如果 geoair-geo-tools 负责 Geometry 处理，那么 geoair-adv-query 更偏向“如何把空间对象查出来、筛出来、分页出来”。',
          '在真实 GIS 系统里，它通常位于 Service 与数据库之间，承担高级空间数据访问层的职责。'
        ]
      },
      {
        title: '核心接口组成',
        items: [
          'IAdvExecutor 聚合了数据源获取、事务模板、基础 CRUD、DDL、空间操作、条件构造与分页查询能力。',
          'IAdvGeoOpt 负责相交、距离、BBox 等空间操作，适合地图范围查询、缓冲区分析和空间筛选。',
          'IAdvWhereSelectOpt 负责 Fluent 风格的查询构造，适合把复杂业务条件写成更可维护的链式表达。',
          'IAdvSimplePageOpt 负责分页能力，适合地图要素列表、表格数据和条件检索页面。'
        ]
      },
      {
        title: '适用场景',
        items: [
          '地图框选查询：前端传入 bbox，后端根据范围筛选点、线、面要素。',
          '专题图筛选：按行业、状态、行政区划等属性条件叠加空间条件做组合查询。',
          '多库适配：同一套 GIS 查询能力需要兼容 PostGIS、MySQL 或 Oracle Spatial。',
          '空间数据服务：为地图接口、要素检索、统计分析或矢量瓦片查询提供底层执行能力。'
        ]
      },
      {
        title: '和其他模块的关系',
        items: [
          '通常与 geoair-geo-tools 配合：前者负责查，后者负责算、转和处理 Geometry。',
          '在多租户或多数据源系统中，常与 geoair-dynamic-ds 配合完成运行时库切换。',
          '如果上层还需要数据库可视化管理或数据服务配置，可以继续配合 geoair-db-service。'
        ]
      }
    ],
    usageExamples: [
      {
        title: '基础条件查询',
        description: '适合后台列表、业务筛选和属性检索，先从常规字段过滤开始。',
        code: `List<User> users = executor.wSelectList(User.class, builder -> builder\n  .select("id", "name", "status")\n  .where(w -> w.eq(User::getStatus, 1)\n    .like(User::getName, "新区"))\n  .orderBy(o -> o.desc(User::getId)));`
      },
      {
        title: '地图范围 BBox 查询',
        description: '当前端地图缩放或拖拽后，可用 bbox 只查询视域内要素，减少全量返回。',
        code: `String bbox = "116.30,39.85,116.55,40.02";\n\nList<Map<String, Object>> rows = executor.wSelectMapList("poi_table", builder -> builder\n  .select("id", "name", "geom")\n  .whereGeo(g -> g.bbox("geom", bbox))\n  .limit(500));`
      },
      {
        title: '属性条件 + 空间相交组合查询',
        description: '适合专题图、行政区分析或图层筛选，把属性条件和空间条件放在一次查询里。',
        code: `String polygonWkt = "POLYGON((116.3 39.8,116.6 39.8,116.6 40.0,116.3 40.0,116.3 39.8))";\n\nList<RiverEntity> rivers = executor.wSelectList(RiverEntity.class, builder -> builder\n  .select("id", "river_name", "geom")\n  .where(w -> w.eq(RiverEntity::getLevel, 2))\n  .whereGeo(g -> g.intersectsWkt("geom", polygonWkt, 4326)));`
      },
      {
        title: '分页空间检索',
        description: '适合前端表格、分页列表和管理端检索结果页。',
        code: `GiPager<CompanyEntity> pager = executor.wSelectPage(CompanyEntity.class, builder -> builder\n  .page(1, 20)\n  .select("id", "company_name", "geom")\n  .where(w -> w.eq(CompanyEntity::getDeleted, 0))\n  .whereGeo(g -> g.distanceLt("geom", 116.40, 39.90, 3000, 4326)));`
      },
      {
        title: '动态数据源联合使用',
        description: '适合一套系统同时连接业务库、专题库或租户库时，先切库再执行空间查询。',
        code: `GirDynamicStackDataSource.push("tenant-gis-ds");\ntry {\n  List<Map<String, Object>> plots = executor.wSelectMapList("land_plot", builder -> builder\n    .select("id", "plot_name", "geom")\n    .whereGeo(g -> g.bbox("geom", "116.1,39.7,116.7,40.1")));\n} finally {\n  GirDynamicStackDataSource.poll();\n}`
      },
      {
        title: '动态 SQL 片段查询',
        description: '适合条件项很多、是否拼接由入参决定的 GIS 检索接口。',
        code: `String sql = "" +\n  "SELECT id, name, geom FROM project_layer " +\n  "<where>" +\n  "  <if test='status != null'> AND status = #{status} </if>" +\n  "  <if test='keyword != null and keyword != \"\"'> AND name like concat('%', #{keyword}, '%') </if>" +\n  "</where>";\n\nList<Map<String, Object>> rows = executor.dynamicSelect(sql, params);`
      }
    ],
    related: ['geo-tools', 'dynamic-ds', 'db-service']
  },
  {
    slug: 'file-tran',
    route: '/modules/geo/file-tran',
    title: 'geoair-file-tran',
    group: 'geo',
    summary: '空间文件与数据库互转模块，使用 Reader -> Transformer -> Writer 管道处理 GeoJSON、Shapefile、PostGIS 等格式。',
    tags: ['文件转换', 'Shapefile', 'GeoJSON'],
    capabilities: [
      '支持 GeoJSON、Shapefile、PostGIS、GeoPackage、CSV、FlatGeobuf 等格式。',
      '适合批处理导入导出与空间数据迁移。',
      '支持进度监听、消费者回调与异常处理。'
    ],
    quickStart: 'GeoFileReader -> GeoFileTran -> GeoFileWriter',
    example: '适合把历史 Shapefile 数据批量迁移到 PostGIS，或导出成 GeoJSON。',
    related: ['geo-tools', 'adv-query']
  },
  {
    slug: 'mvt',
    route: '/modules/geo/mvt',
    title: 'geoair-mvt',
    group: 'geo',
    summary: '矢量瓦片能力集合，包含实时 MVT 服务、离线 Spark 生成与工具库。',
    tags: ['MVT', '矢量瓦片', 'Spark'],
    capabilities: [
      '提供实时矢量瓦片服务与离线批处理生成。',
      '支持密度优化、简化、PBF 编码与管道构建。',
      '可与 PostGIS 空间查询和地图前端配合使用。'
    ],
    quickStart: 'geoair-mvt-tools + geoair-real-mvt + geoair-static-mvt-spark',
    example: '适合地图平台的高缩放级别要素渲染与海量图层切片预生成。',
    related: ['geo-tools', 'map-tile-forge', 'map-tile-fuser']
  },
  {
    slug: 'map-tile-forge',
    route: '/modules/geo/map-tile-forge',
    title: 'geoair-map-tile-forge',
    group: 'geo',
    summary: '统一栅格瓦片服务层，支持多种存储格式、存储后端与压缩方式。',
    tags: ['瓦片服务', 'S3', '栅格地图'],
    capabilities: [
      '支持 XYZ、ArcGIS Compact、3D Terrain 与 Cesium 3D Tiles。',
      '可对接本地文件与 AWS S3。',
      '内置缓存层与多种压缩部署模式。'
    ],
    quickStart: '适合提供统一瓦片读取与发布入口。',
    example: '适合把历史离线瓦片和对象存储中的瓦片统一暴露为地图服务。',
    related: ['mvt', 'map-tile-fuser', 'by-gwc']
  },
  {
    slug: 'map-tile-fuser',
    route: '/modules/geo/map-tile-fuser',
    title: 'geoair-map-tile-fuser',
    group: 'geo',
    summary: '多源瓦片融合模块，负责拼接连续栅格图像并做缓存预热与完整性校验。',
    tags: ['瓦片融合', '缓存', '影像拼接'],
    capabilities: [
      '支持多数据源并行拉取与边缘裁剪。',
      '根据金字塔层级自适应选择分辨率。',
      '可对接本地文件、网络源和 MBTiles。'
    ],
    quickStart: '适合需要把多个瓦片源拼成统一底图服务的场景。',
    example: '在多部门地图服务并存的情况下，可以用它统一输出连续图层。',
    related: ['map-tile-forge', 'mvt']
  },
  {
    slug: 'geoserver',
    route: '/modules/geo/geoserver',
    title: 'geoair-geoserver',
    group: 'geo',
    summary: '以嵌入式方式运行 GeoServer，并支持数据源、工作区与图层的编程式发布。',
    tags: ['GeoServer', 'OGC', 'WMS/WFS'],
    capabilities: [
      '无需独立容器即可在应用中集成 GeoServer。',
      '支持图层、工作区和 PostGIS 图层自动发布。',
      '适合构建 WMS、WFS 等 OGC 服务。'
    ],
    quickStart: '适合需要将 OGC 能力嵌入现有 Spring 服务的 GIS 系统。',
    example: '当应用既要做业务逻辑又要提供标准地图服务时，这个模块能减少部署复杂度。',
    related: ['by-gwc', 'geo-tools']
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
    related: ['map-tile-forge', 'geoserver']
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
    summary: '运行时动态多数据源模块，支持 AOP 切换、读写分离、连接池适配与事务管理。',
    tags: ['动态数据源', 'AOP', '读写分离'],
    capabilities: [
      '支持 Druid、Hikari、BoneCP、C3P0、DBCP2 等连接池。',
      '通过注解和 ThreadLocal 路由动态切换数据源。',
      '支持 SQL 解析下的读写分离与编程式事务。'
    ],
    quickStart: '@EnableDynamicDs',
    example: '适合一套系统同时连接多个业务库、租户库或 GIS 数据源的场景。',
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
