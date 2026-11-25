package cn.geoair.map.tile.forge.fuser.mbtiles.maintenance;

/** AUTO 根据库内 auto_vacuum 状态选用增量或完整回收。 */
public enum MbtilesReclaimMode {
    AUTO,
    INCREMENTAL,
    VACUUM;

    public static MbtilesReclaimMode parse(String value) {
        if (value == null) return AUTO;
        for (MbtilesReclaimMode mode : values())
            if (mode.name().equalsIgnoreCase(value)) return mode;
        throw new IllegalArgumentException("空间回收方式无效");
    }
}
