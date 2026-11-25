package cn.geoair.map.tile.forge.fuser.encoding;

/** 编码作用域仅由获得它的线程按嵌套顺序归还，避免串入线程池的下一项任务。 */
public final class TileEncodingScope implements AutoCloseable {
    private final Thread owner = Thread.currentThread();
    private final TileEncodingContext previous;
    private final TileEncodingContext installed;
    private boolean closed;

    TileEncodingScope(TileEncodingContext previous, TileEncodingContext installed) {
        this.previous = previous;
        this.installed = installed;
    }

    @Override
    public void close() {
        if (Thread.currentThread() != owner) throw new IllegalStateException("编码作用域必须由原线程关闭");
        if (!closed) {
            TileEncodingContext.restore(previous, installed);
            closed = true;
        }
    }
}
