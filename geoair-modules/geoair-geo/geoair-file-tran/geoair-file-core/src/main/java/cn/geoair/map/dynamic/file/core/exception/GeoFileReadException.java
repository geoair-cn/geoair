package cn.geoair.map.dynamic.file.core.exception;

/** 读取阶段异常。 */
public class GeoFileReadException extends RuntimeException {

    public GeoFileReadException(String message) {
        super(message);
    }

    public GeoFileReadException(String message, Throwable cause) {
        super(message, cause);
    }
}
