package cn.geoair.web.mime;

/** MIME类型类，用于表示和处理各种媒体类型的格式信息。 包括MIME类型字符串、文件扩展名、格式名称等元数据。 */
public interface IMimeTypeGetter {

    GiMimeType checkForExtension(String fileExtension);

    GiMimeType checkForFormat(String formatStr);
}
