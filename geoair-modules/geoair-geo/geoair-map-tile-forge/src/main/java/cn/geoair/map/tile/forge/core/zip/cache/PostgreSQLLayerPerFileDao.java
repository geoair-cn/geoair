package cn.geoair.map.tile.forge.core.zip.cache;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.base.util.GutilObject;
import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamMap;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.tile.forge.core.caches.CacheProvider;
import cn.geoair.map.tile.forge.core.caches.NoOpCacheProvider;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.hutool.core.collection.ListUtil;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class PostgreSQLLayerPerFileDao implements LayerPerFileDao, AutoCloseable {
    public static GiLogger log = GirLoggerFactory.getLogger();
    protected final String dataId;
    protected static final String CACHE_STATUS_TABLE = "cache_status";
    private final String layerCacheTableName;
    private final IAdvExecutor iAdvExecutor;

    public enum CacheStatus {
        NOT_CACHED,
        CACHING,
        CACHED,
        CACHE_FAILED
    }

    public PostgreSQLLayerPerFileDao(IAdvExecutor iAdvExecutor, String dataId) {
        this.dataId = dataId;
        this.iAdvExecutor = iAdvExecutor;
        this.layerCacheTableName = "tile_central_directory_" + dataId;
        try {
            init();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void init() throws SQLException {
        createLayerTableIfNotExists();
        initCacheStatus();
    }

    private void createLayerTableIfNotExists() throws SQLException {
        if (iAdvExecutor.dIsTableExists(layerCacheTableName)) {
            return;
        }
        String createTableSql =
                String.format(
                        "CREATE TABLE IF NOT EXISTS %s ("
                                + "id BIGSERIAL PRIMARY KEY, "
                                + "local_header_offset BIGINT NOT NULL, "
                                + "data_offset BIGINT, "
                                + "compression_method BIGINT NOT NULL, "
                                + "compressed_size BIGINT NOT NULL, "
                                + "uncompressed_size BIGINT NOT NULL, "
                                + "name TEXT, "
                                + "entry_size INTEGER NOT NULL, "
                                + "directory_is BOOLEAN NOT NULL, "
                                + "xyz_path TEXT, "
                                + "x TEXT, "
                                + "y TEXT, "
                                + "z TEXT, "
                                + "file_name TEXT"
                                + ")",
                        layerCacheTableName);

        String createIndexSql =
                String.format(
                        "CREATE INDEX IF NOT EXISTS idx_%s_xyz_path ON %s(xyz_path);"
                                + "CREATE INDEX IF NOT EXISTS idx_%s_xyz ON %s(x,y,z);"
                                + "CREATE INDEX IF NOT EXISTS idx_%s_file ON %s(file_name)",
                        dataId,
                        layerCacheTableName,
                        dataId,
                        layerCacheTableName,
                        dataId,
                        layerCacheTableName);

        iAdvExecutor.dExecuteDDL(createTableSql, layerCacheTableName, "创建图层缓存表");
        iAdvExecutor.dExecuteDDL(createIndexSql, layerCacheTableName + "_index", "创建索引");
    }

    private void initCacheStatus() throws SQLException {
        String createStatusTableSql =
                String.format(
                        "CREATE TABLE IF NOT EXISTS %s ("
                                + "id BIGSERIAL PRIMARY KEY, "
                                + "layer_name TEXT UNIQUE NOT NULL, "
                                + "cache_status TEXT NOT NULL, "
                                + "cache_time TIMESTAMP, "
                                + "cache_size BIGINT, "
                                + "last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                                + ")",
                        CACHE_STATUS_TABLE);
        if (!iAdvExecutor.dIsTableExists(CACHE_STATUS_TABLE)) {
            iAdvExecutor.dExecuteDDL(createStatusTableSql, CACHE_STATUS_TABLE, "创建状态表");
        }
        String checkSql =
                String.format("SELECT 1 FROM %s WHERE layer_name = #{dataId}", CACHE_STATUS_TABLE);
        Number exists =
                iAdvExecutor.bSelectNumber(checkSql, SqlParamMap.of().addOne("dataId", dataId));

        if (exists == null || exists.intValue() == 0) {
            String insertSql =
                    String.format(
                            "INSERT INTO %s (layer_name, cache_status) VALUES (#{dataId}, #{status})",
                            CACHE_STATUS_TABLE);

            iAdvExecutor.bInsertBySql(
                    insertSql,
                    SqlParamMap.of()
                            .addOne("dataId", dataId)
                            .addOne("status", CacheStatus.NOT_CACHED.name()));
            log.info("图层{}缓存状态已初始化为：NOT_CACHED", dataId);
        }
    }

    public void updateCacheStatus(CacheStatus status) throws SQLException {
        updateCacheStatus(status, 0);
    }

    public void updateCacheStatus(CacheStatus status, long cacheSize) throws SQLException {

        String sql =
                String.format(
                        "UPDATE %s SET "
                                + "cache_status = #{status}, "
                                + "last_update = CURRENT_TIMESTAMP, "
                                + "cache_time = CASE WHEN #{status} = 'CACHED' THEN CURRENT_TIMESTAMP ELSE cache_time END, "
                                + "cache_size = CASE WHEN #{status} = 'CACHED' THEN #{cacheSize} ELSE cache_size END "
                                + "WHERE layer_name = #{dataId}",
                        CACHE_STATUS_TABLE);

        iAdvExecutor.bUpdateBySql(
                sql,
                SqlParamMap.of()
                        .addOne("status", status.name())
                        .addOne("cacheSize", cacheSize)
                        .addOne("dataId", dataId));

        log.info("图层{}状态已更新为：{}", dataId, status);
    }

    public CacheStatus getCacheStatus() throws SQLException {
        String sql =
                String.format(
                        "SELECT cache_status FROM %s WHERE layer_name = #{dataId}",
                        CACHE_STATUS_TABLE);
        GirAdvOneRow girAdvOneRow =
                iAdvExecutor.bSelectOne(sql, SqlParamMap.of().addOne("dataId", dataId));
        String cacheStatus = girAdvOneRow.getStr("cache_status");
        return cacheStatus == null ? CacheStatus.NOT_CACHED : CacheStatus.valueOf(cacheStatus);
    }

    @Override
    public String getTableName() {
        return layerCacheTableName;
    }

    public void insert(TileCentralDirectoryModel entry) throws SQLException {

        String sql =
                String.format(
                        "INSERT INTO %s ("
                                + "local_header_offset, data_offset, compression_method, compressed_size, "
                                + "uncompressed_size, name, entry_size, directory_is, xyz_path, x, y, z, file_name"
                                + ") VALUES ("
                                + "#{localHeaderOffset}, #{dataOffset}, #{compressionMethod}, #{compressedSize}, "
                                + "#{uncompressedSize}, #{name}, #{entrySize}, #{directoryIs}, #{xyzPath}, #{x}, #{y}, #{z}, #{fileName}"
                                + ")",
                        layerCacheTableName);

        iAdvExecutor.bInsertBySql(
                sql,
                SqlParamMap.of()
                        .addOne("localHeaderOffset", entry.getLocalHeaderOffset())
                        .addOne("dataOffset", entry.getDataOffset())
                        .addOne("compressionMethod", entry.getCompressionMethod())
                        .addOne("compressedSize", entry.getCompressedSize())
                        .addOne("uncompressedSize", entry.getUncompressedSize())
                        .addOne("name", entry.getName())
                        .addOne("entrySize", entry.getEntrySize())
                        .addOne("directoryIs", entry.isDirectoryIs())
                        .addOne("xyzPath", entry.getXyzPath())
                        .addOne("x", entry.getX())
                        .addOne("y", entry.getY())
                        .addOne("z", entry.getZ())
                        .addOne("fileName", entry.getFileName()));
    }

    @Override
    public void batchInsert(List<TileCentralDirectoryModel> entries) throws SQLException {
        if (entries == null || entries.isEmpty()) {
            return;
        }

        // 字段顺序（必须和下面map的key严格对应）
        List<String> headers =
                ListUtil.of(
                        "local_header_offset",
                        "data_offset",
                        "compression_method",
                        "compressed_size",
                        "uncompressed_size",
                        "name",
                        "entry_size",
                        "directory_is",
                        "xyz_path",
                        "x",
                        "y",
                        "z",
                        "file_name");

        // 组装批量数据
        List<Map<String, Object>> rowsData = new ArrayList<>();
        for (TileCentralDirectoryModel entry : entries) {
            Map<String, Object> row = new HashMap<>();
            row.put("local_header_offset", entry.getLocalHeaderOffset());
            row.put("data_offset", entry.getDataOffset());
            row.put("compression_method", entry.getCompressionMethod());
            row.put("compressed_size", entry.getCompressedSize());
            row.put("uncompressed_size", entry.getUncompressedSize());
            row.put("name", entry.getName());
            row.put("entry_size", entry.getEntrySize());
            row.put("directory_is", entry.isDirectoryIs());
            row.put("xyz_path", entry.getXyzPath());
            row.put("x", entry.getX());
            row.put("y", entry.getY());
            row.put("z", entry.getZ());
            row.put("file_name", entry.getFileName());

            rowsData.add(row);
        }

        iAdvExecutor.bInsertBatch(layerCacheTableName, headers, rowsData);
        log.info("图层{}批量插入{}条缓存数据成功", dataId, entries.size());
    }

    public TileCentralDirectoryModel findByXyzPath(String xyzPath) throws SQLException {

        String sql =
                String.format("SELECT * FROM %s WHERE xyz_path = #{xyzPath}", layerCacheTableName);
        GirAdvOneRow girAdvOneRow =
                iAdvExecutor.bSelectOne(sql, SqlParamMap.of().addOne("xyzPath", xyzPath));
        return GutilObject.isEmpty(girAdvOneRow)
                ? null
                : girAdvOneRow.toBeanObj(TileCentralDirectoryModel.class);
    }

    public TileCentralDirectoryModel findByXyz(String x, String y, String z) throws SQLException {

        String sql =
                String.format(
                        "SELECT * FROM %s WHERE x = #{x} AND y = #{y} AND z = #{z}",
                        layerCacheTableName);
        GirAdvOneRow girAdvOneRow =
                iAdvExecutor.bSelectOne(
                        sql, SqlParamMap.of().addOne("x", x).addOne("y", y).addOne("z", z));
        return GutilObject.isEmpty(girAdvOneRow)
                ? null
                : girAdvOneRow.toBeanObj(TileCentralDirectoryModel.class);
    }

    // 3. 根据 fileName 查询
    public TileCentralDirectoryModel findByFileName(String fileName) throws SQLException {

        String sql =
                String.format(
                        "SELECT * FROM %s WHERE file_name = #{fileName}", layerCacheTableName);
        GirAdvOneRow girAdvOneRow =
                iAdvExecutor.bSelectOne(sql, SqlParamMap.of().addOne("fileName", fileName));
        return GutilObject.isEmpty(girAdvOneRow)
                ? null
                : girAdvOneRow.toBeanObj(TileCentralDirectoryModel.class);
    }

    public TileCentralDirectoryModel findById(Long id) throws SQLException {

        String sql = String.format("SELECT * FROM %s WHERE id = #{id}", layerCacheTableName);
        GirAdvOneRow girAdvOneRow = iAdvExecutor.bSelectOne(sql, SqlParamMap.of().addOne("id", id));
        return GutilObject.isEmpty(girAdvOneRow)
                ? null
                : girAdvOneRow.toBeanObj(TileCentralDirectoryModel.class);
    }

    @Override
    public void findBySql(String sql, Consumer<TileCentralDirectoryModel> consumer)
            throws SQLException {

        iAdvExecutor.bSelectObjListStream(sql, TileCentralDirectoryModel.class, consumer);
    }

    @Override
    public void findAll(Consumer<TileCentralDirectoryModel> consumer) throws SQLException {

        String sql = String.format("SELECT * FROM %s", layerCacheTableName);
        findBySql(sql, consumer);
    }

    @Override
    public boolean cacheEnableIs(GirLayerConfigContext layerConfigContext) {
        try {
            return getCacheStatus() == CacheStatus.CACHED;
        } catch (Exception e) {
            log.error("获取缓存状态异常", e);
            return false;
        }
    }

    @Override
    public void doPreCacheStart() {
        try {
            updateCacheStatus(CacheStatus.CACHING);
            log.info("图层{}缓存开始", dataId);
        } catch (Exception e) {
            log.error("图层{}缓存启动失败", dataId, e);
        }
    }

    @Override
    public void doPreCacheEnd() {
        try {
            CacheProvider cacheProvider = getCacheProvider();
            String cacheKey = "postgresql_cache_" + dataId;
            cacheProvider.put(cacheKey, "CACHED".getBytes(), -1);
            updateCacheStatus(CacheStatus.CACHED, 0);
            log.info("图层{}缓存完成", dataId);
        } catch (Exception e) {
            log.error("图层{}缓存结束失败", dataId, e);
            try {
                updateCacheStatus(CacheStatus.CACHE_FAILED);
            } catch (SQLException ex) {
                log.error("更新缓存失败状态失败", ex);
            }
        }
    }

    @Override
    public void delCache() {
        try {

            String deleteSql = String.format("TRUNCATE TABLE %s", layerCacheTableName);
            iAdvExecutor.bUpdateBySql(deleteSql, SqlParamMap.of());

            CacheProvider cacheProvider = getCacheProvider();
            String cacheKey = "postgresql_cache_" + dataId;
            if (cacheProvider.exists(cacheKey)) {
                cacheProvider.evict(cacheKey);
            }

            updateCacheStatus(CacheStatus.NOT_CACHED);
            log.info("图层{}缓存已清空", dataId);
        } catch (Exception e) {
            log.error("图层{}删除缓存失败", dataId, e);
        }
    }

    @Override
    public CacheProvider getCacheProvider() {
        return new NoOpCacheProvider();
    }

    private TileCentralDirectoryModel mapResultSetToEntry(ResultSet rs) throws SQLException {
        TileCentralDirectoryModel entry =
                new TileCentralDirectoryModel(
                        rs.getLong("local_header_offset"),
                        rs.getObject("data_offset") != null ? rs.getLong("data_offset") : null,
                        rs.getLong("compression_method"),
                        rs.getLong("compressed_size"),
                        rs.getLong("uncompressed_size"),
                        rs.getString("name"),
                        rs.getInt("entry_size"));
        entry.setId(rs.getLong("id"));
        entry.setDirectoryIs(rs.getBoolean("directory_is"));
        entry.setXyzPath(rs.getString("xyz_path"));
        entry.setX(rs.getString("x"));
        entry.setY(rs.getString("y"));
        entry.setZ(rs.getString("z"));
        entry.setFileName(rs.getString("file_name"));
        return entry;
    }

    @Override
    public void close() {}
}
