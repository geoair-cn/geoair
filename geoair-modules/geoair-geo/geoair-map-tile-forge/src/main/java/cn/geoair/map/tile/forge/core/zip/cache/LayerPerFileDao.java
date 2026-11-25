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

    /** 没有源版本的旧索引仅可由旧调用链使用，按需读取会安全回退到源归档。 */
    default String getArchiveVersion() throws SQLException {
        return null;
    }

    default void setArchiveVersion(String version) throws SQLException {}

    /** 请求路径只读取已完整发布且版本匹配的索引；未实现时由调用方回到归档。 */
    default TileCentralDirectoryModel findReadyByFileName(String version, String fileName)
            throws SQLException {
        return null;
    }

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

    /** 构建失败时保留可重试状态，旧驱动可继续沿用默认实现。 */
    default void doPreCacheFailed() {}

    void delCache();

    CacheProvider getCacheProvider();
}
