package cn.geoair.map.tile.forge.core.zip.cache;

import cn.geoair.map.tile.forge.core.caches.CacheProvider;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;

import java.io.Closeable;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author ：zhangjun
 * @date ：Created in 2025/11/21 16:13
 * @description： TODO
 */
public interface LayerPerFileDao extends Closeable {

    void init() throws SQLException;

    String getTableName();

    void insert(TileCentralDirectoryEntry entry) throws SQLException;

    void batchInsert(List<TileCentralDirectoryEntry> entries) throws SQLException;

    TileCentralDirectoryEntry findByXyzPath(String xyzPath) throws SQLException;

    TileCentralDirectoryEntry findByFileName(String fileName) throws SQLException;

    TileCentralDirectoryEntry findByXyz(String x, String y, String z) throws SQLException;

    TileCentralDirectoryEntry findById(Long id) throws SQLException;

    void findBySql(String sql, Consumer<TileCentralDirectoryEntry> consumer) throws SQLException;

    void findAll(Consumer<TileCentralDirectoryEntry> consumer) throws SQLException;

    boolean cacheEnableIs(GirLayerConfigContext layerConfigContext) throws SQLException;

    void doPreCacheEnd();

    void doPreCacheStart();

    void delCache();

    CacheProvider getCacheProvider();
}
