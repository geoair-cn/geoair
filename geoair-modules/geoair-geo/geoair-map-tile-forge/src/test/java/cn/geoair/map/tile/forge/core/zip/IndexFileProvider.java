package cn.geoair.map.tile.forge.core.zip;

import cn.geoair.map.tile.forge.core.caches.FileCacheProvider;
import cn.geoair.map.tile.forge.core.caches.NoOpCacheProvider;
import java.io.IOException;
import java.nio.file.*;

/** 临时文件模拟流式备份，禁止回退整库字节数组。 */
final class IndexFileProvider extends NoOpCacheProvider implements FileCacheProvider {
    final Path backup;
    IndexFileProvider(Path backup) {this.backup=backup;}
    @Override public void putFile(Object key,Path source,long milliseconds) throws IOException {
        Files.copy(source,backup,StandardCopyOption.REPLACE_EXISTING);
    }
    @Override public boolean copyTo(Object key,Path target) throws IOException {
        if(!Files.isRegularFile(backup)) return false;
        Files.copy(backup,target,StandardCopyOption.REPLACE_EXISTING);return true;
    }
    @Override public boolean exists(Object key) {return Files.isRegularFile(backup);}
    @Override public Object getObject(Object key) {throw new AssertionError("不应整库读入字节数组");}
    @Override public void put(Object key,Object value,long milliseconds) {throw new AssertionError("不应整库写入字节数组");}
}
