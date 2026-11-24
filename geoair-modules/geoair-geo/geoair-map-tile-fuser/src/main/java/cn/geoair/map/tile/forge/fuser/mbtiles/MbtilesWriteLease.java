package cn.geoair.map.tile.forge.fuser.mbtiles;

/** 写入或统计的一次占用，必须在获取它的线程归还。 */
public final class MbtilesWriteLease implements AutoCloseable {
    private MbtilesWriteCoordinator gate;
    private final MbtilesAccessLease access;

    MbtilesWriteLease(MbtilesWriteCoordinator gate, MbtilesAccessLease access) {
        this.gate = gate;
        this.access = access;
    }

    public boolean hasWaitingWrites() {
        return gate != null && gate.hasWaitingWrites();
    }

    @Override
    public void close() {
        if (gate != null) {
            gate.release();
            gate = null;
            access.close();
        }
    }
}
