package cn.geoair.map.tile.forge.core.caches;

import java.io.IOException;
import java.nio.file.Path;

/** 大型派生文件的流式备份扩展，避免把整份索引复制到 Java 堆。 */
public interface FileCacheProvider extends CacheProvider {
    void putFile(Object key, Path source, long milliseconds) throws IOException;

    boolean copyTo(Object key, Path target) throws IOException;
}
