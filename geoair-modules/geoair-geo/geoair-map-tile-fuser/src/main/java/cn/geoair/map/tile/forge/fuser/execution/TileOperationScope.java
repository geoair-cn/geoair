package cn.geoair.map.tile.forge.fuser.execution;

/** 在调用线程绑定上下文，退出后恢复嵌套调用之前的上下文。 */
public final class TileOperationScope implements AutoCloseable {
    private final TileOperationContext previous;

    TileOperationScope(TileOperationContext previous) {
        this.previous = previous;
    }

    @Override
    public void close() {
        TileOperationContext.restore(previous);
    }
}
