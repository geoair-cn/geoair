package cn.geoair.map.tile.forge.fuser.mbtiles.maintenance;

/** 只开放已核对的 SQLite 检查点模式。 */
public enum MbtilesCheckpointMode {
    PASSIVE,
    TRUNCATE;

    public static MbtilesCheckpointMode parse(String value) {
        if (value == null) return PASSIVE;
        for (MbtilesCheckpointMode mode : values())
            if (mode.name().equalsIgnoreCase(value)) return mode;
        throw new IllegalArgumentException("WAL合并方式无效");
    }
}
