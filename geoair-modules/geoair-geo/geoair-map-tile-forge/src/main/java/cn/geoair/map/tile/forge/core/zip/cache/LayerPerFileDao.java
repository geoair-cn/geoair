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

    void insert(TileCentralDirectoryModel entry) throws SQLException;

    void batchInsert(List<TileCentralDirectoryModel> entries) throws SQLException;

    TileCentralDirectoryModel findByXyzPath(String xyzPath) throws SQLException;

    TileCentralDirectoryModel findByFileName(String fileName) throws SQLException;

    TileCentralDirectoryModel findByXyz(String x, String y, String z) throws SQLException;

    TileCentralDirectoryModel findById(Long id) throws SQLException;

    void findBySql(String sql, Consumer<TileCentralDirectoryModel> consumer) throws SQLException;

    void findAll(Consumer<TileCentralDirectoryModel> consumer) throws SQLException;

    boolean cacheEnableIs(GirLayerConfigContext layerConfigContext) throws SQLException;

    void doPreCacheEnd();

    void doPreCacheStart();

    void delCache();

    CacheProvider getCacheProvider();
}
