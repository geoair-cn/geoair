package cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v2;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.base.percent.GiPercentConsumer;
import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.geoair.map.dynamic.adv.query.apo.BBoxApo;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsTypeGeom;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.adv.spring.AdvExecutorFactory;
import cn.geoair.map.dynamic.mvt.tools.model.PbfInfo;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.ReadStrategy;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.*;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.statistics.StatisticUtils;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.utils.DataReadCommonUtils;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.utils.SparkTaskSerializableUtil;
import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.unit.DataSizeUtil;
import cn.hutool.core.lang.caller.CallerUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.spark.TaskContext;
import org.apache.spark.api.java.JavaFutureAction;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.VoidFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.storage.StorageLevel;
import scala.Tuple2;
import scala.collection.JavaConverters;
import scala.collection.Seq;
import scala.reflect.ClassTag;
import scala.reflect.ClassTag$;

import javax.sql.DataSource;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 矢量瓦片生成器 V2 — 内存优化版 + 进度条。
 * <p>
 * 相对于原版 {@code SparkVectorTileGenerator} 的优化：
 * <ul>
 *   <li>使用 {@link cn.geoair.map.dynamic.statics.mvt.spark.vectile.utils.SparkTaskSerializableUtil.MapToTileFunction}
 *       （TileIterator 懒生成）替代 MapToTileFunction1（HashMap 全量收集）</li>
 *   <li>去掉 transform 阶段的 persist（一对一映射，不需要回溯）</li>
 *   <li>去掉 tileFeatures 的 persist（流式传递到 reduceByKey）</li>
 *   <li>去掉 tileFeatures.count()（避免强制物化膨胀数据）</li>
 *   <li>仅保留 rawFeatures 的 persist（唯一一次缓存）</li>
 *   <li>通过 {@link ProgressTracker} 实现基于 SparkListener 的进度跟踪</li>
 * </ul>
 *
 * @see ProgressTracker
 * @see SparkTaskFunctions.MapToTileFunctionSingleZoom
 */
public class SparkVectorTileGeneratorV2 implements Serializable {

    public static GiLogger log = GirLoggerFactory.getLogger();
    private transient SparkSession sparkSession;

    private static final int DEFAULT_REDUCE_PARTITION = 1000;
    private static final int TILE_BATCH_SIZE = 300;
    final long BATCH_SIZE_THRESHOLD = 900 * 1024; // 900KB
    String insertSqlTemplate = "INSERT INTO %s (id, z, x, tms_y, y, grid_srid, tile_data, layer_name, edition, insert_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    public SparkVectorTileGeneratorV2(SparkSession sparkSession) {
        this.sparkSession = sparkSession;
    }

    /**
     * 主流程：读取数据 → 转换 → 流式映射到瓦片 → 聚合 → 流式写入PG（带进度跟踪）。
     */
    public void doGenerate(TileSliceParameter parameter) throws Exception {
        doGenerate(parameter, null);
    }

    /**
     * 主流程（带外部进度回调）。
     *
     * @param parameter       切片参数
     * @param percentConsumer 进度回调（可为 null），在 executor 线程中直接调用（local 模式），
     *                        accept(allCount, currentCount)。内部用 Serializable 包装避免闭包序列化失败
     */
    public void doGenerate(TileSliceParameter parameter, GiPercentConsumer percentConsumer) throws Exception {
        // ==================== 初始化进度跟踪 ====================
        ProgressTracker tracker = ProgressTracker.init(sparkSession, 4);

        // ==================== 打印参数 ====================
        JSONObject entries = JSONUtil.parseObj(parameter);
        entries.remove("inputSource");
        entries.remove("outputSource");
        Log initLog = LogFactory.get(CallerUtil.getCallerCaller());
        initLog.info("{} V2（内存优化版）开始切片，参数:\n{}",
                this.getClass().getSimpleName(), entries.toStringPretty());

        DataSourceConfig outputSource = parameter.getOutputSource();

        // ==================== 创建输出表 ====================
        createTableDDL(parameter);

        // ==================== Stage 1: 读取数据 ====================
        tracker.setStageName("读取数据");

        JavaRDD<GirAdvOneRow> rawFeatures;
        ReadStrategy strategy = Optional.ofNullable(parameter.getReadStrategy()).orElse(ReadStrategy.ID_PAGE);
        switch (strategy) {
            case ID_PAGE:
                rawFeatures = readDataByIdPage(parameter);
                break;
            case BBOX:
                rawFeatures = readDataByBBox(parameter);
                break;
            default:
                throw new IllegalArgumentException("不支持的读取策略：" + strategy);
        }

        // 唯一的一次 persist：原始要素
        JavaRDD<GirAdvOneRow> persistedFeaturesRDD = rawFeatures.persist(StorageLevel.MEMORY_AND_DISK());
        long count = persistedFeaturesRDD.count();
        tracker.getFeaturesRead().add(count);
        tracker.completeStage("要素: " + String.format("%,d", count));

        // ==================== Stage 2: 空间转换 ====================
        tracker.setStageName("空间转换");

        // 不 persist！一对一 map，流式传递到下游
        JavaRDD<GirAdvOneRow> transformedFeatures =
                persistedFeaturesRDD.map(new SparkTaskSerializableUtil.TransformFeatureFunction(parameter));

        tracker.completeStage("要素: " + String.format("%,d", count));

        // ==================== Stage 3: 瓦片映射 + 聚合 ====================
        tracker.setStageName("瓦片映射");

        // 使用 MapToTileFunction（TileIterator 懒生成，不 persist）
        JavaPairRDD<String, List<GirAdvOneRow>> tileFeatures =
                transformedFeatures.flatMapToPair(
                        new SparkTaskSerializableUtil.MapToTileFunction(parameter));

        // 统计路径（可选）
        if (parameter.isStatisticsEnabled()) {
            runStatisticsPipeline(parameter, transformedFeatures, count, tracker);
        }

        // 聚合：reduceByKey 流式聚合，不需要 tileFeatures 先物化
        JavaPairRDD<String, List<GirAdvOneRow>> aggregatedRDD =
                tileFeatures.reduceByKey(
                        new SparkTaskSerializableUtil.AggregateAndLimitFeatureFunction(parameter),
                        DEFAULT_REDUCE_PARTITION);

        // 触发聚合计算并统计瓦片数
        long tileCount = aggregatedRDD.count();
        tracker.completeStage("瓦片: " + String.format("%,d", tileCount));

        // 阶段1-3 进度回调：60%
        if (percentConsumer != null) {
            percentConsumer.accept(100L, 60L);
        }

        // ==================== Stage 4: 流式写入PG ====================
        tracker.setStageName("写入PG");

        try {
            streamWriteToPg(aggregatedRDD, parameter, tracker, tileCount, percentConsumer);
        } catch (Throwable e) {
            log.error("流式写入PG过程中发生异常", e);
            throw e;
        }

        tracker.completeStage();
        tracker.printSummary();

        log.info("V2 切片流程执行完成");
    }

    // ==================== 统计路径 ====================

    private void runStatisticsPipeline(
            TileSliceParameter parameter,
            JavaRDD<GirAdvOneRow> transformedFeatures,
            long count,
            ProgressTracker tracker) {

        JavaPairRDD<String, List<GirAdvOneRow>> tileFeaturesByZoom =
                transformedFeatures.flatMapToPair(
                        new SparkTaskFunctions.MapToTileFunctionSingleZoom(parameter, parameter.getMaxZoom()));

        TileSliceParameter copy = parameter.copy();
        copy.setFeatureLimit(1000)
                .setFeatureLimitEnabled(true)
                .setDropDensestAsNeeded(false)
                .setCoalesceDensestAsNeeded(false);

        JavaPairRDD<String, List<GirAdvOneRow>> aggregatedRDDByZoom =
                tileFeaturesByZoom.reduceByKey(
                        new SparkTaskSerializableUtil.AggregateAndLimitFeatureFunction(copy),
                        DEFAULT_REDUCE_PARTITION);

        PbfTargetInfo instance = PbfTargetInfo.getInstance();
        instance.setSaveFeatureList(true);

        JavaRDD<Tuple2<String, PbfInfo>> pbfRDD =
                aggregatedRDDByZoom.map(
                        new SparkTaskSerializableUtil.GeneratePbfFunction(parameter, instance));

        JavaFutureAction<List<Tuple2<String, PbfInfo>>> listJavaFutureAction = pbfRDD.takeAsync(500);
        log.info("统计路径：抽样 500 个 PBF...");
        try {
            List<Tuple2<String, PbfInfo>> tuple2s = listJavaFutureAction.get();
            Seq<Tuple2<String, PbfInfo>> tuple2Seq =
                    JavaConverters.asScalaIteratorConverter(tuple2s.iterator()).asScala().toSeq();
            ClassTag<Tuple2<String, PbfInfo>> classTag = ClassTag$.MODULE$.apply(Tuple2.class);
            JavaRDD<Tuple2<String, PbfInfo>> tuple2sRDD =
                    sparkSession.sparkContext()
                            .parallelize(tuple2Seq, DEFAULT_REDUCE_PARTITION, classTag)
                            .toJavaRDD();

            AdvEnumsTypeGeom typeGeom = parameter.getTypeGeom();
            String geomType = typeGeom != null ? typeGeom.name() : "Unknown";
            StatisticUtils.statAndWriteJson(tuple2sRDD, geomType, parameter.getStaticTableName(), parameter, count, sparkSession);
        } catch (Exception e) {
            log.error("统计路径执行失败", e);
        }
    }

    // ==================== 流式写入 ====================

    /**
     * Serializable 包装器，将 GiPercentConsumer 包装为可序列化版本。
     * <p>
     * GiPercentConsumer 本身不继承 Serializable，直接传入 Spark 闭包会序列化失败。
     * 此包装器实现了 Serializable，使闭包可以正常序列化。
     */
    private static class SerializableConsumer implements Serializable {
        private static final long serialVersionUID = 1L;
        private final GiPercentConsumer consumer;

        SerializableConsumer(GiPercentConsumer consumer) {
            this.consumer = consumer;
        }

        void accept(long allCount, long currentCount) {
            if (consumer != null) {
                consumer.accept(allCount, currentCount);
            }
        }

        boolean isPresent() {
            return consumer != null;
        }
    }

    private void streamWriteToPg(
            JavaPairRDD<String, List<GirAdvOneRow>> aggregatedRDD,
            TileSliceParameter parameter,
            ProgressTracker tracker,
            long estimatedTotalTiles,
            GiPercentConsumer percentConsumer) {

        // 用 Serializable 包装器包装 consumer，避免闭包序列化失败
        final SerializableConsumer progressCallback =
                percentConsumer != null ? new SerializableConsumer(percentConsumer) : null;

        aggregatedRDD.foreachPartition(
                (VoidFunction<Iterator<Tuple2<String, List<GirAdvOneRow>>>>) partitionIterator -> {
                    Log log = Log.get();
                    DataSourceConfig outputSource = parameter.getOutputSource();
                    DataSource dataSource = outputSource.toDataSource();

                    // 判断当前是否在 executor 环境
                    // local 模式：executor 和 driver 同 JVM，consumer 可直接调用
                    // cluster 模式：TaskContext.get() != null 但 consumer 需要 Serializable
                    boolean isExecutor = TaskContext.get() != null;

                    int outGridSrid = parameter.getOutGridSrid();
                    String edition = parameter.getEdition();
                    String rootTableName = outputSource.getTableNameForSql();

                    List<Row> rootBatchRows = new ArrayList<>(TILE_BATCH_SIZE);
                    AtomicLong rootTotalCount = new AtomicLong(0);
                    long rootBatchNum = 0;
                    long currentBatchSize = 0;
                    long partitionStartTime = System.currentTimeMillis();

                    // 进度回调（仅 executor 侧，local 模式下 consumer 可直接调用）
                    Runnable notifyProgress = () -> {
                        if (isExecutor && progressCallback != null) {
                            try {
                                progressCallback.accept(estimatedTotalTiles, rootTotalCount.get());
                            } catch (Exception ignored) {
                            }
                        }
                    };

                    try {
                        SparkTaskSerializableUtil.GeneratePbfFunction pbfFunc =
                                new SparkTaskSerializableUtil.GeneratePbfFunction(parameter, PbfTargetInfo.getInstance());

                        while (partitionIterator.hasNext()) {
                            Tuple2<String, List<GirAdvOneRow>> aggregatedTuple = partitionIterator.next();
                            Tuple2<String, PbfInfo> pbfTuple = null;
                            try {
                                pbfTuple = pbfFunc.call(aggregatedTuple);
                            } catch (Exception e) {
                                log.error("PBF生成失败", e);
                                continue;
                            }
                            PbfInfo pbfInfo1 = pbfTuple._2;
                            if (pbfTuple == null || pbfInfo1 == null) continue;

                            long singleSize = 0;
                            try {
                                singleSize = pbfInfo1.getDataLength();
                            } catch (Exception ignored) {
                            }

                            // ================= Root 表 =================
                            if (StringUtils.isNotBlank(rootTableName)) {
                                Row row = buildRow(pbfTuple, false, false, parameter);
                                if (row != null) {
                                    if (singleSize >= BATCH_SIZE_THRESHOLD) {
                                        if (!rootBatchRows.isEmpty()) {
                                            rootBatchNum++;
                                            executeBatchInsert(rootBatchRows, rootTableName, dataSource);
                                            rootTotalCount.addAndGet(rootBatchRows.size());
                                            tracker.getTilesWritten().add(rootBatchRows.size());
                                            tracker.getBatchesWritten().add(1);
                                            tracker.getBytesWritten().add(currentBatchSize);
                                            log.info("{}:分区内Root表批次:{} 写入完成，本次提交条数：{}，累计瓦片数：{}，批量提交大小：{} ,outGridSrid:{},edition:{}",
                                                    rootTableName, rootBatchNum, rootBatchRows.size(), rootTotalCount.get(),
                                                    DataSizeUtil.format(currentBatchSize), outGridSrid, edition);
                                            notifyProgress.run();
                                            rootBatchRows.clear();
                                            currentBatchSize = 0;
                                        }
                                        rootBatchRows.add(row);
                                        rootBatchNum++;
                                        executeBatchInsert(rootBatchRows, rootTableName, dataSource);
                                        rootTotalCount.addAndGet(1);
                                        tracker.getTilesWritten().add(1);
                                        tracker.getBatchesWritten().add(1);
                                        tracker.getBytesWritten().add(singleSize);
                                        log.info("{}:分区内Root表单条超大数据提交完成，大小：{}，累计瓦片数：{} ,outGridSrid:{}",
                                                rootTableName, DataSizeUtil.format(singleSize), rootTotalCount.get(), outGridSrid);
                                        notifyProgress.run();
                                        rootBatchRows.clear();
                                        currentBatchSize = 0;
                                    } else {
                                        rootBatchRows.add(row);
                                        currentBatchSize += singleSize;

                                        boolean needFlush = rootBatchRows.size() >= TILE_BATCH_SIZE || currentBatchSize >= BATCH_SIZE_THRESHOLD;
                                        if (needFlush) {
                                            rootBatchNum++;
                                            executeBatchInsert(rootBatchRows, rootTableName, dataSource);
                                            rootTotalCount.addAndGet(rootBatchRows.size());
                                            tracker.getTilesWritten().add(rootBatchRows.size());
                                            tracker.getBatchesWritten().add(1);
                                            tracker.getBytesWritten().add(currentBatchSize);
                                            log.info("{}:分区内Root表批次:{} 写入完成，本次提交条数：{}，累计瓦片数：{}，批量提交大小：{} ,outGridSrid:{}",
                                                    rootTableName, rootBatchNum, rootBatchRows.size(), rootTotalCount.get(),
                                                    DataSizeUtil.format(currentBatchSize), outGridSrid);
                                            notifyProgress.run();
                                            rootBatchRows.clear();
                                            currentBatchSize = 0;
                                        }
                                    }
                                }
                            }

                            // 清理内存
                            PbfInfo pbfInfo = pbfInfo1;
                            pbfInfo.setDataBoundary(new byte[0]);
                            pbfInfo.setData(new byte[0]);
                            pbfInfo.setDataLabel(new byte[0]);
                            List<GirAdvOneRow> gtcAdvOneRows = aggregatedTuple._2;
                            if (gtcAdvOneRows != null) {
                                try {
                                    gtcAdvOneRows.clear();
                                } catch (Exception e) {
                                }
                            }
                        }

                        // 处理剩余数据
                        if (!rootBatchRows.isEmpty()) {
                            executeBatchInsert(rootBatchRows, rootTableName, dataSource);
                            rootTotalCount.addAndGet(rootBatchRows.size());
                            tracker.getTilesWritten().add(rootBatchRows.size());
                            tracker.getBatchesWritten().add(1);
                            tracker.getBytesWritten().add(currentBatchSize);
                            log.info("{}:分区内Root表最后批次写入完成，剩余条数：{}，累计总数：{}，提交大小：{} outGridSrid:{}",
                                    rootTableName, rootBatchRows.size(), rootTotalCount.get(),
                                    DataSizeUtil.format(currentBatchSize), outGridSrid);
                            notifyProgress.run();
                        }

                        long partitionElapsed = System.currentTimeMillis() - partitionStartTime;
                        double tps = partitionElapsed > 0 ? rootTotalCount.get() * 1000.0 / partitionElapsed : 0;
                        log.info("{} :分区内Root表最终完成，累计写入瓦片数：{} ,outGridSrid:{},edition:{},速度: {} 瓦片/秒",
                                rootTableName, rootTotalCount.get(), outGridSrid, edition, String.format("%.0f", tps));

                    } catch (Exception e) {
                        log.error("写入PG失败", e);
                        throw new RuntimeException(e);
                    }
                });

        // 日志汇总
        log.info(
                "所有图层流式写入完成，表名：Root={}, Label={}, Boundary={}",
                parameter.getOutputSource().getTableName(),
                parameter.getTableNameLabel(),
                parameter.getTableNameBoundary());
    }

    // ==================== 写入工具 ====================

    private Row buildRow(
            Tuple2<String, PbfInfo> tuple,
            boolean isLabel,
            boolean isBoundary,
            TileSliceParameter parameter)
            throws Exception {
        if (isLabel) {
            if (tuple._2.getDataLabel() == null) return null;
            return new SparkTaskSerializableUtil.BuildRowLabelFunction(parameter).call(tuple);
        } else if (isBoundary) {
            if (tuple._2.getDataBoundary() == null) return null;
            return new SparkTaskSerializableUtil.BuildRowBoundaryFunction(parameter).call(tuple);
        } else {
            if (tuple._2.getData() == null) return null;
            return new SparkTaskSerializableUtil.BuildRowFunction(parameter).call(tuple);
        }
    }

    private void executeBatchInsert(List<Row> batchRows, String tableName, DataSource dataSource) throws Exception {
        log.info("====================批量提交开始===================");
        Connection connection = dataSource.getConnection();
        connection.setAutoCommit(false);
        PreparedStatement preparedStatement = null;
        StopWatch stopWatch = new StopWatch();
        try {
            preparedStatement = connection.prepareStatement(String.format(insertSqlTemplate, tableName));
            stopWatch.start();
            for (Row row : batchRows) {
                preparedStatement.setString(1, IdUtil.getSnowflakeNextIdStr()); // id
                preparedStatement.setInt(2, row.getInt(0));    // z
                preparedStatement.setInt(3, row.getInt(1));    // x
                preparedStatement.setInt(4, row.getInt(2));    // tms_y
                preparedStatement.setInt(5, row.getInt(3));    // y
                preparedStatement.setInt(6, row.getInt(4));    // grid_srid
                preparedStatement.setBytes(7, row.getAs(5));   // tile_data
                preparedStatement.setString(8, row.getString(6)); // layer_name
                preparedStatement.setString(9, row.getString(7)); // edition
                preparedStatement.setLong(10, System.currentTimeMillis());
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
            connection.commit();
            stopWatch.stop();
        } finally {
            connection.setAutoCommit(true);
            IoUtil.close(preparedStatement);
            IoUtil.close(connection);
        }
        log.info("====================批量提交结束：耗时：{}，条数：{}=====", stopWatch.getLastTaskTimeMillis(), batchRows.size());
    }

    // ==================== 数据读取 ====================

    private JavaRDD<GirAdvOneRow> readDataByIdPage(TileSliceParameter parameter) throws Exception {
        if (parameter == null || parameter.getInputSource() == null) {
            throw new IllegalArgumentException("输入参数不能为空，inputSource 必须配置");
        }
        DataSourceConfig inputSource = parameter.getInputSource();
        DataSource dataSource = inputSource.toDataSource();
        IAdvExecutor iAdvExecutor = AdvExecutorFactory.getAdvExecutorByDataSource(dataSource);

        long totalCount = iAdvExecutor.pCount(parameter.getQueryStatement());
        if (totalCount == 0) {
            throw new RuntimeException("查询结果为空，无数据可处理");
        }
        int estimatedSingleRowSizeKB = 1;
        int maxMemoryPerPartitionMB = 256 * 1024;
        int maxRowPerPartition = (maxMemoryPerPartitionMB * 1024) / estimatedSingleRowSizeKB;

        int calcPartitionNum = (int) Math.ceil((double) totalCount / maxRowPerPartition);
        int maxPartionNum = Math.min(calcPartitionNum, (int) Math.max(1, totalCount));
        log.info("ID分页分片数量：{}（总数据量：{}，单分区最大条数：{}）", maxPartionNum, totalCount, maxRowPerPartition);

        int countPerTask = (int) Math.ceil((double) totalCount / maxPartionNum);
        countPerTask = Math.min(Math.max(countPerTask, 100), maxRowPerPartition);

        String orderFieldName = Optional.ofNullable(parameter.getIdFieldName())
                .filter(StrUtil::isNotBlank)
                .orElse(parameter.getGeomFieldName());

        List<Integer> pageNumbers = DataReadCommonUtils.buildPageNumberList(totalCount, maxPartionNum);
        Dataset<Integer> pageNumDs = sparkSession
                .createDataset(pageNumbers, Encoders.INT())
                .repartition(Math.min(pageNumbers.size(), maxPartionNum));
        JavaRDD<Integer> pageNumRdd = pageNumDs.javaRDD();

        return pageNumRdd.flatMap(
                new SparkTaskSerializableUtil.IdPageFlatMapFunction(
                        parameter, parameter.getQueryStatement(), orderFieldName, countPerTask));
    }

    private JavaRDD<GirAdvOneRow> readDataByBBox(TileSliceParameter parameter) throws Exception {
        if (parameter == null) {
            throw new IllegalArgumentException("输入参数不能为空，inputSource 必须配置");
        }
        DataSourceConfig inputSource = parameter.getInputSource();
        IAdvExecutor iAdvExecutor = AdvExecutorFactory.getAdvExecutorByDataSource(inputSource.toDataSource());

        BBoxApo bBoxApo = iAdvExecutor.eGetExtent(parameter.getQueryStatement(), parameter.getGeomFieldName());
        if (bBoxApo == null) {
            throw new RuntimeException("无法获取数据的空间范围");
        }

        int maxPartionNum = Optional.ofNullable(parameter.getMaxPartionNum()).orElse(100);
        List<String> partitionConditions = DataReadCommonUtils.buildBboxPartitionConditions(bBoxApo, maxPartionNum, parameter.getSourceDataSrid());
        log.info("BBox分片数量：" + partitionConditions.size());

        Dataset<String> partitionConditionsDs = sparkSession
                .createDataset(partitionConditions, Encoders.STRING())
                .repartition(partitionConditions.size());
        JavaRDD<String> partitionConditionRdd = partitionConditionsDs.javaRDD();

        return partitionConditionRdd.flatMap(
                new SparkTaskSerializableUtil.BboxFlatMapFunction(
                        parameter, parameter.getQueryStatement(), parameter.getGeomFieldName(), parameter.getSourceDataSrid()));
    }

    // ==================== DDL ====================

    private void createTableDDL(TileSliceParameter parameter) {
        DataSourceConfig outputSource = parameter.getOutputSource();
        DataSource dataSource = outputSource.toDataSource();
        IAdvExecutor iAdvExecutor = AdvExecutorFactory.getAdvExecutorByDataSource(dataSource);
        String tableNameForSql = outputSource.getTableNameForSql();
        String tableNameWithSchema = iAdvExecutor.tbGetTableNameWithSchema(tableNameForSql);
        boolean exists = iAdvExecutor.dIsTableExists(tableNameWithSchema);

        if (!exists) {
            String tempLate = "   CREATE TABLE {tableNameWithSchema} (\n" +
                    "                          \"id\" text COLLATE \"pg_catalog\".\"default\",\n" +
                    "                          \"z\" int4,\n" +
                    "                          \"x\" int4,\n" +
                    "                          \"tms_y\" int4,\n" +
                    "                          \"y\" int4,\n" +
                    "                          \"grid_srid\" int4,\n" +
                    "                          \"tile_data\" bytea,\n" +
                    "                          \"layer_name\" text COLLATE \"pg_catalog\".\"default\",\n" +
                    "                          \"edition\" text COLLATE \"pg_catalog\".\"default\",\n" +
                    "                          \"insert_time\" int8\n" +
                    "                        )\n" +
                    "                        ;\n" +
                    "                        CREATE INDEX \"zxy_{UUID}\" ON {tableNameWithSchema} USING btree (\n" +
                    "                          \"z\" \"pg_catalog\".\"int4_ops\" ASC NULLS LAST,\n" +
                    "                          \"x\" \"pg_catalog\".\"int4_ops\" ASC NULLS LAST,\n" +
                    "                          \"y\" \"pg_catalog\".\"int4_ops\" ASC NULLS LAST,\n" +
                    "                          \"grid_srid\" \"pg_catalog\".\"int4_ops\" ASC NULLS LAST,\n" +
                    "                          \"insert_time\" \"pg_catalog\".\"int8_ops\" ASC NULLS LAST\n" +
                    "                        );";
            log.info("检测到表不存在，执行创建表动作！");
            String sqlDDL = tempLate.replace("{tableNameWithSchema}", tableNameWithSchema).replace("{UUID}", IdUtil.getSnowflakeNextIdStr());
            iAdvExecutor.dExecuteDDL(sqlDDL, tableNameWithSchema, "创建表");
        }
    }
}
