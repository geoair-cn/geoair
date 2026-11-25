package cn.geoair.map.tile.forge.fuser.mbtiles.statistics;

import java.sql.SQLException;

@FunctionalInterface
interface SqlWork {
    void execute() throws SQLException;
}
