# OOM 问题分析与修复方案

## 一、核心问题：用了流式的写入，但没用流式的映射

### 1.1 MapToTileFunction1（正在使用）vs MapToTileFunction（已写好但未使用）

**当前调用链（SparkVectorTileGenerator.doGenerate 第128行）：**

```java
tileFeatures = transformedFeatures.flatMapToPair(
        new SparkTaskSerializableUtil.MapToTileFunction1(parameter))  // ← 用的是 1！
        .persist(StorageLevel.MEMORY_AND_DISK());
```

**MapToTileFunction1.call() 内部（已标记 @Deprecated）：**

```java
// 全量收集到 HashMap，再转 List 输出
Map<String, List<GirAdvOneRow>> tileMap = new HashMap<>();
// 双重循环遍历所有 zoom * 所有 x * 所有 y，全部收集到 HashMap
tileMap.computeIfAbsent(quadKey, k -> new ArrayList<>()).add(feature);
```

**MapToTileFunction.call() 内部（未标记 @Deprecated，注释写的"无OOM版本"）：**

```java
// 返回 TileIterator，逐个瓦片懒生成，不一次性收集
return VectorTileCommonUtils.mapSingleFeatureToTilesStream(
        feature, geomFieldName, minZoom, maxZoom, outGridSrid);
```

**OOM 放大效应：**

一个几何范围大的要素可能命中几千甚至几万个瓦片，每个瓦片都持有对同一个 `GirAdvOneRow` 的引用。
放大到整表数据量：

```
百万要素 × 每个要素命中 N 个瓦片 × 每个瓦片一个 ArrayList 开销 = 指数级膨胀
```

然后又 `persist(MEMORY_AND_DISK())`，Spill 到磁盘后序列化/反序列化也极其昂贵。

### 1.2 TileIterator 的内存优势

`TileIterator` 实现了 `Iterator<Tuple2<String, List<GirAdvOneRow>>>`，按 zoom/x/y 顺序逐个瓦片返回，
每次 `next()` 只创建一个 `singletonList`，不积累任何中间集合。

```
MapToTileFunction1:  O(要素数 × 瓦片命中数) ← 峰值内存
MapToTileFunction:   O(1) 每次只产出一个瓦片  ← 峰值内存
```

---

## 二、三次 persist 的叠加开销

当前 `doGenerate` 中有三次 `persist(MEMORY_AND_DISK())`：

```java
// 第1次：原始要素
JavaRDD<GirAdvOneRow> persistedFeaturesRDD =
        rawFeatures.persist(StorageLevel.MEMORY_AND_DISK());

// 第2次：转换后要素
JavaRDD<GirAdvOneRow> transformedFeatures =
        persistedFeaturesRDD.map(new TransformFeatureFunction(parameter))
                .persist(StorageLevel.MEMORY_AND_DISK());

// 第3次：膨胀后的瓦片映射（最大的那份）
JavaPairRDD<String, List<GirAdvOneRow>> tileFeatures =
        transformedFeatures.flatMapToPair(new MapToTileFunction1(parameter))
                .persist(StorageLevel.MEMORY_AND_DISK());
```

**总缓存量 = 原始数据 + 转换后数据 + 膨胀后的瓦片映射**

第3次 persist 的数据量远大于前两次，因为一个要素会被复制到多个瓦片的 List 中。

---

## 三、tileFeatures.count() 强制物化

```java
log.info("要素映射到瓦片总条数：{}", tileFeatures.count());
```

`.count()` 是 action 算子，会触发第3次 persist 的全量物化。
此时全部膨胀后的瓦片映射必须同时驻留在内存/磁盘中。

---

## 四、统计路径的额外开销

当 `statisticsIs=true` 时，额外执行：

```java
// 对全量数据再做一次全量瓦片映射（只映射到 maxZoom）
tileFeaturesByZoom = transformedFeatures.flatMapToPair(
        new MapToTileFunctionToStatic(parameter, maxZoom));  // 也是 HashMap 版本

// reduceByKey 聚合
aggregatedRDDByZoom = tileFeaturesByZoom.reduceByKey(...)

// 生成 PBF → takeAsync(500) → 再 parallelize 成 RDD
pbfRDD.takeAsync(500)  // 把 500 个 PBF（含 byte[] 数据）拉到 driver 内存
```

问题：
- `MapToTileFunctionToStatic` 也是 HashMap 全量收集，同样的膨胀问题
- `takeAsync(500)` 把 500 个 PBF（含 byte[] 数据）拉到 driver 内存
- `parallelize(tuple2Seq)` 又转回 RDD，等于同一份数据在 driver 里存了一份

---

## 五、reduceByKey 聚合阶段

```java
aggregatedRDD = tileFeatures.reduceByKey(
        new AggregateAndLimitFeatureFunction(parameter),
        DEFAULT_REDUCE_PARTITION);
```

高密度瓦片（如海洋、沙漠等大面积区域）可能聚集上万个要素到同一个 key 下。
`AggregateAndLimitFeatureFunction` 内部用 `Stream.concat + collect` 合并两个 List，
不会在合并过程中做截断，直到合并完成才调用 `limitTileFeatures`。

---

## 六、修复方案

### 6.1 最高优先级：切换到 MapToTileFunction（流式映射）

```java
// 修改前（OOM 版本）：
tileFeatures = transformedFeatures.flatMapToPair(
        new SparkTaskSerializableUtil.MapToTileFunction1(parameter))
        .persist(StorageLevel.MEMORY_AND_DISK());

// 修改后（流式版本）：
tileFeatures = transformedFeatures.flatMapToPair(
        new SparkTaskSerializableUtil.MapToTileFunction(parameter));
        // 不 persist！流式传递到 reduceByKey
```

**预期效果：** 峰值内存从 O(要素数 × 瓦片命中数) 降到 O(单个瓦片的要素聚合数)

### 6.2 去掉 transform 的 persist

```java
// 修改前：
JavaRDD<GirAdvOneRow> transformedFeatures =
    persistedFeaturesRDD.map(new TransformFeatureFunction(parameter))
        .persist(StorageLevel.MEMORY_AND_DISK());

// 修改后：直接串联，不缓存
JavaRDD<GirAdvOneRow> transformedFeatures =
    persistedFeaturesRDD.map(new TransformFeatureFunction(parameter));
```

**理由：** transform 是一对一映射，不需要回溯，persist 纯浪费内存。

### 6.3 去掉 tileFeatures.count()

```java
// 删除这一行：
log.info("要素映射到瓦片总条数：{}", tileFeatures.count());
```

**理由：** 这个 count 强制物化了全部膨胀后的 tileFeatures，仅用于日志。
改为在写入完成后从 writer 端统计总数。

### 6.4 统计路径改用单 zoom 的 TileIterator

`MapToTileFunctionToStatic` 也应使用 TileIterator 版本（只需支持单 zoom），
避免 HashMap 全量收集。

### 6.5 AggregateAndLimitFeatureFunction 加早截断

在 `Stream.concat` 之前先检查两个 list 的总大小，
如果已经超过 limit，直接对较大的那个 list 做截断，避免合并出超大临时 List。

---

## 七、修复后的预期数据流

```
rawFeatures (persist，仅一次)
  → transform (不 persist，一对一流式)
    → flatMapToPair(MapToTileFunction) (TileIterator 懒生成，不 persist)
      → reduceByKey (聚合，DEFAULT_REDUCE_PARTITION=1000 个分区)
        → streamWriteToPg (foreachPartition 逐批写入)
```

**persist 次数：** 3 → 1（仅 rawFeatures）
**峰值内存：** 从"膨胀后的全量瓦片映射"降到"单个分区内的聚合结果"

---

## 八、优化效果量化分析

> **Q: 这样的优化会有什么效果？我的预期是能够又快又省内存。**

### 8.1 先说结论

| 维度 | 变化 | 说明 |
|---|---|---|
| **内存** | **大幅降低** | 最大收益，从可能 OOM 降到稳定运行 |
| **磁盘 IO** | **大幅降低** | 去掉 2 次 persist 后不再有大量 Spill/反序列化 |
| **CPU** | **略有增加** | 有得有失，下面展开 |
| **总耗时** | **大概率更快** | 内存不 OOM → 不触发 GC 停顿和磁盘 Spill → 整体更快 |

### 8.2 逐项分析

#### (1) MapToTileFunction1 → TileIterator + 去掉 persist

**省了什么：**
- 不再构建中间 HashMap（每个要素省掉 N 个 Entry + N 个 ArrayList 对象）
- 不再 persist 膨胀后的 tileFeatures（省掉序列化 + 磁盘写入 + 反序列化）
- 不再有 Spill（当前数据量一大就 Spill 到磁盘，反序列化极慢）

**代价是什么：**
- 没有 persist 后，`reduceByKey` 需要重算上游链（transform → flatMapToPair）。
  对于每个分区内的数据，要素会被：
  - 从 rawFeatures persist 读取一次
  - transform 一次
  - 通过 TileIterator 映射到瓦片一次
  - 进入 reduceByKey 聚合
  这意味着 **transform + 瓦片映射会执行两次**（reduceByKey 内部 shuffle 前后各一次）。

**但是：**
- 当前版本 persist 了 tileFeatures，但 Spill 到磁盘后再读回来的序列化/反序列化开销
  往往 **比重新计算还慢**（尤其是 Java 对象序列化）。
- TileIterator 的瓦片映射逻辑非常轻量：算 quadKey + 创建 singletonList，全是纯 CPU 计算。
- transform（几何校验 + 坐标转换）才是 CPU 大头，但只是一对一 map，流水线化后几乎无额外开销。

**量化估算（以 100 万要素为例）：**

```
当前版本（persist）：
  写入 tileFeatures persist：100万要素 × 平均50瓦片 = 5000万条 KV → 序列化 → 磁盘
  reduceByKey 读取：磁盘 → 反序列化 → 5000万条 KV
  总磁盘 IO：约 5000万 × 2 = 1亿次序列化/反序列化

优化后（不 persist）：
  reduceByKey 重算：100万要素 × 2次 transform + 2次瓦片映射 = 纯 CPU
  总磁盘 IO：0（除了 rawFeatures 的 persist，这是必须的）
```

**结论：CPU 重算的代价 < 磁盘 Spill 的代价，尤其在你的场景（算力强、内存不足）。**

#### (2) 去掉 tileFeatures.count()

**省了什么：**
- 去掉一个完整的 action，这个 action 会触发 tileFeatures 的全量物化
- 在当前设计中，这可能是 OOM 的直接触发点

**代价：** 无。日志可以通过 writer 端的 `rootTotalCount` 替代。

#### (3) 去掉 transform 的 persist

**省了什么：**
- 一份与 rawFeatures 等大的内存/磁盘缓存

**代价：** 几乎为零。transform 是一对一 map，在新设计中只被下游消费一次（通过 TileIterator 流入 reduceByKey），不会被多次回溯。

### 8.3 内存模型对比

```
当前版本内存分布（假设 100万要素，zoom 4-15，平均命中50瓦片）：

  rawFeatures persist:        ~2GB（估算，取决于单要素大小）
  transformedFeatures persist: ~2GB（等大）
  tileFeatures persist:       ~100GB（膨胀50倍，Spill到磁盘）
  reduceByKey shuffle:        ~100GB（再次磁盘IO）
  ─────────────────────────────────
  峰值内存需求：远超可用内存 → OOM

优化后内存分布：

  rawFeatures persist:        ~2GB（保留，唯一的缓存）
  transform + TileIterator:   流式，无缓存
  reduceByKey 每个分区:       仅该分区的聚合结果（几十MB级别）
  ─────────────────────────────────
  峰值内存需求：~2GB + 分区数 × 几十MB ≈ 可控范围内
```

### 8.4 速度模型对比

```
当前版本耗时分解：

  读取 PostGIS:      ████░░░░░░  约10%
  transform:         ██░░░░░░░░  约5%
  瓦片映射+persist:  ████████████████████  约50%  ← 最慢，包含序列化+磁盘IO
  count():           ████░░░░░░  约10%  ← 强制物化
  reduceByKey:       ██████░░░░  约15%
  streamWriteToPg:   ███░░░░░░░  约10%
  总计：             受限于磁盘IO

优化后耗时分解：

  读取 PostGIS:      ██████░░░░  约20%
  transform×2:       ████░░░░░░  约10%  ← 两次，但纯CPU
  瓦片映射×2:        ██████░░░░  约15%  ← 两次，纯CPU，无序列化
  reduceByKey:       ████████░░  约25%  ← 不再等Spill
  streamWriteToPg:   ██████░░░░  约20%
  GC/其他:           ████░░░░░░  约10%
  总计：             受限于CPU（你的强项）
```

### 8.5 总结

```
你的场景：算力强、内存不足

优化方向：用 CPU 换内存，减少磁盘中间态

具体收益：
  ✅ 内存：从 OOM 风险 → 稳定运行（最大收益）
  ✅ 磁盘IO：减少约 90%（不再有 tileFeatures 的 Spill）
  ✅ GC：大幅减少（对象数量从 5000万+ 降到几百万）
  ✅ 总耗时：大概率更快（消除了 Spill 和 GC 停顿）
  ⚠️ CPU：增加约 30-50%（transform + 瓦片映射执行两次）
         但你说了"算力很强大"，这是可以接受的代价

一句话：当前版本是"省内存算力但用磁盘换"，却换崩了；
        优化后是"用算力省内存"，恰好匹配你的硬件特征。
```
