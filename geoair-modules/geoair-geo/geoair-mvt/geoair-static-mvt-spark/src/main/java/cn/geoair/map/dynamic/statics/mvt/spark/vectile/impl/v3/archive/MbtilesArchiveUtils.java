package cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v3.archive;

import cn.geoair.map.dynamic.tools.grid.dto.TileYAxis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 将 V3 本地瓦片目录归档为标准 MBTiles SQLite 文件。
 *
 * <p>该工具只在 Driver 端单进程执行，避免多个 Spark executor 并发写入同一个 SQLite 文件。</p>
 *
 * @author 张逢吉
 */
public final class MbtilesArchiveUtils {

    private static final String CREATE_TILES_SQL = "CREATE TABLE tiles ("
            + "zoom_level INTEGER NOT NULL, tile_column INTEGER NOT NULL, tile_row INTEGER NOT NULL, "
            + "tile_data BLOB NOT NULL, PRIMARY KEY (zoom_level, tile_column, tile_row))";
    private static final String CREATE_METADATA_SQL = "CREATE TABLE metadata ("
            + "name TEXT NOT NULL PRIMARY KEY, value TEXT)";

    private MbtilesArchiveUtils() {
    }

    /**
     * 将中间目录中的 PBF 瓦片归档为 MBTiles。
     *
     * @param stagingDirectory Spark 并行切片阶段产生的目录
     * @param targetFile 目标 .mbtiles 文件
     * @param stagingYAxis 中间目录使用的 Y 轴约定
     * @param overwrite 是否可覆盖已有归档文件
     * @param batchSize 每次 SQLite 事务写入的瓦片数量
     * @param metadata 写入 MBTiles metadata 表的键值对
     */
    public static void archive(
            Path stagingDirectory, Path targetFile, TileYAxis stagingYAxis, boolean overwrite,
            int batchSize, Map<String, String> metadata) throws IOException {
        if (targetFile == null) {
            throw new IllegalArgumentException("MBTiles 输出文件不能为空");
        }
        if (batchSize <= 0) {
            throw new IllegalArgumentException("MBTiles batchSize 必须大于 0");
        }
        Path archiveFile = targetFile.toAbsolutePath().normalize();
        List<V3TileArchiveFiles.TileFile> tiles = V3TileArchiveFiles.listTiles(stagingDirectory, stagingYAxis);
        Map<String, String> archiveMetadata = new LinkedHashMap<>();
        if (metadata != null) {
            archiveMetadata.putAll(metadata);
        }
        archiveMetadata.put("bounds", calculateBounds(tiles));
        prepareTarget(archiveFile, overwrite);
        Path temporaryFile = archiveFile.resolveSibling(archiveFile.getFileName() + ".tmp-" + UUID.randomUUID());
        try {
            writeArchive(temporaryFile, tiles, batchSize, archiveMetadata);
            moveCompletedArchive(temporaryFile, archiveFile, overwrite);
        } catch (Exception e) {
            Files.deleteIfExists(temporaryFile);
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            throw new IOException("生成 MBTiles 归档失败: " + archiveFile, e);
        }
    }

    /** 判断文件是否为可读取的 MBTiles SQLite 文件。 */
    public static boolean isMbtiles(Path file) {
        if (file == null || !Files.isRegularFile(file)) {
            return false;
        }
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath())) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT 1 FROM sqlite_master WHERE type='table' AND name='tiles'")) {
                return statement.executeQuery().next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private static void writeArchive(
            Path output, List<V3TileArchiveFiles.TileFile> tiles, int batchSize,
            Map<String, String> metadata) throws Exception {
        Class.forName("org.sqlite.JDBC");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + output.toAbsolutePath())) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA journal_mode=DELETE");
                statement.execute("PRAGMA synchronous=FULL");
                statement.execute(CREATE_TILES_SQL);
                statement.execute(CREATE_METADATA_SQL);
            }
            connection.setAutoCommit(false);
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO tiles (zoom_level,tile_column,tile_row,tile_data) VALUES (?,?,?,?)")) {
                int count = 0;
                for (V3TileArchiveFiles.TileFile tile : tiles) {
                    insert.setInt(1, tile.z);
                    insert.setInt(2, tile.x);
                    insert.setInt(3, V3TileArchiveFiles.toTmsY(tile.z, tile.xyzY));
                    insert.setBytes(4, Files.readAllBytes(tile.path));
                    insert.addBatch();
                    count++;
                    if (count >= batchSize) {
                        insert.executeBatch();
                        connection.commit();
                        count = 0;
                    }
                }
                if (count > 0) {
                    insert.executeBatch();
                }
                writeMetadata(connection, metadata);
                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private static void writeMetadata(Connection connection, Map<String, String> metadata) throws SQLException {
        if (metadata == null || metadata.isEmpty()) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT OR REPLACE INTO metadata (name,value) VALUES (?,?)")) {
            for (Map.Entry<String, String> entry : metadata.entrySet()) {
                statement.setString(1, entry.getKey());
                statement.setString(2, entry.getValue());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static void prepareTarget(Path targetFile, boolean overwrite) throws IOException {
        Path parent = targetFile.toAbsolutePath().normalize().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        if (!overwrite && Files.exists(targetFile)) {
            throw new IOException("目标 MBTiles 文件已存在且未开启覆盖: " + targetFile);
        }
    }

    private static void moveCompletedArchive(Path temporaryFile, Path targetFile, boolean overwrite) throws IOException {
        if (overwrite) {
            Files.move(temporaryFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
        } else {
            Files.move(temporaryFile, targetFile);
        }
    }

    private static String calculateBounds(List<V3TileArchiveFiles.TileFile> tiles) {
        double minLon = Double.MAX_VALUE;
        double minLat = Double.MAX_VALUE;
        double maxLon = -Double.MAX_VALUE;
        double maxLat = -Double.MAX_VALUE;
        for (V3TileArchiveFiles.TileFile tile : tiles) {
            double n = (double) (1L << tile.z);
            minLon = Math.min(minLon, tile.x / n * 360D - 180D);
            maxLon = Math.max(maxLon, (tile.x + 1D) / n * 360D - 180D);
            minLat = Math.min(minLat, latitude(tile.xyzY + 1D, n));
            maxLat = Math.max(maxLat, latitude(tile.xyzY, n));
        }
        return String.format(Locale.ROOT, "%.7f,%.7f,%.7f,%.7f", minLon, minLat, maxLon, maxLat);
    }

    private static double latitude(double y, double n) {
        return Math.toDegrees(Math.atan(Math.sinh(Math.PI * (1D - 2D * y / n))));
    }

    /** 构造标准的 MBTiles 元数据 Map。 */
    public static Map<String, String> metadata(String name, String edition, int minZoom, int maxZoom, String json) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("name", name);
        metadata.put("format", "pbf");
        metadata.put("type", "overlay");
        metadata.put("version", edition == null || edition.trim().isEmpty() ? "1.0" : edition);
        metadata.put("minzoom", String.valueOf(minZoom));
        metadata.put("maxzoom", String.valueOf(maxZoom));
        metadata.put("scheme", "tms");
        if (json != null && !json.trim().isEmpty()) {
            metadata.put("json", json);
        }
        return metadata;
    }
}
