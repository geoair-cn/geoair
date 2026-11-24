# 图层上游鉴权与可控预缓存

本次能力在 GeoAir `J8-dev-SNAPSHOT` 源码中实现。TileFuser 继续引用已发布的
`J8.1.6-R2`，没有替换依赖、发布 Maven 包或修改线上部署。

## 每个图层独立配置上游访问参数

`PxyLayerInfo.upstreamRequest` 保存固定参数，两种网络 Getter
（`GoogleWebTileGetter`、`Grid4490WebTileGetter`）均使用该配置。
原有图层的代理开关、代理主机、端口仍由 `PxyLayerInfo` 的代理配置控制。

配置示例（业务方保存后构建 PxyLayerInfo，不是 Spring 自动读取的全局配置）：

```json
{
  "layerName": "partner_image",
  "path": "https://tiles.example.com/{z}/{x}/{y}.png",
  "upstreamRequest": {
    "referer": "https://maps.example.com/viewer/index.html",
    "origin": "https://maps.example.com",
    "headers": {
      "Authorization": "Bearer <token>",
      "X-API-Key": "<key>"
    },
    "queryParameters": {
      "token": "<query-token>"
    },
    "followRedirects": true,
    "maxAttempts": 3
  }
}
```

- Referer 可以包含页面路径。Origin 仅允许 HTTP/HTTPS 协议、域名和端口。
- 请求头名不区分大小写。合并顺序是默认头 → Referer/Origin → headers → 动态策略。
- 查询参数会正常 URL 编码；同名参数替换 URL 中的旧值，不重复追加。
- Bearer、Basic、Cookie、API Key 均可通过固定请求头提供；不自动继承浏览器请求头。
- 每次尝试创建独立请求对象。共享连接池不保存或自动转发上游 Set-Cookie。
- 跟随重定向时仅允许同来源，防止凭证自动转交其他站点；跨来源服务配置最终地址。
- 禁止换行注入以及 Host、Content-Length、Connection、Transfer-Encoding 等连接控制头。
- 固定凭证配置和动态策略从 PxyLayerInfo 的 toString 中排除，HTTP 工具日志隐藏路径和查询参数。
  业务管理接口仍需使用自己的 VO 白名单，不能直接把 PxyLayerInfo 返回到公开接口。

## 动态 Token、签名和特殊鉴权

通过 `PxyLayerInfo.setUpstreamRequestCustomizer(...)` 在服务端注册策略。
策略不参与 JSON 序列化/反序列化，不能由页面提交任意可执行代码。

```java
layer.setUpstreamRequestCustomizer(request -> {
    // credentialService 为业务系统已有的凭证服务，不是库内置实现。
    // 可按图层、z/x/y、URL、查询参数计算签名。
    boolean refresh = request.getPreviousStatus() == 401
            || request.getPreviousStatus() == 403;
    String token = credentialService.getToken(request.getLayerName(), refresh);
    request.getHeaders().put("Authorization", "Bearer " + token);
});
```

回调提供图层名、展开后的 URL、z/x/y、当前尝试次数、上次 HTTP 状态；
其中 Y 已转换为上游实际行号。允许修改本次 URL、请求头和查询参数。
签名应使用最终查询参数和请求头，不能只对未合并的原始模板签名。

每次尝试都会重新调用策略。401/403 在第一次失败后允许带动态策略重试一次，
仍受 maxAttempts 限制；固定参数不会反复重试鉴权失败。
429、服务器错误和连接失败按最大尝试次数重试，共享一个总时间预算。
404/204 表示源瓦片缺失，返回 null；其他耗尽重试的失败抛出带状态的
TileOperationException，不再把鉴权错误当作透明瓦片成功返回。

动态凭证服务的共享缓存、刷新锁和凭证生命周期由业务方负责。
回调中的外部 I/O 应读取 `TileOperationContext.current()` 的剩余时间和取消令牌，
不能另开一个无限等待的鉴权请求。修改图层配置时应整体替换配置对象；
不要在正在处理请求时原地修改同一个 headers/queryParameters Map。

## 可复用的预缓存能力

| 能力 | 入口 |
|---|---|
| 完整块快速判定、边界二分、孔洞和多面 | TileRegionMask |
| 文件目录扫描、MBTiles 行范围批量查询 | TileCache.findExisting |
| 总时间预算、在途 HTTP/SQL 中止、取消信号 | TileOperationContext / TileCancellationToken |
| 多级别轮流投递、固定数量分段并发 | SegmentedTilePrecache |
| 原始读取、严格融合加载 | TileContentLoaders |
| 各级别进度与分类计数 | TilePrecacheProgress |
| 原始与输出网格分别统计 | TileCacheInspector |

旧预缓存入口保留，新的执行器需要业务方显式接入，不会自动替换原有调度逻辑。

### 执行流程与坐标约定

1. 用各级别外包矩形的闭区间行列计算候选总数，不逐片预扫描。
2. 每段先判断范围，完全在外直接跳过，完全在内直接接受，穿过边界的列块二分。
3. 一段使用 BitSet 保存位置；文件驱动按目录查询，MBTiles 按列范围查询，不读取图片 BLOB。
4. 只下载范围内尚未缓存的瓦片；各级别交替投递，分段可以并发。
5. 已有、新下载、范围外跳过、缺失、失败、超时分别计数，处理数除以候选总数得到进度。

TileRegionMask 显式接收网格转换器与 TileYAxis，范围 Geometry 使用 EPSG:4326。
任务 RangeApo、mask、loader 和 cache 的输入行号必须一致，不能把 XYZ 行号直接交给
要求 TMS 的 Getter。当前内置 LayerTileGetter 使用内部 TMS 行号，再依据图层配置转换
到上游原点。网格不是只用 SRID 就能代替：4326 的不同矩阵需传入对应转换器。

```java
// range 为业务层已按目标矩阵计算的闭区间，region4326 为已验证的面范围。
TileRegionMask mask = new TileRegionMask(region4326, zoom, grid, TileYAxis.TMS);
TilePrecacheLevel level = new TilePrecacheLevel(zoom, range, mask);
TileCancellationToken cancellation = new TileCancellationToken();
TilePrecacheOptions options = new TilePrecacheOptions();
options.setSegmentSize(10000);
options.setTileTimeoutMillis(60000);

// sourceGetter 的行号契约与上述计划一致；原始缓存名由业务方明确指定。
List<TilePrecacheProgress> result = SegmentedTilePrecache.execute(
    originalCacheName, Collections.singletonList(level), format,
    originalCache, TileContentLoaders.original(sourceGetter),
    downloadWorkers, cancellation, options,
    progress -> progressService.record(progress)
);
```

融合输出使用 `TileContentLoaders.fused`，由 TileBoundsResolver 把输出瓦片行列转换为
融合器需要的源坐标系范围。严格模式遇到任何缺失源片会停止本张融合，不保存部分空白结果。
受控融合不再把源读取隐式分派到公共 ForkJoinPool，并发由分段工作池管理。

### 并发、停止及状态落库

- 调用方提供 ExecutorService，库不限定同时运行的任务数或级别数。
  pendingSegments 只是投递窗口（默认 256），用于控制排队内存，线程容量由调用方决定。
- 协调入口必须运行在独立协调线程，不能占用传入的同一个下载工作池。
- 停止调用 cancellation.cancel()，不要直接 shutdownNow 下载池或取消 Future 来冒充任务完成。
  调用方须保持工作池可用，直到 execute 返回。
- HTTP 在途请求会主动 abort，新的批量 SQLite 查询会取消 Statement；
  普通文件 I/O、图片解码及自定义加载器在阶段边界检查取消/超时。
  这不是强杀 Java 线程，无法承诺中断任意卡死的第三方 native I/O。
- execute 等待已提交分段真实退出后返回。取消保留已写入缓存，未处理数量保持为未处理；
  调用方通过令牌区分已取消和正常完成，并检查失败/缺失/超时计数决定业务终态。
- 回调按同一级别的累计快照顺序通知，不同级别可能并发通知。回调必须快速、线程安全。
  回调或基础设施异常会取消剩余工作、等其收尾后抛出异常；不能把抛异常的执行记录为完成。
- 重启恢复、任务 ID、心跳、执行权、持久化状态和重试归业务系统管理。
  库没有内置任务表；恢复任务重新执行计划会通过已有缓存跳过成功写入的瓦片。

### 缓存统计及一致性边界

TileCacheInspector.inspect 按图层名和 ORIGINAL_GRID_SUFFIX 分别选取缓存驱动。
每个网格提供驱动类型、各级别数量、瓦片内容字节数及物理文件字节数。
MBTiles 物理字节数包含数据库、WAL、SHM；FILE 物理字节数包含图层目录下文件。
统计是读取期间的快照，不与正在下载或删除缓存的任务组成事务。

文件批量命中会排除空文件、超过大小限制的文件和过期文件；MBTiles 排除空 BLOB 和超大 BLOB。
批量存在性判断不解码图片，不等同于图片完整性检查。读查询不会创建不存在的 MBTiles 数据库。
自定义旧驱动通过接口默认实现逐片查询；不支持统计时返回 supported=false，不能当成零占用。

## 验证

新增测试使用本地 HTTP 服务、临时 FILE/SQLite 缓存和内存图片，不访问真实业务鉴权服务。
覆盖固定头/查询参数、动态 401 刷新、Cookie 隔离、重定向、请求取消、慢响应总超时、
批量缓存与逐片读取一致性、边界二分与孔洞、多级别并发、工作线程异常/提交拒绝、取消后收尾、
源片缺失不缓存透明图以及受控融合实际图片结果。另保留同网格输出和 Getter 工厂回归。

运行时需要显式 `-DskipTests=false`，根 POM 默认跳过测试。

## 大容量缓存与原始网格能力（2026-11-07）

以下能力位于 `J8-dev-SNAPSHOT` 源码。当前应用仍使用已发布的 `J8.1.6-R2`，接入方升级库时再选择这些入口。

### 大容量 MBTiles 源读取

普通 `MBTilesTileGetter` 共享只读读取器，构造 Getter 时不统计整库瓦片数，也不创建源库表。读取器按真实文件路径复用连接池，默认最多 8 个文件、每文件 2 条连接、闲置 60 秒回收。每连接页缓存默认 2048 KiB，关闭 mmap；查询默认 3 秒，连接等待默认 2 秒。SQL 先检查图片大小，再把允许的 BLOB 复制到 Java，超限或读库错误向上传递。兼容 `tiles` 视图、中文及带空格的 Windows 路径。

需要调整资源限制时创建共享读取器并注入 Getter，读取器的生命周期由调用方管理。

```java
MbtilesReadOptions readOptions = new MbtilesReadOptions();
readOptions.setMaxOpenFiles(8);
readOptions.setConnectionsPerFile(2);
readOptions.setQueryTimeoutMillis(3000);
try (MbtilesSourceReader reader = new MbtilesSourceReader(readOptions);
     MBTilesTileGetter getter = new MBTilesTileGetter(layerInfo, reader)) {
    Resource tile = getter.getTileResource(z, x, tmsRow);
}
```

同名源文件被替换后会校验文件身份，已有读取完成后才能切换连接池。源文件池满且全部使用中时明确失败，由调用方重试。关闭 Getter 只关闭该 Getter；共享读取器需由其拥有者关闭。

### 统计与瓦片写入协调

`MBTilesTileCache` 的写入和删除按物理文件与 `MbtilesIncrementalStatistics` 共用协调器。等待中的瓦片写入先执行；增量扫描默认每批最多 512 条，时间片目标 25 毫秒，批次之间让出 25 毫秒。出现写入等待时在行边界提前提交。时间片是让行目标，单次底层 I/O、提交和文件系统停顿不构成硬实时保证。持续写入时统计可能等待。

`MbtilesStatisticsOptions` 可配置增量批大小、只读批大小、时间片、让行间隔和写锁等待。协调范围为同一 JVM 内使用该缓存驱动的调用；外部 SQLite 写入仍依靠数据库事务和锁处理。

普通 `statistics` 对普通 rowid 表按批聚合，释放每批读事务，并在并发修改后拒绝发布混合结果。旧视图和 WITHOUT ROWID 表保留兼容聚合方式。需要显示进度时使用独立引擎或缓存驱动入口。

```java
MbtilesScanProgress readOnly = cache.scanStatistics(layerName, sidecarPath,
        progress -> recordProgress(progress));
```

只读扫描始终以 `mode=ro` 打开源库，侧车保存进度；sidecar 为 null 时仅保存内存汇总。即使源库已经安装增量索引，只读请求也不会恢复或推进增量索引。坐标索引可用时先报告各级数量和范围，精确字节数仍需要扫描记录。STALE 或 INTERRUPTED 不能作为当前完整结果。

```java
MbtilesStatisticsOptions statisticsOptions = new MbtilesStatisticsOptions();
statisticsOptions.setBatchSize(512);
MbtilesIncrementalStatistics incremental = new MbtilesIncrementalStatistics(statisticsOptions);
TransactionalStatsSnapshot result = incremental.scan(mbtilesPath,
        progress -> recordIncrementalProgress(progress));
```

增量统计必须显式调用，安装前校验源库。支持普通 rowid 表、标准四列和唯一坐标主键；不兼容的触发器、外键或结构会拒绝安装。初始扫描、游标和汇总在同一事务提交；后续增删改由源库触发器维护。`resume`、`read`、`uninstall` 和 `recalculateBounds` 分别用于继续扫描、读取结果、显式关闭和重新确认边界。边界重算遇到瓦片写入等待会回滚让行，可稍后重试。库不会自行查找文件或恢复业务任务。

只读进度的数量表示物理记录数，字节数为瓦片数据长度；普通缓存 `statistics` 仍排除空瓦片。WAL、SHM 和空闲数据库页属于物理空间开销。

### 原始网格预缓存

`OriginalGridPrecache` 使用源读取器真实矩阵生成范围和级别计划，输入范围为 EPSG:4326，级别按源网格解释。墨卡托范围会裁剪有效纬度后投影；边界筛选使用读取器的实际瓦片边界，保留孔洞和触边语义。行号遵守 Getter 的 TMS 契约，由源读取器与缓存驱动各自处理存储方向。

```java
OriginalGridPrecache original = new OriginalGridPrecache(layerInfo, cache);
List<TilePrecacheProgress> levels = original.execute(region4326, 10, 16,
        downloadWorkers, cancellation, options, progress -> recordProgress(progress));
```

只写 `<图层名>_original_grid`，使用源格式，不做融合输出。已有、下载、跳过、缺失、失败和超时沿用分段执行器的真实结果；确认写入成功后才计为下载。也可传入现成 Getter 和缓存实例，复用业务已创建的源读取器。接入方维护任务表、身份和权限，可选用下文的公平调度和断点恢复。

### 文件缓存并发

同一缓存根目录下的 `FileTileCache` 实例共用有限数量的分段锁，协调同一列的读取、写入、存在性检查和单张删除，避免读取原地覆盖过程中的半张图片。磁盘布局和过期规则沿用现有实现，锁只在当前 JVM 内生效。

### 本次回归范围

迁移并补充了真实 ZIP 4501 条批次提交、失败回滚、重试去重、同归档重复提交、3D Tiles/S3M 相对资源读取、中文路径、读取器并发和超大 BLOB 拒绝、事务增量冲突与进程异常退出、只读统计失效、写入优先级和取消等待测试。原始网格覆盖墨卡托、经纬度及两种行号原点；并发测试使用真实 SQLite 源数据和缓存，分别让只读统计、增量统计与 1024 张预缓存写入同时执行。验证使用临时数据，不代表客户数百 GB 数据的实际吞吐量。

### 融合完整性与图像处理扩展

新增融合完整性结果、同网格直读工具和统一渲染计划。部分源片不可用时可显式保留可用影像，转换缓存只保存完整结果。图像处理通过可插拔接口读取原始或转换缓存，写入独立结果层，不修改源缓存；PNG/JPEG 按真实内容识别。FILE/MBTiles 可由驱动显式准备输出存储。

接口、示例、兼容边界和测试见 [融合完整性与瓦片图像处理](doc/fusion-image-processing.md)。Google 模板仅为明确调用的可选规则，不是默认处理流程。

### 栅格缓存编码与 MBTiles 维护

新增显式编码策略、混合 PNG/JPEG 缓存适配、原文件可恢复转码、按物理文件协调的独占维护，以及返回真实部分结果的 WAL 合并和空间回收接口。旧工厂不自动启用编码，缓存路径与文件名不变；业务任务与权限由接入方维护。

接入示例、兼容边界和验证说明见 [栅格缓存编码、混合格式和 MBTiles 维护](doc/cache-encoding-maintenance.md)。

### 任务编码与预缓存图像处理组合

新增不可变任务编码快照、显式跨线程绑定，以及下载/读取缓存后继续处理的组合入口。新下载图片在缓存压缩前交给处理器，处理结果单独编码并写入独立结果层；下载、检测和落盘分阶段统计。旧预缓存和只处理已有缓存入口保持原行为。

接入示例、异常/取消和测试边界见 [任务编码与预缓存图像处理组合](doc/precache-image-pipeline.md)。

### 出图、缓存代次与可恢复预缓存

- `TileServiceTran` 在图层禁用缓存时直接访问源。同网格沿用现有行号转换，跨网格按请求范围融合。刷新请求同样绕开缓存。
- `TileComposer` 按输出矩阵 XYZ 请求组合成员图像，接收经纬度面、多面及范围内外规则。只有裁切后的整张图像无可见像素才读父级。调用方用结果状态判断是否允许缓存；无覆盖结果可以按自己的透明瓦片协议写入。
- `SegmentedTilePrecache.executeResumable` 和 `OriginalGridPrecache.executeResumable` 共享调用方提供的 `TilePrecacheBatchGate`。同一工作池使用同一个调度器，并把协调线程放在工作池之外。带 `TilePrecacheCheckpoint` 时，每个完成批次在一笔事务中保存范围、计数和失败坐标。调用方提供稳定的源、目标缓存、矩阵原点和图像处理策略身份，相关配置变化后提交新任务。完整续跑补未完成批次，失败补跑仅选择持久化失败坐标。重放时阶段计数记录实际发生的处理次数。
- `TileRenderQueue` 用接入方提供的键合并在途请求。发布版本、缓存代次和坐标应进入键。独立取消等待不会终止其他调用。`TileCacheGeneration` 原子持久化当前代次，并通过 `writeIfCurrent` 拦住旧请求的写入。接入方管理缓存文件及旧代次清理，单个指针在同一进程内由一个实例维护。
- `TileImageCodec.encodeForResponse` 可指定 JPEG 质量和不透明底色，输入与目标格式一致时直接复用字节。`WmtsTileFormats` 按 MIME 类型生成对应瓦片模板。`MbtilesSourceReader.sampleTile` 返回所选级别内实际存在的一张瓦片坐标，Y 为文件原始行号。
