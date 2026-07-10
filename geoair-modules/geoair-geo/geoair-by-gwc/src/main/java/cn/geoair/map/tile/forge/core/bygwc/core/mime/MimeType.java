package cn.geoair.map.tile.forge.core.bygwc.core.mime;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.tile.forge.core.bygwc.io.Resource;
import cn.hutool.core.io.FileUtil;

import java.io.IOException;

/**
 * MIME类型类，用于表示和处理各种媒体类型的格式信息。
 * 包括MIME类型字符串、文件扩展名、格式名称等元数据。
 */
public class MimeType {
    /**
     * 日志记录器
     */
    private static GiLogger log = GirLoggerFactory.getLogger(MimeType.class);
    /**
     * MIME类型字符串，如 "image/png"
     */
    protected String mimeType;

    /**
     * 格式名称，可能与mimeType不同
     */
    protected String format;

    /**
     * 文件扩展名，如 ".png"
     */
    protected String fileExtension;

    /**
     * 内部名称，用于内部识别，如图像渲染器选择
     */
    protected String internalName;

    /**
     * 是否支持切片（瓦片化），无损栅格图像通常支持
     */
    protected boolean supportsTiling;



    /**
     * 构造方法，创建MIME类型实例
     *
     * @param mimeType       MIME类型字符串
     * @param fileExtension  文件扩展名
     * @param internalName   内部名称
     * @param format         格式名称
     * @param supportsTiling 是否支持切片
     */
    protected MimeType(
            String mimeType,
            String fileExtension,
            String internalName,
            String format,
            boolean supportsTiling) {
        this.mimeType = mimeType;
        this.fileExtension = fileExtension;
        this.internalName = internalName;
        this.format = format;
        this.supportsTiling = supportsTiling;
    }

    /**
     * 获取该格式的MIME标识符字符串
     *
     * @return MIME类型字符串
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * 根据资源获取MIME类型字符串
     * 子类可重写此方法以根据资源内容动态确定MIME类型
     *
     * @param resource 资源对象
     * @return MIME类型字符串
     * @throws IOException 读取资源时可能抛出的IO异常
     */
    public String getMimeType(Resource resource) throws IOException {
        return mimeType;
    }

    /**
     * 返回格式字符串，可能与MIME类型不同
     * 如果格式名称为空，则返回MIME类型作为备选
     *
     * @return 格式名称或MIME类型
     */
    public String getFormat() {
        if (format != null) {
            return format;
        }
        return mimeType;
    }

    /**
     * 获取该格式最常用的文件扩展名
     *
     * @return 文件扩展名
     */
    public String getFileExtension() {
        return fileExtension;
    }

    /**
     * 获取内部名称，用于内部目的，如选择图像渲染器
     *
     * @return 内部名称
     */
    public String getInternalName() {
        return internalName;
    }

    /**
     * 判断该格式是否支持切片（可分割为小块或组合为大块）
     * 实际上这意味着它必须是无损栅格图像格式
     *
     * @return true表示支持切片，false表示不支持
     */
    public boolean supportsTiling() {
        return supportsTiling;
    }

    /**
     * 判断是否为矢量格式而非栅格格式
     * 矢量格式的输出不应有边距（guttering）
     *
     * @return {@code true} 表示矢量或其他非栅格格式，
     * 这类格式应用边距会导致结果不正确
     */
    public boolean isVector() {
        return false;
    }

    /**
     * 根据格式字符串获取对应的MIME类型对象
     * 依次检查图像、XML、文本、应用程序MIME类型
     *
     * @param formatStr 格式字符串（可以是MIME类型或格式名称）
     * @return 对应的MimeType对象
     * @throws MimeException 如果格式字符串为空或不支持该格式
     */
    public static MimeType createFromFormat(String formatStr) throws MimeException {
        if (formatStr == null) {
            throw new MimeException("formatStr was not set");
        }
        // 检查是否为图像MIME类型
        MimeType mimeType = ImageMime.checkForFormat(formatStr);
        if (mimeType != null) {
            return mimeType;
        }
        // 检查是否为XML MIME类型
        mimeType = XMLMime.checkForFormat(formatStr);
        if (mimeType != null) {
            return mimeType;
        }
        // 检查是否为文本MIME类型
        mimeType = TextMime.checkForFormat(formatStr);
        if (mimeType != null) {
            return mimeType;
        }
        // 检查是否为应用程序MIME类型
        mimeType = ApplicationMime.checkForFormat(formatStr);
        if (mimeType != null) {
            return mimeType;
        }
        // 所有检查都未匹配，抛出异常
        return ApplicationMime.stream;
    }

    /**
     * 根据文件扩展名获取对应的MIME类型对象
     * 依次检查图像、XML、文本、应用程序MIME类型
     *
     * @param fileExtension 文件扩展名（如 ".png"）
     * @return 对应的MimeType对象，如果不支持则返回null
     */
    public static MimeType createFromExtension(String fileExtension) throws MimeException {
        // 检查是否为图像MIME类型
        MimeType mimeType = ImageMime.checkForExtension(fileExtension);
        if (mimeType != null) {
            return mimeType;
        }
        // 检查是否为XML MIME类型
        mimeType = XMLMime.checkForExtension(fileExtension);
        if (mimeType != null) {
            return mimeType;
        }
        // 检查是否为文本MIME类型
        mimeType = TextMime.checkForExtension(fileExtension);
        if (mimeType != null) {
            return mimeType;
        }
        // 检查是否为应用程序MIME类型
        mimeType = ApplicationMime.checkForExtension(fileExtension);
        if (mimeType != null) {
            return mimeType;
        }
        // 所有检查都未匹配，记录调试日志并返回null
//        log.debug("Unsupported MIME type: " + fileExtension + ", returning null");
        return ApplicationMime.stream;
    }


    public static MimeType createFromFileName(String fileName) throws MimeException {
        String suffix = FileUtil.getSuffix(fileName);
        return createFromExtension(suffix);
    }

    /**
     * 比较两个MimeType对象是否相等
     * 基于格式名称（忽略大小写）进行比较
     *
     * @param obj 要比较的对象
     * @return 如果格式名称相同则返回true，否则返回false
     */
    public boolean equals(Object obj) {
        if (obj != null && obj.getClass() == this.getClass()) {
            MimeType mimeObj = (MimeType) obj;
            if (this.format.equalsIgnoreCase(mimeObj.format)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 计算哈希值，基于格式名称
     *
     * @return 格式名称的哈希值
     */
    public int hashCode() {
        return format.hashCode();
    }

    /**
     * 判断otherMimeType是否与此MimeType兼容
     * 兼容条件：完全相同，或者otherMimeType以此MIME类型为前缀
     *
     * @param otherMimeType 要检查兼容性的MIME类型
     * @return 如果兼容则返回true，否则返回false
     */
    public boolean isCompatible(String otherMimeType) {
        return mimeType.equalsIgnoreCase(otherMimeType)
               || (otherMimeType != null
                   && otherMimeType.toLowerCase().startsWith(mimeType.toLowerCase()));
    }

    /**
     * 返回MIME类型字符串表示
     *
     * @return MIME类型字符串
     */
    @Override
    public String toString() {
        return mimeType;
    }
}
