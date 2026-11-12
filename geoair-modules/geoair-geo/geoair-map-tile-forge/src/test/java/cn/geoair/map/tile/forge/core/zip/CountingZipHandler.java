package cn.geoair.map.tile.forge.core.zip;

import cn.geoair.map.tile.forge.core.zip.model.CentralDirectoryModel;
import cn.geoair.map.tile.forge.core.zip.model.EocdInfo;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.atomic.AtomicInteger;

class CountingZipHandler extends LocalCompressionHandler {
    final AtomicInteger extractions = new AtomicInteger();
    final AtomicInteger searches = new AtomicInteger();
    final AtomicInteger scans = new AtomicInteger();
    volatile boolean fail;
    volatile boolean mutate;
    @Override public CentralDirectoryModel findEntryInCentralDir(EocdInfo eocd, String path, String source) throws IOException {
        searches.incrementAndGet(); return super.findEntryInCentralDir(eocd, path, source);
    }
    @Override public void scanAllEntries(EocdInfo eocd, String source, TerminatingConsumer<CentralDirectoryModel> consumer) throws IOException {
        scans.incrementAndGet(); super.scanAllEntries(eocd, source, consumer);
    }
    @Override public void readAndDecompressEntryToLocal(CentralDirectoryModel entry, String source, String output) throws IOException {
        extractions.incrementAndGet();
        if (fail) { Files.write(Paths.get(output), new byte[]{1}); throw new IOException("模拟提取中途失败"); }
        super.readAndDecompressEntryToLocal(entry, source, output);
        if (mutate) Files.setLastModifiedTime(Paths.get(source), FileTime.fromMillis(System.currentTimeMillis() + 60000));
    }
}
