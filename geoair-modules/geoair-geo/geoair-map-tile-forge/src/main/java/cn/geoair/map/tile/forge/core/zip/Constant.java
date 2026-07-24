package cn.geoair.map.tile.forge.core.zip;

/**
 * @author ：张俊 &#064;date ：Created in 2025/11/13 17:19 &#064;description： TODO
 */
public interface Constant {
    /** 通用IO缓冲区大小（8KB） 用于文件流读写时的缓冲区，平衡内存占用和IO效率，8KB是磁盘IO的常见块大小适配值 */
    static final int BUFFER_SIZE = 8192;

    /** ZIP文件EOCD（中央目录结束记录）的签名标识（0x06054b50） ZIP协议规定的EOCD起始标记，用于在文件末尾定位EOCD结构的起始位置 */
    static final int EOCD_SIGNATURE = 0x06054b50;

    /** ZIP文件中央目录条目的签名标识（0x02014b50） 每个中央目录条目（对应一个文件的元数据）都以此签名开头，用于解析中央目录时识别条目边界 */
    static final int CENTRAL_DIR_SIGNATURE = 0x02014b50;

    /** ZIP文件本地文件头的签名标识（0x04034b50） 每个文件的本地文件头（存储文件名、压缩方法等基础信息）以此签名开头，用于定位文件数据的起始位置 */
    static final int LOCAL_FILE_HEADER_SIGNATURE = 0x04034b50;

    /** ZIP64格式中定位器（Locator）的签名标识（0x07064b50） 用于在ZIP64文件中定位ZIP64 EOCD的位置，仅在文件大小超过4GB时出现 */
    static final int ZIP64_LOCATOR_SIGNATURE = 0x07064b50;

    /** ZIP64格式中EOCD（扩展中央目录结束记录）的签名标识（0x06064b50） ZIP64格式的扩展EOCD结构，支持64位偏移量和大小字段，用于处理超过4GB的大文件 */
    static final int ZIP64_EOCD_SIGNATURE = 0x06064b50;

    /** 标准EOCD结构的基础大小（22字节） 不包含注释的情况下，EOCD的固定长度为22字节，用于计算EOCD在文件末尾的搜索范围 */
    static final int EOCD_BASE_SIZE = 22;

    /** 最大分块大小（5MB） 处理大文件时的分片上限，避免单次读取/解压操作占用过多内存，同时减少S3请求次数 */
    static final int MAX_CHUNK_SIZE = 5 * 1024 * 1024; // 5MB分片上限
}
