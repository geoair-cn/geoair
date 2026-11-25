package cn.geoair.map.tile.forge.fuser.utils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/** 瓦片图片的受限读取和编码工具。 */
public final class TileImageUtils {

    private static final int BUFFER_SIZE = 8192;

    private TileImageUtils() {}

    public static BufferedImage readImage(byte[] imageBytes) throws IOException {
        if (imageBytes == null || imageBytes.length == 0) {
            return null;
        }
        validateByteSize(imageBytes.length);
        try (ImageInputStream input =
                ImageIO.createImageInputStream(new ByteArrayInputStream(imageBytes))) {
            return readImage(input);
        }
    }

    public static BufferedImage readImage(File imageFile) throws IOException {
        if (imageFile == null || !imageFile.isFile()) {
            return null;
        }
        validateByteSize(imageFile.length());
        try (ImageInputStream input = ImageIO.createImageInputStream(imageFile)) {
            return readImage(input);
        }
    }

    public static byte[] readAllLimited(InputStream input) throws IOException {
        try (LimitedByteArrayOutputStream output =
                new LimitedByteArrayOutputStream(TileResourceLimits.getMaxTileBytes())) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int length;
            while ((length = input.read(buffer)) != -1) {
                output.write(buffer, 0, length);
            }
            return output.toByteArray();
        }
    }

    public static byte[] writeImage(BufferedImage image, String format) throws IOException {
        if (image == null) {
            return null;
        }
        TileResourceLimits.validateImageDimensions(image.getWidth(), image.getHeight());
        try (LimitedByteArrayOutputStream output =
                new LimitedByteArrayOutputStream(TileResourceLimits.getMaxTileBytes())) {
            if (!ImageIO.write(image, format, output)) {
                throw new IOException("不支持的图片输出格式: " + format);
            }
            return output.toByteArray();
        }
    }

    private static BufferedImage readImage(ImageInputStream input) throws IOException {
        if (input == null) {
            return null;
        }
        Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
        if (!readers.hasNext()) {
            return null;
        }
        ImageReader reader = readers.next();
        try {
            reader.setInput(input, true, true);
            TileResourceLimits.validateImageDimensions(reader.getWidth(0), reader.getHeight(0));
            return reader.read(0);
        } finally {
            reader.dispose();
        }
    }

    private static void validateByteSize(long size) throws IOException {
        if (size < 0 || size > TileResourceLimits.getMaxTileBytes()) {
            throw new IOException("瓦片数据超过大小限制: " + size);
        }
    }

    private static final class LimitedByteArrayOutputStream extends ByteArrayOutputStream {
        private final int maxBytes;

        private LimitedByteArrayOutputStream(int maxBytes) {
            super(Math.min(maxBytes, BUFFER_SIZE));
            this.maxBytes = maxBytes;
        }

        @Override
        public synchronized void write(int value) {
            ensureRemaining(1);
            super.write(value);
        }

        @Override
        public synchronized void write(byte[] buffer, int offset, int length) {
            ensureRemaining(length);
            super.write(buffer, offset, length);
        }

        private void ensureRemaining(int length) {
            if (length < 0 || count > maxBytes - length) {
                throw new IllegalArgumentException("瓦片数据超过大小限制: " + maxBytes);
            }
        }
    }
}
