package cn.geoair.web.util;

import cn.geoair.web.mime.*;

import java.util.List;

/**
 * @author ：张俊
 * @date ：Created in 2026/7/10 12:08
 * @description： TODO
 */
public class GutilMimeType {

    /**
     * 根据格式字符串获取对应的MIME类型对象
     * 依次检查图像、XML、文本、应用程序MIME类型
     *
     * @param formatStr 格式字符串（可以是MIME类型或格式名称）
     * @return 对应的MimeType对象
     */
    public static GiMimeType createFromFormat(String formatStr) throws MimeException {
        if (formatStr == null) {
            throw new MimeException("formatStr was not set");
        }
        List<IMimeTypeGetter> mimeTypeGetters = SpiMimeLoader.getMimeTypeGetters();
        for (IMimeTypeGetter mimeTypeGetter : mimeTypeGetters) {
            GiMimeType giMimeType = mimeTypeGetter.checkForFormat(formatStr);
            if (giMimeType != null) {
                return giMimeType;
            }
        }
        // 所有检查都未匹配，抛出异常
        return GirApplicationMime.stream;
    }

    /**
     * 根据文件扩展名获取对应的MIME类型对象
     * 依次检查图像、XML、文本、应用程序MIME类型
     *
     * @param fileExtension 文件扩展名（如 ".png"）
     * @return 对应的MimeType对象，如果不支持则返回null
     */
    public static GiMimeType createFromExtension(String fileExtension) throws MimeException {
        List<IMimeTypeGetter> mimeTypeGetters = SpiMimeLoader.getMimeTypeGetters();
        for (IMimeTypeGetter mimeTypeGetter : mimeTypeGetters) {
            GiMimeType giMimeType = mimeTypeGetter.checkForExtension(fileExtension);
            if (giMimeType != null) {
                return giMimeType;
            }
        }
        return GirApplicationMime.stream;
    }


    public static void main(String[] args) {
        GiMimeType png = createFromExtension("png");
        System.out.println(png.toString());
    }

}
