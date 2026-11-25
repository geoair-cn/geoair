package cn.geoair.map.tile.forge.core.bygwc.compact;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * ArcGIS紧凑缓存的抽象基类。
 *
 * @author Bjoern Saxe
 */
public abstract class ArcGISCompactCache {
    /** 日志记录器 */
    private static final GiLogger logger = GirLoggerFactory.getLogger(ArcGISCompactCache.class);

    /** bundlx文件扩展名 */
    public static final String BUNDLX_EXT = ".bundlx";

    /** bundle文件扩展名 */
    public static final String BUNDLE_EXT = ".bundle";

    /** bundlx文件中的最大索引数 */
    protected static final int BUNDLX_MAXIDX = 128;

    /** 缓存根路径 */
    protected String pathToCacheRoot = "";

    /**
     * 获取瓦片的资源对象。
     *
     * @param zoom 瓦片的缩放级别
     * @param row 瓦片的行号
     * @param col 瓦片的列号
     * @return 如果瓦片存在，则返回与瓦片图像数据关联的资源对象；否则返回null
     */
    public abstract BundleFileResource getBundleFileResource(int zoom, int row, int col);

    /**
     * 根据缩放级别、列号和行号构建不带文件扩展名的bundle路径。
     *
     * @param zoom 缩放级别
     * @param row 行号
     * @param col 列号
     * @return 不带文件扩展名的完整路径字符串，格式为.../Lzz/RrrrrCcccc，其中c和r至少有4个字符
     */
    public String buildBundleFilePath(int zoom, int row, int col) {
        StringBuilder bundlePath = new StringBuilder(pathToCacheRoot);

        // 计算基础行号和列号
        int baseRow = (row / BUNDLX_MAXIDX) * BUNDLX_MAXIDX;
        int baseCol = (col / BUNDLX_MAXIDX) * BUNDLX_MAXIDX;

        // 格式化缩放级别字符串，确保至少两位数
        String zoomStr = Integer.toString(zoom);
        if (zoomStr.length() < 2) zoomStr = "0" + zoomStr;

        // 将基础行号和列号转换为十六进制字符串
        StringBuilder rowStr = new StringBuilder(Integer.toHexString(baseRow));
        StringBuilder colStr = new StringBuilder(Integer.toHexString(baseCol));

        // 列号和行号至少需要4个字符长度
        final int padding = 4;

        // 对列号进行零填充
        while (colStr.length() < padding) colStr.insert(0, "0");

        // 对行号进行零填充
        while (rowStr.length() < padding) rowStr.insert(0, "0");

        // 构建完整的bundle文件路径
        bundlePath
                .append("L")
                .append(zoomStr)
                .append(File.separatorChar)
                .append("R")
                .append(rowStr)
                .append("C")
                .append(colStr);

        return bundlePath.toString();
    }

    /**
     * 从使用小端字节序的文件中读取数据。
     *
     * @param filePath 文件路径
     * @param offset 读取偏移量
     * @param length 读取字节数
     * @return 包含读取字节的ByteBuffer，并设置为小端字节序。字节缓冲区的长度是4的倍数， 因此即使读取的字节数较少也能使用getInt()和getLong()方法
     */
    protected ByteBuffer readFromLittleEndianFile(String filePath, long offset, int length) {
        ByteBuffer result = null;

        try (RandomAccessFile file = new RandomAccessFile(filePath, "r")) {
            // 定位到指定偏移量
            file.seek(offset);

            // 填充到4的倍数以便可以使用getInt()和getLong()
            int padding = 4 - (length % 4);
            byte[] data = new byte[length + padding];

            // 从文件中读取指定长度的数据
            if (file.read(data, 0, length) != length) throw new IOException("读取的字节数不足或已到达文件末尾");

            // 创建小端字节序的ByteBuffer
            result = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        } catch (IOException e) {
            logger.warn("从小端字节序文件读取数据失败", e);
        }

        return result;
    }
}
