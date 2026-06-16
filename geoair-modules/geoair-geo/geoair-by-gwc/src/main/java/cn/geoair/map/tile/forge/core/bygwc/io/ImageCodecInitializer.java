package cn.geoair.map.tile.forge.core.bygwc.io;



import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.tile.forge.core.bygwc.io.codec.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 图片编码器/解码器初始化工具类
 * 模拟 GWC 中的编码器/解码器配置
 */
public class ImageCodecInitializer {

    static GiLogger logger = GirLoggerFactory.getLogger();

    private static Map<String, ImageEncoder> encoders = new HashMap<>();
    private static Map<String, ImageDecoder> decoders = new HashMap<>();


    static {
        initialize();
    }

    public static void initialize() {


        // 初始化编码器
        initEncoders();

        // 初始化解码器
        initDecoders();

        logger.info("=== 编码器初始化完成 ===");
        encoders.forEach((name, encoder) ->
                logger.info("  - " + name + ": " + encoder.getClass().getSimpleName())
        );

        logger.info("\n=== 解码器初始化完成 ===");
        decoders.forEach((name, decoder) ->
                logger.info("  - " + name + ": " + decoder.getClass().getSimpleName())
        );
    }

    private static void initEncoders() {
        // PNG 编码器
        List<String> pngWriterSpis = Arrays.asList(
                "com.sun.media.imageioimpl.plugins.png.CLibPNGImageWriterSpi",
                "com.sun.imageio.plugins.png.PNGImageWriterSpi"
        );
        Map<String, String> pngParams = new HashMap<>();
        pngParams.put("COMPRESSION", "FILTERED");
        pngParams.put("COMPRESSION_RATE", "0.75");

        PNGImageEncoder pngEncoder = new PNGImageEncoder(
                false,           // isMultiImage
                0.25f,
                pngParams,// pngParams
                false
        );
        encoders.put("image/png", pngEncoder);
        encoders.put("image/png; mode=24bit", pngEncoder);

        // GIF 编码器
        List<String> gifMimeTypes = Arrays.asList("image/gif");
        List<String> gifWriterSpis = Arrays.asList(
                "com.sun.media.imageioimpl.plugins.gif.GIFImageWriterSpi",
                "com.sun.media.imageio.plugins.gif.GIFImageWriterSpi"
        );
        Map<String, String> gifParams = new HashMap<>();
        gifParams.put("COMPRESSION", "NULL");
        gifParams.put("COMPRESSION_RATE", "NULL");

        ImageEncoderImpl gifEncoder = new ImageEncoderImpl(
                true,            // isMultiImage
                gifMimeTypes,    // mimeTypes

                gifParams
        );
        encoders.put("image/gif", gifEncoder);

        // JPEG 编码器
        List<String> jpegMimeTypes = Arrays.asList("image/jpeg");
        List<String> jpegWriterSpis = Arrays.asList(
                "com.sun.media.imageioimpl.plugins.jpeg.CLibJPEGImageWriterSpi",
                "com.sun.imageio.plugins.jpeg.JPEGImageWriterSpi"
        );
        Map<String, String> jpegParams = new HashMap<>();
        jpegParams.put("COMPRESSION", "JPEG");
        jpegParams.put("COMPRESSION_RATE", "0.75");

        ImageEncoderImpl jpegEncoder = new ImageEncoderImpl(
                true,            // isMultiImage
                jpegMimeTypes,   // mimeTypes

                jpegParams
        );
        encoders.put("image/jpeg", jpegEncoder);

        // TIFF 编码器
        List<String> tiffMimeTypes = Arrays.asList("image/tiff");
        List<String> tiffWriterSpis = Arrays.asList(
                "it.geosolutions.imageioimpl.plugins.tiff.TIFFImageWriterSpi",
                "com.sun.media.imageioimpl.plugins.tiff.TIFFImageWriterSpi"
        );
        Map<String, String> tiffParams = new HashMap<>();
        tiffParams.put("COMPRESSION", "Deflate");
        tiffParams.put("COMPRESSION_RATE", "0.75");

        ImageEncoderImpl tiffEncoder = new ImageEncoderImpl(
                false,           // isMultiImage
                tiffMimeTypes,   // mimeTypes

                tiffParams
        );
        encoders.put("image/tiff", tiffEncoder);

        // BMP 编码器
        List<String> bmpMimeTypes = Arrays.asList("image/bmp");
        List<String> bmpWriterSpis = Arrays.asList(
                "com.sun.media.imageioimpl.plugins.bmp.BMPImageWriterSpi",
                "com.sun.imageio.plugins.bmp.BMPImageWriterSpi"
        );
        Map<String, String> bmpParams = new HashMap<>();
        bmpParams.put("COMPRESSION", "NULL");
        bmpParams.put("COMPRESSION_RATE", "NULL");

        ImageEncoderImpl bmpEncoder = new ImageEncoderImpl(
                true,            // isMultiImage
                bmpMimeTypes,    // mimeTypes

                bmpParams
        );
        encoders.put("image/bmp", bmpEncoder);
    }

    private static void initDecoders() {
        // PNG 解码器
        List<String> pngMimeTypes = Arrays.asList("image/png", "image/png; mode=24bit");
        ImageDecoderImpl pngDecoder = new ImageDecoderImpl(
                false,           // isMultiImage
                pngMimeTypes   // mimeTypes
        );
        decoders.put("image/png", pngDecoder);
        decoders.put("image/png; mode=24bit", pngDecoder);

        // GIF 解码器
        List<String> gifMimeTypes = Arrays.asList("image/gif");
        List<String> gifReaderSpis = Arrays.asList(
                "com.sun.imageio.plugins.gif.GIFImageReaderSpi"
        );

        ImageDecoderImpl gifDecoder = new ImageDecoderImpl(
                true,            // isMultiImage
                gifMimeTypes
        );
        decoders.put("image/gif", gifDecoder);

        // JPEG 解码器
        List<String> jpegMimeTypes = Arrays.asList("image/jpeg");
        List<String> jpegReaderSpis = Arrays.asList(
                "com.sun.media.imageioimpl.plugins.jpeg.CLibJPEGImageReaderSpi",
                "com.sun.imageio.plugins.jpeg.JPEGImageReaderSpi"
        );

        ImageDecoderImpl jpegDecoder = new ImageDecoderImpl(
                true,            // isMultiImage
                jpegMimeTypes
        );
        decoders.put("image/jpeg", jpegDecoder);

        // TIFF 解码器
        List<String> tiffMimeTypes = Arrays.asList("image/tiff");
        List<String> tiffReaderSpis = Arrays.asList(
                "it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReaderSpi",
                "com.sun.media.imageioimpl.plugins.tiff.TIFFImageReaderSpi"
        );

        ImageDecoderImpl tiffDecoder = new ImageDecoderImpl(
                false,           // isMultiImage
                tiffMimeTypes
        );
        decoders.put("image/tiff", tiffDecoder);

        // BMP 解码器
        List<String> bmpMimeTypes = Arrays.asList("image/bmp");
        List<String> bmpReaderSpis = Arrays.asList(
                "com.sun.media.imageioimpl.plugins.bmp.BMPImageReaderSpi",
                "com.sun.imageio.plugins.bmp.BMPImageReaderSpi"
        );

        ImageDecoderImpl bmpDecoder = new ImageDecoderImpl(
                true,            // isMultiImage
                bmpMimeTypes
        );
        decoders.put("image/bmp", bmpDecoder);
    }

    /**
     * 获取编码器
     */
    public static ImageEncoder getEncoder(String mimeType) {
        ImageEncoder encoder = encoders.get(mimeType);
        if (encoder == null) {
            throw new IllegalArgumentException("不支持的 MIME 类型: " + mimeType);
        }
        return encoder;
    }

    /**
     * 获取解码器
     */
    public static ImageDecoder getDecoder(String mimeType) {
        ImageDecoder decoder = decoders.get(mimeType);
        if (decoder == null) {
            throw new IllegalArgumentException("不支持的 MIME 类型: " + mimeType);
        }
        return decoder;
    }

    /**
     * 获取所有编码器
     */
    public static Map<String, ImageEncoder> getAllEncoders() {
        return Collections.unmodifiableMap(encoders);
    }

    /**
     * 获取所有解码器
     */
    public static Map<String, ImageDecoder> getAllDecoders() {
        return Collections.unmodifiableMap(decoders);
    }

    /**
     * 获取所有编码器
     */
    public static List<ImageEncoder> getAllEncodersList() {
        return Collections.unmodifiableMap(encoders).entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toList());
    }

    /**
     * 获取所有解码器
     */
    public static List<ImageDecoder> getAllDecodersList() {
        return Collections.unmodifiableMap(decoders).entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toList());

    }

    /**
     * 支持的编码格式列表
     */
    public static List<String> getSupportedEncoderFormats() {
        return new ArrayList<>(encoders.keySet());
    }

    /**
     * 支持的解码格式列表
     */
    public static List<String> getSupportedDecoderFormats() {
        return new ArrayList<>(decoders.keySet());
    }

    public static void main(String[] args) {
        logger.info("=== 图片编码器/解码器初始化工具 ===\n");

        // 初始化
        initialize();

        // 测试获取编码器
        logger.info("\n=== 测试获取编码器 ===");
        String[] testMimeTypes = {"image/png", "image/jpeg", "image/gif", "image/tiff", "image/bmp"};
        for (String mimeType : testMimeTypes) {
            try {
                ImageEncoder encoder = getEncoder(mimeType);
                logger.info("✓ 获取编码器成功: " + mimeType + " -> " + encoder.getClass().getSimpleName());
            } catch (Exception e) {
                logger.info("✗ 获取编码器失败: " + mimeType + " - " + e.getMessage());
            }
        }

        // 测试获取解码器
        logger.info("\n=== 测试获取解码器 ===");
        for (String mimeType : testMimeTypes) {
            try {
                ImageDecoder decoder = getDecoder(mimeType);
                logger.info("✓ 获取解码器成功: " + mimeType + " -> " + decoder.getClass().getSimpleName());
            } catch (Exception e) {
                logger.info("✗ 获取解码器失败: " + mimeType + " - " + e.getMessage());
            }
        }

        // 打印支持列表
        logger.info("\n=== 支持的编码格式 ===");
        getSupportedEncoderFormats().forEach(format -> logger.info("  - " + format));

        logger.info("\n=== 支持的解码格式 ===");
        getSupportedDecoderFormats().forEach(format -> logger.info("  - " + format));
    }
}
