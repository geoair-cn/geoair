package cn.geoair.map.tile.forge.core.zip;

import cn.geoair.map.tile.forge.core.zip.model.CentralDirectoryModel;
import cn.geoair.map.tile.forge.core.zip.model.EocdInfo;

import java.io.IOException;
import java.util.List;

/**
 * 压缩文件处理器接口
 */
public interface ICompressionHandler {

    /**
     * 从ZIP中读取指定文件并写入本地
     *
     * @param zipSource           ZIP源（本地路径或S3键名，由实现类解析）
     * @param targetFilePathInZip ZIP内部目标文件路径（如"a/b/c.png"）
     * @param localOutputPath     本地输出路径
     * @throws IOException 处理失败时抛出
     */
    void readFileFromZipToLocal(String zipSource, String targetFilePathInZip, String localOutputPath) throws IOException;

    /**
     * 从ZIP中读取指定文件的字节数据
     *
     * @param zipSource           ZIP源（本地路径或S3键名）
     * @param targetFilePathInZip ZIP内部目标文件路径
     * @return 解压后的文件字节数组
     * @throws IOException 处理失败时抛出
     */
    byte[] readFileFromZip(String zipSource, String targetFilePathInZip) throws IOException;

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
     * @param source   ZIP源（本地路径或S3键名）
     * @return EOCD信息
     * @throws IOException 解析失败时抛出
     */
    EocdInfo parseEocd(long fileSize, String source) throws IOException;

    /**
     * 在ZIP中央目录中查找目标文件
     *
     * @param eocd       EOCD信息
     * @param targetPath ZIP内部目标路径
     * @param source     ZIP源
     * @return 中央目录条目
     * @throws IOException 查找失败时抛出
     */
    CentralDirectoryModel findEntryInCentralDir(EocdInfo eocd, String targetPath, String source) throws IOException;

    /**
     * 读取并解压ZIP中的指定条目
     *
     * @param entry  中央目录条目
     * @param source ZIP源
     * @return 解压后的字节数据
     * @throws IOException 处理失败时抛出
     */
    byte[] readAndDecompressEntry(CentralDirectoryModel entry, String source) throws IOException;

    /**
     * 读取并解压ZIP中的指定条目到本地目录
     *
     * @param entry           中央目录条目
     * @param source          ZIP源
     * @param localOutputPath 本地目录
     * @return 解压后的字节数据
     * @throws IOException 处理失败时抛出
     */
    void readAndDecompressEntryToLocal(CentralDirectoryModel entry, String source, String localOutputPath) throws IOException;

    /**
     * 检查下面的路径是否存在于ZIP中
     *
     * @param zipSource    ZIP源
     * @param checkedPaths 需要检查的路径
     * @return 存在的路径列表
     * @throws IOException
     */
    List<String> checkedPathsInZip(String zipSource, List<String> checkedPaths) throws IOException;


    /**
     * 扫描ZIP中的所有条目
     *
     * @param eocd          EOCD信息，包含中央目录的位置和大小信息
     * @param source        ZIP源文件路径（本地路径或S3键名）
     * @param entryConsumer 条目消费者，用于处理每个扫描到的中央目录条目
     * @throws IOException 扫描过程中发生IO异常时抛出
     */
    void scanAllEntries(EocdInfo eocd, String source, TerminatingConsumer<CentralDirectoryModel> entryConsumer) throws IOException;

    void scanAllEntries(String source, TerminatingConsumer<CentralDirectoryModel> entryConsumer) throws IOException;

    // ---------------------------- 带进度回调的重载 ----------------------------
    // 下面几个重载只是为了把进度消费者放进 ReadProgressScope，实现类的读取逻辑不用改。
    // 传 null 等价于不上报，和调用不带 ProgressConsumer 的版本完全一致。
    // 需要在批量任务或排障时观察进度，传 new LogProgressConsumer() 即可。

    /**
     * 从ZIP中读取指定文件并写入本地，过程中上报读取进度
     *
     * @param progressConsumer 进度消费者，可为 null
     */
    default void readFileFromZipToLocal(String zipSource, String targetFilePathInZip, String localOutputPath,
                                        ProgressConsumer progressConsumer) throws IOException {
        ProgressConsumer previous = ReadProgressScope.begin(progressConsumer);
        try {
            readFileFromZipToLocal(zipSource, targetFilePathInZip, localOutputPath);
        } finally {
            ReadProgressScope.end(previous);
        }
    }

    /**
     * 从ZIP中读取指定文件的字节数据，过程中上报读取进度
     *
     * @param progressConsumer 进度消费者，可为 null
     */
    default byte[] readFileFromZip(String zipSource, String targetFilePathInZip,
                                   ProgressConsumer progressConsumer) throws IOException {
        ProgressConsumer previous = ReadProgressScope.begin(progressConsumer);
        try {
            return readFileFromZip(zipSource, targetFilePathInZip);
        } finally {
            ReadProgressScope.end(previous);
        }
    }

    /**
     * 读取并解压ZIP中的指定条目，过程中上报读取进度
     *
     * @param progressConsumer 进度消费者，可为 null
     */
    default byte[] readAndDecompressEntry(CentralDirectoryModel entry, String source,
                                          ProgressConsumer progressConsumer) throws IOException {
        ProgressConsumer previous = ReadProgressScope.begin(progressConsumer);
        try {
            return readAndDecompressEntry(entry, source);
        } finally {
            ReadProgressScope.end(previous);
        }
    }

    /**
     * 读取并解压ZIP中的指定条目到本地目录，过程中上报读取进度
     *
     * @param progressConsumer 进度消费者，可为 null
     */
    default void readAndDecompressEntryToLocal(CentralDirectoryModel entry, String source, String localOutputPath,
                                               ProgressConsumer progressConsumer) throws IOException {
        ProgressConsumer previous = ReadProgressScope.begin(progressConsumer);
        try {
            readAndDecompressEntryToLocal(entry, source, localOutputPath);
        } finally {
            ReadProgressScope.end(previous);
        }
    }
}
