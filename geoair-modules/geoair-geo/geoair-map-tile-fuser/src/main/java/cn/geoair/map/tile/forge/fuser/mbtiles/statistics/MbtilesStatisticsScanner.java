package cn.geoair.map.tile.forge.fuser.mbtiles.statistics;


import java.io.IOException;
import cn.geoair.map.tile.forge.fuser.mbtiles.MbtilesAccessCoordinator;
import cn.geoair.map.tile.forge.fuser.mbtiles.MbtilesAccessLease;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationContext;

/**
 * 对原 MBTiles 只读；侧车与每批游标和汇总同事务提交。
 * 每批提交之间用 data_version 检测任何并发事务，结果仅表示扫描时点。
 */

public class MbtilesStatisticsScanner {
    private final int batchSize;
    public MbtilesStatisticsScanner() { this(new MbtilesStatisticsOptions()); }
    public MbtilesStatisticsScanner(MbtilesStatisticsOptions options) { batchSize = options.copy().getReadOnlyBatchSize(); }

    /** 开始新扫描，旧的不完整快照不自动信任。调用方负责同一源和侧车只运行一个任务。 */
    public MbtilesScanProgress scan(Path source,Path sidecar,Consumer<MbtilesScanProgress> listener) throws SQLException,IOException {
        TileOperationContext.checkCurrent();
        if (!Files.isRegularFile(source)) throw new IOException("MBTiles 文件不存在");
        if (sidecar != null && (source.toAbsolutePath().normalize().equals(sidecar.toAbsolutePath().normalize())
                || (Files.exists(sidecar) && Files.isSameFile(source, sidecar))))
            throw new IOException("统计侧车不能与源文件相同");
        Path parent=sidecar == null ? null : sidecar.toAbsolutePath().getParent();
        if(parent!=null) Files.createDirectories(parent);
        try (Connection target=DriverManager.getConnection(sidecar == null ? "jdbc:sqlite::memory:" : "jdbc:sqlite:"+sidecar.toUri());
             Connection input=DriverManager.getConnection("jdbc:sqlite:"+source.toUri()+"?mode=ro")) {
            requireRowIdTable(input);
            initialize(target);
            String initial=fingerprint(source);
            int sourceVersion=version(input);
            Map<Integer,CacheZoomStatistics> levels=new TreeMap<>();
            long cursor=Long.MIN_VALUE, processed=0;
            boolean first=true;
            Integer zoom=null;
            save(target,MbtilesScanState.RUNNING.name(),cursor,processed,zoom,initial,levels);
            if(listener!=null) listener.accept(snapshot(MbtilesScanState.RUNNING.name(),cursor,processed,zoom,initial,levels));
            if(Thread.currentThread().isInterrupted()) {
                save(target,MbtilesScanState.INTERRUPTED.name(),cursor,processed,zoom,initial,levels);
                return snapshot(MbtilesScanState.INTERRUPTED.name(),cursor,processed,zoom,initial,levels);
            }
            List<CacheZoomStatistics> overview = coordinateOverview(input, source, items -> {
                if (listener != null) {
                    MbtilesScanProgress partial = snapshot(MbtilesScanState.RUNNING.name(),Long.MIN_VALUE,0,null,initial,Collections.emptyMap());
                    partial.setCoordinateOverview(items); listener.accept(partial);
                }
            });
            while(true) {
                TileOperationContext.checkCurrent();
                if(Thread.currentThread().isInterrupted()) {
                    save(target,MbtilesScanState.INTERRUPTED.name(),cursor,processed,zoom,initial,levels);
                    return snapshot(MbtilesScanState.INTERRUPTED.name(),cursor,processed,zoom,initial,levels);
                }
                if(version(input)!=sourceVersion || !initial.equals(fingerprint(source))) {
                    save(target,MbtilesScanState.STALE.name(),cursor,processed,zoom,initial,levels);
                    return snapshot(MbtilesScanState.STALE.name(),cursor,processed,zoom,initial,levels);
                }
                int count=0;
                // 行数仍按 rowid 有界扫描；在 SQLite 内分级汇总，避免每片瓦片经过 JDBC。
                String invalid="typeof(zoom_level)<>'integer' OR typeof(tile_column)<>'integer' OR typeof(tile_row)<>'integer'"
                        +" OR zoom_level<0 OR zoom_level>2147483647 OR tile_column<0 OR tile_column>2147483647"
                        +" OR tile_row<0 OR tile_row>2147483647";
                try(MbtilesAccessLease lease=MbtilesAccessCoordinator.backgroundRead(source);
                    PreparedStatement query=input.prepareStatement(
                        "SELECT zoom_level,COUNT(*),SUM(tile_bytes),MIN(tile_column),MAX(tile_column),MIN(tile_row),MAX(tile_row),MAX(rid),SUM(invalid) FROM ("
                                +"SELECT rowid AS rid,zoom_level,tile_column,tile_row,COALESCE(LENGTH(tile_data),0) AS tile_bytes,CASE WHEN "
                                +invalid+" THEN 1 ELSE 0 END AS invalid FROM tiles "
                                +(first ? "" : "WHERE rowid>? ")+"ORDER BY rowid LIMIT ?) GROUP BY zoom_level")) {
                    int parameter=1;
                    if (!first) query.setLong(parameter++,cursor);
                    query.setInt(parameter,batchSize);
                    try(ResultSet rows=query.executeQuery()) {
                        while(rows.next()) {
                            if (rows.getLong(9)>0) throw new SQLException("MBTiles 瓦片坐标不是受支持的非负整数");
                            int z=rows.getInt(1); long groupCount=rows.getLong(2), groupCursor=rows.getLong(8);
                            CacheZoomStatistics level=levels.get(z);
                            if(level==null){level=new CacheZoomStatistics(z);levels.put(z,level);}
                            level.setTileCount(Math.addExact(level.getTileCount(),groupCount));
                            level.setSize(Math.addExact(level.getSize(),rows.getLong(3)));
                            level.addGridCoordinate(rows.getInt(4),rows.getInt(6));
                            level.addGridCoordinate(rows.getInt(5),rows.getInt(7));
                            if(zoom==null || groupCursor>cursor){cursor=groupCursor;zoom=z;}
                            processed=Math.addExact(processed,groupCount);count=Math.addExact(count,(int)groupCount);
                        }
                    }
                }
                first=false;
                if(version(input)!=sourceVersion || !initial.equals(fingerprint(source))) {
                    save(target,MbtilesScanState.STALE.name(),cursor,processed,zoom,initial,levels);
                    return snapshot(MbtilesScanState.STALE.name(),cursor,processed,zoom,initial,levels);
                }
                String state=count==0?MbtilesScanState.COMPLETED.name():MbtilesScanState.RUNNING.name();
                save(target,state,cursor,processed,zoom,initial,levels);
                if(version(input)!=sourceVersion || !initial.equals(fingerprint(source))) {
                    save(target,MbtilesScanState.STALE.name(),cursor,processed,zoom,initial,levels);
                    return snapshot(MbtilesScanState.STALE.name(),cursor,processed,zoom,initial,levels);
                }
                MbtilesScanProgress progress=snapshot(state,cursor,processed,zoom,initial,levels);
                progress.setCoordinateOverview(overview);
                if(listener!=null) listener.accept(progress);
                if(count==0) return progress;
                // 只读扫描也会争用磁盘，每批交还事务后短暂让出 I/O。
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(5));
            }
        }
    }

    /** rowid 游标只适用于普通表；视图中的空 rowid 会导致漏扫后误报完成。 */
    private static void requireRowIdTable(Connection db) throws SQLException {
        try (Statement s=db.createStatement();ResultSet r=s.executeQuery("SELECT type,sql FROM sqlite_master WHERE name='tiles'")) {
            if (!r.next() || !"table".equals(r.getString(1))) throw new SQLException("只读统计暂不支持 tiles 视图或缺失的 tiles 表");
            String ddl=String.valueOf(r.getString(2)).toUpperCase(Locale.ROOT);
            if (ddl.matches("(?s).*WITHOUT\\s+ROWID.*") || ddl.matches("(?s).*CREATE\\s+VIRTUAL\\s+TABLE.*"))
                throw new SQLException("只读统计需要普通 rowid tiles 表");
        }
        try (Statement s=db.createStatement();ResultSet r=s.executeQuery("PRAGMA table_info(tiles)")) {
            while(r.next()) if("rowid".equalsIgnoreCase(r.getString("name")))
                throw new SQLException("tiles 自定义 rowid 列不支持作为统计游标");
        }
    }

    /** 只使用已有的完整坐标索引，不为只读统计建索引，也不回表读取图片列。 */
    List<CacheZoomStatistics> coordinateOverview(Connection db, Consumer<List<CacheZoomStatistics>> listener) throws SQLException {
        try { return coordinateOverview(db,null,listener); }
        catch(IOException failure) { throw new SQLException("统计等待维护失败",failure); }
    }
    private List<CacheZoomStatistics> coordinateOverview(Connection db,Path source,Consumer<List<CacheZoomStatistics>> listener) throws SQLException,IOException {
        String index = null;
        List<String> candidates = new ArrayList<>();
        try (Statement s = db.createStatement(); ResultSet r = s.executeQuery("PRAGMA index_list(tiles)")) {
            while (r.next()) if (r.getInt("partial") == 0 && r.getInt("unique") == 1) candidates.add(r.getString("name"));
        }
        for (String candidate : candidates) {
            List<String> names = new ArrayList<>();
            try (Statement s = db.createStatement(); ResultSet r = s.executeQuery("PRAGMA index_info(" + quote(candidate) + ")")) {
                while (r.next()) names.add(r.getString("name"));
            }
            if (names.equals(Arrays.asList("zoom_level","tile_column","tile_row"))) {
                index = candidate; break;
            }
        }
        if (index == null) return null; // 不支持的结构直接使用原来的有界扫描。
        Map<Integer,CacheZoomStatistics> overview = new TreeMap<>();
        int sourceVersion = version(db);
        boolean first = true; long z = 0, x = 0, y = 0;
        while (true) {
            TileOperationContext.checkCurrent();
            int count = 0;
            // 每批关闭读事务，避免统计在旧 DELETE 模式缓存库上长期阻塞下载写入。
            try (MbtilesAccessLease lease=source==null ? null : MbtilesAccessCoordinator.backgroundRead(source);
                 PreparedStatement s = db.prepareStatement("SELECT zoom_level,tile_column,tile_row,typeof(zoom_level),typeof(tile_column),typeof(tile_row) FROM tiles INDEXED BY "
                    + quote(index) + (first ? "" : " WHERE (zoom_level,tile_column,tile_row)>(?,?,?)")
                    + " ORDER BY zoom_level,tile_column,tile_row LIMIT " + batchSize)) {
                if (!first) { s.setLong(1,z); s.setLong(2,x); s.setLong(3,y); }
                try (ResultSet r = s.executeQuery()) {
                    while (r.next()) {
                        if (Thread.currentThread().isInterrupted()) return null;
                        z=r.getLong(1); x=r.getLong(2); y=r.getLong(3);
                        if (!"integer".equals(r.getString(4)) || !"integer".equals(r.getString(5)) || !"integer".equals(r.getString(6))
                                || z<0 || z>Integer.MAX_VALUE || x<0 || x>Integer.MAX_VALUE || y<0 || y>Integer.MAX_VALUE)
                            throw new SQLException("MBTiles 瓦片坐标不是受支持的非负整数");
                        CacheZoomStatistics level=overview.computeIfAbsent((int)z, CacheZoomStatistics::new);
                        level.setTileCount(Math.addExact(level.getTileCount(),1)); level.setSize(-1);
                        level.addGridCoordinate((int)x,(int)y); count++;
                    }
                }
            }
            first=false;
            if (version(db) != sourceVersion) return null;
            if (listener != null) listener.accept(snapshot(MbtilesScanState.RUNNING.name(),0,0,null,"",overview).getLevels());
            if (count<batchSize) return snapshot(MbtilesScanState.RUNNING.name(),0,0,null,"",overview).getLevels();
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(5));
        }
    }
    private static String quote(String name) { return "\"" + name.replace("\"", "\"\"") + "\""; }

    /** 读取上次保存的进度。RUNNING 代表进程意外退出，不能未经重新校验续扫。 */
    public MbtilesScanProgress read(Path source,Path sidecar) throws SQLException,IOException {
        if(!Files.isRegularFile(sidecar)) return null;
        try(Connection connection=DriverManager.getConnection("jdbc:sqlite:"+sidecar.toUri()+"?mode=ro");
            Statement statement=connection.createStatement();
            ResultSet row=statement.executeQuery("SELECT state,cursor,processed,zoom,fingerprint FROM scan_state WHERE id=1")) {
            if(!row.next()) return null;
            String state=row.getString(1),saved=row.getString(5);
            long cursor=row.getLong(2),processed=row.getLong(3);
            int number=row.getInt(4); Integer zoom=row.wasNull()?null:number;
            if(!Files.isRegularFile(source) || !saved.equals(fingerprint(source))) state=MbtilesScanState.STALE.name();
            else if(MbtilesScanState.RUNNING.name().equals(state)) state=MbtilesScanState.INTERRUPTED.name();
            Map<Integer,CacheZoomStatistics> levels=readLevels(connection);
            return snapshot(state,cursor,processed,zoom,saved,levels);
        }
    }

    private static void initialize(Connection db) throws SQLException {
        try(Statement s=db.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS scan_state(id INTEGER PRIMARY KEY,state TEXT NOT NULL,cursor INTEGER NOT NULL,processed INTEGER NOT NULL,zoom INTEGER,fingerprint TEXT NOT NULL)");
            s.execute("CREATE TABLE IF NOT EXISTS scan_level(zoom INTEGER PRIMARY KEY,count INTEGER NOT NULL,bytes INTEGER NOT NULL,min_x INTEGER,max_x INTEGER,min_y INTEGER,max_y INTEGER)");
        }
    }

    private static void save(Connection db,String state,long cursor,long processed,Integer zoom,String fingerprint,
                             Map<Integer,CacheZoomStatistics> levels) throws SQLException {
        db.setAutoCommit(false);
        try(PreparedStatement st=db.prepareStatement("INSERT OR REPLACE INTO scan_state VALUES(1,?,?,?,?,?)");
            PreparedStatement level=db.prepareStatement("INSERT OR REPLACE INTO scan_level VALUES(?,?,?,?,?,?,?)")) {
            st.setString(1,state);st.setLong(2,cursor);st.setLong(3,processed);
            if(zoom==null)st.setNull(4,Types.INTEGER);else st.setInt(4,zoom);
            st.setString(5,fingerprint);st.executeUpdate();
            try(Statement clear=db.createStatement()){clear.executeUpdate("DELETE FROM scan_level");}
            for(CacheZoomStatistics v:levels.values()) {
                level.setInt(1,v.getZoomLevel());level.setLong(2,v.getTileCount());level.setLong(3,v.getSize());
                level.setInt(4,v.getMinX());level.setInt(5,v.getMaxX());level.setInt(6,v.getMinY());level.setInt(7,v.getMaxY());level.addBatch();
            }
            level.executeBatch();db.commit();
        } catch(SQLException e) {db.rollback();throw e;} finally {db.setAutoCommit(true);}
    }

    private static Map<Integer,CacheZoomStatistics> readLevels(Connection db) throws SQLException {
        Map<Integer,CacheZoomStatistics> result=new TreeMap<>();
        try(Statement s=db.createStatement();ResultSet r=s.executeQuery("SELECT zoom,count,bytes,min_x,max_x,min_y,max_y FROM scan_level ORDER BY zoom")) {
            while(r.next()) {
                CacheZoomStatistics level=new CacheZoomStatistics(r.getInt(1));level.setTileCount(r.getLong(2));level.setSize(r.getLong(3));
                level.setGridRange(r.getInt(4),r.getInt(5),r.getInt(6),r.getInt(7));result.put(level.getZoomLevel(),level);
            }
        }
        return result;
    }
    private static int version(Connection input) throws SQLException {
        try(Statement s=input.createStatement();ResultSet r=s.executeQuery("PRAGMA data_version")) {r.next();return r.getInt(1);}
    }
    /** 文件元数据只作为变化探测，不保证可检测伪造时间戳的替换。 */
    public String fingerprint(Path path) throws IOException {
        return attributes(path)+"|"+attributes(path.resolveSibling(path.getFileName()+"-wal"));
    }
    private String attributes(Path path) throws IOException {
        if(!Files.exists(path)) return "missing";
        BasicFileAttributes a=Files.readAttributes(path,BasicFileAttributes.class);
        return String.valueOf(a.fileKey())+":"+a.size()+":"+a.lastModifiedTime().toString();
    }
    private static MbtilesScanProgress snapshot(String state,long cursor,long count,Integer zoom,String fingerprint,
                                                 Map<Integer,CacheZoomStatistics> levels) {
        List<CacheZoomStatistics> copies=new ArrayList<>();
        for (CacheZoomStatistics original : levels.values()) {
            CacheZoomStatistics copy=new CacheZoomStatistics(original.getZoomLevel());
            copy.setTileCount(original.getTileCount());copy.setSize(original.getSize());
            if(original.getMinX()!=null) copy.setGridRange(original.getMinX(),original.getMaxX(),original.getMinY(),original.getMaxY());
            copies.add(copy);
        }
        return new MbtilesScanProgress(state,count,cursor,zoom,fingerprint,Collections.unmodifiableList(copies));
    }
}
