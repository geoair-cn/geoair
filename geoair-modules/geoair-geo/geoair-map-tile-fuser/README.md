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
