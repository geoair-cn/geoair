package cn.geoair.map.tile.forge.core.zip;

import static org.junit.Assert.*;

import cn.geoair.map.tile.forge.core.caches.CacheProvider;
import cn.geoair.map.tile.forge.core.config.TileTempPathConfig;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.zip.cache.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

public class ArchiveFileProviderTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void fileBackupAndRestoreContainCommittedWalPagesWithoutByteArrays() throws Exception {
        String previous = TileTempPathConfig.getInstance().getTempPath();
        TileTempPathConfig.getInstance().setTempPath(folder.getRoot().toString());
        IndexFileProvider provider =
                new IndexFileProvider(folder.getRoot().toPath().resolve("backup.sqlite"));
        try (SQLiteLayerPerFileDao index =
                new SQLiteLayerPerFileDao("stream") {
                    @Override
                    public CacheProvider getCacheProvider() {
                        return provider;
                    }
                }) {
            index.doPreCacheStart();
            List<TileCentralDirectoryModel> batch = new ArrayList<>();
            for (int i = 0; i < 2000; i++) {
                TileCentralDirectoryModel entry = new TileCentralDirectoryModel();
                entry.setLocalHeaderOffset(0L);
                entry.setCompressionMethod(0L);
                entry.setCompressedSize(1L);
                entry.setUncompressedSize(1L);
                entry.setEntrySize(46);
                entry.setName("entry_" + i);
                batch.add(entry);
            }
            index.batchInsert(batch);
            index.doPreCacheEnd();
            assertEquals(2000, count(provider.backup));
            index.delCache();
            assertFalse(Files.exists(Paths.get(index.getTableName())));
            assertTrue(index.cacheEnableIs(new GirLayerConfigContext()));
            assertEquals(2000, count(Paths.get(index.getTableName())));
            assertEquals(SQLiteLayerPerFileDao.CacheStatus.CACHED, index.getCacheStatus());
        } finally {
            TileTempPathConfig.getInstance().setTempPath(previous);
        }
    }

    private long count(Path file) throws Exception {
        try (Connection db = DriverManager.getConnection("jdbc:sqlite:" + file.toUri());
                Statement sql = db.createStatement();
                ResultSet rows = sql.executeQuery("SELECT COUNT(*) FROM tile_central_directory")) {
            rows.next();
            return rows.getLong(1);
        }
    }
}
