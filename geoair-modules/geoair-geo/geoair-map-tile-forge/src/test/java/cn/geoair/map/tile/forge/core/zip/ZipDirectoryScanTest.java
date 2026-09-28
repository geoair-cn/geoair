package cn.geoair.map.tile.forge.core.zip;

import cn.geoair.map.tile.forge.core.zip.model.CentralDirectoryModel;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.*;

public class ZipDirectoryScanTest {
    private static final byte[] CONTENT = "terrain metadata".getBytes(StandardCharsets.UTF_8);

    @Test
    public void scansByBlockAndExtractsLazily() throws Exception {
        MemoryHandler handler = new MemoryHandler(zip(1000));
        List<CentralDirectoryModel> entries = new ArrayList<>();
        handler.scanAllEntries("test", (entry, total, index) -> {
            assertNull(entry.getDataOffset());
            entries.add(entry);
            return true;
        });
        assertEquals(1000, entries.size());
        assertEquals("Only EOCD tail and one directory block", 2, handler.reads);
        assertArrayEquals(CONTENT, handler.readAndDecompressEntry(entries.get(999), "test"));
        assertNotNull(entries.get(999).getDataOffset());
        assertEquals(4, handler.reads);
    }

    @Test
    public void supportsZip64CountAndEntriesCrossingBlockBoundary() throws Exception {
        MemoryHandler handler = new MemoryHandler(zip(66000));
        final long[] count = {0};
        final CentralDirectoryModel[] last = {null};
        final long[] position = {0};
        final String[] boundary = {null};
        handler.scanAllEntries("test", (entry, total, index) -> {
            assertEquals(66000L, total.longValue());
            assertEquals(count[0]++, index.longValue());
            assertEquals(name(index.intValue()), entry.getName());
            last[0] = entry;
            long next = position[0] + entry.getEntrySize();
            if (position[0] < 5 * 1024 * 1024 && next > 5 * 1024 * 1024) boundary[0] = entry.getName();
            position[0] = next;
            return true;
        });
        assertEquals(66000, count[0]);
        assertTrue("Reads must scale with blocks, not entries", handler.reads <= 6);
        assertArrayEquals(CONTENT, handler.readAndDecompressEntry(last[0], "test"));
        assertNotNull(boundary[0]);
        assertArrayEquals(CONTENT, handler.readFileFromZip("test", boundary[0]));
        assertEquals(Arrays.asList(boundary[0]), handler.checkedPathsInZip("test", Arrays.asList(boundary[0])));
    }

    @Test
    public void stopsBeforeReadingNextBlock() throws Exception {
        MemoryHandler handler = new MemoryHandler(zip(66000));
        final int[] count = {0};
        handler.scanAllEntries("test", (entry, total, index) -> {
            count[0]++;
            return false;
        });
        assertEquals(1, count[0]);
        assertEquals("Tail, ZIP64 locator, ZIP64 EOCD and first block", 4, handler.reads);
    }

    @Test
    public void decodesZip64EntryFieldsWithoutReadingLocalHeaders() throws Exception {
        byte[] name = "layer.json".getBytes(StandardCharsets.UTF_8);
        ByteBuffer directory = ByteBuffer.allocate(46 + name.length + 28).order(ByteOrder.LITTLE_ENDIAN);
        directory.putInt(0, 0x02014b50);
        directory.putInt(20, -1);
        directory.putInt(24, -1);
        directory.putShort(28, (short) name.length);
        directory.putShort(30, (short) 28);
        directory.putInt(42, -1);
        directory.position(46);
        directory.put(name).putShort((short) 1).putShort((short) 24);
        directory.putLong(6000000000L).putLong(5000000000L).putLong(7000000000L);
        MemoryHandler handler = new MemoryHandler(directory.array());
        cn.geoair.map.tile.forge.core.zip.model.EocdInfo eocd =
                new cn.geoair.map.tile.forge.core.zip.model.EocdInfo(0L, 0L, 1L, 1L,
                        (long) directory.capacity(), 0L, 0L);
        handler.scanAllEntries(eocd, "test", (entry, total, index) -> {
            assertEquals(6000000000L, entry.getUncompressedSize());
            assertEquals(5000000000L, entry.getCompressedSize());
            assertEquals(7000000000L, entry.getLocalHeaderOffset());
            assertNull(entry.getDataOffset());
            return true;
        });
        assertEquals(1, handler.reads);
    }

    @Test
    public void propagatesConsumerFailures() throws Exception {
        MemoryHandler handler = new MemoryHandler(zip(1));
        try {
            handler.scanAllEntries("test", (entry, total, index) -> {
                throw new IllegalStateException("database failed");
            });
            fail("Must not report a partial cache as successful");
        } catch (IllegalStateException expected) {
            assertEquals("database failed", expected.getMessage());
        }
    }

    @Test
    public void rejectsShortDirectoryReads() throws Exception {
        MemoryHandler handler = new MemoryHandler(zip(1)) {
            @Override
            protected byte[] readRange(String source, long start, long end) throws IOException {
                byte[] bytes = super.readRange(source, start, end);
                return reads == 2 ? Arrays.copyOf(bytes, bytes.length - 1) : bytes;
            }
        };
        try {
            handler.scanAllEntries("test", (entry, total, index) -> true);
            fail("Must reject truncated directory");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("不完整"));
        }
    }

    private static String name(int index) {
        return "中文 terrain/long-directory-for-block-boundary/" + index + "/layer.json";
    }

    private static byte[] zip(int count) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (int i = 0; i < count; i++) {
                zip.putNextEntry(new ZipEntry(name(i)));
                zip.write(CONTENT);
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    private static class MemoryHandler extends AbstractZipCompressionHandler {
        private final byte[] zip;
        int reads;

        MemoryHandler(byte[] zip) { this.zip = zip; }

        @Override
        protected byte[] readRange(String source, long start, long end) throws IOException {
            reads++;
            if (start < 0 || end >= zip.length || start > end) throw new IOException("Invalid range");
            return Arrays.copyOfRange(zip, (int) start, (int) end + 1);
        }

        @Override
        public long getFileSize(String source) { return zip.length; }
    }
}
