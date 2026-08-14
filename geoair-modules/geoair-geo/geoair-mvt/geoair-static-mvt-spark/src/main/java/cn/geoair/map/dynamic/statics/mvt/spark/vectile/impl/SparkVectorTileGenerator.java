package cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
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
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.utils.VectorTileCommonUtils;
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


public class SparkVectorTileGenerator implements Serializable {
    public static GiLogger log = GirLoggerFactory.getLogger();
    private transient SparkSession sparkSession;

    private static final int DEFAULT_MAX_PARTITION = 20; // 这里是初始的分区数量

    // 1000 个分区代表总共有 1000 个任务要执行，但 Spark 会根据集群的并行度（由 Executor 数量、每个 Executor
    // 的核心数决定）来控制「同时运行的任务数」；
    // 执行节点只有 8 个：假设每个节点只运行 1 个 Executor、每个 Executor 分配 1 个核心，那么集群的最大并行度就是 8—— 同一时间最多只有 8
    // 个分区任务在执行；
    // 任务分批执行：先执行 8 个分区的任务（创建 8 个数据库连接），其中一个任务执行完成后，再调度第 9 个分区的任务（创建第 9 个连接，此时前 8 个中已有 1
    // 个释放）。
    // 总结： 分区数量越大，单个运行的分区的内存占用越小，越不容易oom
    private static final int DEFAULT_REDUCE_PARTITION = 1000;

    private static final int DEFAULT_TRANSFORM_PARTITION = 1000;

    private static final int TILE_BATCH_SIZE = 300; //最大300条一次提交

    final long BATCH_SIZE_THRESHOLD = 900 * 1024; // 数据量限制900KB一次提交
    String insertSqlTemplate = "INSERT INTO  %s (id, z, x, tms_y, y, grid_srid, tile_data, layer_name, edition, insert_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    public SparkVectorTileGenerator(SparkSession sparkSession) {
        this.sparkSession = sparkSession;
    }

    /**
     * 主流程：读取数据 → 转换 → 映射到瓦片 → 聚合 → 流式写入PG
     */
    public void doGenerate(TileSliceParameter parameter)
            throws Exception {
        JSONObject entries = JSONUtil.parseObj(parameter);
        // 移除敏感字段和非参数字段，避免打印到日志
        entries.remove("inputSource");
        entries.remove("outputSource");
        Log log = LogFactory.get(CallerUtil.getCallerCaller());
        log.info(
                "{} 执行器开始切片（流式逐批写入模式），切片参数信息：\n {}",
                this.getClass().getSimpleName(),
                entries.toStringPretty());

//        Map<String, String> pgParams = VectorTileCommonUtils.buildPgWriteParams(parameter);
        DataSourceConfig outputSource = parameter.getOutputSource();
        createTableDDL(parameter);
        // 1. 读取数据（仅此处持久化，避免重复读取PG）
        JavaRDD<GirAdvOneRow> rawFeatures = null;
        ReadStrategy strategy =
                Optional.ofNullable(parameter.getReadStrategy()).orElse(ReadStrategy.ID_PAGE);

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
        // 仅此处持久化，避免重复读取PG
        JavaRDD<GirAdvOneRow> persistedFeaturesRDD =
                rawFeatures.persist(StorageLevel.MEMORY_AND_DISK());
        long count = persistedFeaturesRDD.count();
        log.info("查询得到的所有的要素数量为:{}", count);

        // 2. 空间转换（修复几何、坐标系转换）
        JavaRDD<GirAdvOneRow> transformedFeatures =
                persistedFeaturesRDD.map(
                                new SparkTaskSerializableUtil.TransformFeatureFunction(parameter))
                        .persist(StorageLevel.MEMORY_AND_DISK());

        // 3. 要素映射到瓦片
        JavaPairRDD<String, List<GirAdvOneRow>> tileFeatures = null;
        tileFeatures =
                transformedFeatures.flatMapToPair(
                                new SparkTaskSerializableUtil.MapToTileFunction1(parameter))
                        .persist(StorageLevel.MEMORY_AND_DISK());
        log.info("=================================================");
        log.info("要素映射到瓦片总条数：{}", tileFeatures.count());
        log.info("=================================================");

        if (parameter.isStatisticsEnabled()) {
            JavaPairRDD<String, List<GirAdvOneRow>> tileFeaturesByZoom =
                    transformedFeatures.flatMapToPair(
                            new SparkTaskSerializableUtil.MapToTileFunctionToStatic(
                                    parameter, parameter.getMaxZoom()));
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
            // 抽样500个pbf瓦片,然后转成rdd，避免oom，无解
            JavaFutureAction<List<Tuple2<String, PbfInfo>>> listJavaFutureAction =
                    pbfRDD.takeAsync(500);
            log.info("抽样500条数据完成！");
            List<Tuple2<String, PbfInfo>> tuple2s = listJavaFutureAction.get();
            Seq<Tuple2<String, PbfInfo>> tuple2Seq =
                    JavaConverters.asScalaIteratorConverter(tuple2s.iterator()).asScala().toSeq();
            ClassTag<Tuple2<String, PbfInfo>> classTag = ClassTag$.MODULE$.apply(Tuple2.class);
            JavaRDD<Tuple2<String, PbfInfo>> tuple2sRDD =
                    sparkSession
                            .sparkContext()
                            .parallelize(tuple2Seq, DEFAULT_REDUCE_PARTITION, classTag)
                            .toJavaRDD();
            AdvEnumsTypeGeom typeGeom = parameter.getTypeGeom();
            String geomType = typeGeom != null ? typeGeom.name() : "Unknown";
            StatisticUtils.statAndWriteJson(
                    tuple2sRDD,
                    geomType,
                    parameter.getStaticTableName(),
                    parameter,
                    count,
                    sparkSession);
        }


        // 4. 聚合
        JavaPairRDD<String, List<GirAdvOneRow>> aggregatedRDD =
                tileFeatures.reduceByKey(
                        new SparkTaskSerializableUtil.AggregateAndLimitFeatureFunction(parameter),
                        DEFAULT_REDUCE_PARTITION);

        // 主图层写入
        log.info("开始流式写入主图层，表名：{}", outputSource.getTableName());

        try {
            streamWriteToPg(aggregatedRDD, parameter);
        } catch (Throwable e) {
            log.error("流式写入PG过程中发生异常", e);
            throw e;
        }

        log.info("流式边生成边写入流程执行完成");
    }

    private void streamWriteToPg(
            JavaPairRDD<String, List<GirAdvOneRow>> aggregatedRDD,
            TileSliceParameter parameter
    ) {

        aggregatedRDD.foreachPartition(
                (VoidFunction<Iterator<Tuple2<String, List<GirAdvOneRow>>>>)
                        partitionIterator -> {
                            Log log = Log.get();
                            DataSourceConfig outputSource = parameter.getOutputSource();
                            DataSource dataSource = outputSource.toDataSource();
                            // 最终日志
                            int outGridSrid = parameter.getOutGridSrid();
                            String edition = parameter.getEdition();
                            // ================= 每个表独立连接 =================
                            List<Row> rootBatchRows = new ArrayList<>(TILE_BATCH_SIZE);
                            // ================= 日志统计变量 =================
                            AtomicLong rootTotalCount = new AtomicLong(0);
                            long rootBatchNum = 0;
                            // 每个批次总字节数
                            long currentBatchSize = 0;
                            String rootTableName = outputSource.getTableNameForSql();
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

                                    // 获取当前单条数据大小
                                    long singleSize = 0;
                                    try {
                                        singleSize = pbfInfo1.getDataLength();
                                    } catch (Exception ignored) {
                                    }

                                    // ================= Root 表 =================
                                    if (StringUtils.isNotBlank(rootTableName)) {
                                        Row row = buildRow(pbfTuple, false, false, parameter);
                                        if (row != null) {
                                            // 规则：单条 >= 300KB，直接单独提交
                                            if (singleSize >= BATCH_SIZE_THRESHOLD) {
                                                // 先把之前批次提交掉
                                                if (!rootBatchRows.isEmpty()) {
                                                    rootBatchNum++;
                                                    executeBatchInsert(rootBatchRows, rootTableName, dataSource);
                                                    rootTotalCount.addAndGet(rootBatchRows.size());
                                                    log.info("{}:分区内Root表批次:{} 写入完成，本次提交条数：{}，累计瓦片数：{}，批量提交大小：{} ,outGridSrid:{},edition:{}",
                                                            rootTableName, rootBatchNum, rootBatchRows.size(), rootTotalCount.get(),
                                                            DataSizeUtil.format(currentBatchSize), outGridSrid, edition);
                                                    rootBatchRows.clear();
                                                    currentBatchSize = 0;
                                                }
                                                // 单独提交这条超大数据
                                                rootBatchRows.add(row);
                                                rootBatchNum++;
                                                executeBatchInsert(rootBatchRows, rootTableName, dataSource);
                                                rootTotalCount.addAndGet(1);
                                                log.info("{}:分区内Root表单条超大数据提交完成，大小：{}，累计瓦片数：{} ,outGridSrid:{}",
                                                        rootTableName, DataSizeUtil.format(singleSize), rootTotalCount.get(), outGridSrid);
                                                rootBatchRows.clear();
                                                currentBatchSize = 0;
                                            } else {
                                                // 正常加入批次
                                                rootBatchRows.add(row);
                                                currentBatchSize += singleSize;

                                                // 触发提交：条数够 OR 大小超300KB
                                                boolean needFlush = rootBatchRows.size() >= TILE_BATCH_SIZE || currentBatchSize >= BATCH_SIZE_THRESHOLD;
                                                if (needFlush) {
                                                    rootBatchNum++;
                                                    executeBatchInsert(rootBatchRows, rootTableName, dataSource);
                                                    rootTotalCount.addAndGet(rootBatchRows.size());
                                                    log.info("{}:分区内Root表批次:{} 写入完成，本次提交条数：{}，累计瓦片数：{}，批量提交大小：{} ,outGridSrid:{}",
                                                            rootTableName, rootBatchNum, rootBatchRows.size(), rootTotalCount.get(),
                                                            DataSizeUtil.format(currentBatchSize), outGridSrid);
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
                                    log.info("{}:分区内Root表最后批次写入完成，剩余条数：{}，累计总数：{}，提交大小：{} outGridSrid:{}",
                                            rootTableName, rootBatchRows.size(), rootTotalCount.get(),
                                            DataSizeUtil.format(currentBatchSize), outGridSrid);
                                }


                                // 最终日志
                                log.info("{} :分区内Root表最终完成，累计写入瓦片数：{} ,outGridSrid:{},edition:{}", rootTableName, rootTotalCount.get(), outGridSrid, edition);


                            } catch (Exception e) {
                                log.error("写入PG失败", e);

                                throw new RuntimeException(e);
                            } finally {

                            }
                        });


        // 日志汇总
        log.info(
                "所有图层流式写入完成，表名：Root={}, Label={}, Boundary={}",
                parameter.getOutputSource().getTableName(),
                parameter.getTableNameLabel(),
                parameter.getTableNameBoundary());
    }



    private void executeBatchInsert(List<Row> batchRows, String tableName, DataSource dataSource) throws Exception {
        log.info("====================批量提交开始===================");
        Connection connection = dataSource.getConnection();
        PreparedStatement preparedStatement = null;
        StopWatch stopWatch = new StopWatch();
        try {
            connection.setAutoCommit(false);
            preparedStatement = connection.prepareStatement(String.format(insertSqlTemplate, tableName));
            stopWatch.start();
            for (Row row : batchRows) {
                // 按schema顺序设置参数
                preparedStatement.setString(1, IdUtil.getSnowflakeNextIdStr()); // id
                preparedStatement.setInt(2, row.getInt(0)); // z
                preparedStatement.setInt(3, row.getInt(1)); // x
                preparedStatement.setInt(4, row.getInt(2)); // tms_ y
                preparedStatement.setInt(5, row.getInt(3)); // y
                preparedStatement.setInt(6, row.getInt(4)); // grid_srid
                preparedStatement.setBytes(7, row.getAs(5)); // tile_data
                preparedStatement.setString(8, row.getString(6)); // layer_name
                preparedStatement.setString(9, row.getString(7)); // edition
                preparedStatement.setLong(10, System.currentTimeMillis()); // 时间戳
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
            connection.commit();
            connection.setAutoCommit(true);
            stopWatch.stop();
        } finally {
            IoUtil.close(preparedStatement);
            IoUtil.close(connection);
        }


        log.info("====================批量提交结束：耗时：{}，条数：{}=====", stopWatch.getLastTaskTimeMillis(), batchRows.size());
    }

    /**
     * 按ID分片读取PostGIS数据
     */
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
        int maxMemoryPerPartitionMB = 256 * 1024;  //256G
        int maxRowPerPartition = (maxMemoryPerPartitionMB * 1024) / estimatedSingleRowSizeKB;

        int maxPartionNum;
        int calcPartitionNum = (int) Math.ceil((double) totalCount / maxRowPerPartition);
        maxPartionNum = Math.min(calcPartitionNum, (int) Math.max(1, totalCount));
        log.info("ID分页分片数量：{}（总数据量：{}，单分区最大条数：{}）", maxPartionNum, totalCount, maxRowPerPartition);

        int countPerTask = (int) Math.ceil((double) totalCount / maxPartionNum);
        countPerTask = Math.min(Math.max(countPerTask, 100), maxRowPerPartition);

        String orderFieldName =
                Optional.ofNullable(parameter.getIdFieldName())
                        .filter(StrUtil::isNotBlank)
                        .orElse(parameter.getGeomFieldName());

        List<Integer> pageNumbers =
                DataReadCommonUtils.buildPageNumberList(totalCount, maxPartionNum);
        Dataset<Integer> pageNumDs =
                sparkSession
                        .createDataset(pageNumbers, Encoders.INT())
                        .repartition(Math.min(pageNumbers.size(), maxPartionNum));
        JavaRDD<Integer> pageNumRdd = pageNumDs.javaRDD();

        JavaRDD<GirAdvOneRow> featureRDD =
                pageNumRdd.flatMap(
                        new SparkTaskSerializableUtil.IdPageFlatMapFunction(
                                parameter,
                                parameter.getQueryStatement(),
                                orderFieldName,
                                countPerTask));
        return featureRDD;
    }

    /**
     * 按BBox空间分片读取PostGIS数据
     */
    private JavaRDD<GirAdvOneRow> readDataByBBox(TileSliceParameter parameter) throws Exception {
        if (parameter == null) {
            throw new IllegalArgumentException("输入参数不能为空，inputSource 必须配置");
        }
        DataSourceConfig inputSource = parameter.getInputSource();
        IAdvExecutor iAdvExecutor = AdvExecutorFactory.getAdvExecutorByDataSource(inputSource.toDataSource());

        BBoxApo bBoxApo =
                iAdvExecutor.eGetExtent(
                        parameter.getQueryStatement(), parameter.getGeomFieldName());
        if (bBoxApo == null) {
            throw new RuntimeException("无法获取数据的空间范围");
        }

        int maxPartionNum = Optional.ofNullable(parameter.getMaxPartionNum()).orElse(100);
        List<String> partitionConditions =
                DataReadCommonUtils.buildBboxPartitionConditions(
                        bBoxApo, maxPartionNum, parameter.getSourceDataSrid());
        log.info("BBox分片数量：" + partitionConditions.size());
        Dataset<String> partitionConditionsDs =
                sparkSession
                        .createDataset(partitionConditions, Encoders.STRING())
                        .repartition(partitionConditions.size());
        JavaRDD<String> partitionConditionRdd = partitionConditionsDs.javaRDD();

        JavaRDD<GirAdvOneRow> featureRDD =
                partitionConditionRdd.flatMap(
                        new SparkTaskSerializableUtil.BboxFlatMapFunction(
                                parameter,
                                parameter.getQueryStatement(),
                                parameter.getGeomFieldName(),
                                parameter.getSourceDataSrid()));
        return featureRDD;
    }

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

    private void createTableDDL(TileSliceParameter parameter) {
        DataSourceConfig outputSource = parameter.getOutputSource();
        DataSource dataSource = outputSource.toDataSource();
        IAdvExecutor iAdvExecutor = AdvExecutorFactory.getAdvExecutorByDataSource(dataSource);
        String tableNameForSql = outputSource.getTableNameForSql();
        String tableNameWithSchema = iAdvExecutor.tbGetTableNameWithSchema(tableNameForSql);
        boolean b = iAdvExecutor.dIsTableExists(tableNameWithSchema);
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
        if (!b) {
            log.info("检测到表不存在，执行创建表动作！");
            String sqlDDL = tempLate.replace("{tableNameWithSchema}", tableNameWithSchema).replace("{UUID}", IdUtil.getSnowflakeNextIdStr());
            iAdvExecutor.dExecuteDDL(sqlDDL, tableNameWithSchema, "创建表");
        }
    }
}
