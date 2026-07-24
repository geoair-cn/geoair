package cn.geoair.web.mime;

/** MIME类型类，用于表示和处理各种媒体类型的格式信息。 包括MIME类型字符串、文件扩展名、格式名称等元数据。 */
public interface GiMimeType {

    /**
     * 获取该格式的MIME标识符字符串
     *
     * @return MIME类型字符串
     */
    String getMimeType();

    /**
     * 返回格式字符串，可能与MIME类型不同 如果格式名称为空，则返回MIME类型作为备选
     *
     * @return 格式名称或MIME类型
     */
    String getFormat();

    /**
     * 获取该格式最常用的文件扩展名
     *
     * @return 文件扩展名
     */
    String getFileExtension();

    /**
     * 获取内部名称，用于内部目的，如选择图像渲染器
     *
     * @return 内部名称
     */
    String getInternalName();

    /**
     * 比较两个MimeType对象是否相等 基于格式名称（忽略大小写）进行比较
     *
     * @param obj 要比较的对象
     * @return 如果格式名称相同则返回true，否则返回false
     */
    boolean equals(Object obj);

    /**
     * 计算哈希值，基于格式名称
     *
     * @return 格式名称的哈希值
     */
    int hashCode();

    /**
     * 判断otherMimeType是否与此MimeType兼容 兼容条件：完全相同，或者otherMimeType以此MIME类型为前缀
     *
     * @param otherMimeType 要检查兼容性的MIME类型
     * @return 如果兼容则返回true，否则返回false
     */
    boolean isCompatible(String otherMimeType);
}
