package cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl;

import cn.geoair.base.util.GutilObject;
import cn.geoair.comp.dynamic.ds.utils.DataSourceDruidFastCreate;
import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.geoair.map.dynamic.adv.query.apo.BBoxApo;
import cn.geoair.map.dynamic.adv.query.dialect.pg.AdvExecutorPG;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsTypeGeom;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
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

import com.alibaba.druid.pool.DruidDataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.spark.api.java.JavaFutureAction;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
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

@Slf4j
public class SparkVectorTileGenerator implements Serializable {

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

    public SparkVectorTileGenerator(SparkSession sparkSession) {
        this.sparkSession = sparkSession;
    }

    /**
     * 边生成边写入，无全量缓存（极致内存优化版）
     */
    public void doGenerate(TileSliceParameter parameter)
            throws Exception {
        JSONObject entries = JSONUtil.parseObj(parameter);
        entries.remove("inputConnectInfo");
        entries.remove("outPutConnectInfo");
        entries.remove("outPutUrl");
        entries.remove("inputUrl");
        Log log = LogFactory.get(CallerUtil.getCallerCaller());
        log.info(
                "{} 执行器开始切片（流式逐批写入模式），切片参数信息：\n {}",
                this.getClass().getSimpleName(),
                entries.toStringPretty());

        Map<String, String> pgParams = VectorTileCommonUtils.buildPgWriteParams(parameter);

        createTableDDL(pgParams.get("table"), parameter);
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

        // // 2. 空间转换（无持久化，流式处理） 这一步改成在driver中执行
        JavaRDD<GirAdvOneRow> transformedFeatures =
                persistedFeaturesRDD.map(
                                new SparkTaskSerializableUtil.TransformFeatureFunction(parameter))
                        .persist(StorageLevel.MEMORY_AND_DISK());

        // 3. 要素映射到瓦片（无持久化）
        JavaPairRDD<String, List<GirAdvOneRow>> tileFeatures = null;
        tileFeatures =
                transformedFeatures.flatMapToPair(
                                new SparkTaskSerializableUtil.MapToTileFunction1(parameter))
                        .persist(StorageLevel.MEMORY_AND_DISK());
        log.error("=================================================");
        log.info("要素映射到瓦片总条数：{}", tileFeatures.count());
        log.error("=================================================");

        if (parameter.isStatisticsIs()) {
            JavaPairRDD<String, List<GirAdvOneRow>> tileFeaturesByZoom =
                    transformedFeatures.flatMapToPair(
                            new SparkTaskSerializableUtil.MapToTileFunctionToStatic(
                                    parameter, parameter.getMaxZoom()));
            TileSliceParameter copy = parameter.copy();
            copy.setFeatureLimit(1000)
                    .setEnableFeatureLimitIs(true)
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

            //            Seq<Tuple2<String, PbfInfo>> tuple2Seq =
            // CollectionConverters.asScala(tuple2s);
            //            ClassTag<Tuple2<String, PbfInfo>> classTag =
            // ClassTag$.MODULE$.apply(Tuple2.class);
            JavaSparkContext jsc = new JavaSparkContext(sparkSession.sparkContext());
            JavaRDD<Tuple2<String, PbfInfo>> tuple2sRDD =
                    jsc.parallelize(tuple2s, DEFAULT_REDUCE_PARTITION);
            //            JavaRDD<Tuple2<String, PbfInfo>> tuple2sRDD =
            //                    sparkSession
            //                            .sparkContext()
            //                            .parallelize(tuple2Seq, DEFAULT_REDUCE_PARTITION,
            // classTag)
            //                            .toJavaRDD();
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
        log.info("开始流式写入主图层，表名：{}", pgParams.get("tableName"));

        try {
            streamWriteToPg(aggregatedRDD, parameter, pgParams);
        } catch (Throwable e) {
            log.info("触发无法捕获的异常");
            log.error(e);
        }

        log.info("流式边生成边写入流程执行完成");
    }

    private void streamWriteToPg(
            JavaPairRDD<String, List<GirAdvOneRow>> aggregatedRDD,
            TileSliceParameter parameter,
            Map<String, String> pgParams) {
        aggregatedRDD.foreachPartition(
                (VoidFunction<Iterator<Tuple2<String, List<GirAdvOneRow>>>>)
                        partitionIterator -> {
                            Log log = Log.get();
                            PgConnectInfoWithTable outPutConnectWithTable = parameter.getOutPutConnectWithTable();
                            DataSource dataSource = outPutConnectWithTable.toDataSource();
                            // 最终日志
                            int outGridSrid = parameter.getOutGridSrid();
                            String edition = parameter.getEdition();
                            // ================= 每个表独立连接 =================
                            Connection connRoot = null;

                            PreparedStatement rootPstmt = null;

                            List<Row> rootBatchRows = new ArrayList<>(TILE_BATCH_SIZE);

                            // ================= 日志统计变量 =================
                            AtomicLong rootTotalCount = new AtomicLong(0);

                            long rootBatchNum = 0;

                            // 每个批次总字节数
                            long currentBatchSize = 0;

                            String rootTableName = pgParams.get("tableName");

                            String insertSqlTemplate = "INSERT INTO tile_cache.%s (id, z, x, tms_y, y, grid_srid, tile_data, layer_name, edition, insert_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                            try {
                                // 建立三个独立连接
                                if (StringUtils.isNotBlank(rootTableName)) {
                                    connRoot = dataSource.getConnection();
                                    connRoot.setAutoCommit(false);
                                    rootPstmt = connRoot.prepareStatement(String.format(insertSqlTemplate, StrUtil.wrap(rootTableName, "\"")));
                                }


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
                                    if (rootPstmt != null) {
                                        Row row = buildRow(pbfTuple, false, false, parameter);
                                        if (row != null) {
                                            // 规则：单条 >= 300KB，直接单独提交
                                            if (singleSize >= BATCH_SIZE_THRESHOLD) {
                                                // 先把之前批次提交掉
                                                if (!rootBatchRows.isEmpty()) {
                                                    rootBatchNum++;
                                                    executeBatchInsert(rootPstmt, rootBatchRows);
                                                    connRoot.commit();
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
                                                executeBatchInsert(rootPstmt, rootBatchRows);
                                                connRoot.commit();
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
                                                    executeBatchInsert(rootPstmt, rootBatchRows);
                                                    connRoot.commit();
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
                                if (rootPstmt != null && !rootBatchRows.isEmpty()) {
                                    executeBatchInsert(rootPstmt, rootBatchRows);
                                    connRoot.commit();
                                    rootTotalCount.addAndGet(rootBatchRows.size());
                                    log.info("{}:分区内Root表最后批次写入完成，剩余条数：{}，累计总数：{}，提交大小：{} outGridSrid:{}",
                                            rootTableName, rootBatchRows.size(), rootTotalCount.get(),
                                            DataSizeUtil.format(currentBatchSize), outGridSrid);
                                }


                                // 最终日志
                                log.info("{} :分区内Root表最终完成，累计写入瓦片数：{} ,outGridSrid:{},edition:{}", rootTableName, rootTotalCount.get(), outGridSrid, edition);


                            } catch (Exception e) {
                                log.error("写入PG失败", e);
                                try {
                                    if (connRoot != null) connRoot.rollback();
                                } catch (Exception ignored) {
                                }
                                throw new RuntimeException(e);
                            } finally {
                                IoUtil.close(dataSource);
                                IoUtil.close(rootPstmt);
                                IoUtil.close(connRoot);
                            }
                        });


        // 日志汇总
        log.info(
                "所有图层流式写入完成，表名：Root={}, Label={}, Boundary={}",
                pgParams.get("tableName"),
                parameter.getTableNameLabel(),
                parameter.getTableNameBoundary());
    }

    /**
     * 执行批次插入
     */
    private void executeBatchInsert(PreparedStatement pstmt, List<Row> batchRows) throws Exception {
        log.info("====================批量提交开始===================");
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        for (Row row : batchRows) {
            // 按schema顺序设置参数
            pstmt.setString(1, IdUtil.getSnowflakeNextIdStr()); // z
            pstmt.setInt(2, row.getInt(0)); // z
            pstmt.setInt(3, row.getInt(1)); // x
            pstmt.setInt(4, row.getInt(2)); // tms_ y
            pstmt.setInt(5, row.getInt(3)); // y
            pstmt.setInt(6, row.getInt(4)); // grid_srid
            pstmt.setBytes(7, row.getAs(5)); // tile_data
            pstmt.setString(8, row.getString(6)); // layer_name
            pstmt.setString(9, row.getString(7)); // edition
            pstmt.setLong(10, System.currentTimeMillis()); // 时间戳
            pstmt.addBatch();
        }
        pstmt.executeBatch();
        stopWatch.stop();
        log.info("====================批量提交结束：耗时：{}，条数：{}=====", stopWatch.getLastTaskTimeMillis(), batchRows.size());
    }

    /**
     * 按ID分片读取PostGIS数据（仅此处persist rawFeatures）
     */
    private JavaRDD<GirAdvOneRow> readDataByIdPage(TileSliceParameter parameter) throws Exception {
        if (parameter == null || parameter.getInputConnectSimple() == null) {
            throw new IllegalArgumentException("输入参数不能为空，inputUrl必须配置");
        }
        PgConnectInfoSimple pgConnectInfo = parameter.getInputConnectSimple();
        DataSource dataSource = pgConnectInfo.toDataSource();
        IAdvExecutor iAdvExecutor = new AdvExecutorPG(dataSource);

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
     * 按BBox空间分片读取PostGIS数据（仅此处persist rawFeatures）
     */
    private JavaRDD<GirAdvOneRow> readDataByBBox(TileSliceParameter parameter) throws Exception {
        if (parameter == null) {
            throw new IllegalArgumentException("输入参数不能为空，inputUrl必须配置");
        }
        PgConnectInfoSimple pgConnectInfo = parameter.getInputConnectSimple();
        IAdvExecutor iAdvExecutor = new AdvExecutorPG(pgConnectInfo.toDataSource());

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

    private void createTableDDL(String tableName, TileSliceParameter parameter) {
        PgConnectInfoWithTable outPutConnectWithTable = parameter.getOutPutConnectWithTable();
        DataSource dataSource = outPutConnectWithTable.toDataSource();

        IAdvExecutor iAdvExecutor = new AdvExecutorPG(dataSource);
        String tableNameWithSchema = iAdvExecutor.tbGetTableNameWithSchema(tableName);
        boolean b = iAdvExecutor.dIsTableExists(tableName);
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
            iAdvExecutor.dExecuteDDL(sqlDDL, tableName, "创建表");
        }
    }
}
