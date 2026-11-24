# 栅格缓存编码、混合格式和 MBTiles 维护

## 接入范围

此扩展位于 GeoAir J8 的 `geoair-map-tile-fuser`。不依赖 TileFuser 的图层表、任务表、管理接口、界面、系统设置或调度器。业务方自行管理权限、任务状态、重启恢复、进度保存和范围选择。

新能力显式启用。现有缓存工厂不会自动套编码策略；`TileCache.get` 仍返回存储的真实字节；旧读取器和融合器的默认格式契约不变。文件名和 MBTiles 路径不变，也不建立一份转换后的 MBTiles。完整整理仍需明确发起，不在请求地图时自动运行。

## 下载时选择存储编码

`encoding.TileEncodingMode` 提供以下模式：

| 模式 | 作用 |
| --- | --- |
| LEGACY | 默认；原样调用旧缓存驱动，不增加图片解码 |
| SOURCE | 保留已识别源图片的真实编码和字节 |
| JPEG | 按质量 1～100 编码；含非全不透明像素时保留 PNG |
| PNG | 无损 PNG 编码；已是 PNG 时保留原字节 |

```java
MBTilesTileCache raw = new MBTilesTileCache(cacheRoot);
EncodedTileCache encoded = new EncodedTileCache(raw,
        layer -> new TileEncodingPolicy(TileEncodingMode.JPEG.name(), 75));
encoded.put(layerName, z, x, y, downloadedBytes, ImageMime.png);
```

策略可以由接入方按图层查询。MBTiles 按坐标保存实际图片 BLOB，原有 PNG/JPEG 记录可与新记录共存。FILE 缓存按真实格式落扩展名；同列锁内完成写入和旧扩展名清理，避免并发 PNG/JPEG 写入互相删除。装饰器的读取、删除、存在性和批量存在性查询识别这两种扩展名。

逐片读取按图片内容判断格式，不仅看 URL、图层声明或 `metadata.format`。转码引擎写入私有标记 `geoair.encoding=per-tile`，保留历史 `format`；普通装饰器不会自动修改元数据。外部只相信单一 `metadata.format` 的工具不保证兼容混合库，不能把混合库宣称为全部 JPEG。

## 响应和融合的格式适配

对外响应需保证声明与字节一致。需要固定格式的调用方使用以下显式入口：

```java
byte[] png = encoded.getNormalized(layerName, z, x, y, ImageMime.png);
LayerTileGetter pngSource = new FormattedLayerTileGetter(source, ImageMime.png);
```

原 `get` 不会为了声明格式再次有损编码。`FormattedLayerTileGetter` 保留源矩阵和行号契约，供需要统一图片格式的融合链使用。透明 PNG 请求 JPEG 时拒绝输出，防止默默丢掉透明边界；融合建议统一到 PNG 后再叠加，最终存储时应用编码策略。

编码和格式识别复用统一的单张字节、图片尺寸和像素限制；MBTiles 缓存读取在 SQL 内限制超大 BLOB，避免先把异常大瓦片搬入 Java 堆再检查。

预检查、图片处理及水印扫描仍应读取真实图片内容。有损 JPEG 可能影响细节及模板检测置信度，需重新选取样本和阈值；内容能解码不等于检测效果完全相同。

## 物理文件协调和取消

`mbtiles.MbtilesAccessCoordinator` 按真实文件路径协调：

- `operation`：一次普通读写；独占维护期间快速抛出 `MbtilesMaintenanceException`，调用方可映射为“维护中，请稍后重试”。
- `task`：登记一项长任务。维护等待既有任务及操作收尾，新任务在维护后开始。
- `maintenance`：独占维护；必须由执行线程关闭租约。
- `backgroundRead`：后台统计按批等待，在批次之间允许维护进入。

```java
try (MbtilesAccessLease task = MbtilesAccessCoordinator.task(file, cancelled)) {
    // 工作线程执行预缓存；此处必须等所有工作线程退出后再归还任务租约。
}
```

多文件任务使用 `MbtilesTaskReservation.acquire(files, cancelled)`，避免拿着部分文件的任务租约等待另一部分文件。不能在仍持有任务租约时同步申请同一文件的独占维护；应先收尾任务，再安排维护。

源读取器、MBTiles 缓存读写、统计和新维护 DAO 已接入此协调器。低优先级转码提交、增量统计/回收复用 `MbtilesWriteCoordinator`，有瓦片写入等待时让其先落盘。仅同一 JVM 内通过这些入口访问的调用受到协调；直接 JDBC、其他进程和外部 SQLite 工具仍由数据库锁处理，不会被此库强制停止。不要手动删除 WAL/SHM 文件。

取消由 `BooleanSupplier`、线程中断和当前 `TileOperationContext` 共同检查；SQLite 长操作安装 progress handler。取消和超时会保留最后一次已提交结果，不承诺底层磁盘 I/O 的硬实时中断。

## 文件信息、WAL 合并和空间回收

```java
Path file = raw.layerFile(layerName);
MbtilesMaintenance maintenance = new MbtilesMaintenance();
MbtilesFileInfo info = maintenance.inspect(file);
MbtilesMaintenanceResult checkpoint = maintenance.checkpoint(
        file, MbtilesCheckpointMode.PASSIVE, cancelled);
MbtilesMaintenanceResult reclaimed = maintenance.reclaim(
        file, MbtilesReclaimMode.AUTO, cancelled, listener);
```

`inspect` 通过只读连接读取页信息、文件/WAL/SHM 占用、空闲页、磁盘空间和回收能力，不扫描全部瓦片。WAL 合并读取 SQLite 返回的 busy、日志页和已合并页；有读事务阻挡时返回 `PARTIAL`，不能显示为全部成功。`TRUNCATE` 走独占维护，`PASSIVE` 允许普通并发访问。非 WAL 数据库按 SQLite 的返回值解释，不伪造日志页数。

回收方式：

| 方式 | 行为 |
| --- | --- |
| AUTO | 库已启用 `auto_vacuum=INCREMENTAL` 时分批回收，否则显式完整整理 |
| INCREMENTAL | 仅已有增量回收能力的库；每批最多 512 页并在批间让出，普通库拒绝执行 |
| VACUUM | 独占等待，重新检查空间，先确认 WAL 合并完整，再完整重写整理并检查后续 WAL |

完整整理要求可用空间至少主文件两倍加 64 MiB，属于保守检查，并非磁盘空间预留保证。此接口不会自动改变 `auto_vacuum` 模式。增量回收不等于瓦片重编码，也不能回收未删除或重写记录中的有效内容。

旧 `MbtilesUtils.vacuum(String)` / `compactDatabase(String)` 保持布尔签名，转入上述受保护入口。旧 `walCheckpoint` 读取实际合并状态，部分合并返回 false，不再以“SQL 执行过”代表完整完成。直接 DataSource 工具由接入方保证任务协调，新业务优先使用类型化门面。

## 原文件可恢复转码

```java
MbtilesTranscodeOptions options = new MbtilesTranscodeOptions();
options.setCheckpointId("cache_job_123");
options.setExpectedFileIdentity(MbtilesInPlaceTranscoder.fileIdentity(file));
options.setEncoding(new TileEncodingPolicy(TileEncodingMode.JPEG.name(), 75));
options.setSourceFormat(MbtilesSourceFormat.PNG);
options.setMinZoom(0);
options.setMaxZoom(18);
options.setOnlyIfSmaller(true);
MbtilesTranscodeProgress result = new MbtilesInPlaceTranscoder().execute(
        file, options, null, cancelled, progressListener);
```

要求可写的标准 `tiles` 表、`metadata(name,value)` 和完整的唯一 `(zoom_level,tile_column,tile_row)` 索引；视图或缺索引库拒绝转码，避免大库排序扫描。按坐标索引每批读取最多 128 个键，先记录截止坐标，不预先全表 COUNT，不依赖 rowid，也不把全部瓦片放入内存。

逐片读取、编码后以旧 BLOB 为条件更新；并发改变过的瓦片计入冲突并保留其新值。瓦片改写与恢复点保存在同库同事务，取消或异常回滚两者。恢复点默认以 `geoair.transcode.` 为前缀，构造 DAO 时可指定接入方命名空间。

任务恢复必须传入相同 checkpointId、编码参数、级别和范围版本；文件身份或参数变化时拒绝沿用。建议业务提交时保存 `expectedFileIdentity`，重启后使用原值，不重新覆盖它。该身份基于真实路径、文件键和创建时间，不能发现“同一文件对象被原地整体改写”的全部情况；业务数据修订号及授权仍由接入方检查。

自定义范围由 `Predicate<MbtilesTileKey>` 判断，必须设置稳定的 `scopeId`，例如包含范围内容哈希和算法版本。`MbtilesTileKey.row` 是库内行号；范围映射必须结合真实源矩阵和 XYZ/TMS 存储方向，库不猜测行政区坐标。

进度报告实际处理、已转换、跳过、透明保护、无收益、并发冲突、失败、输入输出字节和各级累计。处理期间总数为 null，不冒充准确百分比；结束后确定本次实际处理数。回调收到独立快照，可节流写回业务任务表。`complete=true` 仅说明遍历结束，还需检查 failed/conflicts。

目标格式已匹配的记录会跳过，不反复有损编码 JPEG；质量调整不强制重压已有 JPEG。单张坏图记失败并保留原内容，其余瓦片继续；数据库/范围计算故障中断本次执行，保留恢复点。要重新处理已计失败的记录，应新建 checkpointId。新插入记录中位于已提交游标之前或初始截止坐标之后的记录不属于可恢复遍历保证，后续任务再处理。

## 验证边界

测试迁移了编码质量/透明保护、混合缓存及扩展名并发、物理文件租约、真实 SQLite WAL 部分合并/截断、分批回收、完整整理、磁盘不足拒绝、坐标游标、取消回滚、CAS 冲突和跨实例恢复。既有只读/增量统计与预缓存并发测试也纳入回归。

测试使用临时 SQLite 和真实图片，并非客户 500～800 GB 文件的吞吐或停机时间基准。该扩展没有发布新版本，也没有替换 TileFuser 已发布依赖。

2026-11-15 使用 JDK 8u144 完成模块回归：29 个测试类、120 项测试，失败/错误/跳过均为 0，其中本次迁移及补充 22 项。HTTP 重定向测试发现 Windows/JDK8 本地 HttpServer 的空响应连接复用会被重置；测试服务显式声明 `Connection: close` 后通过，生产 HTTP 请求逻辑未修改。
