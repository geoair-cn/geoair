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
pbfRDD.takeAsync(500)  // 把 500 个 PBF 全部拉到 driver
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
