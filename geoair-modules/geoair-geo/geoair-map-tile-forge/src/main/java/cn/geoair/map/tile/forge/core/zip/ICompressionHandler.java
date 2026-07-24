package cn.geoair.map.tile.forge.core.zip;

import cn.geoair.map.tile.forge.core.zip.model.CentralDirectoryModel;
import cn.geoair.map.tile.forge.core.zip.model.EocdInfo;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** 压缩文件处理器接口 */
public interface ICompressionHandler {

    /**
     * 从ZIP中读取指定文件并写入本地
     *
     * @param zipSource ZIP源（本地路径或S3键名，由实现类解析）
     * @param targetFilePathInZip ZIP内部目标文件路径（如"a/b/c.png"）
     * @param localOutputPath 本地输出路径
     * @throws IOException 处理失败时抛出
     */
    void readFileFromZipToLocal(
            String zipSource, String targetFilePathInZip, String localOutputPath)
            throws IOException;

    /**
     * 从ZIP中读取指定文件的字节数据
     *
     * @param zipSource ZIP源（本地路径或S3键名）
     * @param targetFilePathInZip ZIP内部目标文件路径
     * @return 解压后的文件字节数组
     * @throws IOException 处理失败时抛出
     */
    byte[] readFileFromZip(String zipSource, String targetFilePathInZip) throws IOException;

    /**
     * 分块读取文件内容（适用于大文件）
     *
     * @param source 源文件（本地路径或S3键名）
     * @param startOffset 起始偏移量（字节）
     * @param totalSize 总读取大小（字节）
     * @param chunkSize 每块大小（字节）
     * @return 分块数据列表
     * @throws IOException 读取失败时抛出
     */
    List<byte[]> readFileByChunks(String source, long startOffset, long totalSize, int chunkSize)
            throws IOException;

    /**
     * 异步分块读取文件内容
     *
     * @param source 源文件（本地路径或S3键名）
     * @param startOffset 起始偏移量
     * @param totalSize 总大小
     * @param chunkSize 分块大小
     * @return 异步结果
     */
    CompletableFuture<List<byte[]>> asyncReadFileByChunks(
            String source, long startOffset, long totalSize, int chunkSize);

    /**
     * 获取文件大小（字节）
     *
     * @param source 源文件（本地路径或S3键名）
     * @return 文件大小
     */
    long getFileSize(String source);

    /**
     * 解析ZIP的EOCD（中央目录结束记录）
     *
     * @param fileSize 文件总大小
     * @param source ZIP源（本地路径或S3键名）
     * @return EOCD信息
     * @throws IOException 解析失败时抛出
     */
    EocdInfo parseEocd(long fileSize, String source) throws IOException;

    /**
     * 在ZIP中央目录中查找目标文件
     *
     * @param eocd EOCD信息
     * @param targetPath ZIP内部目标路径
     * @param source ZIP源
     * @return 中央目录条目
     * @throws IOException 查找失败时抛出
     */
    CentralDirectoryModel findEntryInCentralDir(EocdInfo eocd, String targetPath, String source)
            throws IOException;

    /**
     * 读取并解压ZIP中的指定条目
     *
     * @param entry 中央目录条目
     * @param source ZIP源
     * @return 解压后的字节数据
     * @throws IOException 处理失败时抛出
     */
    byte[] readAndDecompressEntry(CentralDirectoryModel entry, String source) throws IOException;

    /**
     * 读取并解压ZIP中的指定条目到本地目录
     *
     * @param entry 中央目录条目
     * @param source ZIP源
     * @param localOutputPath 本地目录
     * @return 解压后的字节数据
     * @throws IOException 处理失败时抛出
     */
    void readAndDecompressEntryToLocal(
            CentralDirectoryModel entry, String source, String localOutputPath) throws IOException;

    /**
     * 检查下面的路径是否存在于ZIP中
     *
     * @param zipSource ZIP源
     * @param checkedPaths 需要检查的路径
     * @return 存在的路径列表
     * @throws IOException
     */
    List<String> checkedPathsInZip(String zipSource, List<String> checkedPaths) throws IOException;

    /**
     * 扫描ZIP中的所有条目
     *
     * @param eocd EOCD信息，包含中央目录的位置和大小信息
     * @param source ZIP源文件路径（本地路径或S3键名）
     * @param entryConsumer 条目消费者，用于处理每个扫描到的中央目录条目
     * @throws IOException 扫描过程中发生IO异常时抛出
     */
    void scanAllEntries(
            EocdInfo eocd, String source, TerminatingConsumer<CentralDirectoryModel> entryConsumer)
            throws IOException;

    void scanAllEntries(String source, TerminatingConsumer<CentralDirectoryModel> entryConsumer)
            throws IOException;
}
