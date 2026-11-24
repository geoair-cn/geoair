# 任务编码与预缓存图像处理组合

本扩展把 TileFuser 的任务级编码和“预缓存并处理图片”流程提取为 GeoAir J8 的显式能力。旧入口继续只下载缺片，旧 `CachedTileImageProcessor.process` 继续只处理已有缓存并输出 PNG；工厂不会自动开启编码或图像规则。

## 1. 提交时确定编码快照

```java
TileEncodingSnapshot snapshot = new TileEncodingSnapshot(
        layerOriginalEncoding, layerOutputEncoding);
snapshot = snapshot.override(TileEncodingTarget.ORIGINAL,
        new TileEncodingPolicy("JPEG", 75));
```

构造和覆盖均复制、校验参数，返回的策略也为副本。`ORIGINAL` 表示源网格缓存，`OUTPUT` 表示融合转换缓存；二者不是图片格式，也不决定输出坐标系。覆盖其中一项不改变另一项或图层默认对象。模式沿用 LEGACY / SOURCE / PNG / JPEG，质量为 1～100；有透明像素时保留 PNG。

任务表保存最终两项策略和数据修订号，重试/恢复读取原快照，不重新套用修改后的图层配置。库不保存业务任务、权限或全局配置。

```java
TileCache cache = new EncodedTileCache(rawCache, layerDefaultPolicyResolver);
try (TileEncodingScope scope = snapshot.bindings(originalCacheName, outputCacheName).bind()) {
    // 在此同步执行分段预缓存。两个缓存名须显式传入，不通过名字后缀猜测策略。
}
```

任务绑定优先于装饰器的图层默认策略，未绑定的缓存保持原策略。`SegmentedTilePrecache` 在协调线程捕获一次绑定并显式传给每个分段，包括融合读取中隐式写入的原始缓存。调用方自建更深层线程时还需使用 `TileEncodingContext.capture().wrap(work)` 传递；不能依赖线程池继承 ThreadLocal。作用域只能由创建线程按后进先出顺序关闭，异常退出恢复原绑定。

原始 `TileCache` 不自动转码；需要任务编码的源/输出链须显式使用 `EncodedTileCache`。已有瓦片不在普通预缓存中重新编码，更改 JPEG 质量不会批量改写历史数据。

## 2. 组合下载与处理

```java
TilePrecachePipeline pipeline = new TilePrecachePipeline(
        resultCache, resultCacheName, processor,
        (z, x, y) -> TileImageProcessingContext.ORIGINAL,
        new TileEncodingPolicy("JPEG", 80),
        tileResult -> recordTileSummary(tileResult));

try (TileEncodingScope scope = snapshot.bindings(originalCacheName, outputCacheName).bind()) {
    List<TilePrecacheProgress> result = SegmentedTilePrecache.execute(
            selectedCacheName, levels, sourceFormat, cache, loader, pipeline,
            workers, cancellation, options, progress -> recordLevelProgress(progress));
}
```

流水线逐张执行：

1. 分块筛选范围、批量查询存在性。
2. 命中则读已有图片；索引快照命中但内容已过期/删除时补下载。
3. 未命中则加载并确认源缓存写入；写入失败不发布处理结果。
4. 对新下载图片使用本张尚未缓存编码的字节处理，避免 JPEG 降质先影响检测；已有图片使用当前真实存储字节。
5. 仅 PROCESSED 结果按独立结果编码保存。UNCHANGED / REVIEW / SKIPPED / FAILED 不复制源瓦片。

压缩前字节仅在当前瓦片调用栈内传递，不进入长期任务上下文，不保存全部图片。并发内存仍取决于调用方工作池和单张限制。加载器若已在内部压缩，库无法还原其压缩前内容。已有有损 JPEG 的检测效果也不能等同于未压缩样本。

原始网格使用 `OriginalGridPrecache.execute(region4326, min, max, pipeline, workers, cancellation, options, listener)`；缓存名通过 `getCacheName()` 获得，继续使用真实源矩阵和 Getter 行号契约。融合转换可复用 `TileContentLoaders.fused`；处理缩放必须由调用方依据同一张输出的真实 `TileRenderPlan` 计算：

```java
TileProcessingContextResolver scales = (z, x, y) ->
        TileImageProcessingContext.from(resolveActualRenderPlan(z, x, y));
```

不能把转换网格一律当成单位比例。库只提供接口，具体规则可插拔；Google 模板须显式传入，默认不运行。共享的规则、缩放解析器与逐片回调应支持工作线程并发。

## 3. 结果缓存独立编码

结果编码是第三项显式策略，与源网格/融合缓存编码分开。调用方想沿用源策略时可取 `snapshot.policy(target)` 传入；默认兼容旧 PNG 行为时传入 PNG 策略。

新入口 `CachedTileImageProcessor.processEncoded(..., encoding)` 返回 `TileImageProcessingOutcome`，仍然只读已有缓存，不下载。处理器返回的图片经过尺寸/字节限制检查，结果按真实 PNG/JPEG 格式落盘。FILE 使用真实扩展名，MBTiles 同坐标保存实际 BLOB；不改源文件名、路径或数据。库内嵌套 `EncodedTileCache` 会被显式结果写入绕过，避免重复有损编码；自定义驱动须保证 `put` 不自行再次转码。

结果图层必须与源缓存名称不同，名称碰撞在派发工作之前拒绝。调用方还须保证实际物理文件没有被自定义映射到同一个文件；库不能识别任意外部驱动的别名规则。处理输出是稀疏结果层，不能当作包含全部源图片的完整副本。

## 4. 进度、失败与停止

`TilePrecacheProgress.counts` 每个候选最多计一次，百分比仍以初始化行列乘积为分母。`stages` 额外报告源缓存读取/下载、图像处理状态、结果写入状态，阶段之间有重叠，不能相加作为总处理数。

- `stages.loads`：保留实际命中、下载、范围跳过、缺片和源读取/写入失败。
- `stages.processing`：已经返回处理结果的累计，包含处理失败与待复核；超时或取消前尚未返回结果的调用不冒充完成检查。
- `stages.writes`：区分 NOT_REQUIRED / STORED / ENCODING_FAILED / FAILED，`getStored()` 仅为已确认提交的结果数。

同一张下载成功但结果写入失败时，下载事实为 1、整体失败为 1、结果保存为 0，不重复累加整体进度。收到写入成功后才观察到取消时保留已提交数量。100% 表示候选处理结束，仍需检查失败、缺失、待复核等结果，不能据此宣称全部成功。

单张下载、处理、编码或写入失败记入该张结果后继续其他图片；单张超时沿用分段执行器的时间预算，不中断全部任务。停止会检查当前上下文和中断标志，并等已派发工作真实退出；不承诺强制终止任意不响应取消的第三方算法。处理回调/进度持久化异常属于接入故障，保留原因并向协调方传播，不能吞成普通坏图。

逐片回调含结果/异常及可能的图片字节；接入方只保存摘要、有限样本和必要原因，不把所有回调对象积存在内存。重试、已处理版本去重、恢复时数据修订校验、任务终态由接入方管理。

## 5. 并发维护边界

流水线使用调用方线程池，无业务任务数或级别数限制。协调线程不能占用同一工作池。MBTiles 普通操作沿用现有物理文件协调；需要让独占维护等待整项业务任务时，由接入方对源/结果文件统一调用 `MbtilesTaskReservation.acquire`，等全部工作退出后关闭租约，不能持有任务租约再同步申请同文件独占维护。

## 6. 验证范围

迁移测试使用本地 HTTP 图片服务、真实 FILE/SQLite 缓存及已有水印样本，覆盖任务覆盖/防串扰/嵌套恢复、隐式源缓存写入、压缩前传递、命中继续处理、原始与转换入口、3857/4490 源与两种行原点、结果独立编码和透明保护、坏图/写入失败、单张超时继续、取消前后提交事实和回调故障传播。

不涉及 TileFuser 的任务表或页面迁移，不替换其已发布的 J8.1.6-R2 依赖，也不发布开发版库。临时小数据测试不代表客户数百 GB 文件的性能基准。

2026-11-15 使用 JDK 8u144 完成 fuser 模块回归：33 个测试类、143 项测试，失败、错误和跳过均为 0；本次迁移与补充 4 个测试类、23 项测试。
