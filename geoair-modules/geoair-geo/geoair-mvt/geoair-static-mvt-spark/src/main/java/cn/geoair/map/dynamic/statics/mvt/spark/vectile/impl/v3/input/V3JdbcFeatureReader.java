package cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v3.input;

import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.geoair.map.dynamic.adv.query.apo.BBoxApo;
import cn.geoair.map.dynamic.adv.query.apo.OrderApo;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsOrder;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.adv.spring.AdvExecutorFactory;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.ReadStrategy;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.DataSourceConfig;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.TileSliceParameter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.MvtLayerSliceParameter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.V3JdbcInputConfig;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v3.V3DataReadUtils;
import cn.hutool.core.collection.ListUtil;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.SparkSession;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * V3 JDBC 要素读取器，保持原 V3 的 ID 分页与 BBox 分区行为。
 *
 * @author 张逢吉
 */
final class V3JdbcFeatureReader implements V3FeatureReader {

    private static final int DEFAULT_READ_PARTITION = 20;

    @Override
    public JavaRDD<GirAdvOneRow> read(SparkSession sparkSession, MvtLayerSliceParameter layer) throws Exception {
        V3JdbcInputConfig jdbc = layer.getInputConfig().getJdbc();
        TileSliceParameter parameter = new TileSliceParameter()
                .setLayerName(layer.getLayerName())
                .setGeomFieldName(layer.getGeomFieldName())
                .setIdFieldName(layer.getIdFieldName())
                .setSourceDataSrid(layer.resolveSourceDataSrid())
                .setInputSource(jdbc.getDataSource())
                .setQueryStatement(jdbc.getQueryStatement())
                .setReadStrategy(jdbc.getReadStrategy())
                .setMaxPartionNum(jdbc.getMaxPartitionNum());
        ReadStrategy strategy = Optional.ofNullable(jdbc.getReadStrategy()).orElse(ReadStrategy.ID_PAGE);
        if (strategy == ReadStrategy.ID_PAGE) {
            return readByIdPage(sparkSession, parameter, layer.getLayerName());
        }
        if (strategy == ReadStrategy.BBOX) {
            return readByBbox(sparkSession, parameter, layer.getLayerName());
        }
        throw new IllegalArgumentException("V3 JDBC 图层 " + layer.getLayerName() + " 不支持读取策略: " + strategy);
    }

    private JavaRDD<GirAdvOneRow> readByIdPage(
            SparkSession sparkSession, TileSliceParameter parameter, String layerName) throws Exception {
        IAdvExecutor executor = AdvExecutorFactory.getAdvExecutorByDataSource(parameter.getInputSource().toDataSource());
        long totalCount = executor.pCount(parameter.getQueryStatement());
        if (totalCount <= 0) {
            throw new IllegalArgumentException("图层 " + layerName + " 查询结果为空");
        }
        int requested = Optional.ofNullable(parameter.getMaxPartionNum()).orElse(DEFAULT_READ_PARTITION);
        int partitionNum = (int) Math.max(1, Math.min((long) Math.max(1, requested), totalCount));
        int countPerTask = (int) Math.min(Integer.MAX_VALUE,
                Math.max(1L, (totalCount + partitionNum - 1L) / partitionNum));
        String orderField = parameter.getIdFieldName() == null || parameter.getIdFieldName().trim().isEmpty()
                ? parameter.getGeomFieldName() : parameter.getIdFieldName();
        List<Integer> pages = V3DataReadUtils.buildPageNumberList(totalCount, partitionNum);
        Dataset<Integer> dataSet = sparkSession.createDataset(pages, Encoders.INT())
                .repartition(Math.min(pages.size(), partitionNum));
        return dataSet.javaRDD().flatMap(new IdPageFunction(
                parameter, parameter.getQueryStatement(), orderField, countPerTask));
    }

    private JavaRDD<GirAdvOneRow> readByBbox(
            SparkSession sparkSession, TileSliceParameter parameter, String layerName) throws Exception {
        IAdvExecutor executor = AdvExecutorFactory.getAdvExecutorByDataSource(parameter.getInputSource().toDataSource());
        BBoxApo extent = executor.eGetExtent(parameter.getQueryStatement(), parameter.getGeomFieldName());
        if (extent == null) {
            throw new IllegalArgumentException("图层 " + layerName + " 无法获取空间范围");
        }
        int partitionNum = Math.max(1,
                Optional.ofNullable(parameter.getMaxPartionNum()).orElse(DEFAULT_READ_PARTITION));
        List<String> conditions = V3DataReadUtils.buildBboxPartitionConditions(
                extent, partitionNum, parameter.getSourceDataSrid());
        Dataset<String> dataSet = sparkSession.createDataset(conditions, Encoders.STRING())
                .repartition(conditions.size());
        return dataSet.javaRDD().flatMap(new BboxFunction(
                parameter, parameter.getQueryStatement(), parameter.getGeomFieldName(),
                parameter.getSourceDataSrid()));
    }

    /** Spark executor 中执行的 JDBC 分页读取函数。 */
    private static final class IdPageFunction implements FlatMapFunction<Integer, GirAdvOneRow> {
        private static final long serialVersionUID = 1L;
        private final TileSliceParameter parameter;
        private final String queryStatement;
        private final String orderFieldName;
        private final int countPerTask;

        private IdPageFunction(TileSliceParameter parameter, String queryStatement,
                String orderFieldName, int countPerTask) {
            this.parameter = parameter;
            this.queryStatement = queryStatement;
            this.orderFieldName = orderFieldName;
            this.countPerTask = countPerTask;
        }

        @Override
        public Iterator<GirAdvOneRow> call(Integer pageNum) {
            DataSourceConfig source = parameter.getInputSource();
            IAdvExecutor executor = AdvExecutorFactory.getAdvExecutorByDataSource(source.toDataSource());
            String orderSql = executor.pBuildSqlWithOrder(queryStatement,
                    ListUtil.of(OrderApo.create(orderFieldName, AdvEnumsOrder.升序)));
            return executor.bSelectList(
                    executor.pBuildPageSql(orderSql, countPerTask, pageNum, true)).iterator();
        }
    }

    /** Spark executor 中执行的 JDBC 空间范围读取函数。 */
    private static final class BboxFunction implements FlatMapFunction<String, GirAdvOneRow> {
        private static final long serialVersionUID = 1L;
        private final TileSliceParameter parameter;
        private final String queryStatement;
        private final String geomFieldName;
        private final int sourceDataSrid;

        private BboxFunction(TileSliceParameter parameter, String queryStatement,
                String geomFieldName, int sourceDataSrid) {
            this.parameter = parameter;
            this.queryStatement = queryStatement;
            this.geomFieldName = geomFieldName;
            this.sourceDataSrid = sourceDataSrid;
        }

        @Override
        public Iterator<GirAdvOneRow> call(String condition) {
            String[] coords = condition.split(",");
            String sql = V3DataReadUtils.buildBboxQuerySql(queryStatement, geomFieldName,
                    Double.parseDouble(coords[0]), Double.parseDouble(coords[2]),
                    Double.parseDouble(coords[1]), Double.parseDouble(coords[3]), sourceDataSrid);
            IAdvExecutor executor = AdvExecutorFactory.getAdvExecutorByDataSource(
                    parameter.getInputSource().toDataSource());
            return executor.bSelectList(sql).iterator();
        }
    }
}
