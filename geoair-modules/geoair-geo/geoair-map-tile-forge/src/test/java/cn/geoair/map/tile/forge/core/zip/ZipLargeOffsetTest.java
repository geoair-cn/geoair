package cn.geoair.map.tile.forge.core.zip;

import static org.junit.Assert.*;

import cn.geoair.map.tile.forge.core.config.ZipReadOptions;
import cn.geoair.map.tile.forge.core.zip.model.EocdInfo;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

public class ZipLargeOffsetTest {
    @Test
    public void scans800GbOffsetAcrossConfiguredBlocksWithoutReadingFileBodies() throws Exception {
        long offset = 800L * 1024 * 1024 * 1024;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (int i = 0; i < 100; i++) {
            byte[] name = ("目录/" + i + "/a+b.glb").getBytes(StandardCharsets.UTF_8);
            ByteBuffer entry =
                    ByteBuffer.allocate(46 + name.length + 12).order(ByteOrder.LITTLE_ENDIAN);
            entry.putInt(0, 0x02014b50);
            entry.putShort(8, (short) 0x0800);
            entry.putShort(28, (short) name.length);
            entry.putShort(30, (short) 12);
            entry.putInt(42, -1);
            entry.position(46);
            entry.put(name).putShort((short) 1).putShort((short) 8).putLong(offset - i - 1);
            output.write(entry.array());
        }
        SparseZipHandler handler = new SparseZipHandler(offset, output.toByteArray());
        EocdInfo eocd = new EocdInfo(0L, 0L, 100L, 100L, (long) output.size(), offset, 0L);
        ZipReadOptions options = ZipReadOptions.getInstance();
        int previous = options.getDirectoryBlockBytes();
        options.setDirectoryBlockBytes(4096);
        try {
            AtomicInteger count = new AtomicInteger();
            handler.scanAllEntries(
                    eocd,
                    "virtual",
                    (entry, total, current) -> {
                        assertEquals(offset - count.get() - 1, entry.getLocalHeaderOffset());
                        assertEquals("目录/" + count.getAndIncrement() + "/a+b.glb", entry.getName());
                        assertNull(entry.getDataOffset());
                        return true;
                    });
            assertEquals(100, count.get());
            assertEquals((output.size() + 4095) / 4096, handler.reads);
        } finally {
            options.setDirectoryBlockBytes(previous);
        }
    }
}
