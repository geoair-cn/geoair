package cn.geoair.map.tile.forge.fuser.mbtiles;

import static org.junit.Assert.*;

import cn.geoair.map.dynamic.tools.grid.dto.TileZxyApo;
import cn.geoair.map.tile.forge.fuser.entity.PxyLayerInfo;
import cn.geoair.map.tile.forge.fuser.provider.impl.MBTilesTileGetter;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

/** 不扫描源库即可按坐标读取，覆盖中文路径、视图、并发和源文件切换。 */
public class MbtilesSourceReaderTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    private Path source(String name, int value) throws Exception {
        Path path = folder.getRoot().toPath().resolve(name);
        try (Connection db = DriverManager.getConnection("jdbc:sqlite:" + path.toUri());
                Statement sql = db.createStatement()) {
            sql.execute(
                    "CREATE TABLE physical_tiles(zoom_level INTEGER,tile_column INTEGER,tile_row INTEGER,tile_data BLOB, PRIMARY KEY(zoom_level,tile_column,tile_row))");
            sql.execute("CREATE VIEW tiles AS SELECT * FROM physical_tiles");
            try (PreparedStatement insert =
                    db.prepareStatement("INSERT INTO physical_tiles VALUES(2,1,2,?)")) {
                insert.setBytes(1, new byte[] {(byte) value});
                insert.executeUpdate();
            }
        }
        return path;
    }

    @Test
    public void samplesOnlyUsableCoordinatesFromTheRequestedZoomRange() throws Exception {
        Path file = source("取样 #数据.mbtiles", 7);
        try (Connection db = DriverManager.getConnection("jdbc:sqlite:" + file.toUri());
                Statement sql = db.createStatement()) {
            sql.execute(
                    "INSERT INTO physical_tiles VALUES (14,9,6,zeroblob(3)), (14,9,7,zeroblob(1024)), (1,0,0,zeroblob(0))");
        }
        MbtilesReadOptions options = new MbtilesReadOptions();
        options.setMaxTileBytes(16);
        try (MbtilesSourceReader reader = new MbtilesSourceReader(options)) {
            TileZxyApo sample = reader.sampleTile(file.toString(), 10, 16);
            assertNotNull(sample);
            assertEquals(14, sample.getZ());
            assertEquals(9, sample.getX());
            assertEquals(6, sample.getY());
            assertNull(reader.sampleTile(file.toString(), 0, 1));
        }
    }

    @Test
    public void sharesBoundedPoolsAndReadsViewsThroughUnicodeUriPaths() throws Exception {
        Path file = source("中文 #空格.mbtiles", 7);
        MbtilesReadOptions options = new MbtilesReadOptions();
        options.setConnectionsPerFile(2);
        options.setMaxOpenFiles(1);
        try (MbtilesSourceReader reader = new MbtilesSourceReader(options)) {
            ExecutorService workers = Executors.newFixedThreadPool(8);
            try {
                List<Future<byte[]>> results = new ArrayList<>();
                for (int i = 0; i < 100; i++)
                    results.add(workers.submit(() -> reader.readTile(file.toString(), 2, 1, 2)));
                for (Future<byte[]> result : results)
                    assertArrayEquals(new byte[] {7}, result.get(5, TimeUnit.SECONDS));
            } finally {
                workers.shutdownNow();
            }
            Path next = source("next.mbtiles", 8);
            assertArrayEquals(new byte[] {8}, reader.readTile(next.toString(), 2, 1, 2));
            Files.delete(file); // 空闲连接池被回收后，Windows 同样允许删除。
        }
    }

    @Test
    public void rejectsOversizedBlobAndReopensReplacedFiles() throws Exception {
        Path file = source("source.mbtiles", 1);
        MbtilesReadOptions options = new MbtilesReadOptions();
        options.setMaxTileBytes(32);
        try (MbtilesSourceReader reader = new MbtilesSourceReader(options)) {
            assertArrayEquals(new byte[] {1}, reader.readTile(file.toString(), 2, 1, 2));
            source("new.mbtiles", 2);
            // 先通过另一个文件淘汰旧连接，兼容 Windows 对打开数据库文件的占用规则。
            try (Connection db = DriverManager.getConnection("jdbc:sqlite:" + file.toUri());
                    Statement sql = db.createStatement()) {
                sql.execute("UPDATE physical_tiles SET tile_data=zeroblob(1048576)");
            }
            assertThrows(SQLException.class, () -> reader.readTile(file.toString(), 2, 1, 2));
            // 已关闭读取器可以重新打开同名替换文件。
        }
        Files.delete(file);
        Files.move(folder.getRoot().toPath().resolve("new.mbtiles"), file);
        try (MbtilesSourceReader reader = new MbtilesSourceReader()) {
            assertArrayEquals(new byte[] {2}, reader.readTile(file.toString(), 2, 1, 2));
        }
    }

    @Test
    public void ordinaryGetterDoesNotCreateMetadataOrCountTheSourceTable() throws Exception {
        Path file = source("getter.mbtiles", 3);
        PxyLayerInfo layer = new PxyLayerInfo();
        layer.setPath(file.toString());
        layer.setGridSrid(3857);
        layer.setTileRowOrigin("bottom-left");
        try (MbtilesSourceReader reader = new MbtilesSourceReader();
                MBTilesTileGetter getter = new MBTilesTileGetter(layer, reader)) {
            assertArrayEquals(new byte[] {3}, getter.getTileResource(2, 1, 2).getByteData());
        }
        try (Connection db = DriverManager.getConnection("jdbc:sqlite:" + file.toUri());
                Statement sql = db.createStatement();
                ResultSet rows =
                        sql.executeQuery(
                                "SELECT COUNT(*) FROM sqlite_master WHERE name='metadata'")) {
            rows.next();
            assertEquals(0, rows.getInt(1));
        }
    }
}
