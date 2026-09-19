package cn.geoair.map.dynamic.statics.mvt.v4.input;

import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.geoair.map.dynamic.adv.query.apo.BBoxApo;
import cn.geoair.map.dynamic.adv.query.apo.OrderApo;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsOrder;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.adv.spring.AdvExecutorFactory;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.ReadStrategy;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.MvtLayerSliceParameter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.V3JdbcInputConfig;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v3.V3DataReadUtils;
import cn.hutool.core.collection.ListUtil;

import java.util.List;
import java.util.Optional;

/**
 * V4 JDBC 要素读取器：保留 V3 的 ID 分页与空间范围（BBOX）两种策略。
 *
 * <p>SQL 拼装完全复用 {@link V3DataReadUtils}，执行走 {@link IAdvExecutor}，
 * 因此与 V3 读到的是<b>同样的语句、同样的行</b>；差别只在执行方式 ——
 * V3 把分页/范围切成 Spark 分区并行读，V4 是单机顺序读（数据量决定顺序读是否够用，
 * 这一点在 {@code V4开发计划.md} 的风险一节中记录）。</p>
 *
 * <p>方言由 {@link AdvExecutorFactory} 按数据源的产品名动态分派，
 * 因此 MySQL / PostgreSQL / Kingbase / OpenGauss / SQLServer / Oracle / 达梦 / SQLite 都可用；
 * 其中 BBOX 策略拼接的是 PostGIS 的空间函数，与 V3 有同样的限制。</p>
 *
 * @author 张逢吉
 */
public final class V4JdbcFeatureReader implements V4FeatureReader {

    private static final int DEFAULT_PARTITION = 20;

    @Override
    public void read(MvtLayerSliceParameter layer, V4RowConsumer consumer) throws Exception {
        V3JdbcInputConfig jdbc = layer.getInputConfig().getJdbc();
        IAdvExecutor executor = AdvExecutorFactory.getAdvExecutorByDataSource(
                jdbc.getDataSource().toDataSource());
        ReadStrategy strategy = Optional.ofNullable(jdbc.getReadStrategy()).orElse(ReadStrategy.ID_PAGE);
        if (strategy == ReadStrategy.ID_PAGE) {
            readByIdPage(executor, layer, jdbc, consumer);
            return;
        }
        if (strategy == ReadStrategy.BBOX) {
            readByBbox(executor, layer, jdbc, consumer);
            return;
        }
        throw new IllegalArgumentException(
                "V4 JDBC 图层 " + layer.getLayerName() + " 不支持读取策略: " + strategy);
    }

    /** ID 分页：先数总数，再按页顺序取。与 V3 的分页边界算法同一份实现。 */
    private void readByIdPage(IAdvExecutor executor, MvtLayerSliceParameter layer,
            V3JdbcInputConfig jdbc, V4RowConsumer consumer) throws Exception {
        String query = jdbc.getQueryStatement();
        long totalCount = executor.pCount(query);
        if (totalCount <= 0) {
            throw new IllegalArgumentException("图层 " + layer.getLayerName() + " 查询结果为空");
        }
        int requested = Optional.ofNullable(jdbc.getMaxPartitionNum()).orElse(DEFAULT_PARTITION);
        int partitionNum = (int) Math.max(1, Math.min((long) Math.max(1, requested), totalCount));
        int countPerTask = (int) Math.min(Integer.MAX_VALUE,
                Math.max(1L, (totalCount + partitionNum - 1L) / partitionNum));
        String orderField = layer.getIdFieldName() == null || layer.getIdFieldName().trim().isEmpty()
                ? layer.getGeomFieldName() : layer.getIdFieldName();
        String orderSql = executor.pBuildSqlWithOrder(query,
                ListUtil.of(OrderApo.create(orderField, AdvEnumsOrder.升序)));
        List<Integer> pages = V3DataReadUtils.buildPageNumberList(totalCount, partitionNum);
        for (Integer pageNum : pages) {
            List<GirAdvOneRow> rows = executor.bSelectList(
                    executor.pBuildPageSql(orderSql, countPerTask, pageNum, true));
            if (rows == null) {
                continue;
            }
            for (GirAdvOneRow row : rows) {
                if (row != null) {
                    consumer.accept(row);
                }
            }
        }
    }

    /** 空间范围：先取整体范围，再切成若干矩形条件分块读。 */
    private void readByBbox(IAdvExecutor executor, MvtLayerSliceParameter layer,
            V3JdbcInputConfig jdbc, V4RowConsumer consumer) throws Exception {
        String query = jdbc.getQueryStatement();
        BBoxApo extent = executor.eGetExtent(query, layer.getGeomFieldName());
        if (extent == null) {
            throw new IllegalArgumentException("图层 " + layer.getLayerName() + " 无法获取空间范围");
        }
        int partitionNum = Math.max(1,
                Optional.ofNullable(jdbc.getMaxPartitionNum()).orElse(DEFAULT_PARTITION));
        int sourceSrid = layer.resolveSourceDataSrid();
        String fromSource = toFromSource(query);
        List<String> conditions = V3DataReadUtils.buildBboxPartitionConditions(
                extent, partitionNum, sourceSrid);
        for (String condition : conditions) {
            String[] coords = condition.split(",");
            String sql = V3DataReadUtils.buildBboxQuerySql(fromSource, layer.getGeomFieldName(),
                    Double.parseDouble(coords[0]), Double.parseDouble(coords[2]),
                    Double.parseDouble(coords[1]), Double.parseDouble(coords[3]), sourceSrid);
            List<GirAdvOneRow> rows = executor.bSelectList(sql);
            if (rows == null) {
                continue;
            }
            for (GirAdvOneRow row : rows) {
                if (row != null) {
                    consumer.accept(row);
                }
            }
        }
    }

    /**
     * 把 {@code queryStatement} 转成可以接在 {@code FROM} 后面的表表达式。
     *
     * <p>{@link V3DataReadUtils#buildBboxQuerySql} 的写法是
     * {@code select * from {queryStatement} as ttt where ST_Intersects(...)}，
     * 也就是它把入参当成<b>表名</b>。但管理端下发的 {@code queryStatement} 是一条完整的
     * {@code SELECT}（{@code TileParameterBuilder.resolveQueryStatement} 生成
     * {@code SELECT * FROM schema.table}，用户也可以填自定义 SQL），
     * 直接拼进去会得到 {@code select * from SELECT * FROM ... as ttt} —— 实测 PostgreSQL 报
     * "语法错误 在 SELECT 或附近的"。（V3 的 {@code BboxFunction} 有同样的问题，
     * 只是管理端固定用 {@code ID_PAGE}，这条路径没被跑到过。）
     *
     * <p>因此这里判断一下：是查询就套一层括号变成派生表，是裸表名就原样传下去。</p>
     */
    private static String toFromSource(String queryStatement) {
        String trimmed = queryStatement == null ? "" : queryStatement.trim();
        boolean isQuery = trimmed.regionMatches(true, 0, "select", 0, 6)
                || trimmed.regionMatches(true, 0, "with", 0, 4);
        if (!isQuery) {
            return trimmed;
        }
        String body = trimmed.endsWith(";") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
        return "(" + body + ")";
    }
}
