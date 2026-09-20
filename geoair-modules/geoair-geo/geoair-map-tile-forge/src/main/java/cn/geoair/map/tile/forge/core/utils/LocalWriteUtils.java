package cn.geoair.map.tile.forge.core.utils;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 本地落盘工具
 * <p>
 * 瓦片、bundle 这类文件此前是直接往目标路径写的，而 FileOutputStream 一构造，
 * 目标路径就已经存在一个 0 字节的文件。并发的读请求会把它当成「已就绪的文件」，
 * 于是拿到半截数据；抽取过程中一旦出错，这份坏文件还会长期占着位置，
 * 之后所有落到同一个文件的请求全部读到坏数据。
 * <p>
 * 这里的做法是统一先写到临时文件，写完之后一次性替换成目标文件：
 * 要么目标文件不存在，要么就是完整内容，不存在中间状态。
 *
 * @author 张俊
 */
public final class LocalWriteUtils {

    private static GiLogger log = GirLoggerFactory.getLogger();

    /**
     * 临时文件后缀，与目标文件同目录，替换完成后会被删掉
     */
    private static final String TEMP_WRITE_SUFFIX = ".geoair-tmp";

    private LocalWriteUtils() {
    }

    /**
     * 取目标文件对应的临时文件
     *
     * @param targetFile 最终要落到的文件
     * @return 同目录下的临时文件
     */
    public static File tempFileOf(File targetFile) {
        return new File(targetFile.getAbsolutePath() + TEMP_WRITE_SUFFIX);
    }

    /**
     * 把临时文件替换成目标文件，尽量走原子移动，文件系统不支持时退化为普通替换
     *
     * @param source 已经写完的临时文件
     * @param target 目标文件
     * @throws IOException 替换失败
     */
    public static void replaceAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            // 个别文件系统不支持原子移动，退化为普通替换，至少不会再出现半截文件
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * 静默删除已存在的临时文件，失败只记日志，不影响主流程
     *
     * @param path 待删除的路径
     */
    public static void deleteIfExistsQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("删除临时文件失败：{}", path, e);
        }
    }
}
