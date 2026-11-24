package cn.geoair.map.tile.forge.fuser.mbtiles;

import java.nio.file.Path;

/** 单个物理文件的维护状态；只在自身监视器下更新计数。 */
final class MbtilesAccessState {
    final Path path;
    int operations;
    int tasks;
    int reservations;
    Thread maintenanceOwner;
    boolean pending;

    MbtilesAccessState(Path path) {
        this.path = path;
    }
}
