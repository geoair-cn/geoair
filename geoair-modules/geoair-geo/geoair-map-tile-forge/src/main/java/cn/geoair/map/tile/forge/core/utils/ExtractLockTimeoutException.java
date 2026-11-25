package cn.geoair.map.tile.forge.core.utils;

/** 保持 RuntimeException 兼容性，同时让调用方无需解析中文文案即可识别等待超时。 */
public class ExtractLockTimeoutException extends RuntimeException {
    public ExtractLockTimeoutException(long timeoutMillis) {
        super("等待文件抽取锁超时（" + timeoutMillis + "ms）");
    }
}
