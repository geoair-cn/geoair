package cn.geoair.map.dynamic.statics.mvt.v4;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.dynamic.mvt.tools.model.PbfInfo;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.DataSourceConfig;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.MultiLayerTileSliceParameter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.V3TileOutputConfig;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.V3TileOutputType;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v3.MultiLayerMvtEncoderV3;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v3.V3TileFeatureGroup;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v3.output.V3TileStore;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v3.output.V3TileStoreFactory;
import cn.geoair.map.dynamic.tools.GirGeoTools;
import cn.geoair.map.dynamic.tools.grid.dto.TileYAxis;
import cn.geoair.map.dynamic.tools.grid.dto.TileZxyApo;
import cn.hutool.core.util.IdUtil;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * V4 的瓦片写出器：把聚合单元编码成 PBF 并落到目标介质。
 *
 * <p>编码调用的是 <b>V3 的同一个编码器</b>（{@link MultiLayerMvtEncoderV3#encode}），
 * 因此超限降级、属性输出规则、几何简化等行为与 V3 完全一致 —— 这是 V4 与 V3 产物可比的前提。</p>
 *
 * <p>与 V3 的差别只在执行方式：V3 在 {@code foreachPartition} 里每个分区开一个存储会话，
 * V4 是单线程顺序写，全程只开一个会话（PostgreSQL 则是一条连接 + 批量提交）。</p>
 *
 * @author 张逢吉
 */
final class V4TileWriter implements Closeable {

    private static final GiLogger LOG = GirLoggerFactory.getLogger();
    private static final int WRITE_BATCH_SIZE = 300;

    private final MultiLayerTileSliceParameter parameter;
    private final V4SliceProgress progress;
    private final boolean postgresql;
    private final V3TileOutputConfig outputConfig;

    private V3TileStore store;
    private Connection connection;
    private PreparedStatement deleteStatement;
    private PreparedStatement insertStatement;
    private int pendingBatch;
    private long pendingBytes;

    V4TileWriter(MultiLayerTileSliceParameter parameter, V4SliceProgress progress) throws Exception {
        this.parameter = parameter;
        this.progress = progress;
        this.outputConfig = parameter.getOutputConfig();
        this.postgresql = resolveOutputType(parameter) == V3TileOutputType.POSTGRESQL;
        if (postgresql) {
            openPostgresql();
        } else {
            this.store = V3TileStoreFactory.open(outputConfig);
        }
    }

    /** 编码并写出一个瓦片；空瓦片不写、不计数（与 V3 的 isBlankTile 行为一致）。 */
    void write(String tileKey, V3TileFeatureGroup group) throws Exception {
        PbfInfo pbf = MultiLayerMvtEncoderV3.encode(tileKey, group, parameter);
        if (pbf == null || pbf.getData() == null || pbf.getData().length == 0) {
            return;
        }
        if (postgresql) {
            bindDelete(tileKey, pbf);
            bindInsert(tileKey, pbf);
            pendingBatch++;
            pendingBytes += pbf.getData().length;
            if (pendingBatch >= WRITE_BATCH_SIZE) {
                flushPostgresql();
            }
            return;
        }
        TileZxyApo zxy = GirGeoTools.defaultInstance().getTileGridBingMapOpt().quadKeyToXyz(tileKey);
        int outputY = outputConfig.getTileYAxis() == TileYAxis.TMS
                ? getTmsY(zxy, pbf.getGridSrid()) : zxy.getY();
        store.writeTile(zxy.getZ(), zxy.getX(), outputY, pbf.getData(), parameter.isGzipPbf());
        progress.addTile(pbf.getData().length);
    }

    /** 写入任务级元数据（PostgreSQL 输出不写，与 V3 一致）。 */
    void writeMetadata(String metadataJson) throws Exception {
        if (postgresql || store == null || !outputConfig.isWriteMetadata()) {
            return;
        }
        store.writeMetadata(metadataJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private void openPostgresql() throws Exception {
        DataSourceConfig output = parameter.getOutputConfig().getPostgresqlOutputSource();
        String table = output.getTableNameForSql();
        connection = output.toDataSource().getConnection();
        connection.setAutoCommit(false);
        deleteStatement = connection.prepareStatement(
                "DELETE FROM " + table + " WHERE z=? AND x=? AND y=? AND grid_srid=? AND layer_name=?"
                        + " AND COALESCE(edition, '') = COALESCE(?, '')");
        insertStatement = connection.prepareStatement(
                "INSERT INTO " + table + " (id,z,x,tms_y,y,grid_srid,tile_data,layer_name,edition,insert_time)"
                        + " VALUES (?,?,?,?,?,?,?,?,?,?)");
    }

    private void bindDelete(String tileKey, PbfInfo pbf) throws Exception {
        TileZxyApo zxy = GirGeoTools.defaultInstance().getTileGridBingMapOpt().quadKeyToXyz(tileKey);
        deleteStatement.setInt(1, zxy.getZ());
        deleteStatement.setInt(2, zxy.getX());
        deleteStatement.setInt(3, zxy.getY());
        deleteStatement.setInt(4, pbf.getGridSrid());
        deleteStatement.setString(5, parameter.getTileSetName());
        deleteStatement.setString(6, parameter.getEdition());
        deleteStatement.addBatch();
    }

    private void bindInsert(String tileKey, PbfInfo pbf) throws Exception {
        TileZxyApo zxy = GirGeoTools.defaultInstance().getTileGridBingMapOpt().quadKeyToXyz(tileKey);
        insertStatement.setString(1, IdUtil.getSnowflakeNextIdStr());
        insertStatement.setInt(2, zxy.getZ());
        insertStatement.setInt(3, zxy.getX());
        insertStatement.setInt(4, getTmsY(zxy, pbf.getGridSrid()));
        insertStatement.setInt(5, zxy.getY());
        insertStatement.setInt(6, pbf.getGridSrid());
        insertStatement.setBytes(7, pbf.getData());
        insertStatement.setString(8, parameter.getTileSetName());
        insertStatement.setString(9, parameter.getEdition());
        insertStatement.setLong(10, System.currentTimeMillis());
        insertStatement.addBatch();
    }

    private void flushPostgresql() throws Exception {
        if (pendingBatch <= 0) {
            return;
        }
        deleteStatement.executeBatch();
        insertStatement.executeBatch();
        connection.commit();
        progress.addTiles(pendingBatch, pendingBytes);
        progress.addBatch();
        pendingBatch = 0;
        pendingBytes = 0L;
    }

    /** PostgreSQL 输出恒写 TMS 行号，与 V3 一致。 */
    private static int getTmsY(TileZxyApo zxy, int gridSrid) {
        if (gridSrid == 3857) {
            return GirGeoTools.defaultInstance().getTileGrid3857Opt()
                    .convertY(zxy.getZ(), zxy.getY(), TileYAxis.XYZ, TileYAxis.TMS);
        }
        return GirGeoTools.defaultInstance().getTileGrid4326SeparateOpt()
                .convertY(zxy.getZ(), zxy.getY(), TileYAxis.XYZ, TileYAxis.TMS);
    }

    private static V3TileOutputType resolveOutputType(MultiLayerTileSliceParameter parameter) {
        if (parameter.getOutputConfig() == null || parameter.getOutputConfig().getOutputType() == null) {
            throw new IllegalArgumentException("V4 outputConfig 与 outputType 不能为空");
        }
        return parameter.getOutputConfig().getOutputType();
    }

    @Override
    public void close() {
        if (postgresql) {
            try {
                flushPostgresql();
                connection.setAutoCommit(true);
            } catch (Exception e) {
                LOG.error("V4 PostgreSQL 收尾提交失败", e);
            }
            closeQuietly(insertStatement);
            closeQuietly(deleteStatement);
            closeQuietly(connection);
            return;
        }
        if (store != null) {
            try {
                store.close();
            } catch (Exception e) {
                LOG.warn("V4 存储会话关闭失败: {}", e.getMessage());
            }
        }
    }

    private static void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception e) {
            LOG.warn("V4 资源关闭失败: {}", e.getMessage());
        }
    }
}
