package cn.geoair.map.tile.forge.core.zip;


import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.tile.forge.core.zip.decompression.DecompressionLimits;
import cn.geoair.map.tile.forge.core.zip.model.CentralDirectoryModel;
import cn.geoair.map.tile.forge.core.zip.model.EocdInfo;
import cn.geoair.map.tile.forge.core.utils.LocalWriteUtils;
import cn.hutool.core.io.unit.DataSizeUtil;


import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * ZIP压缩文件处理抽象基类
 * 封装通用的ZIP解析、解压逻辑，子类只需实现文件读取的具体细节
 */

public abstract class AbstractZipCompressionHandler implements ICompressionHandler {
    public static GiLogger log = GirLoggerFactory.getLogger();
    // ------------------------------ 通用常量（子类共享） ------------------------------
    protected static final int BUFFER_SIZE = 8192;
    protected static final int EOCD_SIGNATURE = 0x06054b50;
    protected static final int CENTRAL_DIR_SIGNATURE = 0x02014b50;
    protected static final int LOCAL_FILE_HEADER_SIGNATURE = 0x04034b50;
    protected static final int ZIP64_LOCATOR_SIGNATURE = 0x07064b50;
    protected static final int ZIP64_EOCD_SIGNATURE = 0x06064b50;
    protected static final int EOCD_BASE_SIZE = 22;
    protected static final int MAX_CHUNK_SIZE = 5 * 1024 * 1024; // 5MB

    /**
     * 单次读取小于这个字节数就没必要上报进度了，省得小请求刷屏
     */
    protected static final long PROGRESS_MIN_RANGE_BYTES = 1024L * 1024L;

    // ZIP64相关常量
    private static final int ZIP64_EXTRA_FIELD_ID = 0x0001;
    private static final long ZIP64_MAGIC_NUMBER = 0xFFFFFFFFL;

    // ------------------------------ 文件名编码相关 ------------------------------

    /**
     * 通用位标记第 11 位。置位表示该条目的文件名与注释按 UTF-8 编码，这是 ZIP 规范里
     * 唯一权威的编码声明；不置位时规范只能含糊地说"按原始 ZIP 的字符集"。
     */
    private static final int FLAG_UTF8_FILENAME = 0x0800;

    /**
     * 覆盖默认文件名编码的系统属性，例如 -Dgeoair.zip.filename.charset=GBK。
     * 指定的编码会被提到候选列表最前面，用于处理"没有声明 UTF-8 但确实是 UTF-8"这类包。
     */
    private static final String FILENAME_CHARSET_PROPERTY = "geoair.zip.filename.charset";

    /**
     * 未声明编码、且 UTF-8 也解不通（或解出来不可信）时的候选文件名编码，按尝试顺序排列。
     * UTF-8 不在这里，它在主流程里单独先试；这里放的是它失败之后的退路。
     * GBK 在前：国内瓦片包多由 Windows 工具打包，本地编码即 GBK。
     * CP437 兜底：单字节编码，任何字节都能映射，走到这里一定不会失败。
     */
    private static final Charset[] FILENAME_CHARSETS = resolveFilenameCharsets();


    @Override
    public void readFileFromZipToLocal(String source, String path, String output) throws IOException {
        byteToLocal(output, readFileFromZip(source, path));
    }

    @Override
    public byte[] readFileFromZip(String source, String path) throws IOException {
        CentralDirectoryModel entry = findEntryInCentralDir(parseEocd(getFileSize(source), source), path, source);
        if (entry == null) throw new FileNotFoundException("ZIP中未找到路径: " + path);
        return readAndDecompressEntry(entry, source);
    }

    @Override
    public EocdInfo parseEocd(long fileSize, String source) throws IOException {
        if (fileSize < EOCD_BASE_SIZE) throw new IOException("ZIP文件不足22字节");
        long start = Math.max(0, fileSize - (65535L + EOCD_BASE_SIZE));
        byte[] tail = readExact(source, start, fileSize - start);
        IOException last = null;
        for (int p = tail.length - EOCD_BASE_SIZE; p >= 0; p--) {
            if (readInt(tail, p) != EOCD_SIGNATURE) continue;
            int comment = readShort(tail, p + 20) & 0xFFFF;
            if (p + EOCD_BASE_SIZE + comment != tail.length) continue;
            try {
                long endPosition = start + p;
                EocdInfo e = new EocdInfo(readShort(tail, p + 4) & 0xFFFFL,
                        readShort(tail, p + 6) & 0xFFFFL, readShort(tail, p + 8) & 0xFFFFL,
                        readShort(tail, p + 10) & 0xFFFFL, readInt(tail, p + 12) & ZIP64_MAGIC_NUMBER,
                        readInt(tail, p + 16) & ZIP64_MAGIC_NUMBER, comment);
                long directoryEnd = endPosition;
                boolean zip64Required = e.getTotalEntries() == 65535
                        || e.getCentralDirSize() == ZIP64_MAGIC_NUMBER
                        || e.getCentralDirOffset() == ZIP64_MAGIC_NUMBER;
                // 一些打包工具即使经典 EOCD 字段没有溢出，也会同时写 ZIP64 EOCD。
                // 定位器固定紧邻经典 EOCD，因此始终探测它，避免把合法的 76 字节
                // ZIP64 EOCD + locator 误判成中央目录与 EOCD 之间的非法间隙。
                byte[] locator;
                if (p >= 20) {
                    locator = Arrays.copyOfRange(tail, p - 20, p);
                } else if (zip64Required && endPosition >= 20) {
                    locator = readExact(source, endPosition - 20, 20);
                } else {
                    locator = new byte[0];
                }
                if (locator.length == 20 && readInt(locator, 0) == ZIP64_LOCATOR_SIGNATURE) {
                        if (readInt(locator, 4) != 0 || readInt(locator, 16) != 1) throw new IOException("不支持分卷ZIP64");
                        long z = readLong(locator, 8);
                        if (z < 0 || z > endPosition - 20 - 56) throw new IOException("ZIP64记录范围无效");
                        byte[] record = readExact(source, z, 56);
                        long recordSize = readLong(record, 4);
                        if (readInt(record, 0) != ZIP64_EOCD_SIGNATURE || recordSize < 44
                                || recordSize != endPosition - 20 - z - 12) throw new IOException("ZIP64记录长度或签名无效");
                        e = new EocdInfo(readInt(record, 16) & ZIP64_MAGIC_NUMBER,
                                readInt(record, 20) & ZIP64_MAGIC_NUMBER, readLong(record, 24), readLong(record, 32),
                                readLong(record, 40), readLong(record, 48), comment);
                        directoryEnd = z;
                } else if (zip64Required) {
                    throw new IOException("缺少ZIP64定位器");
                }
                long size = e.getCentralDirSize(), offset = e.getCentralDirOffset(), count = e.getTotalEntries();
                if (e.getDiskNumber() != 0 || e.getStartDisk() != 0 || e.getDiskEntries() != count
                        || count < 0 || size < 0 || offset < 0 || offset > directoryEnd
                        || size != directoryEnd - offset || count > size / 46
                        || (count == 0 && size != 0)) throw new IOException("ZIP中央目录范围或条目数量无效");
                e.setFileSize(fileSize);
                return e;
            } catch (IOException e) { last = e; }
        }
        throw new IOException("未找到有效ZIP结束记录: " + source, last);
    }

    @Override
    public CentralDirectoryModel findEntryInCentralDir(EocdInfo eocd, String path, String source) throws IOException {
        String target = normalizePath(path);
        CentralDirectoryModel[] result = {null};
        scanAllEntries(eocd, source, (entry, total, index) -> {
            if (matchesPath(entry.getName(), target)) { result[0] = entry; return false; }
            return true;
        });
        return result[0];
    }

    @Override
    public List<String> checkedPathsInZip(String source, List<String> paths) throws IOException {
        if (paths == null || paths.isEmpty()) return Collections.emptyList();
        Map<String, String> pending = new LinkedHashMap<>();
        for (String path : paths) pending.put(path, normalizePath(path));
        Set<String> found = new LinkedHashSet<>();
        scanAllEntries(source, (entry, total, index) -> {
            Iterator<Map.Entry<String, String>> it = pending.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, String> candidate = it.next();
                if (matchesPath(entry.getName(), candidate.getValue())) { found.add(candidate.getKey()); it.remove(); }
            }
            return !pending.isEmpty();
        });
        return new ArrayList<>(found);
    }

    private boolean matchesPath(String name, String target) {
        if (target.isEmpty()) return false;
        if (name.equalsIgnoreCase(target)) return true;
        if (!target.endsWith("/") && target.substring(target.lastIndexOf('/') + 1).contains(".")) return false;
        // 调用方仍可使用无尾斜杠的目录查询，但不能把无扩展名文件改名为目录。
        String prefix = target.endsWith("/") ? target : target + "/";
        return name.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    @Override
    public void scanAllEntries(String source, TerminatingConsumer<CentralDirectoryModel> consumer) throws IOException {
        scanAllEntries(parseEocd(getFileSize(source), source), source, consumer);
    }

    @Override
    public byte[] readAndDecompressEntry(CentralDirectoryModel entry, String source) throws IOException {
        DecompressionLimits.validateCompressedSize(entry.getCompressedSize());
        DecompressionLimits.validateExpectedSize(entry.getUncompressedSize());
        long fileSize = getFileSize(source), headerOffset = entry.getLocalHeaderOffset();
        if (headerOffset < 0 || headerOffset > fileSize - 30) throw new IOException("ZIP文件头范围无效");
        byte[] header = readExact(source, headerOffset, 30);
        int flags = readShort(header, 6) & 0xFFFF;
        if (readInt(header, 0) != LOCAL_FILE_HEADER_SIGNATURE) throw new IOException("ZIP文件头签名无效，拒绝猜测偏移");
        if ((flags & 0x41) != 0) throw new IOException("不支持加密ZIP条目");
        if ((readShort(header, 8) & 0xFFFF) != entry.getCompressionMethod()) throw new IOException("ZIP压缩方式不一致");
        long variableLength = (readShort(header, 26) & 0xFFFF) + (readShort(header, 28) & 0xFFFF);
        if (variableLength > fileSize - headerOffset - 30) throw new IOException("ZIP文件头长度越界");
        long dataOffset = headerOffset + 30 + variableLength;
        if (dataOffset > fileSize || entry.getCompressedSize() < 0
                || entry.getCompressedSize() > fileSize - dataOffset) throw new IOException("ZIP数据范围无效");
        if (entry.getDataOffset() != null && entry.getDataOffset() != dataOffset) throw new IOException("缓存的ZIP数据偏移已失效");
        entry.setDataOffset(dataOffset);
        long expectedCrc;
        Long alternativeCrc = null;
        if (entry.getCrc32() != null) {
            expectedCrc = entry.getCrc32();
        } else if ((flags & 8) == 0) {
            expectedCrc = readInt(header, 14) & ZIP64_MAGIC_NUMBER;
        } else {
            // 兼容没有CRC字段的旧SQLite/PG缓存，从数据描述符取得校验值，无需重扫全包。
            long descriptor = dataOffset + entry.getCompressedSize();
            if (descriptor > fileSize - 8) throw new IOException("ZIP数据描述符不完整");
            byte[] crc = readExact(source, descriptor, 8);
            expectedCrc = readInt(crc, 0) & ZIP64_MAGIC_NUMBER;
            if (expectedCrc == 0x08074b50L) {
                // 无签名描述符的CRC也可能恰好等于可选签名。
                alternativeCrc = expectedCrc;
                expectedCrc = readInt(crc, 4) & ZIP64_MAGIC_NUMBER;
            }
        }
        byte[] compressed = readExact(source, dataOffset, entry.getCompressedSize());
        byte[] result = entry.getDecompressionHandler().decompress(compressed, entry.getUncompressedSize());
        if (result.length != entry.getUncompressedSize()) throw new IOException("ZIP解压长度不匹配");
        java.util.zip.CRC32 crc = new java.util.zip.CRC32();
        crc.update(result);
        if (crc.getValue() != expectedCrc && (alternativeCrc == null || crc.getValue() != alternativeCrc))
            throw new IOException("ZIP内容CRC校验失败: " + entry.getName());
        return result;
    }

    @Override
    public void readAndDecompressEntryToLocal(CentralDirectoryModel entry, String source, String output) throws IOException {
        byteToLocal(output, readAndDecompressEntry(entry, source));
    }

    private byte[] readExact(String source, long offset, long length) throws IOException {
        if (offset < 0 || length < 0 || length > Integer.MAX_VALUE - 8 || offset > Long.MAX_VALUE - length)
            throw new IOException("ZIP读取范围无效");
        if (length == 0) return new byte[0];
        byte[] bytes = readRange(source, offset, offset + length - 1);
        if (bytes.length != length) throw new EOFException("ZIP范围读取不完整");
        return bytes;
    }

    @Override
    public void scanAllEntries(EocdInfo eocd, String source, TerminatingConsumer<CentralDirectoryModel> entryConsumer) throws IOException {
        ZipDirectoryPipeline.scan(this, eocd, source, entryConsumer);
    }

    void scanDirectoryEntries(EocdInfo eocd, String source, TerminatingConsumer<CentralDirectoryModel> entryConsumer) throws IOException {
        // 中央目录已经包含名称、压缩大小和本地头位置。扫描时不读取各文件的本地头，
        // dataOffset 留空，由 readAndDecompressEntry 在实际取文件时按需解析。
        // 按连续块读取，跨块条目由 reader 拼接，避免每个条目发起多个 S3 Range 请求。
        long offset = eocd.getCentralDirOffset();
        long size = eocd.getCentralDirSize();
        if (offset < 0 || size < 0 || offset > Long.MAX_VALUE - size || eocd.getTotalEntries() < 0) {
            throw new IOException("无效的ZIP中央目录范围");
        }
        CentralDirectoryReader reader = new CentralDirectoryReader(this, source, offset, size);
        for (long index = 0; index < eocd.getTotalEntries(); index++) {
            if (Thread.currentThread().isInterrupted()) {
                throw new java.io.InterruptedIOException("ZIP中央目录扫描已中断");
            }
            byte[] header = reader.read(46);
            if (readInt(header, 0) != CENTRAL_DIR_SIGNATURE) {
                throw new IOException("无效的ZIP中央目录签名，条目: " + index);
            }
            int nameLen = readShort(header, 28) & 0xFFFF;
            int extraLen = readShort(header, 30) & 0xFFFF;
            int commentLen = readShort(header, 32) & 0xFFFF;
            byte[] variable = reader.read(nameLen + extraLen + commentLen);
            int flags = readShort(header, 8) & 0xFFFF;
            String name = normalizePath(decodeEntryName(variable, nameLen, extraLen, flags));
            long compressed = readInt(header, 20) & ZIP64_MAGIC_NUMBER;
            long uncompressed = readInt(header, 24) & ZIP64_MAGIC_NUMBER;
            long localOffset = readInt(header, 42) & ZIP64_MAGIC_NUMBER;
            long disk = readShort(header, 34) & 0xFFFF;
            boolean zip64 = compressed == ZIP64_MAGIC_NUMBER || uncompressed == ZIP64_MAGIC_NUMBER
                    || localOffset == ZIP64_MAGIC_NUMBER || disk == 0xFFFF;
            if (zip64) {
                boolean found = false;
                int end = nameLen + extraLen;
                for (int p = nameLen; p + 4 <= end;) {
                    int id = readShort(variable, p) & 0xFFFF;
                    int length = readShort(variable, p + 2) & 0xFFFF;
                    if (length > end - p - 4) throw new IOException("ZIP扩展字段不完整: " + name);
                    if (id == ZIP64_EXTRA_FIELD_ID) {
                        ByteBuffer values = ByteBuffer.wrap(variable, p + 4, length).slice().order(ByteOrder.LITTLE_ENDIAN);
                        int required = (uncompressed == ZIP64_MAGIC_NUMBER ? 8 : 0)
                                + (compressed == ZIP64_MAGIC_NUMBER ? 8 : 0)
                                + (localOffset == ZIP64_MAGIC_NUMBER ? 8 : 0) + (disk == 0xFFFF ? 4 : 0);
                        if (values.remaining() < required) throw new IOException("ZIP64扩展字段不完整: " + name);
                        if (uncompressed == ZIP64_MAGIC_NUMBER) uncompressed = values.getLong();
                        if (compressed == ZIP64_MAGIC_NUMBER) compressed = values.getLong();
                        if (localOffset == ZIP64_MAGIC_NUMBER) localOffset = values.getLong();
                        if (disk == 0xFFFF) disk = values.getInt() & ZIP64_MAGIC_NUMBER;
                        found = true;
                        break;
                    }
                    p += 4 + length;
                }
                if (!found) throw new IOException("缺少ZIP64扩展字段: " + name);
            }
            if (disk != 0 || compressed < 0 || uncompressed < 0 || localOffset < 0) {
                throw new IOException("不支持的ZIP分卷或无效的条目范围: " + name);
            }
            CentralDirectoryModel entry = new CentralDirectoryModel(localOffset, null,
                    readShort(header, 10) & 0xFFFF, compressed, uncompressed, name,
                    46 + nameLen + extraLen + commentLen);
            entry.setCrc32(readInt(header, 16) & ZIP64_MAGIC_NUMBER);
            long attributes = readInt(header, 38) & ZIP64_MAGIC_NUMBER;
            entry.setDirectoryIs(name.endsWith("/") || (attributes & 0x10) != 0
                    || ((attributes >>> 16) & 0xF000) == 0x4000);
            // 生产线程按目录顺序送入有界队列，入库回调在调用线程串行执行。
            if (!entryConsumer.accept(entry, eocd.getTotalEntries(), index)) return;
        }
    }

    /** 验证扩展字段边界，并仅接受版本和原始名称CRC匹配的Unicode路径。 */
    private String decodeEntryName(byte[] data, int nameLength, int extraLength, int flags) throws IOException {
        String name = decodeFileName(data, 0, nameLength, flags);
        int end = nameLength + extraLength;
        for (int p = nameLength; p < end;) {
            if (end - p < 4) throw new IOException("ZIP扩展字段头不完整");
            int id = readShort(data, p) & 0xFFFF;
            int length = readShort(data, p + 2) & 0xFFFF;
            if (length > end - p - 4) throw new IOException("ZIP扩展字段长度越界");
            if (id == 0x7075 && (flags & FLAG_UTF8_FILENAME) == 0 && length >= 5 && data[p + 4] == 1) {
                java.util.zip.CRC32 crc = new java.util.zip.CRC32();
                crc.update(data, 0, nameLength);
                if (crc.getValue() == (readInt(data, p + 5) & ZIP64_MAGIC_NUMBER)) {
                    String unicode = strictDecode(data, p + 9, length - 5, StandardCharsets.UTF_8);
                    if (unicode != null) name = unicode;
                }
            }
            p += 4 + length;
        }
        return name;
    }

    /**
     * 解码文件名。
     *
     * @param data               承载文件名的字节数组
     * @param offset             文件名起始偏移
     * @param length             文件名字节长度
     * @param generalPurposeFlag 该条目的通用位标记，用于判断是否声明了 UTF-8
     */
    private String decodeFileName(byte[] data, int offset, int length, int generalPurposeFlag) {
        if (length <= 0) {
            return "";
        }

        String utf8 = strictDecode(data, offset, length, StandardCharsets.UTF_8);

        // 声明了 UTF-8 就按 UTF-8 解。这是规范给的确定信息，不需要也不应该再猜。
        if ((generalPurposeFlag & FLAG_UTF8_FILENAME) != 0) {
            if (utf8 != null) {
                return utf8;
            }
            log.warn("条目声明文件名为 UTF-8，但按 UTF-8 解不出来，退回本地编码，长度={}字节", length);
        }

        // 纯 ASCII 的路径占绝大多数，各编码结果一致，直接返回
        if (isAscii(data, offset, length)) {
            return new String(data, offset, length, StandardCharsets.US_ASCII);
        }

        // 没声明编码就只能判断，而这里能判断的前提是两种编码的"严格程度"差得很远：
        // GBK 汉字的字节序列极少能整段通过 UTF-8 的合法性校验（实测二十来个常用词拼起来的
        // 路径全部被拒），而 UTF-8 的名字几乎都能被 GBK 顺利解成一串乱码（GBK 的字节空间太宽松）。
        // 所以顺序必须是"先信 UTF-8"，反过来先试 GBK 会把正常的 UTF-8 名字全解成乱码。
        if (utf8 != null && !looksMisdecoded(utf8)) {
            return utf8;
        }

        // UTF-8 解不通，或者解出来的字落在只可能由误判产生的区间，退回本地编码
        for (Charset charset : FILENAME_CHARSETS) {
            String name = strictDecode(data, offset, length, charset);
            if (name != null) {
                if (log.isDebugEnabled()) {
                    log.debug("文件名按 {} 解码（UTF-8 结果[{}]不可信），长度={}字节，结果={}",
                            charset.name(), utf8, length, name);
                }
                return name;
            }
        }

        // 候选编码全都解不出来，返回十六进制便于排查
        log.warn("文件名解码失败，长度={}字节，使用十六进制表示", length);
        return bytesToHex(data, offset, length);
    }

    /**
     * 严格解码：遇到非法字节序列直接判定该编码不适用。
     * 这一点是整套兜底能成立的关键——宽松解码（new String(byte[], Charset)）会把非法字节
     * 替换成 U+FFFD 而不报错，等于任何编码都"成功"，兜底也就无从谈起。
     *
     * @return 解码结果；该编码不适用时返回 null
     */
    private static String strictDecode(byte[] data, int offset, int length, Charset charset) {
        try {
            CharsetDecoder decoder = charset.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            return decoder.decode(ByteBuffer.wrap(data, offset, length)).toString();
        } catch (CharacterCodingException e) {
            return null;
        }
    }

    /**
     * 判断整段字节是否都是 ASCII（最高位为 0）
     */
    private static boolean isAscii(byte[] data, int offset, int length) {
        int end = offset + length;
        for (int i = offset; i < end; i++) {
            if (data[i] < 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断 UTF-8 解码结果是否像是"别的编码被误当 UTF-8 解"。
     *
     * 判据是看结果里有没有 U+0080~U+07FF 之间的字符。这个区间是 GBK 汉字字节被当成 UTF-8
     * 双字节序列解码后的唯一落点，覆盖拉丁补充、组合符号、希腊、西里尔、希伯来、阿拉伯；
     * 而正常的 UTF-8 中文名解出来是 CJK 区（U+4E00 起）加 ASCII，不会掉进这个区间。
     *
     * 例子：GBK 的"目录"（c4bfc2bc）刚好也是合法 UTF-8 双字节序列，会被解成"Ŀ¼"，
     * 两个字符都落在拉丁补充区，据此就能识别出来并改用 GBK。
     *
     * 注意这是个启发式判据，对中文名和 ASCII 名可靠；如果文件名本身是西欧语种
     * （带变音符号，同样是拉丁补充区），会被误判成需要回退，这种情况用系统属性
     * {@link #FILENAME_CHARSET_PROPERTY} 显式指定 UTF-8 即可。
     */
    private static boolean looksMisdecoded(String name) {
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if ((c >= 0x0080 && c <= 0x07FF) || c < 0x20) {
                return true;
            }
        }
        return false;
    }

    /**
     * 组装候选文件名编码。系统属性指定了编码时把它放到最前面，其余默认候选仍然保留在后面兜底。
     */
    private static Charset[] resolveFilenameCharsets() {
        List<Charset> candidates = new ArrayList<Charset>();

        String override = System.getProperty(FILENAME_CHARSET_PROPERTY);
        if (override != null && !override.trim().isEmpty()) {
            Charset charset = charsetOrNull(override.trim());
            if (charset != null) {
                candidates.add(charset);
            } else {
                log.warn("系统属性 {}={} 不是可用编码，忽略该配置，改用默认候选", FILENAME_CHARSET_PROPERTY, override);
            }
        }

        for (String name : new String[]{"GBK", "CP437"}) {
            Charset charset = charsetOrNull(name);
            if (charset != null && !candidates.contains(charset)) {
                candidates.add(charset);
            }
        }
        return candidates.toArray(new Charset[candidates.size()]);
    }

    /**
     * 按名称取编码，取不到返回 null（不抛异常）
     */
    private static Charset charsetOrNull(String name) {
        try {
            return Charset.isSupported(name) ? Charset.forName(name) : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 字节数组转十六进制字符串（用于调试）
     */
    private String bytesToHex(byte[] data, int offset, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(String.format("%02x", data[offset + i]));
        }
        return sb.toString();
    }

    private static void byteToLocal(String localOutputPath, byte[] fileData) throws IOException {
        Path targetPath = Paths.get(localOutputPath);
        Files.createDirectories(targetPath.getParent());
        // 先写临时文件再整体替换。之前是直接往目标路径写， FileOutputStream 一构造目标文件就以 0 字节出现，
        // 并发读请求会把它当成已经就绪的文件，拿到半截数据。
        File targetFile = targetPath.toFile();
        File tempFile = LocalWriteUtils.tempFileOf(targetFile);
        try {
            try (OutputStream out = new FileOutputStream(tempFile)) {
                out.write(fileData);
            }
            LocalWriteUtils.replaceAtomically(tempFile.toPath(), targetPath);
            log.debug("文件已写入本地：{}，大小：{}", localOutputPath, DataSizeUtil.format(fileData.length));
        } catch (IOException e) {
            log.error("写入本地文件失败：{}", localOutputPath, e);
            throw new IOException("写入本地文件失败：" + localOutputPath, e);
        } finally {
            LocalWriteUtils.deleteIfExistsQuietly(tempFile.toPath());
        }
    }

    // ------------------------------ 二进制读取工具（子类共享） ------------------------------

    /**
     * 上报一次读取进度，供子类的 readRange 实现调用。
     * <p>
     * 只有单次读取超过 {@link #PROGRESS_MIN_RANGE_BYTES} 才上报，避免小请求把日志刷爆。
     * total 固定为整段请求的长度（不是本次重试的范围），这样重试续读时进度是连续的。
     *
     * @param total   整段请求的总字节数
     * @param current 已读取的字节数
     */
    protected static void reportReadProgress(long total, long current) {
        if (total < PROGRESS_MIN_RANGE_BYTES) {
            return;
        }
        ReadProgressScope.report(total, current);
    }

    protected int readInt(byte[] data, int offset) {
        if (offset + 4 > data.length) {
            throw new IndexOutOfBoundsException("读取int越界，offset:" + offset + ", length:" + data.length);
        }
        return ByteBuffer.wrap(data, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    protected short readShort(byte[] data, int offset) {
        if (offset + 2 > data.length) {
            throw new IndexOutOfBoundsException("读取short越界，offset:" + offset + ", length:" + data.length);
        }
        return ByteBuffer.wrap(data, offset, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
    }

    protected long readLong(byte[] data, int offset) {
        if (offset + 8 > data.length) {
            throw new IndexOutOfBoundsException("读取long越界，offset:" + offset + ", length:" + data.length);
        }
        return ByteBuffer.wrap(data, offset, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
    }

    /**
     * 读取指定范围的字节数据（子类需根据存储类型实现：S3或本地文件）
     */
    protected abstract byte[] readRange(String source, long start, long end) throws IOException;

    /**
     * 获取文件大小（子类实现）
     */
    @Override
    public abstract long getFileSize(String source);

    /**
     * 判断路径是否为文件夹路径
     */
    private boolean isFolderPath(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        return path.endsWith("/") || path.endsWith("\\") ||
               path.endsWith(File.separator);
    }

    /**
     * 标准化路径（支持文件夹）
     */
    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }

        // 记录是否是文件夹路径
        boolean isFolder = isFolderPath(path);

        // 1. 替换反斜杠为斜杠
        String normalized = path.replace("\\", "/");

        // 2. 处理根路径标识
        boolean hasLeadingSlash = normalized.startsWith("/");

        // 3. 分割路径组件并处理相对路径
        String[] parts = normalized.split("/");
        List<String> normalizedParts = new ArrayList<>();

        for (String part : parts) {
            if (part.isEmpty() || part.equals(".")) {
                continue;
            }
            if (part.equals("..")) {
                if (!normalizedParts.isEmpty()) {
                    normalizedParts.remove(normalizedParts.size() - 1);
                }
            } else {
                normalizedParts.add(part);
            }
        }

        // 4. 重建路径
        normalized = String.join("/", normalizedParts);

        // 5. 恢复根路径
        if (hasLeadingSlash && !normalized.isEmpty()) {
            normalized = "/" + normalized;
        }

        // 6. 文件夹路径添加尾部斜杠
        if (isFolder && !normalized.isEmpty() && !normalized.endsWith("/")) {
            normalized += "/";
        }

        return normalized;
    }

}
