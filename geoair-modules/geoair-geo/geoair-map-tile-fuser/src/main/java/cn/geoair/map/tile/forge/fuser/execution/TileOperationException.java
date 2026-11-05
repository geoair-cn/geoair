package cn.geoair.map.tile.forge.fuser.execution;

/** 取消与超时不能被当作缓存未命中继续下载。 */
public class TileOperationException extends RuntimeException {
    private final TileOperationStatus status;

    public TileOperationException(TileOperationStatus status, String message) {
        super(message);
        this.status = status;
    }

    public TileOperationStatus getStatus() {
        return status;
    }
}
