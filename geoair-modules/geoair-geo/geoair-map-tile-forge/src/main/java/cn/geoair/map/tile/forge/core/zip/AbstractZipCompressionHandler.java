package cn.geoair.map.tile.forge.core.zip;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.tile.forge.core.enums.GirCompressionType;
import cn.geoair.map.tile.forge.core.zip.decompression.DecompressionHandler;
import cn.geoair.map.tile.forge.core.zip.model.CentralDirectoryModel;
import cn.geoair.map.tile.forge.core.zip.model.EntryPosition;
import cn.geoair.map.tile.forge.core.zip.model.EocdInfo;
import cn.geoair.map.tile.forge.core.zip.model.LocalFileHeader;
import cn.hutool.core.io.unit.DataSizeUtil;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/** ZIP压缩文件处理抽象基类 封装通用的ZIP解析、解压逻辑，子类只需实现文件读取的具体细节 */
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

    // ZIP64相关常量
    private static final int ZIP64_EXTRA_FIELD_ID = 0x0001;
    private static final long ZIP64_MAGIC_NUMBER = 0xFFFFFFFFL;

    @Override
    public void readFileFromZipToLocal(
            String zipSource, String targetFilePathInZip, String localOutputPath)
            throws IOException {
        byte[] fileData = readFileFromZip(zipSource, targetFilePathInZip);
        byteToLocal(localOutputPath, fileData);
    }

    @Override
    public byte[] readFileFromZip(String zipSource, String targetFilePathInZip) throws IOException {
        long zipFileSize = getFileSize(zipSource);
        EocdInfo eocd = parseEocd(zipFileSize, zipSource);
        CentralDirectoryModel targetEntry =
                findEntryInCentralDir(eocd, targetFilePathInZip, zipSource);
        if (targetEntry == null) {
            throw new IOException("ZIP文件[" + zipSource + "]中未找到目标路径：" + targetFilePathInZip);
        }
        return readAndDecompressEntry(targetEntry, zipSource);
    }

    @Override
    public List<byte[]> readFileByChunks(
            String source, long startOffset, long totalSize, int chunkSize) throws IOException {
        List<byte[]> chunks = new ArrayList<>();
        long remaining = totalSize;
        long currentOffset = startOffset;

        while (remaining > 0) {
            int currentChunkSize = (int) Math.min(remaining, chunkSize);
            byte[] chunk = readRange(source, currentOffset, currentOffset + currentChunkSize - 1);
            chunks.add(chunk);

            currentOffset += currentChunkSize;
            remaining -= currentChunkSize;
        }

        return chunks;
    }

    @Override
    public CompletableFuture<List<byte[]>> asyncReadFileByChunks(
            String source, long startOffset, long totalSize, int chunkSize) {
        try {
            return CompletableFuture.completedFuture(
                    readFileByChunks(source, startOffset, totalSize, chunkSize));
        } catch (IOException e) {
            log.error("异步分块读取失败，source:{}", source, e);
            CompletableFuture<List<byte[]>> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    @Override
    public EocdInfo parseEocd(long fileSize, String source) throws IOException {
        long searchStart = Math.max(0, fileSize - 65536);
        byte[] tailBytes = readRange(source, searchStart, fileSize - 1);

        for (int i = tailBytes.length - EOCD_BASE_SIZE; i >= 0; i--) {
            if (readInt(tailBytes, i) == EOCD_SIGNATURE) {
                long eocdPosition = searchStart + i;
                EocdInfo eocd = parseStandardEocd(tailBytes, i);

                if (eocd.getTotalEntries() == 65535
                        || eocd.getCentralDirOffset() == ZIP64_MAGIC_NUMBER
                        || eocd.getCentralDirSize() == ZIP64_MAGIC_NUMBER) {
                    long locatorPosition = eocdPosition - 20;
                    return parseZip64Eocd(locatorPosition, fileSize, source);
                }
                return eocd;
            }
        }
        throw new IOException("未找到ZIP文件的EOCD记录，source:" + source);
    }

    @Override
    public CentralDirectoryModel findEntryInCentralDir(
            EocdInfo eocd, String targetPath, String source) throws IOException {
        String normalizedTarget = normalizePath(targetPath);
        boolean isFolderCheck = isFolderPath(targetPath);

        log.debug(
                "开始查找路径：原始路径=[{}]，标准化路径=[{}]，是否文件夹=[{}]",
                targetPath,
                normalizedTarget,
                isFolderCheck);

        long totalDirSize = eocd.getCentralDirSize();
        long currentOffset = eocd.getCentralDirOffset();
        long remaining = totalDirSize;
        int chunkCount = 0;

        while (remaining > 0) {
            chunkCount++;
            long chunkSize = Math.min(remaining, MAX_CHUNK_SIZE);
            log.trace("读取中央目录块 {}：偏移={}, 大小={}", chunkCount, currentOffset, chunkSize);

            byte[] dirChunk = readRange(source, currentOffset, currentOffset + chunkSize - 1);
            CentralDirectoryModel entry =
                    findEntryInDirChunk(
                            dirChunk, normalizedTarget, isFolderCheck, currentOffset, source);

            if (entry != null) {
                log.debug("找到目标路径：{}", targetPath);
                return entry;
            }

            currentOffset += chunkSize;
            remaining -= chunkSize;
        }

        log.debug("中央目录中未找到目标路径：{}（共扫描 {} 块）", targetPath, chunkCount);
        return null;
    }

    @Override
    public byte[] readAndDecompressEntry(CentralDirectoryModel entry, String source)
            throws IOException {
        if (Objects.isNull(entry.getDataOffset())) {
            try {
                LocalFileHeader header =
                        readLocalFileHeader(
                                entry.getLocalHeaderOffset(), source, getFileSize(source));
                entry.setDataOffset(header.getDataOffset());
            } catch (Exception e) {
                log.warn("读取本地文件头失败，尝试直接使用偏移量估算", e);
                // 估算数据偏移量（本地文件头固定30字节 + 文件名长度 + 扩展字段长度）
                long estimatedOffset =
                        estimateDataOffset(
                                entry.getLocalHeaderOffset(), source, getFileSize(source));
                entry.setDataOffset(estimatedOffset);
            }
        }
        if (entry.getCompressedSize() > MAX_CHUNK_SIZE
                && entry.getDecompressionHandler().supportStreamingDecompress()) {
            return readLargeEntryData(entry, source);
        }

        byte[] compressedData =
                readRange(
                        source,
                        entry.getDataOffset(),
                        entry.getDataOffset() + entry.getCompressedSize() - 1);
        return entry.getDecompressionHandler()
                .decompress(compressedData, entry.getUncompressedSize());
    }

    @Override
    public void readAndDecompressEntryToLocal(
            CentralDirectoryModel entry, String source, String localOutputPath) throws IOException {
        byte[] bytes = readAndDecompressEntry(entry, source);
        byteToLocal(localOutputPath, bytes);
    }

    @Override
    public List<String> checkedPathsInZip(String zipSource, List<String> checkedPaths)
            throws IOException {
        if (checkedPaths == null || checkedPaths.isEmpty()) {
            return Collections.emptyList();
        }

        // 标准化所有待检查路径并记录类型（文件/文件夹）
        Map<String, String> pathMap = new HashMap<>();
        Map<String, Boolean> pathTypeMap = new HashMap<>();
        Set<String> normalizedCheckPaths = new HashSet<>();

        for (String path : checkedPaths) {
            String normalized = normalizePath(path);
            boolean isFolder = isFolderPath(path);

            pathMap.put(normalized, path);
            pathTypeMap.put(normalized, isFolder);
            normalizedCheckPaths.add(normalized);

            // 文件夹路径添加带/和不带/两种形式
            if (isFolder) {
                String altNormalized =
                        normalized.endsWith("/")
                                ? normalized.substring(0, normalized.length() - 1)
                                : normalized + "/";
                pathMap.put(altNormalized, path);
                pathTypeMap.put(altNormalized, isFolder);
            }
        }

        // 存储存在的路径
        Set<String> existsPaths = new HashSet<>();

        // 获取EOCD信息
        long zipFileSize = getFileSize(zipSource);
        EocdInfo eocd = parseEocd(zipFileSize, zipSource);

        // 扫描中央目录查找匹配路径
        long totalDirSize = eocd.getCentralDirSize();
        long currentOffset = eocd.getCentralDirOffset();
        long remaining = totalDirSize;

        while (remaining > 0 && !normalizedCheckPaths.isEmpty()) {
            long chunkSize = Math.min(remaining, MAX_CHUNK_SIZE);
            byte[] dirChunk = readRange(zipSource, currentOffset, currentOffset + chunkSize - 1);

            // 在当前块中查找所有匹配的路径
            findAllMatchingPaths(dirChunk, normalizedCheckPaths, existsPaths, pathTypeMap);

            currentOffset += chunkSize;
            remaining -= chunkSize;
        }

        // 转换回原始路径格式并去重
        Set<String> resultSet = new HashSet<>();
        for (String normalizedPath : existsPaths) {
            String originalPath = pathMap.get(normalizedPath);
            if (originalPath != null) {
                resultSet.add(originalPath);
            }
        }

        List<String> result = new ArrayList<>(resultSet);
        log.info("路径检查完成：共检查{}个路径，存在{}个", checkedPaths.size(), result.size());
        return result;
    }

    /** 读取ZIP64中央目录结束记录（处理大于4GB的ZIP文件） */
    private EocdInfo readZip64Eocd(long fileSize, String source) throws IOException {
        long fileLength = fileSize;
        // 1. 查找ZIP64定位器（位于ZIP文件末尾，固定20字节）
        long locatorStart = Math.max(0, fileLength - 20);
        byte[] locatorData = readRange(source, locatorStart, fileLength - 1);

        // 校验ZIP64定位器签名
        if (locatorData.length < 20 || readInt(locatorData, 0) != ZIP64_LOCATOR_SIGNATURE) {
            return null; // 非ZIP64格式，返回null
        }

        // 解析ZIP64定位器中的关键信息
        long zip64EocdDiskNumber = readInt(locatorData, 4) & ZIP64_MAGIC_NUMBER; // ZIP64 EOCD所在磁盘号
        long zip64EocdOffset = readLong(locatorData, 8); // ZIP64 EOCD的起始偏移量
        long totalDisks = readInt(locatorData, 16) & ZIP64_MAGIC_NUMBER; // 总磁盘数

        // 2. 读取ZIP64 EOCD记录（最小长度56字节，实际可能更长）
        byte[] zip64EocdData =
                readRange(source, zip64EocdOffset, zip64EocdOffset + 55); // 先读取基础56字节
        if (zip64EocdData.length < 56 || readInt(zip64EocdData, 0) != ZIP64_EOCD_SIGNATURE) {
            throw new IOException("无效的ZIP64中央目录结束记录（签名不匹配）");
        }

        // 解析ZIP64 EOCD的固定字段（按ZIP规范顺序）
        long eocdSize = readLong(zip64EocdData, 4); // ZIP64 EOCD总大小（不含签名）
        long versionMadeBy = readShort(zip64EocdData, 12) & 0xFFFFL; // 创建版本
        long versionNeeded = readShort(zip64EocdData, 14) & 0xFFFFL; // 所需版本
        long diskNumber = readInt(zip64EocdData, 16) & ZIP64_MAGIC_NUMBER; // 当前磁盘号
        long startDisk = readInt(zip64EocdData, 20) & ZIP64_MAGIC_NUMBER; // 中央目录起始磁盘号
        long diskEntries = readLong(zip64EocdData, 24); // 当前磁盘的中央目录条目数
        long totalEntries = readLong(zip64EocdData, 32); // 中央目录条目总数
        long centralDirSize = readLong(zip64EocdData, 40); // 中央目录总大小
        long centralDirOffset = readLong(zip64EocdData, 48); // 中央目录起始偏移量

        // 3. 解析ZIP64 EOCD的注释长度（若存在）
        long commentLength = 0;
        int eocdTotalLength = (int) (eocdSize + 4); // 总长度=EOCD大小+4字节签名
        if (eocdTotalLength > 56) {
            // 读取注释长度字段（位于EOCD末尾）
            byte[] commentLengthData =
                    readRange(
                            source,
                            zip64EocdOffset + eocdTotalLength - 2,
                            zip64EocdOffset + eocdTotalLength - 1);
            commentLength = readShort(commentLengthData, 0) & 0xFFFFL;
        }

        // 4. 构造完整的EocdInfo对象
        return new EocdInfo(
                diskNumber,
                startDisk,
                diskEntries,
                totalEntries,
                centralDirSize,
                centralDirOffset,
                commentLength);
    }

    public void scanAllEntries1(
            EocdInfo eocd, String source, TerminatingConsumer<CentralDirectoryModel> entryConsumer)
            throws IOException {
        // 优先使用ZIP64 EOCD（若存在）
        EocdInfo finalEocd = eocd;
        long fileSize = eocd.getFileSize();
        try {
            EocdInfo zip64Eocd = readZip64Eocd(fileSize, source);
            if (zip64Eocd != null) {
                finalEocd = zip64Eocd;
                log.info(
                        "使用ZIP64中央目录信息：offset={}, size={}, entries={}",
                        finalEocd.getCentralDirOffset(),
                        finalEocd.getCentralDirSize(),
                        finalEocd.getTotalEntries());
            }
        } catch (Exception e) {
            log.warn("读取ZIP64 EOCD失败，使用普通EOCD", e);
        }

        long totalDirSize = finalEocd.getCentralDirSize();
        long currentOffset = finalEocd.getCentralDirOffset();
        long totalEntries = finalEocd.getTotalEntries();
        long remaining = totalDirSize;
        long entryCount = 0;
        long fileLength = fileSize;

        while (remaining > 0) {
            // 读取中央目录条目签名（4字节）
            if (currentOffset + 4 > finalEocd.getCentralDirOffset() + totalDirSize) {
                break;
            }

            // 确保有足够的字节读取签名
            long signatureEnd = currentOffset + 3;
            if (signatureEnd >= fileLength) {
                log.warn("到达文件末尾，无法读取中央目录签名");
                break;
            }

            //            byte[] signatureBytes = readRange(source, currentOffset, signatureEnd);
            //            if (signatureBytes.length < 4) {
            //                log.warn("读取签名数据不足：{}字节", signatureBytes.length);
            //                break;
            //            }

            //            int signature =
            // ByteBuffer.wrap(signatureBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();

            //            if (signature != CENTRAL_DIR_SIGNATURE) {
            //                log.debug("无效的中央目录签名：{}，偏移量：{}", Integer.toHexString(signature),
            // currentOffset);
            //                currentOffset++;
            //                remaining--;
            //                continue;
            //            }

            // 读取条目头部（46字节固定长度）
            long headerEnd = currentOffset + 45;
            if (headerEnd >= fileLength) {
                log.warn("条目头部超出文件范围：{}", headerEnd);
                break;
            }

            byte[] headerData = readRange(source, currentOffset, headerEnd);
            if (headerData.length < 46) {
                log.warn("读取条目头部数据不足：{}字节", headerData.length);
                break;
            }

            // 解析条目长度字段
            int nameLen = readShort(headerData, 28) & 0xFFFF;
            int extraLen = readShort(headerData, 30) & 0xFFFF;
            int commentLen = readShort(headerData, 32) & 0xFFFF;
            int entryTotalLength = 46 + nameLen + extraLen + commentLen;

            if (entryTotalLength > remaining) {
                log.warn("条目长度超过剩余字节：{} > {}，终止解析", entryTotalLength, remaining);
                break;
            }

            // 检查条目是否超出文件范围
            long entryEnd = currentOffset + entryTotalLength - 1;
            if (entryEnd >= fileLength) {
                log.warn("条目超出文件范围：{}", entryEnd);
                break;
            }

            // 读取完整条目数据
            byte[] entryData = readRange(source, currentOffset, entryEnd);

            // 解析核心字段
            long compressionMethod = readShort(entryData, 10) & 0xFFFF;
            long compressedSize32 = readInt(entryData, 20) & ZIP64_MAGIC_NUMBER;
            long uncompressedSize32 = readInt(entryData, 24) & ZIP64_MAGIC_NUMBER;
            long headerOffset32 = readInt(entryData, 42) & ZIP64_MAGIC_NUMBER;

            // 读取文件名
            String fileName = "";
            if (nameLen > 0 && 46 + nameLen <= entryData.length) {
                fileName = decodeFileName(entryData, 46, nameLen);
                fileName = normalizePath(fileName);
            }

            // 判断是否为目录
            boolean isDirectory = false;
            isDirectory = isDirectory(fileName, compressedSize32, uncompressedSize32);

            // 处理ZIP64扩展字段 - 增强版解析
            long compressedSize = compressedSize32;
            long uncompressedSize = uncompressedSize32;
            long headerOffset = headerOffset32;

            // 如果任何字段是ZIP64占位符，强制扫描整个扩展字段
            boolean needZip64Parsing =
                    (compressedSize32 == ZIP64_MAGIC_NUMBER
                            || uncompressedSize32 == ZIP64_MAGIC_NUMBER
                            || headerOffset32 == ZIP64_MAGIC_NUMBER);

            if (needZip64Parsing && extraLen > 0) {
                // 直接扫描整个扩展字段数据，不依赖结构解析
                int extraPos = 46 + nameLen;
                byte[] extraData = Arrays.copyOfRange(entryData, extraPos, extraPos + extraLen);

                // 查找ZIP64扩展字段（ID: 0x0001）
                int pos = 0;
                while (pos + 4 <= extraData.length) {
                    int headerId = (extraData[pos] & 0xFF) | ((extraData[pos + 1] & 0xFF) << 8);
                    int dataSize = (extraData[pos + 2] & 0xFF) | ((extraData[pos + 3] & 0xFF) << 8);

                    if (headerId == ZIP64_EXTRA_FIELD_ID) {
                        log.debug("{}: 找到ZIP64扩展字段，大小：{}", fileName, dataSize);

                        // 解析ZIP64扩展字段内容
                        ByteBuffer buffer =
                                ByteBuffer.wrap(
                                        extraData,
                                        pos + 4,
                                        Math.min(dataSize, extraData.length - pos - 4));
                        buffer.order(ByteOrder.LITTLE_ENDIAN);

                        if (uncompressedSize32 == ZIP64_MAGIC_NUMBER && buffer.remaining() >= 8) {
                            uncompressedSize = buffer.getLong();
                            log.debug("{}: ZIP64未压缩大小 = {}", fileName, uncompressedSize);
                        }
                        if (compressedSize32 == ZIP64_MAGIC_NUMBER && buffer.remaining() >= 8) {
                            compressedSize = buffer.getLong();
                            log.debug("{}: ZIP64压缩大小 = {}", fileName, compressedSize);
                        }
                        if (headerOffset32 == ZIP64_MAGIC_NUMBER && buffer.remaining() >= 8) {
                            headerOffset = buffer.getLong();
                            log.debug("{}: ZIP64本地头偏移 = {}", fileName, headerOffset);
                        }

                        break; // 找到ZIP64字段后退出
                    }

                    pos += 4 + dataSize;
                }

                // 如果仍然没有找到ZIP64字段，尝试从中央目录直接计算偏移量
                if (headerOffset == ZIP64_MAGIC_NUMBER) {
                    log.warn("{}: ZIP64扩展字段未找到，尝试从中央目录位置估算偏移量", fileName);
                    // 这是一个备选方案，可能不准确，但比失败好
                    headerOffset = estimateLocalHeaderOffset(currentOffset, fileLength, source);
                }
            }

            // 校验headerOffset合法性
            Long dataOffset = null;
            if (headerOffset >= 0 && headerOffset < fileLength) {
                try {
                    LocalFileHeader localHeader =
                            readLocalFileHeader(headerOffset, source, fileSize);
                    dataOffset = localHeader.getDataOffset();
                } catch (Exception e) {
                    log.warn("读取本地文件头失败：fileName={}, headerOffset={}", fileName, headerOffset, e);
                    // 尝试估算数据偏移量
                    dataOffset = estimateDataOffset(headerOffset, source, fileSize);
                }
            } else {
                log.warn(
                        "headerOffset超出文件范围：{}（文件长度：{}），fileName={}",
                        headerOffset,
                        fileLength,
                        fileName);
                // 尝试修复偏移量
                if (headerOffset >= fileLength) {
                    log.warn("{}: 尝试修复过大的偏移量", fileName);
                    headerOffset = fileLength - 100000; // 向后偏移
                    if (headerOffset > 0) {
                        try {
                            LocalFileHeader localHeader =
                                    readLocalFileHeader(headerOffset, source, fileSize);
                            dataOffset = localHeader.getDataOffset();
                        } catch (Exception e) {
                            dataOffset = estimateDataOffset(headerOffset, source, fileSize);
                        }
                    }
                }
            }

            // 创建条目并消费
            CentralDirectoryModel entry =
                    new CentralDirectoryModel(
                            headerOffset,
                            dataOffset,
                            compressionMethod,
                            compressedSize,
                            uncompressedSize,
                            fileName,
                            entryTotalLength);
            entry.setDirectoryIs(isDirectory);
            Long allCount = totalEntries;
            Long currentCount = entryCount; // 重新赋值一下，面得accept里面修改了变量的值
            boolean accept = entryConsumer.accept(entry, allCount, currentCount);

            if (!accept) {
                log.info("accept 接收到该中央目录后返回停止条件 {}", entry.getName());
                break;
            }

            // 更新偏移量
            currentOffset += entryTotalLength;
            remaining -= entryTotalLength;
            entryCount++;
        }

        log.info("中央目录扫描完成：共解析{}个条目", entryCount);
    }

    public void scanAllEntries(
            EocdInfo eocd, String source, TerminatingConsumer<CentralDirectoryModel> entryConsumer)
            throws IOException {
        // ===================== 固定配置 =====================
        final int BATCH_SIZE = 500; // 每批解析多少条
        final int QUEUE_CAPACITY = 2000;
        int threads = Math.min(Runtime.getRuntime().availableProcessors() * 2, 8);

        EocdInfo finalEocd = eocd;
        long fileSize = eocd.getFileSize();
        try {
            EocdInfo zip64Eocd = readZip64Eocd(fileSize, source);
            if (zip64Eocd != null) finalEocd = zip64Eocd;
        } catch (Exception e) {
            log.warn("读取ZIP64 EOCD失败，使用普通EOCD");
        }

        long totalDirSize = finalEocd.getCentralDirSize();
        long currentOffset = finalEocd.getCentralDirOffset();
        long totalEntries = finalEocd.getTotalEntries();
        long fileLength = fileSize;
        long remaining = totalDirSize;

        BlockingQueue<CentralDirectoryModel> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
        ExecutorService producerExecutor = Executors.newWorkStealingPool(threads);
        AtomicBoolean producerFinish = new AtomicBoolean(false);
        AtomicBoolean shouldStop = new AtomicBoolean(false);
        AtomicLong entryCount = new AtomicLong(0);
        Thread consumerThread =
                new Thread(
                        () -> {
                            try {
                                while (!shouldStop.get()) {
                                    // 队列获取，超时判断是否结束
                                    CentralDirectoryModel entry =
                                            queue.poll(100, TimeUnit.MILLISECONDS);
                                    if (entry == null && producerFinish.get()) {
                                        break;
                                    }
                                    if (entry == null) continue;

                                    try {
                                        long index = entryCount.incrementAndGet() - 1;
                                        boolean result =
                                                entryConsumer.accept(entry, totalEntries, index);
                                        // 如果消费者返回false，设置停止标志
                                        if (!result) {
                                            shouldStop.set(true);
                                            log.info("消费者返回false，停止扫描，当前已处理：{} 条", index + 1);
                                            break;
                                        }

                                    } catch (Exception e) {
                                        shouldStop.set(true); // 异常时也停止
                                        log.error("消费条目异常", e);
                                    }
                                }
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        },
                        "zip-entry-consumer");
        consumerThread.start();
        try {
            while (remaining > 0 && !shouldStop.get()) {
                List<EntryPosition> batch = new ArrayList<>(BATCH_SIZE);
                while (batch.size() < BATCH_SIZE && remaining > 0 && !shouldStop.get()) {
                    long headerEnd = currentOffset + 45;
                    if (headerEnd >= fileLength) break;

                    byte[] headerData = readRange(source, currentOffset, headerEnd);
                    if (headerData.length < 46) break;

                    int nameLen = readShort(headerData, 28) & 0xFFFF;
                    int extraLen = readShort(headerData, 30) & 0xFFFF;
                    int commentLen = readShort(headerData, 32) & 0xFFFF;
                    int entryTotalLength = 46 + nameLen + extraLen + commentLen;

                    if (entryTotalLength > remaining) break;
                    long entryEnd = currentOffset + entryTotalLength - 1;
                    if (entryEnd >= fileLength) break;

                    batch.add(new EntryPosition(currentOffset, entryTotalLength));
                    currentOffset += entryTotalLength;
                    remaining -= entryTotalLength;
                }

                if (batch.isEmpty()) break;

                // 并行解析
                List<CompletableFuture<Void>> futures = new ArrayList<>();
                for (EntryPosition pos : batch) {
                    if (shouldStop.get()) break;
                    futures.add(
                            CompletableFuture.runAsync(
                                    () -> {
                                        try {
                                            long entryOffset = pos.offset;
                                            int entryLen = pos.totalLength;
                                            long entryEnd = entryOffset + entryLen - 1;
                                            byte[] entryData =
                                                    readRange(source, entryOffset, entryEnd);

                                            // -------------- 你原有解析逻辑 --------------
                                            long compressionMethod =
                                                    readShort(entryData, 10) & 0xFFFF;
                                            long compressedSize32 =
                                                    readInt(entryData, 20) & ZIP64_MAGIC_NUMBER;
                                            long uncompressedSize32 =
                                                    readInt(entryData, 24) & ZIP64_MAGIC_NUMBER;
                                            long headerOffset32 =
                                                    readInt(entryData, 42) & ZIP64_MAGIC_NUMBER;

                                            int nameLen = readShort(entryData, 28) & 0xFFFF;
                                            String fileName = "";
                                            if (nameLen > 0 && 46 + nameLen <= entryData.length) {
                                                fileName = decodeFileName(entryData, 46, nameLen);
                                                fileName = normalizePath(fileName);
                                            }

                                            boolean isDirectory =
                                                    isDirectory(
                                                            fileName,
                                                            compressedSize32,
                                                            uncompressedSize32);
                                            long compressedSize = compressedSize32;
                                            long uncompressedSize = uncompressedSize32;
                                            long headerOffset = headerOffset32;
                                            int extraLen = readShort(entryData, 30) & 0xFFFF;

                                            boolean needZip64Parsing =
                                                    (compressedSize32 == ZIP64_MAGIC_NUMBER
                                                            || uncompressedSize32
                                                                    == ZIP64_MAGIC_NUMBER
                                                            || headerOffset32
                                                                    == ZIP64_MAGIC_NUMBER);

                                            if (needZip64Parsing && extraLen > 0) {
                                                int extraPos = 46 + nameLen;
                                                byte[] extraData =
                                                        Arrays.copyOfRange(
                                                                entryData,
                                                                extraPos,
                                                                extraPos + extraLen);
                                                int p = 0;
                                                while (p + 4 <= extraData.length) {
                                                    int hid =
                                                            (extraData[p] & 0xFF)
                                                                    | ((extraData[p + 1] & 0xFF)
                                                                            << 8);
                                                    int dsz =
                                                            (extraData[p + 2] & 0xFF)
                                                                    | ((extraData[p + 3] & 0xFF)
                                                                            << 8);
                                                    if (hid == ZIP64_EXTRA_FIELD_ID) {
                                                        ByteBuffer buf =
                                                                ByteBuffer.wrap(
                                                                        extraData,
                                                                        p + 4,
                                                                        Math.min(
                                                                                dsz,
                                                                                extraData.length
                                                                                        - p
                                                                                        - 4));
                                                        buf.order(ByteOrder.LITTLE_ENDIAN);
                                                        if (uncompressedSize32 == ZIP64_MAGIC_NUMBER
                                                                && buf.remaining() >= 8)
                                                            uncompressedSize = buf.getLong();
                                                        if (compressedSize32 == ZIP64_MAGIC_NUMBER
                                                                && buf.remaining() >= 8)
                                                            compressedSize = buf.getLong();
                                                        if (headerOffset32 == ZIP64_MAGIC_NUMBER
                                                                && buf.remaining() >= 8)
                                                            headerOffset = buf.getLong();
                                                        break;
                                                    }
                                                    p += 4 + dsz;
                                                }
                                                if (headerOffset == ZIP64_MAGIC_NUMBER) {
                                                    headerOffset =
                                                            estimateLocalHeaderOffset(
                                                                    entryOffset,
                                                                    fileLength,
                                                                    source);
                                                }
                                            }

                                            Long dataOffset = null;
                                            if (headerOffset >= 0 && headerOffset < fileLength) {
                                                try {
                                                    LocalFileHeader lh =
                                                            readLocalFileHeader(
                                                                    headerOffset, source, fileSize);
                                                    dataOffset = lh.getDataOffset();
                                                } catch (Exception e) {
                                                    dataOffset =
                                                            estimateDataOffset(
                                                                    headerOffset, source, fileSize);
                                                }
                                            } else {
                                                if (headerOffset >= fileLength) {
                                                    headerOffset = fileLength - 100000;
                                                    if (headerOffset > 0) {
                                                        try {
                                                            LocalFileHeader lh =
                                                                    readLocalFileHeader(
                                                                            headerOffset,
                                                                            source,
                                                                            fileSize);
                                                            dataOffset = lh.getDataOffset();
                                                        } catch (Exception e) {
                                                            dataOffset =
                                                                    estimateDataOffset(
                                                                            headerOffset,
                                                                            source,
                                                                            fileSize);
                                                        }
                                                    }
                                                }
                                            }

                                            CentralDirectoryModel entry =
                                                    new CentralDirectoryModel(
                                                            headerOffset,
                                                            dataOffset,
                                                            compressionMethod,
                                                            compressedSize,
                                                            uncompressedSize,
                                                            fileName,
                                                            entryLen);
                                            entry.setDirectoryIs(isDirectory);

                                            if (!shouldStop.get()) {
                                                queue.put(entry);
                                            }

                                        } catch (Exception e) {
                                            log.error("解析条目失败", e);
                                        }
                                    },
                                    producerExecutor));
                }

                if (!futures.isEmpty() && !shouldStop.get()) {
                    try {
                        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                                .get(10, TimeUnit.HOURS);
                    } catch (Exception e) {
                        log.warn("等待批次完成超时或异常", e);
                    }
                }
                if (shouldStop.get()) {
                    break;
                }
            }

        } catch (Exception e) {
            log.error("扫描异常", e);
        } finally {
            // 标记生产完成
            producerFinish.set(true);
            producerExecutor.shutdown();
        }

        if (shouldStop.get()) {
            queue.clear();
            log.info("因停止标志，清空队列中未处理的数据");
        }
        // 等待消费者消费完
        try {
            consumerThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("中央目录扫描+消费完成：共解析 {} 个条目", entryCount.get());
    }

    /** 估算本地文件头偏移量（备选方案） */
    private long estimateLocalHeaderOffset(long centralDirOffset, long fileLength, String source) {
        // 从中央目录位置向前查找本地文件头
        long searchStart = Math.max(0, centralDirOffset - 1000000); // 向前1MB
        long searchEnd = centralDirOffset;

        try {
            // 读取一段数据进行搜索
            int searchChunkSize = (int) Math.min(100000, searchEnd - searchStart);
            byte[] searchData = readRange(source, searchStart, searchStart + searchChunkSize - 1);

            // 查找本地文件头签名
            for (int i = 0; i < searchData.length - 3; i++) {
                int sig =
                        (searchData[i] & 0xFF)
                                | ((searchData[i + 1] & 0xFF) << 8)
                                | ((searchData[i + 2] & 0xFF) << 16)
                                | ((searchData[i + 3] & 0xFF) << 24);

                if (sig == LOCAL_FILE_HEADER_SIGNATURE) {
                    long foundOffset = searchStart + i;
                    log.debug("估算找到本地文件头签名在偏移量：{}", foundOffset);
                    return foundOffset;
                }
            }
        } catch (Exception e) {
            log.warn("估算本地文件头偏移量失败", e);
        }

        // 如果找不到，返回一个合理的默认值
        return Math.max(0, centralDirOffset - 10000);
    }

    private static boolean isDirectory(
            String fileName, long compressedSize32, long uncompressedSize32) {
        boolean isDirectory = false;
        // 规则1：文件名以路径分隔符结尾（ZIP标准目录标识）
        if (fileName.endsWith("/") || fileName.endsWith("\\")) {
            isDirectory = true;
        }
        // 规则2：压缩大小和未压缩大小均为0且包含路径分隔符（兼容部分工具创建的目录条目）
        else if (compressedSize32 == 0
                && uncompressedSize32 == 0
                && (fileName.contains("/") || fileName.contains("\\"))) {
            isDirectory = true;
        }
        // 规则3：文件名本身是盘符或根目录（特殊情况处理）
        else if (fileName.matches("^[a-zA-Z]:[/\\\\]?$")) {
            isDirectory = true;
        }
        return isDirectory;
    }

    public void scanAllEntries(
            String source, TerminatingConsumer<CentralDirectoryModel> entryConsumer)
            throws IOException {
        long fileSize = getFileSize(source);
        EocdInfo eocdInfo = parseEocd(fileSize, source);
        eocdInfo.setFileSize(fileSize);
        scanAllEntries(eocdInfo, source, entryConsumer);
    }

    /** 估算数据偏移量（当无法读取本地文件头时使用） */
    private long estimateDataOffset(long headerOffset, String source, long fileSize)
            throws IOException {
        if (headerOffset == ZIP64_MAGIC_NUMBER) {
            log.error("无法估算偏移量：headerOffset是ZIP64占位符");
            // 尝试从文件开头开始查找
            return 0;
        }

        long fileLength = fileSize;

        // 读取本地文件头的前30字节来获取文件名长度和扩展字段长度
        long headerEnd = Math.min(headerOffset + 29, fileLength - 1);
        if (headerOffset > headerEnd) {
            return headerOffset + 30; // 默认偏移
        }

        byte[] headerData = readRange(source, headerOffset, headerEnd);
        if (headerData.length < 30) {
            return headerOffset + 30; // 默认偏移
        }

        try {
            int nameLen = readShort(headerData, 26) & 0xFFFF;
            int extraLen = readShort(headerData, 28) & 0xFFFF;
            return headerOffset + 30 + nameLen + extraLen;
        } catch (Exception e) {
            log.warn("估算数据偏移量失败，使用默认值", e);
            return headerOffset + 30; // 默认偏移
        }
    }

    /** 读取本地文件头（增强容错版） */
    private LocalFileHeader readLocalFileHeader(long offset, String source, long fileSize)
            throws IOException {
        // 首先检查是否是ZIP64占位符
        if (offset == ZIP64_MAGIC_NUMBER) {
            throw new IOException("headerOffset是ZIP64占位符，未解析真实值");
        }

        long fileLength = fileSize;

        // 基本范围检查
        if (offset < 0 || offset >= fileLength) {
            throw new IOException("本地文件头偏移量无效：" + offset + "（文件长度：" + fileLength + "）");
        }

        // 检查是否有足够的字节读取完整的本地文件头
        long headerEnd = offset + 29;
        if (headerEnd >= fileLength) {
            throw new IOException("本地文件头超出文件范围：" + headerEnd);
        }

        // 读取基础头数据
        byte[] headerData = readRange(source, offset, headerEnd);
        if (headerData.length < 30) {
            throw new IOException("本地文件头数据不完整：仅读取到" + headerData.length + "字节");
        }

        // 检查签名（支持小范围搜索）
        int signature = readInt(headerData, 0);
        if (signature != LOCAL_FILE_HEADER_SIGNATURE) {
            // 尝试在更大范围内搜索有效的签名
            long searchStart = Math.max(0, offset - 1000);
            long searchEnd = Math.min(fileLength - 4, offset + 1000);

            boolean found = false;
            for (long i = searchStart; i <= searchEnd; i += 4) { // 按4字节对齐搜索
                if (i + 3 >= fileLength) break;

                byte[] sigData = readRange(source, i, i + 3);
                if (sigData.length < 4) continue;

                int currentSig = readInt(sigData, 0);
                if (currentSig == LOCAL_FILE_HEADER_SIGNATURE) {
                    offset = i;
                    log.debug("在偏移量{}找到有效的本地文件头签名（原偏移量{}）", i, offset);

                    // 重新读取头数据
                    headerEnd = offset + 29;
                    if (headerEnd >= fileLength) {
                        throw new IOException("找到的签名位置超出文件范围：" + headerEnd);
                    }
                    headerData = readRange(source, offset, headerEnd);
                    signature = currentSig;
                    found = true;
                    break;
                }
            }

            if (!found) {
                throw new IOException(
                        "无效的本地文件头签名：0x" + Integer.toHexString(signature) + "，偏移量：" + offset);
            }
        }

        // 解析文件名长度和扩展字段长度
        int nameLen = readShort(headerData, 26) & 0xFFFF;
        int extraLen = readShort(headerData, 28) & 0xFFFF;

        // 计算数据偏移量
        long dataOffset = offset + 30 + nameLen + extraLen;

        // 验证数据偏移量是否有效
        if (dataOffset > fileLength) {
            log.warn("计算出的数据偏移量超出文件范围：{}（文件长度：{}）", dataOffset, fileLength);
            dataOffset = fileLength; // 设为文件末尾
        }

        return new LocalFileHeader(nameLen, extraLen, dataOffset);
    }

    /** 在中央目录分块中查找目标路径（支持文件和文件夹） */
    private CentralDirectoryModel findEntryInDirChunk(
            byte[] dirChunk,
            String normalizedTarget,
            boolean isFolderCheck,
            long chunkOffset,
            String source)
            throws IOException {
        int pos = 0;

        // 处理跨分块的情况：回退查找可能的签名
        if (dirChunk.length > 46) {
            for (int i = Math.min(45, dirChunk.length - 4); i >= 0; i--) {
                if (readInt(dirChunk, i) == CENTRAL_DIR_SIGNATURE) {
                    pos = i;
                    break;
                }
            }
        }

        while (pos + 4 <= dirChunk.length) {
            // 1. 查找中央目录签名
            if (readInt(dirChunk, pos) != CENTRAL_DIR_SIGNATURE) {
                pos++;
                continue;
            }

            // 2. 确保有足够的字节
            if (pos + 46 > dirChunk.length) {
                break; // 跨分块，由外层处理
            }

            // 3. 解析长度字段
            int nameLen = readShort(dirChunk, pos + 28) & 0xFFFF;
            int extraLen = readShort(dirChunk, pos + 30) & 0xFFFF;
            int commentLen = readShort(dirChunk, pos + 32) & 0xFFFF;

            // 4. 检查条目完整性
            int entryTotalLength = 46 + nameLen + extraLen + commentLen;
            if (pos + entryTotalLength > dirChunk.length) {
                break;
            }

            // 5. 解析文件名
            int nameStart = pos + 46;
            String fileName = decodeFileName(dirChunk, nameStart, nameLen);
            String normalizedFile = normalizePath(fileName);

            // 6. 判断当前条目类型（文件/文件夹）
            boolean isFolderEntry = normalizedFile.endsWith("/") || fileName.endsWith("/");

            // 7. 灵活的路径匹配
            boolean isMatch = false;

            if (isFolderCheck) {
                // 文件夹匹配逻辑
                String folderTargetWithSlash =
                        normalizedTarget.endsWith("/") ? normalizedTarget : normalizedTarget + "/";
                String folderFileWithSlash =
                        normalizedFile.endsWith("/") ? normalizedFile : normalizedFile + "/";

                // 精确匹配文件夹
                if (normalizedFile.equals(normalizedTarget)
                        || normalizedFile.equals(folderTargetWithSlash)
                        || folderFileWithSlash.equals(normalizedTarget)) {
                    isMatch = true;
                }
                // 子文件/子文件夹匹配
                else if (normalizedFile.startsWith(folderTargetWithSlash)) {
                    isMatch = true;
                }
            } else {
                // 文件匹配逻辑
                if (normalizedFile.equals(normalizedTarget)) {
                    isMatch = true;
                }
                // 处理文件名大小写问题
                else if (normalizedFile.equalsIgnoreCase(normalizedTarget)) {
                    isMatch = true;
                }
            }

            // 8. 检查Unicode文件名
            String unicodeFileName = null;
            int extraPos = nameStart + nameLen;
            if (!isMatch && extraLen > 0) {
                int currentExtraPos = extraPos;
                while (currentExtraPos + 4 <= extraPos + extraLen) {
                    int headerId = readShort(dirChunk, currentExtraPos) & 0xFFFF;
                    int dataSize = readShort(dirChunk, currentExtraPos + 2) & 0xFFFF;

                    // 处理Unicode文件名扩展
                    if (headerId == 0x7075 || headerId == 0x0007) {
                        int dataPtr = currentExtraPos + 4;
                        if (dataPtr + nameLen <= currentExtraPos + dataSize) {
                            unicodeFileName =
                                    new String(dirChunk, dataPtr, nameLen, StandardCharsets.UTF_8);
                            String normalizedUnicodeFile = normalizePath(unicodeFileName);

                            // 重新检查匹配
                            if (isFolderCheck) {
                                String folderTargetWithSlash =
                                        normalizedTarget.endsWith("/")
                                                ? normalizedTarget
                                                : normalizedTarget + "/";
                                if (normalizedUnicodeFile.equals(normalizedTarget)
                                        || normalizedUnicodeFile.startsWith(
                                                folderTargetWithSlash)) {
                                    isMatch = true;
                                    normalizedFile = normalizedUnicodeFile;
                                }
                            } else {
                                if (normalizedUnicodeFile.equals(normalizedTarget)
                                        || normalizedUnicodeFile.equalsIgnoreCase(
                                                normalizedTarget)) {
                                    isMatch = true;
                                    normalizedFile = normalizedUnicodeFile;
                                }
                            }
                        }
                    }

                    currentExtraPos += 4 + dataSize;
                    if (isMatch) break;
                }
            }

            if (isMatch) {
                log.debug(
                        "找到匹配路径：ZIP内路径=[{}]（{}），目标路径=[{}]（{}）",
                        normalizedFile,
                        isFolderEntry ? "文件夹" : "文件",
                        normalizedTarget,
                        isFolderCheck ? "文件夹" : "文件");

                // 解析核心字段
                long headerOffset32 = readInt(dirChunk, pos + 42) & ZIP64_MAGIC_NUMBER;
                long compressedSize32 = readInt(dirChunk, pos + 20) & ZIP64_MAGIC_NUMBER;
                long uncompressedSize32 = readInt(dirChunk, pos + 24) & ZIP64_MAGIC_NUMBER;
                long compressionMethod = readShort(dirChunk, pos + 10) & 0xFFFF;

                // 解析扩展字段（支持ZIP64）
                long headerOffset = headerOffset32;
                long compressedSize = compressedSize32;
                long uncompressedSize = uncompressedSize32;

                // 直接扫描整个扩展字段
                if (extraLen > 0) {
                    byte[] extraData = Arrays.copyOfRange(dirChunk, extraPos, extraPos + extraLen);
                    int extraPosInChunk = 0;

                    while (extraPosInChunk + 4 <= extraData.length) {
                        int headerId =
                                (extraData[extraPosInChunk] & 0xFF)
                                        | ((extraData[extraPosInChunk + 1] & 0xFF) << 8);
                        int dataSize =
                                (extraData[extraPosInChunk + 2] & 0xFF)
                                        | ((extraData[extraPosInChunk + 3] & 0xFF) << 8);

                        if (headerId == ZIP64_EXTRA_FIELD_ID) {
                            ByteBuffer buffer =
                                    ByteBuffer.wrap(
                                            extraData,
                                            extraPosInChunk + 4,
                                            Math.min(
                                                    dataSize,
                                                    extraData.length - extraPosInChunk - 4));
                            buffer.order(ByteOrder.LITTLE_ENDIAN);

                            if (uncompressedSize32 == ZIP64_MAGIC_NUMBER
                                    && buffer.remaining() >= 8) {
                                uncompressedSize = buffer.getLong();
                            }
                            if (compressedSize32 == ZIP64_MAGIC_NUMBER && buffer.remaining() >= 8) {
                                compressedSize = buffer.getLong();
                            }
                            if (headerOffset32 == ZIP64_MAGIC_NUMBER && buffer.remaining() >= 8) {
                                headerOffset = buffer.getLong();
                                log.debug("条目{}的真实headerOffset：{}", fileName, headerOffset);
                            }

                            break;
                        }

                        extraPosInChunk += 4 + dataSize;
                    }
                }

                // 如果ZIP64解析失败，尝试估算
                if (headerOffset == ZIP64_MAGIC_NUMBER) {
                    log.warn("{}: ZIP64扩展字段解析失败，尝试估算偏移量", fileName);
                    headerOffset =
                            estimateLocalHeaderOffset(
                                    chunkOffset + pos, getFileSize(source), source);
                }

                boolean isDirectory = false;
                isDirectory = isDirectory(fileName, compressedSize32, uncompressedSize32);
                CentralDirectoryModel centralDirectoryModel =
                        new CentralDirectoryModel(
                                headerOffset,
                                null,
                                compressionMethod,
                                compressedSize,
                                uncompressedSize,
                                unicodeFileName != null ? unicodeFileName : fileName,
                                (int) (chunkOffset + pos));
                centralDirectoryModel.setDirectoryIs(isDirectory);
                return centralDirectoryModel;
            }

            // 9. 移动到下一个条目
            pos += entryTotalLength;
        }

        return null;
    }

    /** 在中央目录块中查找所有匹配的路径（支持文件和文件夹） */
    private void findAllMatchingPaths(
            byte[] dirChunk,
            Set<String> checkPaths,
            Set<String> existsPaths,
            Map<String, Boolean> pathTypeMap) {
        int pos = 0;

        // 处理跨分块的情况
        if (dirChunk.length > 46) {
            for (int i = Math.min(45, dirChunk.length - 4); i >= 0; i--) {
                if (readInt(dirChunk, i) == CENTRAL_DIR_SIGNATURE) {
                    pos = i;
                    break;
                }
            }
        }

        while (pos + 4 <= dirChunk.length && !checkPaths.isEmpty()) {
            if (readInt(dirChunk, pos) != CENTRAL_DIR_SIGNATURE) {
                pos++;
                continue;
            }

            if (pos + 46 > dirChunk.length) {
                break;
            }

            // 解析长度字段
            int nameLen = readShort(dirChunk, pos + 28) & 0xFFFF;
            int extraLen = readShort(dirChunk, pos + 30) & 0xFFFF;
            int commentLen = readShort(dirChunk, pos + 32) & 0xFFFF;
            int entryTotalLength = 46 + nameLen + extraLen + commentLen;

            if (pos + entryTotalLength > dirChunk.length) {
                break;
            }

            // 解析文件名
            int nameStart = pos + 46;
            String fileName = decodeFileName(dirChunk, nameStart, nameLen);
            String normalizedFile = normalizePath(fileName);
            boolean isFolderEntry = normalizedFile.endsWith("/") || fileName.endsWith("/");

            // 检查所有待匹配路径
            Iterator<String> iterator = checkPaths.iterator();
            while (iterator.hasNext()) {
                String checkPath = iterator.next();
                Boolean isFolderCheck = pathTypeMap.get(checkPath);
                if (isFolderCheck == null) continue;

                boolean matchFound = false;

                if (isFolderCheck) {
                    // 文件夹匹配
                    String folderCheckWithSlash =
                            checkPath.endsWith("/") ? checkPath : checkPath + "/";
                    if (normalizedFile.equals(checkPath)
                            || normalizedFile.equals(folderCheckWithSlash)
                            || normalizedFile.startsWith(folderCheckWithSlash)) {
                        matchFound = true;
                    }
                } else {
                    // 文件匹配
                    if (normalizedFile.equals(checkPath)
                            || normalizedFile.equalsIgnoreCase(checkPath)) {
                        matchFound = true;
                    }
                }

                if (matchFound) {
                    existsPaths.add(checkPath);
                    iterator.remove();
                    log.trace("找到匹配路径：{}", checkPath);
                }
            }

            // 检查Unicode文件名
            if (!checkPaths.isEmpty() && extraLen > 0) {
                int currentExtraPos = nameStart + nameLen;
                while (currentExtraPos + 4 <= nameStart + nameLen + extraLen) {
                    int headerId = readShort(dirChunk, currentExtraPos) & 0xFFFF;
                    int dataSize = readShort(dirChunk, currentExtraPos + 2) & 0xFFFF;

                    if (headerId == 0x7075 || headerId == 0x0007) {
                        int dataPtr = currentExtraPos + 4;
                        if (dataPtr + nameLen <= currentExtraPos + dataSize) {
                            String unicodeFileName =
                                    new String(dirChunk, dataPtr, nameLen, StandardCharsets.UTF_8);
                            String normalizedUnicodeFile = normalizePath(unicodeFileName);

                            // 再次检查匹配
                            Iterator<String> unicodeIterator = checkPaths.iterator();
                            while (unicodeIterator.hasNext()) {
                                String checkPath = unicodeIterator.next();
                                Boolean isFolderCheck = pathTypeMap.get(checkPath);
                                if (isFolderCheck == null) continue;

                                boolean matchFound = false;
                                if (isFolderCheck) {
                                    String folderCheckWithSlash =
                                            checkPath.endsWith("/") ? checkPath : checkPath + "/";
                                    if (normalizedUnicodeFile.equals(checkPath)
                                            || normalizedUnicodeFile.startsWith(
                                                    folderCheckWithSlash)) {
                                        matchFound = true;
                                    }
                                } else {
                                    if (normalizedUnicodeFile.equals(checkPath)
                                            || normalizedUnicodeFile.equalsIgnoreCase(checkPath)) {
                                        matchFound = true;
                                    }
                                }

                                if (matchFound) {
                                    existsPaths.add(checkPath);
                                    unicodeIterator.remove();
                                    log.trace("找到Unicode匹配路径：{}", checkPath);
                                }
                            }
                        }
                    }

                    currentExtraPos += 4 + dataSize;
                    if (checkPaths.isEmpty()) break;
                }
            }

            pos += entryTotalLength;
        }
    }

    /** 解码文件名（支持多编码容错） */
    private String decodeFileName(byte[] data, int offset, int length) {
        if (length <= 0) {
            return "";
        }
        // 尝试多种编码
        List<Charset> charsets =
                Arrays.asList(
                        StandardCharsets.UTF_8,
                        Charset.forName("GBK"),
                        Charset.forName("ISO-8859-1"),
                        Charset.forName("CP437"),
                        Charset.defaultCharset());

        for (Charset charset : charsets) {
            try {
                return new String(data, offset, length, charset);
            } catch (Exception e) {
                continue;
            }
        }

        // 所有编码都失败，返回十六进制
        log.warn("文件名解码失败，长度={}字节，使用十六进制表示", length);
        return bytesToHex(data, offset, length);
    }

    /** 字节数组转十六进制字符串（用于调试） */
    private String bytesToHex(byte[] data, int offset, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(String.format("%02x", data[offset + i]));
        }
        return sb.toString();
    }

    /** 解析标准EOCD结构 */
    private EocdInfo parseStandardEocd(byte[] data, int offset) {
        //        return new EocdInfo(
        //                (long) readShort(data, offset + 4),
        //                (long) readShort(data, offset + 6),
        //                (long) readShort(data, offset + 8),
        //                (long) readShort(data, offset + 10),
        //                readInt(data, offset + 12) & ZIP64_MAGIC_NUMBER,
        //                readInt(data, offset + 16) & ZIP64_MAGIC_NUMBER,
        //                (long) readShort(data, offset + 20)
        //        );
        return new EocdInfo(
                (long) readShort(data, offset + 4) & 0xFFFFL, // 转为无符号
                (long) readShort(data, offset + 6) & 0xFFFFL,
                (long) readShort(data, offset + 8) & 0xFFFFL,
                (long) readShort(data, offset + 10) & 0xFFFFL, // 关键：totalEntries转无符号
                readInt(data, offset + 12) & ZIP64_MAGIC_NUMBER,
                readInt(data, offset + 16) & ZIP64_MAGIC_NUMBER,
                (long) readShort(data, offset + 20) & 0xFFFFL);
    }

    /** 解析ZIP64 EOCD结构 */
    private EocdInfo parseZip64Eocd(long locatorPosition, long fileSize, String source)
            throws IOException {
        // 确保定位器位置有效
        if (locatorPosition < 0 || locatorPosition + 19 >= fileSize) {
            locatorPosition = Math.max(0, fileSize - 20);
        }

        byte[] locatorData = readRange(source, locatorPosition, locatorPosition + 19);
        if (locatorData.length < 20 || readInt(locatorData, 0) != ZIP64_LOCATOR_SIGNATURE) {
            throw new IOException("无效的ZIP64定位器签名");
        }

        long zip64EocdOffset = readLong(locatorData, 8);
        long zip64EocdEnd = zip64EocdOffset + 55;
        if (zip64EocdOffset < 0 || zip64EocdEnd >= fileSize) {
            throw new IOException("ZIP64 EOCD范围无效：" + zip64EocdOffset + "-" + zip64EocdEnd);
        }

        byte[] zip64Eocd = readRange(source, zip64EocdOffset, zip64EocdEnd);
        if (zip64Eocd.length < 56 || readInt(zip64Eocd, 0) != ZIP64_EOCD_SIGNATURE) {
            throw new IOException("无效的ZIP64 EOCD签名");
        }

        return new EocdInfo(
                readInt(zip64Eocd, 16) & ZIP64_MAGIC_NUMBER,
                readInt(zip64Eocd, 20) & ZIP64_MAGIC_NUMBER,
                readLong(zip64Eocd, 24),
                readLong(zip64Eocd, 32),
                readLong(zip64Eocd, 40),
                readLong(zip64Eocd, 48),
                0L);
    }

    /** 读取大文件的压缩数据（分块解压） */
    private byte[] readLargeEntryData(CentralDirectoryModel entry, String source)
            throws IOException {
        long totalCompressed = entry.getCompressedSize();
        long totalUncompressed = entry.getUncompressedSize();
        long currentOffset = entry.getDataOffset();
        int methodCode = (int) entry.getCompressionMethod();
        GirCompressionType type = GirCompressionType.getByMethodCode(methodCode);
        //        log.debug("使用[{}]适配器处理解压，预期大小: {}字节", type.getText(),
        // entry.getUncompressedSize());

        // 2. 调用对应适配器的解压方法
        DecompressionHandler handler = type.getHandler();

        ByteArrayOutputStream out =
                new ByteArrayOutputStream((int) Math.min(totalUncompressed, Integer.MAX_VALUE));

        try {
            while (totalCompressed > 0) {
                int chunkSize = (int) Math.min(totalCompressed, MAX_CHUNK_SIZE);
                byte[] compressedChunk =
                        readRange(source, currentOffset, currentOffset + chunkSize - 1);
                byte[] decompress = handler.decompress(compressedChunk, chunkSize);
                if (decompress != null && decompress.length != 0) {
                    out.write(decompress);
                }
                currentOffset += chunkSize;
                totalCompressed -= chunkSize;
            }

            if (out.size() != totalUncompressed && totalUncompressed > 0) {
                log.warn("解压大小不匹配，预期:{}, 实际:{}", totalUncompressed, out.size());
            }
            return out.toByteArray();
        } finally {
            out.close();
        }
    }

    private static void byteToLocal(String localOutputPath, byte[] fileData) throws IOException {
        Files.createDirectories(Paths.get(localOutputPath).getParent());
        try (OutputStream out = new FileOutputStream(localOutputPath)) {
            out.write(fileData);
            log.debug("文件已写入本地：{}，大小：{}", localOutputPath, DataSizeUtil.format(fileData.length));
        } catch (IOException e) {
            log.error("写入本地文件失败：{}", localOutputPath, e);
            throw new IOException("写入本地文件失败：" + localOutputPath, e);
        }
    }

    // ------------------------------ 二进制读取工具（子类共享） ------------------------------

    protected int readInt(byte[] data, int offset) {
        if (offset + 4 > data.length) {
            throw new IndexOutOfBoundsException(
                    "读取int越界，offset:" + offset + ", length:" + data.length);
        }
        return ByteBuffer.wrap(data, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    protected short readShort(byte[] data, int offset) {
        if (offset + 2 > data.length) {
            throw new IndexOutOfBoundsException(
                    "读取short越界，offset:" + offset + ", length:" + data.length);
        }
        return ByteBuffer.wrap(data, offset, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
    }

    protected long readLong(byte[] data, int offset) {
        if (offset + 8 > data.length) {
            throw new IndexOutOfBoundsException(
                    "读取long越界，offset:" + offset + ", length:" + data.length);
        }
        return ByteBuffer.wrap(data, offset, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
    }

    /** 读取指定范围的字节数据（子类需根据存储类型实现：S3或本地文件） */
    protected abstract byte[] readRange(String source, long start, long end) throws IOException;

    /** 获取文件大小（子类实现） */
    @Override
    public abstract long getFileSize(String source);

    /** 判断路径是否为文件夹路径 */
    private boolean isFolderPath(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        return path.endsWith("/")
                || path.endsWith("\\")
                || path.endsWith(File.separator)
                || !path.contains(".") && !path.matches(".+\\.[a-zA-Z0-9]+$");
    }

    /** 标准化路径（支持文件夹） */
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
