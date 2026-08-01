package cn.geoair.web.mime.getter;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.web.mime.GiMimeType;
import cn.geoair.web.mime.IMimeTypeGetter;

import static cn.geoair.web.mime.GirImageMime.*;

public class ImageMimeGetter implements IMimeTypeGetter {


    private static GiLogger log = GirLoggerFactory.getLogger(ImageMimeGetter.class);


    @Override
    public GiMimeType checkForFormat(String formatStr) {
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
        } else if (tmpStr.equalsIgnoreCase("webp")) {
            return webp;
        }
        return null;
    }

    @Override
    public GiMimeType checkForExtension(String fileExtension) {
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
        } else if (fileExtension.equalsIgnoreCase("webp")) {
            return webp;
        }
        return null;
    }

}
