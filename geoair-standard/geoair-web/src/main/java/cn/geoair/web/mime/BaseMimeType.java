package cn.geoair.web.mime;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;

/** MIME类型类，用于表示和处理各种媒体类型的格式信息。 包括MIME类型字符串、文件扩展名、格式名称等元数据。 */
public abstract class BaseMimeType implements GiMimeType {
    /** 日志记录器 */
    private static GiLogger log = GirLoggerFactory.getLogger(BaseMimeType.class);
    /** MIME类型字符串，如 "image/png" */
    protected String mimeType;

    /** 格式名称，可能与mimeType不同 */
    protected String format;

    /** 文件扩展名，如 ".png" */
    protected String fileExtension;

    /** 内部名称，用于内部识别，如图像渲染器选择 */
    protected String internalName;

    /**
     * 构造方法，创建MIME类型实例
     *
     * @param mimeType MIME类型字符串
     * @param fileExtension 文件扩展名
     * @param internalName 内部名称
     * @param format 格式名称
     */
    protected BaseMimeType(
            String mimeType, String fileExtension, String internalName, String format) {
        this.mimeType = mimeType;
        this.fileExtension = fileExtension;
        this.internalName = internalName;
        this.format = format;
    }

    /**
     * 获取该格式的MIME标识符字符串
     *
     * @return MIME类型字符串
     */
    @Override
    public String getMimeType() {
        return mimeType;
    }

    /**
     * 返回格式字符串，可能与MIME类型不同 如果格式名称为空，则返回MIME类型作为备选
     *
     * @return 格式名称或MIME类型
     */
    @Override
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
    @Override
    public String getFileExtension() {
        return fileExtension;
    }

    /**
     * 获取内部名称，用于内部目的，如选择图像渲染器
     *
     * @return 内部名称
     */
    @Override
    public String getInternalName() {
        return internalName;
    }

    /**
     * 比较两个MimeType对象是否相等 基于格式名称（忽略大小写）进行比较
     *
     * @param obj 要比较的对象
     * @return 如果格式名称相同则返回true，否则返回false
     */
    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj.getClass() == this.getClass()) {
            BaseMimeType mimeObj = (BaseMimeType) obj;
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
    @Override
    public int hashCode() {
        return format.hashCode();
    }

    /**
     * 判断otherMimeType是否与此MimeType兼容 兼容条件：完全相同，或者otherMimeType以此MIME类型为前缀
     *
     * @param otherMimeType 要检查兼容性的MIME类型
     * @return 如果兼容则返回true，否则返回false
     */
    @Override
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
