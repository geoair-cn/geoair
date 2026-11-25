package cn.geoair.map.tile.forge.fuser.mbtiles.statistics;

import org.sqlite.ProgressHandler;
import org.sqlite.SQLiteConnection;

import java.sql.*;
import java.util.*;
import java.util.function.BooleanSupplier;

/**
 * 文件内事务统计：需要显式安装并修改目标 MBTiles 的 schema。 仅接受四列、单一坐标主键的普通 rowid 表和没有用户触发器的数据库。 不能处理 SQL
 * 外直接修改数据库文件、删除触发器或替换整个数据库。 仅在管理端明确启用后安装。
 */
public final class TransactionalMbtilesStatistics {
    private static final String STATE = "tf_stats_state";
    private static final String LEVEL = "tf_stats_level";
    private static final String PENDING = "tf_stats_pending";
    private static final Map<String, String> TRIGGERS = createTriggers();

    /** 已有同名对象但不属于完整本版本时抛错，不尝试修复或删除它们。 */
    public boolean isInstalled(Connection db) throws SQLException {
        try (Statement s = db.createStatement();
                ResultSet r =
                        s.executeQuery(
                                "SELECT name FROM sqlite_master WHERE name GLOB 'tf_stats_*' LIMIT 1")) {
            if (!r.next()) return false;
        }
        validateInstalled(db);
        return true;
    }

    public void install(Connection db) throws SQLException {
        transaction(
                db,
                () -> {
                    validateUninstalled(db);
                    try (Statement s = db.createStatement()) {
                        s.execute(
                                "CREATE TABLE "
                                        + STATE
                                        + "(id INTEGER PRIMARY KEY CHECK(id=1),started INTEGER NOT NULL,cursor INTEGER NOT NULL,done INTEGER NOT NULL,scanned INTEGER NOT NULL,version INTEGER NOT NULL,schema_cookie INTEGER NOT NULL)");
                        s.execute("INSERT INTO " + STATE + " VALUES(1,0,0,0,0,1,0)");
                        s.execute(
                                "CREATE TABLE "
                                        + LEVEL
                                        + "(z INTEGER PRIMARY KEY,n INTEGER NOT NULL,bytes INTEGER NOT NULL,bounds_dirty INTEGER NOT NULL,minx INTEGER,maxx INTEGER,miny INTEGER,maxy INTEGER)");
                        s.execute(
                                "CREATE TABLE "
                                        + PENDING
                                        + "(kind TEXT NOT NULL,oldrow INTEGER NOT NULL,z INTEGER NOT NULL,x INTEGER NOT NULL,y INTEGER NOT NULL,bytes INTEGER NOT NULL,deleted INTEGER NOT NULL,PRIMARY KEY(kind,oldrow))");
                        for (String sql : TRIGGERS.values()) s.execute(sql);
                        int cookie;
                        try (ResultSet r = s.executeQuery("PRAGMA schema_version")) {
                            r.next();
                            cookie = r.getInt(1);
                        }
                        s.executeUpdate(
                                "UPDATE " + STATE + " SET schema_cookie=" + cookie + " WHERE id=1");
                    }
                });
    }

    /** 显式移除本版本对象；在同一写事务内完成，不删除瓦片。 */
    public void uninstall(Connection db) throws SQLException {
        transaction(
                db,
                () -> {
                    validateInstalled(db);
                    try (Statement s = db.createStatement()) {
                        for (String trigger : TRIGGERS.keySet())
                            s.execute("DROP TRIGGER " + trigger);
                        s.execute("DROP TABLE " + PENDING);
                        s.execute("DROP TABLE " + LEVEL);
                        s.execute("DROP TABLE " + STATE);
                    }
                });
    }

    /** 不读取 tiles 数据；如发现触发器丢失或来源结构变化则拒绝把汇总当作可靠结果。 */
    public void validateInstalled(Connection db) throws SQLException {
        validateTilesSchema(db, true);
        for (Map.Entry<String, String> trigger : TRIGGERS.entrySet()) {
            try (PreparedStatement q =
                    db.prepareStatement(
                            "SELECT sql FROM sqlite_master WHERE type='trigger' AND name=? AND tbl_name='tiles'")) {
                q.setString(1, trigger.getKey());
                try (ResultSet r = q.executeQuery()) {
                    if (!r.next() || !trigger.getValue().equals(r.getString(1)))
                        throw new SQLException("统计触发器缺失或已改变: " + trigger.getKey());
                }
            }
        }
        int cookie;
        try (Statement s = db.createStatement();
                ResultSet r = s.executeQuery("PRAGMA schema_version")) {
            r.next();
            cookie = r.getInt(1);
        }
        try (Statement s = db.createStatement();
                ResultSet r =
                        s.executeQuery(
                                "SELECT version,schema_cookie FROM " + STATE + " WHERE id=1")) {
            if (!r.next() || r.getInt(1) != 1) throw new SQLException("统计版本无效");
            if (r.getInt(2) != cookie) throw new SQLException("数据库结构已发生变化，请重新验证统计");
        }
    }

    /** 每批扫描与进度提交在同一写事务；不同连接修改源瓦片时由源库触发器同步修正。 */
    public TransactionalStatsSnapshot scanBatch(Connection db, int batchSize) throws SQLException {
        return scanBatch(db, batchSize, Long.MAX_VALUE, () -> false);
    }

    /** 在行边界提前提交已完成的前缀，写入等待或时间片用完都不丢失已扫描进度。 */
    public TransactionalStatsSnapshot scanBatch(
            Connection db, int batchSize, long sliceNanos, BooleanSupplier yieldToWriter)
            throws SQLException {
        if (batchSize < 1 || batchSize > 100000)
            throw new IllegalArgumentException("批大小须为 1 至 100000");
        if (sliceNanos < 1) throw new IllegalArgumentException("统计时间片必须大于 0");
        transaction(
                db,
                () -> {
                    long start = System.nanoTime();
                    validateInstalled(db);
                    boolean started, done;
                    long cursor;
                    try (Statement s = db.createStatement();
                            ResultSet r =
                                    s.executeQuery(
                                            "SELECT started,cursor,done FROM "
                                                    + STATE
                                                    + " WHERE id=1")) {
                        if (!r.next()) throw new SQLException("缺少统计状态");
                        started = r.getInt(1) != 0;
                        cursor = r.getLong(2);
                        done = r.getInt(3) != 0;
                    }
                    if (done) return;
                    long last = cursor;
                    int count = 0;
                    Map<Integer, CacheZoomStatistics> batch = new TreeMap<>();
                    // 只读坐标和 BLOB 长度，不将图像复制到 Java。逐行推进便于在写请求到达时提前提交。
                    String sql =
                            "SELECT rowid,zoom_level,tile_column,tile_row,LENGTH(tile_data),"
                                    + "typeof(zoom_level),typeof(tile_column),typeof(tile_row),typeof(tile_data) FROM tiles "
                                    + (started ? "WHERE rowid>? " : "")
                                    + "ORDER BY rowid LIMIT ?";
                    try (PreparedStatement query = db.prepareStatement(sql)) {
                        int i = 1;
                        if (started) query.setLong(i++, cursor);
                        query.setInt(i, batchSize);
                        try (ResultSet row = query.executeQuery()) {
                            while (row.next()) {
                                if (Thread.currentThread().isInterrupted())
                                    throw new SQLException("统计已中断");
                                int z = coordinate(row, 2, 6),
                                        x = coordinate(row, 3, 7),
                                        y = coordinate(row, 4, 8);
                                if (!"blob".equals(row.getString(9)))
                                    throw new SQLException("瓦片数据类型不受支持，统计未推进");
                                CacheZoomStatistics level =
                                        batch.computeIfAbsent(z, CacheZoomStatistics::new);
                                level.addTile(row.getLong(5));
                                level.addGridCoordinate(x, y);
                                last = row.getLong(1);
                                count++;
                                if (yieldToWriter.getAsBoolean()
                                        || System.nanoTime() - start >= sliceNanos) break;
                            }
                        }
                    }
                    try (PreparedStatement ensure =
                                    db.prepareStatement(
                                            "INSERT INTO "
                                                    + LEVEL
                                                    + "(z,n,bytes,bounds_dirty) SELECT ?,0,0,0 WHERE NOT EXISTS(SELECT 1 FROM "
                                                    + LEVEL
                                                    + " WHERE z=?)");
                            PreparedStatement add =
                                    db.prepareStatement(
                                            "UPDATE "
                                                    + LEVEL
                                                    + " SET n=n+?,bytes=bytes+?,"
                                                    + "minx=CASE WHEN minx IS NULL OR ?<minx THEN ? ELSE minx END,"
                                                    + "maxx=CASE WHEN maxx IS NULL OR ?>maxx THEN ? ELSE maxx END,"
                                                    + "miny=CASE WHEN miny IS NULL OR ?<miny THEN ? ELSE miny END,"
                                                    + "maxy=CASE WHEN maxy IS NULL OR ?>maxy THEN ? ELSE maxy END WHERE z=?")) {
                        for (CacheZoomStatistics level : batch.values()) {
                            long z = level.getZoomLevel();
                            ensure.setLong(1, z);
                            ensure.setLong(2, z);
                            ensure.executeUpdate();
                            add.setLong(1, level.getTileCount());
                            add.setLong(2, level.getSize());
                            int parameter = 3;
                            for (Integer boundary :
                                    Arrays.asList(
                                            level.getMinX(),
                                            level.getMaxX(),
                                            level.getMinY(),
                                            level.getMaxY())) {
                                add.setInt(parameter++, boundary);
                                add.setInt(parameter++, boundary);
                            }
                            add.setLong(11, z);
                            add.executeUpdate();
                        }
                    }
                    try (PreparedStatement p =
                            db.prepareStatement(
                                    "UPDATE "
                                            + STATE
                                            + " SET started=CASE WHEN ?>0 THEN 1 ELSE started END,cursor=?,done=CASE WHEN ?=0 THEN 1 ELSE 0 END,scanned=scanned+? WHERE id=1")) {
                        p.setInt(1, count);
                        p.setLong(2, last);
                        p.setInt(3, count);
                        p.setInt(4, count);
                        p.executeUpdate();
                    }
                    // 该写锁保证没有其他语句还在使用这张临时冲突捕获表。
                    try (Statement s = db.createStatement()) {
                        s.executeUpdate("DELETE FROM " + PENDING);
                    }
                });
        return read(db);
    }

    private int coordinate(ResultSet row, int valueColumn, int typeColumn) throws SQLException {
        long value = row.getLong(valueColumn);
        if (!"integer".equals(row.getString(typeColumn)) || value < 0 || value > Integer.MAX_VALUE)
            throw new SQLException("瓦片坐标类型不受支持，统计未推进");
        return (int) value;
    }

    public TransactionalStatsSnapshot read(Connection db) throws SQLException {
        final TransactionalStatsSnapshot[] result = new TransactionalStatsSnapshot[1];
        transaction(db, () -> result[0] = readLocked(db), false);
        return result[0];
    }

    private TransactionalStatsSnapshot readLocked(Connection db) throws SQLException {
        validateInstalled(db);
        boolean done;
        long scanned;
        try (Statement s = db.createStatement();
                ResultSet r = s.executeQuery("SELECT done,scanned FROM " + STATE + " WHERE id=1")) {
            if (!r.next()) throw new SQLException("缺少统计状态");
            done = r.getInt(1) != 0;
            scanned = r.getLong(2);
        }
        List<TransactionalStatsLevel> levels = new ArrayList<>();
        try (Statement s = db.createStatement();
                ResultSet r =
                        s.executeQuery(
                                "SELECT z,n,bytes,bounds_dirty,minx,maxx,miny,maxy FROM "
                                        + LEVEL
                                        + " WHERE n<>0 ORDER BY z")) {
            while (r.next()) {
                boolean dirty = r.getInt(4) != 0;
                levels.add(
                        new TransactionalStatsLevel(
                                r.getInt(1),
                                r.getLong(2),
                                r.getLong(3),
                                dirty,
                                dirty ? null : integer(r, 5),
                                dirty ? null : integer(r, 6),
                                dirty ? null : integer(r, 7),
                                dirty ? null : integer(r, 8)));
            }
        }
        return new TransactionalStatsSnapshot(done, scanned, levels);
    }

    /** 范围可能需要扫描该级别；完成后可以按需调用，不能在未完成时视为前缀范围。 */
    public void recalculateBounds(Connection db, int zoom, BooleanSupplier yieldToWriter)
            throws SQLException {
        SQLiteConnection sqlite = db.unwrap(SQLiteConnection.class);
        ProgressHandler.setHandler(
                sqlite,
                1000,
                new ProgressHandler() {
                    @Override
                    protected int progress() {
                        return Thread.currentThread().isInterrupted()
                                        || yieldToWriter.getAsBoolean()
                                ? 1
                                : 0;
                    }
                });
        try {
            recalculateBounds(db, zoom);
        } finally {
            ProgressHandler.clearHandler(sqlite);
        }
    }

    public void recalculateBounds(Connection db, int zoom) throws SQLException {
        transaction(
                db,
                () -> {
                    validateInstalled(db);
                    try (Statement s = db.createStatement();
                            ResultSet r =
                                    s.executeQuery("SELECT done FROM " + STATE + " WHERE id=1")) {
                        if (!r.next() || r.getInt(1) == 0)
                            throw new SQLException("初次扫描未完成，不能确认完整范围");
                    }
                    try (PreparedStatement p =
                            db.prepareStatement(
                                    "UPDATE "
                                            + LEVEL
                                            + " SET (minx,maxx,miny,maxy,bounds_dirty)=(SELECT MIN(tile_column),MAX(tile_column),MIN(tile_row),MAX(tile_row),0 FROM tiles WHERE zoom_level=?) WHERE z=?")) {
                        p.setInt(1, zoom);
                        p.setInt(2, zoom);
                        p.executeUpdate();
                    }
                },
                false);
    }

    private static Integer integer(ResultSet r, int index) throws SQLException {
        int value = r.getInt(index);
        return r.wasNull() ? null : value;
    }

    private static void validateUninstalled(Connection db) throws SQLException {
        validateTilesSchema(db, false);
        try (Statement s = db.createStatement();
                ResultSet r =
                        s.executeQuery(
                                "SELECT name FROM sqlite_master WHERE name LIKE 'tf_stats_%' LIMIT 1")) {
            if (r.next()) throw new SQLException("统计命名空间已被占用");
        }
    }

    private static void validateTilesSchema(Connection db, boolean installed) throws SQLException {
        try (Statement s = db.createStatement();
                ResultSet r =
                        s.executeQuery("SELECT type,sql FROM sqlite_master WHERE name='tiles'")) {
            if (!r.next()
                    || !"table".equals(r.getString(1))
                    || r.getString(2).toUpperCase(Locale.ROOT).contains("WITHOUT ROWID"))
                throw new SQLException("仅支持普通 rowid tiles 表");
        }
        String[] names = {"zoom_level", "tile_column", "tile_row", "tile_data"};
        String[] types = {"INTEGER", "INTEGER", "INTEGER", "BLOB"};
        try (Statement s = db.createStatement();
                ResultSet r = s.executeQuery("PRAGMA table_info(tiles)")) {
            int i = 0;
            while (r.next()) {
                if (i >= names.length
                        || !names[i].equalsIgnoreCase(r.getString("name"))
                        || !types[i].equalsIgnoreCase(r.getString("type"))
                        || r.getInt("notnull") != 1
                        || r.getInt("pk") != (i < 3 ? i + 1 : 0))
                    throw new SQLException("tiles 列或主键不兼容");
                i++;
            }
            if (i != names.length) throw new SQLException("tiles 列数量不兼容");
        }
        try (Statement s = db.createStatement();
                ResultSet r = s.executeQuery("PRAGMA index_list(tiles)")) {
            while (r.next())
                if (r.getInt("unique") != 0 && !"pk".equals(r.getString("origin")))
                    throw new SQLException("tiles 存在额外唯一索引");
        }
        try (Statement s = db.createStatement();
                ResultSet r =
                        s.executeQuery("SELECT name FROM sqlite_master WHERE type='trigger'")) {
            while (r.next())
                if (!installed || !TRIGGERS.containsKey(r.getString(1)))
                    throw new SQLException("tiles 存在未知触发器");
        }
        try (Statement s = db.createStatement();
                ResultSet r =
                        s.executeQuery(
                                "SELECT name FROM sqlite_temp_master WHERE type='trigger'")) {
            if (r.next()) throw new SQLException("当前连接存在临时触发器");
        }
        List<String> tables = new ArrayList<>();
        try (Statement s = db.createStatement();
                ResultSet r =
                        s.executeQuery(
                                "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'")) {
            while (r.next()) tables.add(r.getString(1));
        }
        for (String table : tables) {
            String identifier = "\"" + table.replace("\"", "\"\"") + "\"";
            try (Statement s = db.createStatement();
                    ResultSet r = s.executeQuery("PRAGMA foreign_key_list(" + identifier + ")")) {
                if (r.next()) throw new SQLException("数据库包含外键，不支持事务统计");
            }
        }
    }

    private static void transaction(Connection db, SqlWork action) throws SQLException {
        transaction(db, action, true);
    }

    private static void transaction(Connection db, SqlWork action, boolean immediate)
            throws SQLException {
        if (!db.getAutoCommit()) throw new SQLException("调用者连接必须处于自动提交状态");
        try (Statement s = db.createStatement()) {
            s.execute(immediate ? "BEGIN IMMEDIATE" : "BEGIN");
        }
        try {
            action.execute();
            try (Statement s = db.createStatement()) {
                s.execute("COMMIT");
            }
        } catch (SQLException | RuntimeException | Error e) {
            try (Statement s = db.createStatement()) {
                s.execute("ROLLBACK");
            } catch (SQLException rollback) {
                e.addSuppressed(rollback);
            }
            throw e;
        }
    }

    private static Map<String, String> createTriggers() {
        String oldIncluded =
                "(SELECT done=1 OR (started=1 AND cursor>=OLD.rowid) FROM "
                        + STATE
                        + " WHERE id=1)";
        String newIncluded =
                "(SELECT done=1 OR (started=1 AND cursor>=NEW.rowid) FROM "
                        + STATE
                        + " WHERE id=1)";
        String ensure =
                "INSERT INTO "
                        + LEVEL
                        + "(z,n,bytes,bounds_dirty) SELECT NEW.zoom_level,0,0,0 WHERE NOT EXISTS(SELECT 1 FROM "
                        + LEVEL
                        + " WHERE z=NEW.zoom_level); ";
        String validate =
                "SELECT CASE WHEN typeof(NEW.zoom_level)<>'integer' OR typeof(NEW.tile_column)<>'integer' OR typeof(NEW.tile_row)<>'integer' OR typeof(NEW.tile_data)<>'blob' OR NEW.zoom_level<0 OR NEW.zoom_level>2147483647 OR NEW.tile_column<0 OR NEW.tile_column>2147483647 OR NEW.tile_row<0 OR NEW.tile_row>2147483647 THEN RAISE(ABORT,'unsupported tile coordinates') END; ";
        String insertConflict =
                "rowid=NEW.rowid OR (zoom_level=NEW.zoom_level AND tile_column=NEW.tile_column AND tile_row=NEW.tile_row)";
        Map<String, String> out = new LinkedHashMap<>();
        out.put(
                "tf_stats_bi",
                "CREATE TRIGGER tf_stats_bi BEFORE INSERT ON tiles BEGIN "
                        + validate
                        + "DELETE FROM "
                        + PENDING
                        + " WHERE kind='I'; INSERT INTO "
                        + PENDING
                        + " SELECT 'I',rowid,zoom_level,tile_column,tile_row,LENGTH(tile_data),0 FROM tiles WHERE "
                        + insertConflict
                        + "; END");
        out.put(
                "tf_stats_bu",
                "CREATE TRIGGER tf_stats_bu BEFORE UPDATE ON tiles BEGIN "
                        + validate
                        + "DELETE FROM "
                        + PENDING
                        + " WHERE kind='U'; INSERT INTO "
                        + PENDING
                        + " SELECT 'U',rowid,zoom_level,tile_column,tile_row,LENGTH(tile_data),0 FROM tiles WHERE rowid<>OLD.rowid AND ("
                        + insertConflict
                        + "); END");
        out.put(
                "tf_stats_ad",
                "CREATE TRIGGER tf_stats_ad AFTER DELETE ON tiles BEGIN "
                        + removeOld(oldIncluded)
                        + "UPDATE "
                        + PENDING
                        + " SET deleted=1 WHERE oldrow=OLD.rowid; END");
        out.put(
                "tf_stats_ai",
                "CREATE TRIGGER tf_stats_ai AFTER INSERT ON tiles BEGIN "
                        + ensure
                        + subtract("I")
                        + addNew(newIncluded)
                        + "DELETE FROM "
                        + PENDING
                        + " WHERE kind='I'; END");
        out.put(
                "tf_stats_au",
                "CREATE TRIGGER tf_stats_au AFTER UPDATE ON tiles BEGIN "
                        + ensure
                        + removeOld(oldIncluded)
                        + subtract("U")
                        + addNew(newIncluded)
                        + "DELETE FROM "
                        + PENDING
                        + "; END");
        return Collections.unmodifiableMap(out);
    }

    private static String subtract(String kind) {
        // NEW.rowid=-1 在 BEFORE INSERT 中也表示自动分配；实际未删除的冲突行不能扣减。
        String filter =
                "kind='"
                        + kind
                        + "' AND deleted=0 AND (oldrow=NEW.rowid OR NOT EXISTS(SELECT 1 FROM tiles t WHERE t.rowid="
                        + PENDING
                        + ".oldrow)) AND (SELECT done=1 OR (started=1 AND cursor>=oldrow) FROM "
                        + STATE
                        + " WHERE id=1)";
        String count =
                "(SELECT count(*) FROM " + PENDING + " WHERE " + filter + " AND z=" + LEVEL + ".z)";
        String boundary =
                "EXISTS(SELECT 1 FROM "
                        + PENDING
                        + " WHERE "
                        + filter
                        + " AND z="
                        + LEVEL
                        + ".z AND (x=minx OR x=maxx OR y=miny OR y=maxy))";
        return "UPDATE "
                + LEVEL
                + " SET bounds_dirty=CASE WHEN n="
                + count
                + " THEN 0 WHEN "
                + boundary
                + " THEN 1 ELSE bounds_dirty END,"
                + "minx=CASE WHEN n="
                + count
                + " THEN NULL ELSE minx END,maxx=CASE WHEN n="
                + count
                + " THEN NULL ELSE maxx END,"
                + "miny=CASE WHEN n="
                + count
                + " THEN NULL ELSE miny END,maxy=CASE WHEN n="
                + count
                + " THEN NULL ELSE maxy END,"
                + "n=n-"
                + count
                + ",bytes=bytes-COALESCE((SELECT sum(bytes) FROM "
                + PENDING
                + " WHERE "
                + filter
                + " AND z="
                + LEVEL
                + ".z),0) WHERE z IN (SELECT z FROM "
                + PENDING
                + " WHERE "
                + filter
                + "); ";
    }

    private static String removeOld(String included) {
        return "UPDATE "
                + LEVEL
                + " SET bounds_dirty=CASE WHEN n=1 THEN 0 WHEN OLD.tile_column=minx OR OLD.tile_column=maxx OR OLD.tile_row=miny OR OLD.tile_row=maxy THEN 1 ELSE bounds_dirty END,"
                + "minx=CASE WHEN n=1 THEN NULL ELSE minx END,maxx=CASE WHEN n=1 THEN NULL ELSE maxx END,"
                + "miny=CASE WHEN n=1 THEN NULL ELSE miny END,maxy=CASE WHEN n=1 THEN NULL ELSE maxy END,"
                + "n=n-1,bytes=bytes-LENGTH(OLD.tile_data) WHERE z=OLD.zoom_level AND "
                + included
                + "; ";
    }

    private static String addNew(String included) {
        return "UPDATE "
                + LEVEL
                + " SET n=n+1,bytes=bytes+LENGTH(NEW.tile_data),"
                + "minx=CASE WHEN minx IS NULL OR NEW.tile_column<minx THEN NEW.tile_column ELSE minx END,"
                + "maxx=CASE WHEN maxx IS NULL OR NEW.tile_column>maxx THEN NEW.tile_column ELSE maxx END,"
                + "miny=CASE WHEN miny IS NULL OR NEW.tile_row<miny THEN NEW.tile_row ELSE miny END,"
                + "maxy=CASE WHEN maxy IS NULL OR NEW.tile_row>maxy THEN NEW.tile_row ELSE maxy END WHERE z=NEW.zoom_level AND "
                + included
                + "; ";
    }
}
