package cn.geoair.web.mime;

import cn.geoair.base.sp.annotation.GkSP;
import cn.geoair.base.sp.support.GirJdkSpLoader;

/** MIME类型类，用于表示和处理各种媒体类型的格式信息。 包括MIME类型字符串、文件扩展名、格式名称等元数据。 */
@GkSP(loader = GirJdkSpLoader.class)
public interface IMimeTypeGetter {

    GiMimeType checkForExtension(String fileExtension);

    GiMimeType checkForFormat(String formatStr);
}
