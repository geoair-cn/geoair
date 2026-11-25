package cn.geoair.web.mime;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;

public class GirImageMime extends BaseMimeType {

    private static GiLogger log = GirLoggerFactory.getLogger(GirImageMime.class);

    public static final GirImageMime png =
            new GirImageMime("image/png", "png", "png", "image/png") {
                public boolean isCompatible(String otherMimeType) {
                    return super.isCompatible(otherMimeType)
                            || otherMimeType.startsWith("image/png");
                }
            };

    public static final GirImageMime jpeg =
            new GirImageMime("image/jpeg", "jpeg", "jpeg", "image/jpeg") {};

    public static final GirImageMime webp =
            new GirImageMime("image/webp", "webp", "webp", "image/webp") {};

    public static final GirImageMime gif = new GirImageMime("image/gif", "gif", "gif", "image/gif");

    public static final GirImageMime tiff =
            new GirImageMime("image/tiff", "tiff", "tiff", "image/tiff");

    public static final GirImageMime png8 =
            new GirImageMime("image/png", "png8", "png", "image/png8") {};

    public static final GirImageMime png24 =
            new GirImageMime("image/png", "png24", "png", "image/png24");

    public static final GirImageMime png_24 =
            new GirImageMime("image/png; mode=24bit", "png_24", "png", "image/png;%20mode=24bit");

    public static final GirImageMime dds = new GirImageMime("image/dds", "dds", "dds", "image/dds");

    public static final GirImageMime jpegPng =
            new GirImageMime("image/vnd.jpeg-png", "jpeg-png", "jpeg-png", "image/vnd.jpeg-png");

    public static final GirImageMime jpegPng8 =
            new GirImageMime(
                    "image/vnd.jpeg-png8", "jpeg-png8", "jpeg-png8", "image/vnd.jpeg-png8");

    private GirImageMime(
            String mimeType, String fileExtension, String internalName, String format) {
        super(mimeType, fileExtension, internalName, format);
    }
}
