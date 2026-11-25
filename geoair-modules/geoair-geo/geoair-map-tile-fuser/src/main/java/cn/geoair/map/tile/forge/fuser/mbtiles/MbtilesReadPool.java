package cn.geoair.map.tile.forge.fuser.mbtiles;

import com.alibaba.druid.pool.DruidDataSource;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Objects;

/** 同一文件的只读连接池及借用状态，由 DAO 的锁统一保护。 */
final class MbtilesReadPool {


    final DruidDataSource dataSource = new DruidDataSource();
    private final Object fileIdentity;
    int readers;
    long lastUsedNanos;

    MbtilesReadPool(Path path, BasicFileAttributes attributes, MbtilesReadOptions options) {
        fileIdentity = identity(attributes);
        dataSource.setDriverClassName("org.sqlite.JDBC");
        dataSource.setUrl("jdbc:sqlite:" + path.toUri().toASCIIString() + "?mode=ro");
        dataSource.setMaxActive(options.getConnectionsPerFile());
        dataSource.setMaxWait(options.getPoolWaitMillis());
        dataSource.setTestWhileIdle(false);
        dataSource.setValidationQuery("SELECT 1");
        // 控制每个原生 SQLite 连接的页缓存，不主动映射大型源文件。
        dataSource.setConnectionInitSqls(Arrays.asList(
                "PRAGMA query_only=ON", "PRAGMA cache_size=-" + options.getPageCacheKiB(), "PRAGMA mmap_size=0",
                "PRAGMA busy_timeout=" + options.getPoolWaitMillis()));
    }

    boolean matches(BasicFileAttributes attributes) {
        return Objects.equals(fileIdentity, identity(attributes));
    }

    private Object identity(BasicFileAttributes attributes) {
        Object key = attributes.fileKey();
        return key == null ? attributes.creationTime() : key;
    }
}
