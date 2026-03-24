package cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.apache.spark.api.java.JavaFutureAction;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.VoidFunction;
import org.apache.spark.sql.*;
import org.apache.spark.storage.StorageLevel;

import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.geoair.map.dynamic.adv.query.apo.BBoxApo;
import cn.geoair.map.dynamic.adv.query.dialect.pg.AdvExecutorPG;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsTypeGeom;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.mvt.tools.model.PbfInfo;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.ReadStrategy;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.PbfTargetInfo;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.PgConnectInfo;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.TileSliceParameter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.statistics.StatisticUtils;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.utils.DataReadCommonUtils;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.utils.SparkTaskSerializableUtil;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.utils.VectorTileCommonUtils;

import cn.hutool.core.lang.caller.CallerUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import lombok.extern.slf4j.Slf4j;
import scala.Tuple2;
import scala.collection.JavaConverters;
import scala.collection.Seq;
import scala.reflect.ClassTag;
import scala.reflect.ClassTag$;

@Slf4j
public class SparkVectorTileGeneratorAll implements Serializable {

	private transient SparkSession sparkSession;

	private static final int DEFAULT_MAX_PARTITION = 20; // 这里是初始的分区数量

	// 1000 个分区代表总共有 1000 个任务要执行，但 Spark 会根据集群的并行度（由 Executor 数量、每个 Executor
	// 的核心数决定）来控制「同时运行的任务数」；
	// 执行节点只有 8 个：假设每个节点只运行 1 个 Executor、每个 Executor 分配 1 个核心，那么集群的最大并行度就是 8—— 同一时间最多只有 8
	// 个分区任务在执行；
	// 任务分批执行：先执行 8 个分区的任务（创建 8 个数据库连接），其中一个任务执行完成后，再调度第 9 个分区的任务（创建第 9 个连接，此时前 8 个中已有 1
	// 个释放）。
	// 总结： 分区数量越大，单个运行的分区的内存占用越小，越不容易oom
	private static final int DEFAULT_REDUCE_PARTITION = 800;

	private static final int TILE_BATCH_SIZE = 200;

	public SparkVectorTileGeneratorAll(SparkSession sparkSession) {
		this.sparkSession = sparkSession;
	}

	/**
	 * 边生成边写入，无全量缓存（极致内存优化版）
	 */
	public void doGenerate(TileSliceParameter parameter) throws Exception {
		JSONObject entries = JSONUtil.parseObj(parameter);
		entries.remove("inputConnectInfo");
		entries.remove("outPutConnectInfo");
		entries.remove("outPutUrl");
		entries.remove("inputUrl");
		Log log = LogFactory.get(CallerUtil.getCallerCaller());
		log.info("{} 执行器开始切片（流式逐批写入模式），切片参数信息：\n {}", this.getClass().getSimpleName(), entries.toStringPretty());

		// 1. 读取数据（仅此处持久化，避免重复读取PG）
		JavaRDD<GirAdvOneRow> rawFeatures = null;
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
		// 仅此处持久化，避免重复读取PG
		JavaRDD<GirAdvOneRow> persistedFeaturesRDD = rawFeatures.persist(StorageLevel.MEMORY_AND_DISK());
		long count = persistedFeaturesRDD.count();
		log.info("查询得到的所有的要素数量为:{}", count);

		// 下面的所有步骤不要加persist ，不然可能会oom，然后也不要加行动算子，不然会阻塞，导致切片时间变长

		// 2. 空间转换（无持久化，流式处理）
		JavaRDD<GirAdvOneRow> transformedFeatures = persistedFeaturesRDD
				.map(new SparkTaskSerializableUtil.TransformFeatureFunction(parameter));

		// 3. 要素映射到瓦片（无持久化）
		JavaPairRDD<String, List<GirAdvOneRow>> tileFeatures = transformedFeatures
				.flatMapToPair(new SparkTaskSerializableUtil.MapToTileFunction(parameter));

		// spark的统计流程
		if (parameter.isStatisticsIs()) {
			JavaPairRDD<String, List<GirAdvOneRow>> tileFeaturesByZoom = transformedFeatures.flatMapToPair(
					new SparkTaskSerializableUtil.MapToTileFunctionToStatic(parameter, parameter.getMaxZoom()));
			TileSliceParameter copy = parameter.copy();
			copy.setFeatureLimit(1000).setEnableFeatureLimitIs(true).setDropDensestAsNeeded(false)
					.setCoalesceDensestAsNeeded(false);
			JavaPairRDD<String, List<GirAdvOneRow>> aggregatedRDDByZoom = tileFeaturesByZoom.reduceByKey(
					new SparkTaskSerializableUtil.AggregateAndLimitFeatureFunction(copy), DEFAULT_REDUCE_PARTITION);
			PbfTargetInfo instance = PbfTargetInfo.getInstance();
			instance.setSaveFeatureList(true);
			JavaRDD<Tuple2<String, PbfInfo>> pbfRDD = aggregatedRDDByZoom
					.map(new SparkTaskSerializableUtil.GeneratePbfFunction(parameter, instance));
			// 抽样500个pbf瓦片,然后转成rdd，避免oom，无解
			JavaFutureAction<List<Tuple2<String, PbfInfo>>> listJavaFutureAction = pbfRDD.takeAsync(500);
			log.info("抽样500条数据完成！");
			List<Tuple2<String, PbfInfo>> tuple2s = listJavaFutureAction.get();
			Seq<Tuple2<String, PbfInfo>> tuple2Seq = JavaConverters.asScalaIteratorConverter(tuple2s.iterator())
					.asScala().toSeq();
			ClassTag<Tuple2<String, PbfInfo>> classTag = ClassTag$.MODULE$.apply(Tuple2.class);
			JavaRDD<Tuple2<String, PbfInfo>> tuple2sRDD = sparkSession.sparkContext()
					.parallelize(tuple2Seq, DEFAULT_REDUCE_PARTITION, classTag).toJavaRDD();
			AdvEnumsTypeGeom typeGeom = parameter.getTypeGeom();
			String geomType = typeGeom != null ? typeGeom.name() : "Unknown";
			StatisticUtils.statAndWriteJson(tuple2sRDD, geomType, parameter.getStaticTableName(), parameter, count,
					sparkSession);
		}

		// 4. 聚合
		JavaPairRDD<String, List<GirAdvOneRow>> aggregatedRDD = tileFeatures.reduceByKey(
				new SparkTaskSerializableUtil.AggregateAndLimitFeatureFunction(parameter), DEFAULT_REDUCE_PARTITION);

		Map<String, String> pgParams = VectorTileCommonUtils.buildPgWriteParams(parameter);

		if (parameter.isCreateLabel() && StrUtil.isNotBlank(parameter.getTableNameLabel())) {
			log.info("开始流式写入Label图层，表名：{}", parameter.getTableNameLabel());
			createTableDDL(parameter.getTableNameLabel(), parameter);
		}

		if (parameter.isCreateBoundary() && StrUtil.isNotBlank(parameter.getTableNameBoundary())) {
			log.info("开始流式写入Boundary图层，表名：{}", parameter.getTableNameBoundary());
			createTableDDL(parameter.getTableNameBoundary(), parameter);
		}
		// 主图层写入
		log.info("开始流式写入主图层，表名：{}", pgParams.get("tableName"));
		createTableDDL(pgParams.get("tableName"), parameter);

		streamWriteToPg(aggregatedRDD, parameter, pgParams);

		log.info("流式边生成边写入流程执行完成");
	}

	private void streamWriteToPg(JavaPairRDD<String, List<GirAdvOneRow>> aggregatedRDD, TileSliceParameter parameter,
			Map<String, String> pgParams) {

		// 1. 生成PBF并在分区内逐批构建Row、批量写入三个表
		aggregatedRDD
				.foreachPartition((VoidFunction<Iterator<Tuple2<String, List<GirAdvOneRow>>>>) partitionIterator -> {
					Log log = Log.get();
					Connection conn = null;

					// 1. Root表（主表）
					PreparedStatement rootPstmt = null;
					List<Row> rootBatchRows = new ArrayList<>(TILE_BATCH_SIZE);
					AtomicLong rootBatchCount = new AtomicLong(0);
					String rootTableName = pgParams.get("tableName");

					// 2. Label表
					PreparedStatement labelPstmt = null;
					List<Row> labelBatchRows = new ArrayList<>(TILE_BATCH_SIZE);
					AtomicLong labelBatchCount = new AtomicLong(0);
					String labelTableName = parameter.getTableNameLabel();

					// 3. Boundary表
					PreparedStatement boundaryPstmt = null;
					List<Row> boundaryBatchRows = new ArrayList<>(TILE_BATCH_SIZE);
					AtomicLong boundaryBatchCount = new AtomicLong(0);
					String boundaryTableName = parameter.getTableNameBoundary();

					try {
						// 初始化PG连接
						Class.forName("org.postgresql.Driver");
						conn = DriverManager.getConnection(pgParams.get("url"), pgParams.get("user"),
								pgParams.get("password"));
						conn.setAutoCommit(false); // 关闭自动提交，批量写入

						// ===== 初始化三个表的PreparedStatement =====
						String insertSqlTemplate = "INSERT INTO %s (id, z, x, tms_y, y, grid_srid, tile_data, layer_name, edition, insert_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
						// Root表
						if (StringUtils.isNotBlank(rootTableName)) {
							rootPstmt = conn.prepareStatement(
									String.format(insertSqlTemplate, StrUtil.wrap(rootTableName, "\"")));
						}
						// Label表
						if (StringUtils.isNotBlank(labelTableName)) {
							labelPstmt = conn.prepareStatement(
									String.format(insertSqlTemplate, StrUtil.wrap(labelTableName, "\"")));
						}
						// Boundary表
						if (StringUtils.isNotBlank(boundaryTableName)) {
							boundaryPstmt = conn.prepareStatement(
									String.format(insertSqlTemplate, StrUtil.wrap(boundaryTableName, "\"")));
						}

						// 初始化PBF生成函数
						SparkTaskSerializableUtil.GeneratePbfFunction pbfFunc = new SparkTaskSerializableUtil.GeneratePbfFunction(
								parameter, PbfTargetInfo.getInstance());

						// 遍历分区内所有数据
						while (partitionIterator.hasNext()) {
							Tuple2<String, List<GirAdvOneRow>> aggregatedTuple = partitionIterator.next();
							Tuple2<String, PbfInfo> pbfTuple = pbfFunc.call(aggregatedTuple);

							if (pbfTuple == null || pbfTuple._2 == null) {
								continue;
							}

							// ===== 分别构建三个表的Row并加入对应批次 =====
							// 1. 写入Root表
							if (StringUtils.isNotBlank(rootTableName)) {
								Row rootRow = buildRow(pbfTuple, false, false, parameter);
								if (rootRow != null) {
									rootBatchRows.add(rootRow);
									rootBatchCount.incrementAndGet();
									// 达到批次大小，写入Root表
									if (rootBatchRows.size() >= TILE_BATCH_SIZE) {
										executeBatchInsert(rootPstmt, rootBatchRows);
										rootBatchRows.clear();
										conn.commit();
										System.gc();
										log.info("分区内Root表批次写入完成，已写入瓦片数：{}", rootBatchCount.get());
									}
								}
							}

							// 2. 写入Label表
							if (StringUtils.isNotBlank(labelTableName)) {
								Row labelRow = buildRow(pbfTuple, true, false, parameter);
								if (labelRow != null) {
									labelBatchRows.add(labelRow);
									labelBatchCount.incrementAndGet();
									// 达到批次大小，写入Label表
									if (labelBatchRows.size() >= TILE_BATCH_SIZE) {
										executeBatchInsert(labelPstmt, labelBatchRows);
										labelBatchRows.clear();
										conn.commit();
										System.gc();
										log.info("分区内Label表批次写入完成，已写入瓦片数：{}", labelBatchCount.get());
									}
								}
							}

							// 3. 写入Boundary表
							if (StringUtils.isNotBlank(boundaryTableName)) {
								Row boundaryRow = buildRow(pbfTuple, false, true, parameter);
								if (boundaryRow != null) {
									boundaryBatchRows.add(boundaryRow);
									boundaryBatchCount.incrementAndGet();
									// 达到批次大小，写入Boundary表
									if (boundaryBatchRows.size() >= TILE_BATCH_SIZE) {
										executeBatchInsert(boundaryPstmt, boundaryBatchRows);
										boundaryBatchRows.clear();
										conn.commit();
										System.gc();
										log.info("分区内Boundary表批次写入完成，已写入瓦片数：{}", boundaryBatchCount.get());
									}
								}
							}

							PbfInfo pbfInfo = pbfTuple._2;
							pbfInfo.setDataBoundary(new byte[0]);
							pbfInfo.setData(new byte[0]);
							pbfInfo.setDataLabel(new byte[0]);
							List<GirAdvOneRow> girAdvOneRows = aggregatedTuple._2;
							girAdvOneRows.clear();
						}

						if (StringUtils.isNotBlank(rootTableName) && !rootBatchRows.isEmpty()) {
							executeBatchInsert(rootPstmt, rootBatchRows);
							conn.commit();
							log.info("分区内Root表剩余批次写入完成，累计写入瓦片数：{}", rootBatchCount.get());
						}
						// 2. Label表剩余数据
						if (StringUtils.isNotBlank(labelTableName) && !labelBatchRows.isEmpty()) {
							executeBatchInsert(labelPstmt, labelBatchRows);
							conn.commit();
							log.info("分区内Label表剩余批次写入完成，累计写入瓦片数：{}", labelBatchCount.get());
						}
						// 3. Boundary表剩余数据
						if (StringUtils.isNotBlank(boundaryTableName) && !boundaryBatchRows.isEmpty()) {
							executeBatchInsert(boundaryPstmt, boundaryBatchRows);
							conn.commit();
							log.info("分区内Boundary表剩余批次写入完成，累计写入瓦片数：{}", boundaryBatchCount.get());
						}

					}
					catch (Exception e) {
						log.error("分区内写入PG失败", e);
						// 事务回滚
						if (conn != null) {
							try {
								conn.rollback();
							}
							catch (Exception rollbackE) {
								log.error("事务回滚失败", rollbackE);
							}
						}
						throw new RuntimeException(e);
					}
					finally {
						// ===== 统一关闭所有资源 =====
						// 关闭PreparedStatement
						if (rootPstmt != null) {
							rootPstmt.close();
						}
						if (labelPstmt != null) {
							labelPstmt.close();
						}
						if (boundaryPstmt != null) {
							boundaryPstmt.close();
						}
						// 关闭连接
						if (conn != null) {
							conn.close();
						}
						// 释放内存
						rootBatchRows.clear();
						labelBatchRows.clear();
						boundaryBatchRows.clear();
					}
				});

		// 日志汇总
		log.info("所有图层流式写入完成，表名：Root={}, Label={}, Boundary={}", pgParams.get("tableName"),
				parameter.getTableNameLabel(), parameter.getTableNameBoundary());
	}

	/**
	 * 执行批次插入
	 */
	private void executeBatchInsert(PreparedStatement pstmt, List<Row> batchRows) throws Exception {
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
	}

	/**
	 * 按ID分片读取PostGIS数据（仅此处persist rawFeatures）
	 */
	private JavaRDD<GirAdvOneRow> readDataByIdPage(TileSliceParameter parameter) throws Exception {
		if (parameter == null || parameter.getInputConnectInfo() == null) {
			throw new IllegalArgumentException("输入参数不能为空，inputUrl必须配置");
		}
		PgConnectInfo pgConnectInfo = parameter.getInputConnectInfo();
		IAdvExecutor iAdvExecutor = new AdvExecutorPG(pgConnectInfo.toDataSource());

		long totalCount = iAdvExecutor.pCount(parameter.getQueryStatement());
		if (totalCount == 0) {
			throw new RuntimeException("查询结果为空，无数据可处理");
		}
		int estimatedSingleRowSizeKB = 10;
		int maxMemoryPerPartitionMB = 256;
		int maxRowPerPartition = (maxMemoryPerPartitionMB * 1024) / estimatedSingleRowSizeKB;

		int maxPartionNum = Optional.ofNullable(parameter.getMaxPartionNum()).orElse(DEFAULT_MAX_PARTITION);
		int calcPartitionNum = (int) Math.ceil((double) totalCount / maxRowPerPartition);
		maxPartionNum = Math.min(Math.min(maxPartionNum, calcPartitionNum), (int) Math.max(1, totalCount));
		log.info("ID分页分片数量：{}（总数据量：{}，单分区最大条数：{}）", maxPartionNum, totalCount, maxRowPerPartition);

		int countPerTask = (int) Math.ceil((double) totalCount / maxPartionNum);
		countPerTask = Math.min(Math.max(countPerTask, 100), maxRowPerPartition);

		String orderFieldName = Optional.ofNullable(parameter.getIdFieldName()).filter(StrUtil::isNotBlank)
				.orElse(parameter.getGeomFieldName());

		List<Integer> pageNumbers = DataReadCommonUtils.buildPageNumberList(totalCount, maxPartionNum);
		Dataset<Integer> pageNumDs = sparkSession.createDataset(pageNumbers, Encoders.INT())
				.repartition(Math.min(pageNumbers.size(), maxPartionNum));
		JavaRDD<Integer> pageNumRdd = pageNumDs.javaRDD();

		JavaRDD<GirAdvOneRow> featureRDD = pageNumRdd.flatMap(new SparkTaskSerializableUtil.IdPageFlatMapFunction(
				parameter, parameter.getQueryStatement(), orderFieldName, countPerTask));
		return featureRDD;
	}

	/**
	 * 按BBox空间分片读取PostGIS数据（仅此处persist rawFeatures）
	 */
	private JavaRDD<GirAdvOneRow> readDataByBBox(TileSliceParameter parameter) throws Exception {
		if (parameter == null) {
			throw new IllegalArgumentException("输入参数不能为空，inputUrl必须配置");
		}
		PgConnectInfo pgConnectInfo = parameter.getInputConnectInfo();
		IAdvExecutor iAdvExecutor = new AdvExecutorPG(pgConnectInfo.toDataSource());

		BBoxApo bBoxApo = iAdvExecutor.eGetExtent(parameter.getQueryStatement(), parameter.getGeomFieldName());
		if (bBoxApo == null) {
			throw new RuntimeException("无法获取数据的空间范围");
		}

		int maxPartionNum = Optional.ofNullable(parameter.getMaxPartionNum()).orElse(100);
		List<String> partitionConditions = DataReadCommonUtils.buildBboxPartitionConditions(bBoxApo, maxPartionNum,
				parameter.getSourceDataSrid());
		log.info("BBox分片数量：" + partitionConditions.size());
		Dataset<String> partitionConditionsDs = sparkSession.createDataset(partitionConditions, Encoders.STRING())
				.repartition(partitionConditions.size());
		JavaRDD<String> partitionConditionRdd = partitionConditionsDs.javaRDD();

		JavaRDD<GirAdvOneRow> featureRDD = partitionConditionRdd
				.flatMap(new SparkTaskSerializableUtil.BboxFlatMapFunction(parameter, parameter.getQueryStatement(),
						parameter.getGeomFieldName(), parameter.getSourceDataSrid()));
		return featureRDD;
	}

	private Row buildRow(Tuple2<String, PbfInfo> tuple, boolean isLabel, boolean isBoundary,
			TileSliceParameter parameter) throws Exception {
		if (isLabel) {
			if (tuple._2.getDataLabel() == null)
				return null;
			return new SparkTaskSerializableUtil.BuildRowLabelFunction(parameter).call(tuple);
		}
		else if (isBoundary) {
			if (tuple._2.getDataBoundary() == null)
				return null;
			return new SparkTaskSerializableUtil.BuildRowBoundaryFunction(parameter).call(tuple);
		}
		else {
			if (tuple._2.getData() == null)
				return null;
			return new SparkTaskSerializableUtil.BuildRowFunction(parameter).call(tuple);
		}
	}

	private void createTableDDL(String tableName, TileSliceParameter parameter) {
		DataSource dataSource = parameter.getOutPutConnectInfo().toDataSource();
		IAdvExecutor iAdvExecutor = new AdvExecutorPG(dataSource);
		boolean b = iAdvExecutor.dIsTableExists(tableName);
		String tempLate = "CREATE TABLE \"{tableName}\" (\n" + "  \"id\" text COLLATE \"pg_catalog\".\"default\",\n"
				+ "  \"z\" int4,\n" + "  \"x\" int4,\n" + "  \"tms_y\" int4,\n" + "  \"y\" int4,\n"
				+ "  \"grid_srid\" int4,\n" + "  \"tile_data\" bytea,\n"
				+ "  \"layer_name\" text COLLATE \"pg_catalog\".\"default\",\n"
				+ "  \"edition\" text COLLATE \"pg_catalog\".\"default\",\n" + "  \"insert_time\" int8 \n " + ")\n"
				+ ";";
		if (!b) {
			log.info("检测到表不存在，执行创建表动作！");
			String sqlDDL = tempLate.replace("{tableName}", tableName);
			iAdvExecutor.dExecuteDDL(sqlDDL, tableName, "创建表");
		}

	}

}
