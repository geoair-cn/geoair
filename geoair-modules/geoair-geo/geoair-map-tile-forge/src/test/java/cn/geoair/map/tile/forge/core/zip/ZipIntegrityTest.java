package cn.geoair.map.tile.forge.core.zip;

import cn.geoair.map.tile.forge.core.zip.model.CentralDirectoryModel;
import cn.geoair.map.tile.forge.core.zip.decompression.ZipHandler;
import cn.geoair.map.tile.forge.core.zip.decompression.UncompressedHandler;
import org.junit.Test;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import static org.junit.Assert.*;

public class ZipIntegrityTest {
    @Test public void acceptsMaximumCommentAndIgnoresFakeEndRecord() throws Exception {
        char[] chars = new char[65535];
        Arrays.fill(chars, 'x');
        for (String comment : Arrays.asList(new String(chars), "PK\u0005\u0006" + new String(new char[18]))) {
            H h = new H(zip("layer.json", new byte[]{1,2,3}, false, comment));
            assertEquals(1, h.parseEocd(h.bytes.length, "test").getTotalEntries());
            assertArrayEquals(new byte[]{1,2,3}, h.readFileFromZip("test", "layer.json"));
        }
    }

    @Test public void preservesExtensionlessFilesAndEmptyFiles() throws Exception {
        for (String name : Arrays.asList("LICENSE", "dir/empty.txt")) {
            H h = new H(zip(name, new byte[0], true, null));
            CentralDirectoryModel e = entry(h);
            assertEquals(name, e.getName());
            assertFalse(e.isDirectoryIs());
            assertArrayEquals(new byte[0], h.readFileFromZip("test", name));
        }
        H dir = new H(zip("folder/", new byte[0], true, null));
        assertTrue(entry(dir).isDirectoryIs());
        assertEquals(Arrays.asList("folder"), dir.checkedPathsInZip("test", Arrays.asList("folder")));
    }

    @Test public void readsOldCacheWithoutCrcForStoredAndDescriptorEntries() throws Exception {
        for (boolean stored : new boolean[]{true, false}) {
            H h = new H(zip("dir/a.bin", new byte[]{1,2,3}, stored, null));
            CentralDirectoryModel e = entry(h);
            e.setCrc32(null); // 模拟SQLite/PG旧记录
            assertArrayEquals(new byte[]{1,2,3}, h.readAndDecompressEntry(e, "test"));
            assertArrayEquals(new byte[]{1,2,3}, h.readAndDecompressEntry(e, "test"));
        }
    }

    @Test public void rejectsCorruptStoredContentsWithAndWithoutCachedCrc() throws Exception {
        for (boolean oldCache : new boolean[]{false, true}) {
            H h = new H(zip("a.bin", new byte[]{1,2,3}, true, null));
            CentralDirectoryModel e = entry(h);
            h.bytes[30 + "a.bin".length()] ^= 1;
            if (oldCache) e.setCrc32(null);
            expectIo(() -> h.readAndDecompressEntry(e, "test"), "CRC");
        }
    }

    @Test public void rejectsCorruptLocalHeaderWithoutSearchingOrGuessing() throws Exception {
        H h = new H(zip("a.bin", new byte[]{1}, true, null));
        CentralDirectoryModel e = entry(h);
        h.bytes[0] = 0;
        int before = h.reads;
        expectIo(() -> h.readAndDecompressEntry(e, "test"), "签名");
        assertEquals(1, h.reads - before);
    }

    @Test public void rejectsStaleCachedOffsetsAndOutOfBoundsData() throws Exception {
        H h = new H(zip("a.bin", new byte[]{1}, true, null));
        CentralDirectoryModel e = entry(h);
        e.setDataOffset(1L);
        expectIo(() -> h.readAndDecompressEntry(e, "test"), "失效");
        e.setDataOffset(null);
        e.setCompressedSize(h.bytes.length);
        expectIo(() -> h.readAndDecompressEntry(e, "test"), "范围");
    }

    @Test public void validatesDeflateEndAndStoredLengthIncludingEmptyFiles() throws Exception {
        Deflater deflater = new Deflater(6, true);
        byte[] buffer = new byte[100];
        deflater.setInput(new byte[]{1,2,3}); deflater.finish();
        int compressedSize = deflater.deflate(buffer);
        byte[] compressed = Arrays.copyOf(buffer, compressedSize);
        deflater.end();
        assertArrayEquals(new byte[]{1,2,3}, new ZipHandler().decompress(compressed, 3));
        expectIo(() -> new ZipHandler().decompress(compressed, 2), "超过");
        expectIo(() -> new ZipHandler().decompress(Arrays.copyOf(compressed, compressed.length-1), 3), "不完整");
        expectIo(() -> new UncompressedHandler().decompress(new byte[]{1}, 0), "不匹配");
        H empty = new H(zip("empty", new byte[0], false, null));
        assertArrayEquals(new byte[0], empty.readFileFromZip("test", "empty"));
    }

    @Test public void acceptsEmptyZipAndRejectsTooShortInput() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new ZipOutputStream(out).close();
        H h = new H(out.toByteArray());
        assertEquals(0, h.parseEocd(h.bytes.length, "test").getTotalEntries());
        expectIo(() -> new H(new byte[3]).parseEocd(3, "test"), "22");
    }

    @Test public void sharesUnicodeExtraFieldDecodingAcrossLookupAndScan() throws Exception {
        String unicode = "地形/layer.json";
        byte[] alias = "alias.json".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        byte[] name = unicode.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        CRC32 crc = new CRC32(); crc.update(alias);
        java.nio.ByteBuffer extra = java.nio.ByteBuffer.allocate(9 + name.length).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        extra.putShort((short) 0x7075).putShort((short) (5 + name.length)).put((byte) 1).putInt((int) crc.getValue()).put(name);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream z = new ZipOutputStream(out, java.nio.charset.Charset.forName("GBK"))) {
            ZipEntry e = new ZipEntry("alias.json"); e.setExtra(extra.array());
            z.putNextEntry(e); z.write(42); z.closeEntry();
        }
        H h = new H(out.toByteArray());
        assertEquals(unicode, entry(h).getName());
        assertArrayEquals(new byte[]{42}, h.readFileFromZip("test", unicode));
        assertEquals(Arrays.asList(unicode), h.checkedPathsInZip("test", Arrays.asList(unicode)));
    }

    private static CentralDirectoryModel entry(H h) throws Exception {
        CentralDirectoryModel[] result = {null};
        h.scanAllEntries("test", (e,t,i) -> {result[0]=e;return false;});
        return result[0];
    }
    private interface IoAction { void run() throws IOException; }
    private static void expectIo(IoAction action, String text) throws Exception {
        try { action.run(); fail("Expected IOException"); }
        catch (IOException e) { assertTrue(e.getMessage(), e.getMessage().contains(text)); }
    }
    private static byte[] zip(String name, byte[] data, boolean stored, String comment) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream z = new ZipOutputStream(out)) {
            if (comment != null) z.setComment(comment);
            ZipEntry e = new ZipEntry(name);
            if (stored) {
                CRC32 crc = new CRC32(); crc.update(data);
                e.setMethod(ZipEntry.STORED); e.setSize(data.length); e.setCompressedSize(data.length); e.setCrc(crc.getValue());
            }
            z.putNextEntry(e); z.write(data); z.closeEntry();
        }
        return out.toByteArray();
    }
    private static class H extends AbstractZipCompressionHandler {
        final byte[] bytes; int reads;
        H(byte[] bytes) { this.bytes=bytes; }
        public long getFileSize(String s) { return bytes.length; }
        protected byte[] readRange(String s, long start, long end) throws IOException {
            reads++;
            if (start < 0 || end >= bytes.length || end < start) throw new IOException("range");
            return Arrays.copyOfRange(bytes, (int) start, (int) end+1);
        }
    }
}
