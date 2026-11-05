package cn.geoair.map.tile.forge.fuser.cache;

import cn.geoair.map.tile.forge.fuser.execution.TileOperationContext;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationGuard;
import cn.geoair.map.tile.forge.fuser.utils.FuserCacheUtils;
import cn.geoair.map.tile.forge.fuser.utils.TileResourceLimits;

import java.io.IOException;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.stream.Stream;

/** 缓存驱动内部的只读查询；调用方无需了解磁盘布局或 SQLite 表。 */
final class CacheInspection {
    private CacheInspection() {}

    static BitSet file(
            Path layer, long expireTime, TileCacheQuery q, TileOperationContext context) {
        BitSet found = new BitSet(q.size());
        boolean reverse = FuserCacheUtils.fileCheckIsNeedReverseY(q.getLayerName());
        int srid = FuserCacheUtils.getCacheGridSrid(q.getLayerName());
        String extension = "." + q.getFormat().getFileExtension();
        try {
            for (long index = q.getFrom(); index < q.getTo(); ) {
                context.check();
                int y = q.y(index),
                        count =
                                (int)
                                        Math.min(
                                                q.getTo() - index,
                                                q.getRows() - index % q.getRows());
                Path directory =
                        layer.resolve(String.valueOf(q.getZoom()))
                                .resolve(String.valueOf(q.x(index)));
                if (Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS))
                    try (DirectoryStream<Path> paths = Files.newDirectoryStream(directory)) {
                        for (Path path : paths) {
                            context.check();
                            String name = path.getFileName().toString();
                            if (!name.endsWith(extension)) continue;
                            int stored;
                            try {
                                stored =
                                        Integer.parseInt(
                                                name.substring(
                                                        0, name.length() - extension.length()));
                            } catch (NumberFormatException e) {
                                continue;
                            }
                            int sourceY =
                                    FuserCacheUtils.getStoreY(q.getZoom(), stored, reverse, srid);
                            if (sourceY < y
                                    || (long) sourceY >= (long) y + count
                                    || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                                continue;
                            try {
                                long size = Files.size(path);
                                if (size > 0
                                        && size <= TileResourceLimits.getMaxTileBytes()
                                        && (expireTime <= 0
                                                || System.currentTimeMillis()
                                                                - Files.getLastModifiedTime(path)
                                                                        .toMillis()
                                                        <= expireTime))
                                    found.set((int) (index - q.getFrom() + sourceY - y));
                            } catch (NoSuchFileException ignored) {
                                /* 并发删除属于未命中 */
                            }
                        }
                    }
                index += count;
            }
            return found;
        } catch (IOException e) {
            throw new IllegalStateException("缓存目录批量查询失败", e);
        }
    }

    private static Connection open(Path path) throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + path.toUri() + "?mode=ro");
    }

    static BitSet mbtiles(Path path, TileCacheQuery q, TileOperationContext context) {
        BitSet found = new BitSet(q.size());
        context.check();
        if (!Files.isRegularFile(path)) return found;
        boolean reverse = FuserCacheUtils.mbtilesCheckIsNeedReverseY(q.getLayerName());
        int srid = FuserCacheUtils.getCacheGridSrid(q.getLayerName());
        try (Connection connection = open(path);
                PreparedStatement query =
                        connection.prepareStatement(
                                "SELECT tile_row FROM tiles WHERE zoom_level=? AND tile_column=?"
                                    + " AND tile_row BETWEEN ? AND ? AND length(tile_data)>0 AND"
                                    + " length(tile_data)<=?");
                TileOperationGuard guard = new TileOperationGuard(context, () -> cancel(query))) {
            for (long index = q.getFrom(); index < q.getTo(); ) {
                context.check();
                query.setQueryTimeout(
                        Math.max(1, (int) ((context.remainingMillis() + 999L) / 1000)));
                int y = q.y(index),
                        count =
                                (int)
                                        Math.min(
                                                q.getTo() - index,
                                                q.getRows() - index % q.getRows());
                int a = FuserCacheUtils.getStoreY(q.getZoom(), y, reverse, srid),
                        b = FuserCacheUtils.getStoreY(q.getZoom(), y + count - 1, reverse, srid);
                query.setInt(1, q.getZoom());
                query.setInt(2, q.x(index));
                query.setInt(3, Math.min(a, b));
                query.setInt(4, Math.max(a, b));
                query.setLong(5, TileResourceLimits.getMaxTileBytes());
                try (ResultSet result = query.executeQuery()) {
                    while (result.next()) {
                        context.check();
                        int sourceY =
                                FuserCacheUtils.getStoreY(
                                        q.getZoom(), result.getInt(1), reverse, srid);
                        found.set((int) (index - q.getFrom() + sourceY - y));
                    }
                }
                index += count;
            }
            context.check();
            return found;
        } catch (SQLException e) {
            context.check();
            throw new IllegalStateException("MBTiles 批量查询失败", e);
        }
    }

    static TileCacheStatistics statistics(
            Path path, String layer, boolean mbtiles, TileOperationContext context) {
        context.check();
        TileCacheStatistics result = new TileCacheStatistics();
        result.setLayerName(layer);
        result.setSupported(true);
        result.setDriver(mbtiles ? CacheStorageKind.MBTILES : CacheStorageKind.FILE);
        if (!Files.exists(path)) return result;
        TreeMap<Integer, TileCacheLevelStatistics> levels = new TreeMap<>();
        try {
            if (mbtiles) {
                result.setStorageBytes(Files.size(path));
                for (String suffix : new String[] {"-wal", "-shm"}) {
                    Path extra = Paths.get(path.toString() + suffix);
                    if (Files.isRegularFile(extra))
                        result.setStorageBytes(result.getStorageBytes() + Files.size(extra));
                }
                try (Connection connection = open(path);
                        Statement statement = connection.createStatement();
                        TileOperationGuard guard =
                                new TileOperationGuard(context, () -> cancel(statement))) {
                    statement.setQueryTimeout(
                            Math.max(1, (int) ((context.remainingMillis() + 999L) / 1000)));
                    try (ResultSet rows =
                            statement.executeQuery(
                                    "SELECT zoom_level,COUNT(*),SUM(length(tile_data)) FROM tiles"
                                        + " WHERE length(tile_data)>0 GROUP BY zoom_level")) {
                        while (rows.next()) {
                            context.check();
                            add(levels, rows.getInt(1), rows.getLong(2), rows.getLong(3));
                        }
                    }
                }
            } else
                try (Stream<Path> stream = Files.walk(path)) {
                    Iterator<Path> iterator = stream.iterator();
                    while (iterator.hasNext()) {
                        context.check();
                        Path file = iterator.next();
                        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) continue;
                        long size;
                        try {
                            size = Files.size(file);
                        } catch (NoSuchFileException ignored) {
                            continue;
                        }
                        result.setStorageBytes(result.getStorageBytes() + size);
                        Path relative = path.relativize(file);
                        if (relative.getNameCount() != 3
                                || size == 0
                                || !relative.getName(2).toString().matches("[0-9]+\\.[A-Za-z0-9]+"))
                            continue;
                        try {
                            int z = Integer.parseInt(relative.getName(0).toString());
                            Integer.parseInt(relative.getName(1).toString());
                            add(levels, z, 1, size);
                        } catch (NumberFormatException ignored) {
                            /* 非瓦片文件仅计入物理空间 */
                        }
                    }
                }
        } catch (IOException | SQLException e) {
            context.check();
            throw new IllegalStateException("缓存统计失败", e);
        }
        for (TileCacheLevelStatistics level : levels.values()) {
            result.getLevels().add(level);
            result.setTileCount(result.getTileCount() + level.getTileCount());
            result.setTileBytes(result.getTileBytes() + level.getTileBytes());
        }
        context.check();
        return result;
    }

    private static void cancel(Statement statement) {
        try {
            statement.cancel();
        } catch (SQLException ignored) {
            /* 连接已关闭 */
        }
    }

    private static void add(
            Map<Integer, TileCacheLevelStatistics> levels, int z, long count, long bytes) {
        TileCacheLevelStatistics level = levels.get(z);
        if (level == null) {
            level = new TileCacheLevelStatistics();
            level.setZoom(z);
            levels.put(z, level);
        }
        level.setTileCount(level.getTileCount() + count);
        level.setTileBytes(level.getTileBytes() + bytes);
    }
}
