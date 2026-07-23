package cn.geoair.map.dynamic.file.core.exception;

/** 写入阶段异常。 */
public class GeoFileWriteException extends RuntimeException {

    public GeoFileWriteException(String message) {
        super(message);
    }

    public GeoFileWriteException(String message, Throwable cause) {
        super(message, cause);
    }
}
