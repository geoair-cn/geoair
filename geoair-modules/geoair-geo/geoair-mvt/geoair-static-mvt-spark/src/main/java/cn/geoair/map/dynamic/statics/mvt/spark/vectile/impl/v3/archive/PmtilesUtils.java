package cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v3.archive;

import cn.geoair.map.dynamic.tools.grid.dto.TileYAxis;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

/**
 * PMTiles V3 归档工具。
 *
 * <p>工具从 {@code z/x/y.pbf} 本地目录生成符合 PMTiles V3 规范的单文件归档，
 * 包含 Header、根/叶目录、GZIP JSON 元数据和 MVT 数据。瓦片数据是否 gzip 由调用参数决定。归档必须在
 * Spark Driver 单进程执行，不能由多个 executor 并发写入同一个 PMTiles 文件。</p>
 *
 * @author 张逢吉
 */
public final class PmtilesUtils {

    /** PMTiles V3 固定 Header 字节数。 */
    public static final int HEADER_LENGTH = 127;
    /** Header 加根目录必须不超过 16 KiB。 */
    public static final int MAX_ROOT_DIRECTORY_LENGTH = 16 * 1024 - HEADER_LENGTH;
    private static final byte[] MAGIC = "PMTiles".getBytes(StandardCharsets.UTF_8);
    private static final int VERSION = 3;
    private static final int NO_COMPRESSION = 1;
    private static final int GZIP_COMPRESSION = 2;
    private static final int MVT_TILE_TYPE = 1;

    private PmtilesUtils() {
    }

    /**
     * 将 V3 中间目录归档为 PMTiles V3。
     *
     * @param stagingDirectory Spark 并行输出的 {@code z/x/y.pbf} 目录
     * @param targetFile 目标 {@code .pmtiles} 文件
     * @param stagingYAxis 中间目录使用的 Y 轴约定
     * @param overwrite 是否允许覆盖已有归档
     * @param gzipPbf 瓦片数据是否已经 gzip 压缩
     * @param jsonMetadata PMTiles JSON 元数据；MVT 必须包含 {@code vector_layers}
     */
    public static void archive(
            Path stagingDirectory, Path targetFile, TileYAxis stagingYAxis,
            boolean overwrite, boolean gzipPbf, String jsonMetadata) throws IOException {
        if (targetFile == null) {
            throw new IllegalArgumentException("PMTiles 输出文件不能为空");
        }
        Path archiveFile = targetFile.toAbsolutePath().normalize();
        List<V3TileArchiveFiles.TileFile> files = V3TileArchiveFiles.listTiles(stagingDirectory, stagingYAxis);
        V3TileArchiveFiles.sortByPmtilesId(files);
        List<DirectoryEntry> tileEntries = buildTileEntries(files);
        DirectoryLayout directories = buildDirectories(tileEntries);
        byte[] metadata = gzip(validJson(jsonMetadata).getBytes(StandardCharsets.UTF_8));
        prepareTarget(archiveFile, overwrite);
        Path temporaryFile = archiveFile.resolveSibling(archiveFile.getFileName() + ".tmp-" + UUID.randomUUID());
        try {
            writeArchive(temporaryFile, files, tileEntries, directories, metadata, gzipPbf);
            moveCompletedArchive(temporaryFile, archiveFile, overwrite);
        } catch (Exception e) {
            Files.deleteIfExists(temporaryFile);
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            throw new IOException("生成 PMTiles 归档失败: " + archiveFile, e);
        }
    }

    /** 判断文件是否为 PMTiles V3。 */
    public static boolean isPmtilesV3(Path file) {
        try {
            readHeader(file);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 读取并校验 PMTiles V3 文件头及分段范围。
     *
     * @param file PMTiles 文件
     * @return 归档 Header 的关键字段
     * @throws IOException 文件不是完整、合法的 PMTiles V3 文件时抛出
     */
    public static PmtilesHeader readHeader(Path file) throws IOException {
        if (file == null || !Files.isRegularFile(file)) {
            throw new IOException("PMTiles 文件不存在: " + file);
        }
        byte[] bytes = new byte[HEADER_LENGTH];
        try (InputStream input = Files.newInputStream(file)) {
            int offset = 0;
            while (offset < bytes.length) {
                int read = input.read(bytes, offset, bytes.length - offset);
                if (read < 0) {
                    throw new IOException("PMTiles Header 长度不足 " + HEADER_LENGTH + " 字节: " + file);
                }
                offset += read;
            }
        }
        if (!Arrays.equals(MAGIC, Arrays.copyOf(bytes, MAGIC.length)) || bytes[7] != VERSION) {
            throw new IOException("不是 PMTiles V3 文件: " + file);
        }
        ByteBuffer header = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        header.position(8);
        PmtilesHeader result = new PmtilesHeader(
                header.getLong(), header.getLong(), header.getLong(), header.getLong(),
                header.getLong(), header.getLong(), header.getLong(), header.getLong(),
                header.getLong(), header.getLong(), header.getLong());
        long fileSize = Files.size(file);
        validateSection("rootDirectory", result.rootDirectoryOffset, result.rootDirectoryLength, fileSize);
        validateSection("jsonMetadata", result.jsonMetadataOffset, result.jsonMetadataLength, fileSize);
        validateSection("tileData", result.tileDataOffset, result.tileDataLength, fileSize);
        if (result.leafDirectoryLength > 0) {
            validateSection("leafDirectory", result.leafDirectoryOffset, result.leafDirectoryLength, fileSize);
        }
        if (result.rootDirectoryOffset != HEADER_LENGTH
                || result.rootDirectoryLength > MAX_ROOT_DIRECTORY_LENGTH) {
            throw new IOException("PMTiles 根目录不满足 16KiB 预读约束: " + file);
        }
        return result;
    }

    /**
     * 将 XYZ 坐标转换为 PMTiles V3 TileID。
     * TileID 为跨越所有层级 Hilbert 曲线位置的累计编号。
     */
    public static long tileId(int z, int x, int y) {
        if (z < 0 || z > 29) {
            throw new IllegalArgumentException("PMTiles 仅支持 0-29 级: " + z);
        }
        int max = (1 << z) - 1;
        if (x < 0 || x > max || y < 0 || y > max) {
            throw new IllegalArgumentException("PMTiles XYZ 坐标超出级别范围: " + z + "/" + x + "/" + y);
        }
        if (z == 0) {
            return 0L;
        }
        long base = ((1L << (z * 2)) - 1L) / 3L;
        long d = 0L;
        long xx = x;
        long yy = y;
        for (long s = 1L << (z - 1); s > 0; s >>= 1) {
            long rx = (xx & s) == 0 ? 0 : 1;
            long ry = (yy & s) == 0 ? 0 : 1;
            d += s * s * ((3 * rx) ^ ry);
            if (ry == 0) {
                if (rx == 1) {
                    xx = s - 1 - xx;
                    yy = s - 1 - yy;
                }
                long temp = xx;
                xx = yy;
                yy = temp;
            }
        }
        return base + d;
    }

    private static List<DirectoryEntry> buildTileEntries(List<V3TileArchiveFiles.TileFile> files) {
        List<DirectoryEntry> entries = new ArrayList<>(files.size());
        long offset = 0L;
        long previousId = -1L;
        for (V3TileArchiveFiles.TileFile file : files) {
            long tileId = tileId(file.z, file.x, file.xyzY);
            if (tileId == previousId) {
                throw new IllegalArgumentException("中间目录包含重复瓦片坐标: " + file.path);
            }
            entries.add(new DirectoryEntry(tileId, offset, file.length, 1));
            offset += file.length;
            previousId = tileId;
        }
        return entries;
    }

    private static DirectoryLayout buildDirectories(List<DirectoryEntry> tileEntries) throws IOException {
        byte[] root = encodeDirectory(tileEntries);
        if (root.length <= MAX_ROOT_DIRECTORY_LENGTH) {
            return new DirectoryLayout(root, new byte[0]);
        }
        int leafSize = Math.min(4096, tileEntries.size());
        while (true) {
            List<byte[]> leaves = new ArrayList<>();
            List<DirectoryEntry> rootEntries = new ArrayList<>();
            long leafOffset = 0L;
            for (int start = 0; start < tileEntries.size(); start += leafSize) {
                int end = Math.min(tileEntries.size(), start + leafSize);
                byte[] leaf = encodeDirectory(tileEntries.subList(start, end));
                leaves.add(leaf);
                rootEntries.add(new DirectoryEntry(tileEntries.get(start).tileId, leafOffset, leaf.length, 0));
                leafOffset += leaf.length;
            }
            root = encodeDirectory(rootEntries);
            if (root.length <= MAX_ROOT_DIRECTORY_LENGTH) {
                ByteArrayOutputStream combined = new ByteArrayOutputStream((int) leafOffset);
                for (byte[] leaf : leaves) {
                    combined.write(leaf);
                }
                return new DirectoryLayout(root, combined.toByteArray());
            }
            if (leafSize >= tileEntries.size()) {
                throw new IOException("PMTiles 根目录超过 16KiB 限制，无法建立两级目录");
            }
            leafSize = Math.min(tileEntries.size(), leafSize * 2);
        }
    }

    private static byte[] encodeDirectory(List<DirectoryEntry> entries) throws IOException {
        if (entries == null || entries.isEmpty()) {
            throw new IllegalArgumentException("PMTiles 目录不能为空");
        }
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        writeVarint(raw, entries.size());
        long previousId = 0L;
        for (DirectoryEntry entry : entries) {
            writeVarint(raw, entry.tileId - previousId);
            previousId = entry.tileId;
        }
        for (DirectoryEntry entry : entries) {
            writeVarint(raw, entry.runLength);
        }
        for (DirectoryEntry entry : entries) {
            writeVarint(raw, entry.length);
        }
        long nextOffset = 0L;
        for (int i = 0; i < entries.size(); i++) {
            DirectoryEntry entry = entries.get(i);
            if (i > 0 && entry.offset == nextOffset) {
                writeVarint(raw, 0L);
            } else {
                writeVarint(raw, entry.offset + 1L);
            }
            nextOffset = entry.offset + entry.length;
        }
        return gzip(raw.toByteArray());
    }

    private static void writeArchive(
            Path output, List<V3TileArchiveFiles.TileFile> files, List<DirectoryEntry> entries,
            DirectoryLayout directories, byte[] metadata, boolean gzipPbf) throws IOException {
        long rootOffset = HEADER_LENGTH;
        long metadataOffset = rootOffset + directories.root.length;
        long leafOffset = metadataOffset + metadata.length;
        long tileDataOffset = leafOffset + directories.leaves.length;
        long tileDataLength = 0L;
        for (DirectoryEntry entry : entries) {
            tileDataLength += entry.length;
        }
        byte[] header = createHeader(rootOffset, directories.root.length, metadataOffset, metadata.length,
                leafOffset, directories.leaves.length, tileDataOffset, tileDataLength, files, entries.size(), gzipPbf);
        try (OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(output))) {
            outputStream.write(header);
            outputStream.write(directories.root);
            outputStream.write(metadata);
            outputStream.write(directories.leaves);
            byte[] buffer = new byte[8192];
            for (V3TileArchiveFiles.TileFile file : files) {
                try (InputStream input = Files.newInputStream(file.path)) {
                    int length;
                    while ((length = input.read(buffer)) >= 0) {
                        outputStream.write(buffer, 0, length);
                    }
                }
            }
        }
    }

    private static byte[] createHeader(
            long rootOffset, long rootLength, long metadataOffset, long metadataLength,
            long leafOffset, long leafLength, long tileDataOffset, long tileDataLength,
            List<V3TileArchiveFiles.TileFile> files, int tileEntries, boolean gzipPbf) {
        Bounds bounds = bounds(files);
        ByteBuffer header = ByteBuffer.allocate(HEADER_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
        header.put(MAGIC);
        header.put((byte) VERSION);
        header.putLong(rootOffset);
        header.putLong(rootLength);
        header.putLong(metadataOffset);
        header.putLong(metadataLength);
        header.putLong(leafOffset);
        header.putLong(leafLength);
        header.putLong(tileDataOffset);
        header.putLong(tileDataLength);
        header.putLong(files.size());
        header.putLong(tileEntries);
        header.putLong(files.size());
        header.put((byte) 1);
        header.put((byte) GZIP_COMPRESSION);
        header.put((byte) (gzipPbf ? GZIP_COMPRESSION : NO_COMPRESSION));
        header.put((byte) MVT_TILE_TYPE);
        header.put((byte) bounds.minZoom);
        header.put((byte) bounds.maxZoom);
        putPosition(header, bounds.minLon, bounds.minLat);
        putPosition(header, bounds.maxLon, bounds.maxLat);
        header.put((byte) ((bounds.minZoom + bounds.maxZoom) / 2));
        putPosition(header, (bounds.minLon + bounds.maxLon) / 2D, (bounds.minLat + bounds.maxLat) / 2D);
        return header.array();
    }

    private static Bounds bounds(List<V3TileArchiveFiles.TileFile> files) {
        Bounds bounds = new Bounds();
        for (V3TileArchiveFiles.TileFile file : files) {
            double n = (double) (1L << file.z);
            bounds.minLon = Math.min(bounds.minLon, longitude(file.x, n));
            bounds.maxLon = Math.max(bounds.maxLon, longitude(file.x + 1D, n));
            bounds.minLat = Math.min(bounds.minLat, latitude(file.xyzY + 1D, n));
            bounds.maxLat = Math.max(bounds.maxLat, latitude(file.xyzY, n));
            bounds.minZoom = Math.min(bounds.minZoom, file.z);
            bounds.maxZoom = Math.max(bounds.maxZoom, file.z);
        }
        return bounds;
    }

    private static double longitude(double x, double n) {
        return x / n * 360D - 180D;
    }

    private static double latitude(double y, double n) {
        return Math.toDegrees(Math.atan(Math.sinh(Math.PI * (1D - 2D * y / n))));
    }

    private static void putPosition(ByteBuffer buffer, double longitude, double latitude) {
        buffer.putInt((int) Math.round(longitude * 10000000D));
        buffer.putInt((int) Math.round(latitude * 10000000D));
    }

    private static void writeVarint(OutputStream output, long value) throws IOException {
        if (value < 0) {
            throw new IllegalArgumentException("PMTiles varint 不能为负数: " + value);
        }
        while ((value & ~0x7FL) != 0L) {
            output.write((int) (value & 0x7F) | 0x80);
            value >>>= 7;
        }
        output.write((int) value);
    }

    private static byte[] gzip(byte[] source) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
            gzip.write(source);
        }
        return output.toByteArray();
    }

    private static String validJson(String json) {
        return json == null || json.trim().isEmpty() ? "{}" : json;
    }

    private static void validateSection(String name, long offset, long length, long fileSize) throws IOException {
        if (offset < HEADER_LENGTH || length < 0 || offset > fileSize || length > fileSize - offset) {
            throw new IOException("PMTiles " + name + " 分段范围非法");
        }
    }

    private static void prepareTarget(Path targetFile, boolean overwrite) throws IOException {
        Path parent = targetFile.toAbsolutePath().normalize().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        if (!overwrite && Files.exists(targetFile)) {
            throw new IOException("目标 PMTiles 文件已存在且未开启覆盖: " + targetFile);
        }
    }

    private static void moveCompletedArchive(Path temporaryFile, Path targetFile, boolean overwrite) throws IOException {
        if (overwrite) {
            Files.move(temporaryFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
        } else {
            Files.move(temporaryFile, targetFile);
        }
    }

    private static final class DirectoryEntry {
        private final long tileId;
        private final long offset;
        private final long length;
        private final long runLength;

        private DirectoryEntry(long tileId, long offset, long length, long runLength) {
            this.tileId = tileId;
            this.offset = offset;
            this.length = length;
            this.runLength = runLength;
        }
    }

    private static final class DirectoryLayout {
        private final byte[] root;
        private final byte[] leaves;

        private DirectoryLayout(byte[] root, byte[] leaves) {
            this.root = root;
            this.leaves = leaves;
        }
    }

    private static final class Bounds {
        private int minZoom = Integer.MAX_VALUE;
        private int maxZoom = Integer.MIN_VALUE;
        private double minLon = Double.MAX_VALUE;
        private double minLat = Double.MAX_VALUE;
        private double maxLon = -Double.MAX_VALUE;
        private double maxLat = -Double.MAX_VALUE;
    }

    /** PMTiles V3 Header 的核心索引字段。 */
    public static final class PmtilesHeader {
        private final long rootDirectoryOffset;
        private final long rootDirectoryLength;
        private final long jsonMetadataOffset;
        private final long jsonMetadataLength;
        private final long leafDirectoryOffset;
        private final long leafDirectoryLength;
        private final long tileDataOffset;
        private final long tileDataLength;
        private final long addressedTiles;
        private final long tileEntries;
        private final long tileContents;

        private PmtilesHeader(
                long rootDirectoryOffset, long rootDirectoryLength, long jsonMetadataOffset, long jsonMetadataLength,
                long leafDirectoryOffset, long leafDirectoryLength, long tileDataOffset, long tileDataLength,
                long addressedTiles, long tileEntries, long tileContents) {
            this.rootDirectoryOffset = rootDirectoryOffset;
            this.rootDirectoryLength = rootDirectoryLength;
            this.jsonMetadataOffset = jsonMetadataOffset;
            this.jsonMetadataLength = jsonMetadataLength;
            this.leafDirectoryOffset = leafDirectoryOffset;
            this.leafDirectoryLength = leafDirectoryLength;
            this.tileDataOffset = tileDataOffset;
            this.tileDataLength = tileDataLength;
            this.addressedTiles = addressedTiles;
            this.tileEntries = tileEntries;
            this.tileContents = tileContents;
        }

        public long getRootDirectoryOffset() {
            return rootDirectoryOffset;
        }

        public long getRootDirectoryLength() {
            return rootDirectoryLength;
        }

        public long getJsonMetadataOffset() {
            return jsonMetadataOffset;
        }

        public long getJsonMetadataLength() {
            return jsonMetadataLength;
        }

        public long getLeafDirectoryOffset() {
            return leafDirectoryOffset;
        }

        public long getLeafDirectoryLength() {
            return leafDirectoryLength;
        }

        public long getTileDataOffset() {
            return tileDataOffset;
        }

        public long getTileDataLength() {
            return tileDataLength;
        }

        public long getAddressedTiles() {
            return addressedTiles;
        }

        public long getTileEntries() {
            return tileEntries;
        }

        public long getTileContents() {
            return tileContents;
        }
    }
}
