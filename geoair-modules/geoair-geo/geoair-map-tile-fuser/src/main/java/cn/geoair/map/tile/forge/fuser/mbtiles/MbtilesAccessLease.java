package cn.geoair.map.tile.forge.fuser.mbtiles;

/** 可由任务收尾线程归还，避免控制线程等待工作线程时发生读写锁升级死锁。 */
public final class MbtilesAccessLease implements AutoCloseable {
    private MbtilesAccessState state;
    private final MbtilesAccessKind kind;

    MbtilesAccessLease(MbtilesAccessState state, MbtilesAccessKind kind) {
        this.state = state;
        this.kind = kind;
    }

    @Override
    public synchronized void close() {
        if (state != null) {
            MbtilesAccessCoordinator.release(state, kind);
            state = null;
        }
    }
}
