package cn.geoair.map.tile.forge.core.zip;

public class ZipReadException extends RuntimeException {
    private final ZipReadFailure failure;

    public ZipReadException(ZipReadFailure failure, String message, Throwable cause) {
        super(message, cause);
        this.failure = failure;
    }

    public ZipReadFailure getFailure() { return failure; }
}
