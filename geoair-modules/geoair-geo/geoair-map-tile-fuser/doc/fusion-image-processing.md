# 融合完整性与瓦片图像处理

本说明对应 GeoAir 开发源码，未修改地图服务平台的发布版依赖。所有新增接口使用 Java 8；没有默认启用水印处理，没有引入管理端任务表、登录或 Spring 配置。

## 融合结果与缓存

`FuserExec.toImageBytes()` 继续可用。新增默认方法 `toImageResult()`，旧实现无需修改即可继续使用；旧实现只有字节结果，默认按完整结果解释。新融合器提供真实源读取计数：

| 状态 | 含义 | 是否进入转换缓存 |
|---|---|---|
| COMPLETE | 全部所需源片成功，或部分位置已明确为真实无覆盖 | 是 |
| PARTIAL | 有可用影像，但还有未读到或读取/解码失败的源片 | 否 |
| NO_COVERAGE | 明确没有覆盖，没有可发布的新影像 | 否 |
| FAILED | 全部源片不可用，或融合/编码失败 | 否 |

`CacheTileFuserExec` 已使用这个结果。不能再把临时源故障生成的透明画布缓存为成功结果。

显式允许部分出图：

```java
GirFuserExec fusion = new GirFuserExec(
        getter, ImageMime.png, bounds, 256, 256,
        MissingTileMeaning.UNAVAILABLE, TileReadFailurePolicy.KEEP_AVAILABLE);
TileFusionResult result = fusion.toImageResult();
```

- 网络或文件读取器返回 null 含义不清楚时使用 UNAVAILABLE。
- 只有驱动契约保证“null 就是不存在的记录，读取异常一定抛出”时，才能使用 NO_COVERAGE。
- 原有构造方法默认 FAIL_FAST，保留旧字节入口的异常传播方式；结果入口将普通异常记录为 FAILED。
- 主动取消和总截止时间到达始终向上传播，不被部分出图策略吞掉。
- 结果包含可用、无覆盖、缺失、失败计数。图层组可以合并成员结果，但业务图层顺序、上下叠加和左右补齐策略仍由应用管理。
- 融合器实例包含一次渲染的可变状态，不可供多个请求并发复用。画布与源图片在成功、失败时均释放；画布分配前执行图片尺寸限制。

## 同网格直接读取

`SameGridTileReader.read(getter, sourceSrid, outputSrid, z, x, xyzY, missingMeaning)` 保留级别与列号，按真实网格行数将外部 XYZ 行号转换一次，交给内部 TMS Getter。

当前内置网格中，3857 与 900913 等价，4326 与 4490 共用经纬度瓦片矩阵。不同坐标系不能直读，仍需融合转换。坐标系、级别、覆盖范围在读取前校验；该规则不能代替自定义网格的原点、分辨率和瓦片尺寸检查。调用者必须先确认使用本库内置矩阵。

原有 `TileServiceTran` 的同网格入口已复用该校验，继续保留其请求范围和 256 像素瓦片限制，不改变公开地址。

## 统一融合计划

`TileRenderPlan.calculate(grid, bounds, width, height)` 或 `fusion.getRenderPlan()` 无需读取源瓦片即可取得：

- 选用的源级别、分辨率和瓦片范围；
- 按实际渲染规则取整的画布宽高；
- 横纵方向各自的缩放比例。

融合器实际渲染使用同一个计算入口，保留原有 0.5% 分辨率余量。转换缓存图像规则使用 `TileImageProcessingContext.from(plan)`，原始缓存使用 `TileImageProcessingContext.ORIGINAL`，不再复制算法或仅凭级别数字猜比例。

## 图像处理与独立缓存输出

`TileImageProcessor` 是纯处理接口。实现返回带规则版本的 PROCESSED、UNCHANGED、REVIEW、SKIPPED 或 FAILED 结果；只有 PROCESSED 可带新图。调用方负责逐瓦片失败记录、进度、任务停止与重试。

`CachedTileImageProcessor.process(...)`：

1. 只读取指定源缓存，不回源下载，也不在原始和转换缓存之间隐式回退。
2. 缓存缺失返回 SKIPPED，未命中和待复核不写副本。
3. 处理成功后统一 PNG，写入独立输出图层；写入失败返回 FAILED。
4. 输入输出使用同一缓存坐标契约。外部 XYZ、内部 TMS 的转换由调用方在进入缓存接口前完成，处理器不再翻转行号。
5. 输出名称限定英文字母、数字、下划线和连字符，并拒绝与源名称经 MBTiles 文件名净化或大小写折叠后的冲突。调用方仍须保证自定义驱动不会将不同逻辑名称映射至同一物理位置。
6. 中断和绑定的 `TileOperationContext` 在读取和写入前检查；规则自身也必须响应取消。已完成的副本由业务任务决定保留或清理。

`TileCache.prepareLayer(name)` 可在任务开始前显式准备输出存储。FILE 创建图层目录，MBTiles 使用正常写入协调器和建库逻辑；重复调用不会清空数据。其他驱动默认返回 false，表示不支持显式准备。首次 put 仍能自动建库，无需应用先创建空文件，逐片处理不会额外取得一次准备锁。

`TileImageFormats` 和 `PngLayerTileGetter` 按实际字节识别 PNG/JPEG 等格式，复用已有受限解码和编码，不根据 URL 扩展名判断，也不回写原缓存。

## 可选 Google 模板规则

`image.watermark.GoogleWatermarkProcessor` 从地图服务平台迁入，只有主动实例化并调用才运行：

- 当前规则版本 google-2026-v2，模板资源独立放在 tile-image-rules/google 下；
- 只覆盖当前模板与 256×256 图片，不承诺任意水印、任意背景都可识别；
- 高置信度局部修补，不确定结果 REVIEW，原图保持不变；
- 转换缓存按真实融合计划缩放模板；缩放超出支持范围时转人工复核；
- 局部插值是近似修补，不等于恢复原始像素。

## 测试对应关系

新增测试使用实际编码、融合和缓存读写，均无需外网：

| 测试 | 覆盖 |
|---|---|
| TileFusionResultTest | 部分缺片、坏片、真实无覆盖、取消及缓存结果 |
| TileRenderPlanTest | 实际渲染与计划一致、横纵缩放、非法范围 |
| SameGridTileReaderTest | 4326/4490、3857/900913、行号与边界校验 |
| CachedTileImageProcessorTest | FILE/MBTiles 副本、源缓存不变、缺失不下载、写入失败与停止 |
| TileImageFormatsTest | 声明 PNG 实际 JPEG、PNG 保真、资源上限 |
| GoogleWatermarkProcessorTest | 留出样本、区域外像素不变、阴性和不确定样本 |
| GoogleWatermarkScalingTest | 两种方向实际转换缓存的模板缩放与保守复核 |

大体积真实数据、现场服务与生产部署需要应用侧单独验收。本次没有替换 TileFuser J8.1.6-R2 依赖、没有安装或发布新版本。
