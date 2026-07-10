package cn.geoair.web.mime;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;

public class GirImageMime extends BaseMimeType implements IMimeTypeGetter {


    private static GiLogger log = GirLoggerFactory.getLogger(GirImageMime.class);


    public static final GirImageMime png =
            new GirImageMime("image/png", "png", "png", "image/png") {
                public boolean isCompatible(String otherMimeType) {
                    return super.isCompatible(otherMimeType)
                           || otherMimeType.startsWith("image/png");
                }
            };

    public static final GirImageMime jpeg =
            new GirImageMime("image/jpeg", "jpeg", "jpeg", "image/jpeg") {


            };

    public static final GirImageMime gif =
            new GirImageMime("image/gif", "gif", "gif", "image/gif");

    public static final GirImageMime tiff =
            new GirImageMime("image/tiff", "tiff", "tiff", "image/tiff");

    public static final GirImageMime png8 =
            new GirImageMime("image/png", "png8", "png", "image/png8") {


            };

    public static final GirImageMime png24 =
            new GirImageMime("image/png", "png24", "png", "image/png24");

    public static final GirImageMime png_24 =
            new GirImageMime(
                    "image/png; mode=24bit",
                    "png_24",
                    "png",
                    "image/png;%20mode=24bit");

    public static final GirImageMime dds =
            new GirImageMime("image/dds", "dds", "dds", "image/dds");

    public static final GirImageMime jpegPng =
            new GirImageMime(
                    "image/vnd.jpeg-png", "jpeg-png", "jpeg-png", "image/vnd.jpeg-png");

    public static final GirImageMime jpegPng8 =
            new GirImageMime(
                    "image/vnd.jpeg-png8",
                    "jpeg-png8",
                    "jpeg-png8",
                    "image/vnd.jpeg-png8"
            );

    private GirImageMime(
            String mimeType,
            String fileExtension,
            String internalName,
            String format) {
        super(mimeType, fileExtension, internalName, format);
    }

    @Override
    public GirImageMime checkForFormat(String formatStr) {
        if (!formatStr.startsWith("image/")) {
            return null;
        }

        // TODO Making a special exception, generalize later
        if (!formatStr.equals("image/png; mode=24bit") && formatStr.contains(";")) {
            if (log.isDebugEnabled()) {
                log.debug("Slicing off " + formatStr.split(";")[1]);
            }
            formatStr = formatStr.split(";")[0];
        }

        final String tmpStr = formatStr.substring(6);
        if (tmpStr.equalsIgnoreCase("png")) {
            return png;
        } else if (tmpStr.equalsIgnoreCase("jpeg")) {
            return jpeg;
        } else if (tmpStr.equalsIgnoreCase("gif")) {
            return gif;
        } else if (tmpStr.equalsIgnoreCase("tiff")) {
            return tiff;
        } else if (tmpStr.equalsIgnoreCase("png8")) {
            return png8;
        } else if (tmpStr.equalsIgnoreCase("png24")) {
            return png24;
        } else if (tmpStr.equalsIgnoreCase("png; mode=24bit")) {
            return png_24;
        } else if (tmpStr.equalsIgnoreCase("png;%20mode=24bit")) {
            return png_24;
        } else if (tmpStr.equalsIgnoreCase("vnd.jpeg-png")) {
            return jpegPng;
        } else if (tmpStr.equalsIgnoreCase("vnd.jpeg-png8")) {
            return jpegPng8;
        }
        return null;
    }

    @Override
    public GirImageMime checkForExtension(String fileExtension) {
        if (fileExtension.equalsIgnoreCase("png")) {
            return png;
        } else if (fileExtension.equalsIgnoreCase("jpeg")
                   || fileExtension.equalsIgnoreCase("jpg")) {
            return jpeg;
        } else if (fileExtension.equalsIgnoreCase("gif")) {
            return gif;
        } else if (fileExtension.equalsIgnoreCase("tiff")) {
            return tiff;
        } else if (fileExtension.equalsIgnoreCase("png8")) {
            return png8;
        } else if (fileExtension.equalsIgnoreCase("png24")) {
            return png24;
        } else if (fileExtension.equalsIgnoreCase("png_24")) {
            return png_24;
        } else if (fileExtension.equalsIgnoreCase("jpeg-png")) {
            return jpegPng;
        } else if (fileExtension.equalsIgnoreCase("jpeg-png8")) {
            return jpegPng8;
        }
        return null;
    }

}
