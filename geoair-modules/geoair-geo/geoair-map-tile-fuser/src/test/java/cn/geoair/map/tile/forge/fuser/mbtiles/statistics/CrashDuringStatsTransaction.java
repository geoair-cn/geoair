package cn.geoair.map.tile.forge.fuser.mbtiles.statistics;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/** 测试进程在源瓦片、统计、游标同时修改且尚未提交时异常退出。 */
public final class CrashDuringStatsTransaction {
    public static void main(String[] args) throws Exception {
        Connection c = DriverManager.getConnection("jdbc:sqlite:" + args[0]);
        Statement s = c.createStatement();
        s.execute("BEGIN IMMEDIATE");
        s.execute("INSERT INTO tiles VALUES(7,7,7,X'0102')");
        s.execute(
                "UPDATE tf_stats_state SET started=1,cursor=9223372036854775807,done=1,scanned=999 WHERE id=1");
        s.execute("UPDATE tf_stats_level SET n=999,bytes=999 WHERE z=7");
        Runtime.getRuntime().halt(23);
    }
}
