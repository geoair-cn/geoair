package cn.geoair.map.tile.forge.core.config;

import javax.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** ZIP 按需读取的资源边界；普通 Java 调用也可通过构造实例配置。 */
@Data
@ConfigurationProperties(prefix = "geoair.zip.read")
public class ZipReadOptions {
    private static volatile ZipReadOptions instance;
    private int directoryBlockBytes = 5 * 1024 * 1024;
    private int rootCacheEntries = 256;
    private int entryCacheEntries = 4096;
    private long missingCacheSeconds = 5;
    private long lockTimeoutMillis = 60000;

    public ZipReadOptions() {
        instance = this;
    }

    public static ZipReadOptions getInstance() {
        ZipReadOptions current = instance;
        if (current == null) {
            synchronized (ZipReadOptions.class) {
                current = instance;
                if (current == null) current = new ZipReadOptions();
            }
        }
        return current;
    }

    @PostConstruct
    public void validate() {
        if (directoryBlockBytes < 4096
                || directoryBlockBytes > 16 * 1024 * 1024
                || rootCacheEntries < 1
                || entryCacheEntries < 1
                || missingCacheSeconds < 0
                || lockTimeoutMillis < 1)
            throw new IllegalArgumentException("ZIP读取配置无效：块大小须为4KB至16MB，容量和等待时间须为正数");
    }
}
