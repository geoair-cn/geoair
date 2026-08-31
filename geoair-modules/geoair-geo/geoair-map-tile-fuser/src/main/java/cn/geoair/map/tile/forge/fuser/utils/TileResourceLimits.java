package cn.geoair.map.tile.forge.fuser.utils;

/**
 * 瓦片资源的安全上限。
 *
 * <p>网络响应、缓存文件和图片解码都会使用这些值。应用可在初始化阶段调用 setter 调整， 不建议在并发请求期间修改。
 */
public final class TileResourceLimits {

    private static volatile int maxTileBytes = 10 * 1024 * 1024;
    private static volatile int maxImageWidth = 4096;
    private static volatile int maxImageHeight = 4096;
    private static volatile long maxImagePixels = 4L * 1024 * 1024;

    private TileResourceLimits() {}

    public static int getMaxTileBytes() {
        return maxTileBytes;
    }

    public static void setMaxTileBytes(int maxTileBytes) {
        if (maxTileBytes <= 0) {
            throw new IllegalArgumentException("maxTileBytes 必须大于 0");
        }
        TileResourceLimits.maxTileBytes = maxTileBytes;
    }

    public static int getMaxImageWidth() {
        return maxImageWidth;
    }

    public static void setMaxImageWidth(int maxImageWidth) {
        if (maxImageWidth <= 0) {
            throw new IllegalArgumentException("maxImageWidth 必须大于 0");
        }
        TileResourceLimits.maxImageWidth = maxImageWidth;
    }

    public static int getMaxImageHeight() {
        return maxImageHeight;
    }

    public static void setMaxImageHeight(int maxImageHeight) {
        if (maxImageHeight <= 0) {
            throw new IllegalArgumentException("maxImageHeight 必须大于 0");
        }
        TileResourceLimits.maxImageHeight = maxImageHeight;
    }

    public static long getMaxImagePixels() {
        return maxImagePixels;
    }

    public static void setMaxImagePixels(long maxImagePixels) {
        if (maxImagePixels <= 0) {
            throw new IllegalArgumentException("maxImagePixels 必须大于 0");
        }
        TileResourceLimits.maxImagePixels = maxImagePixels;
    }

    static void validateImageDimensions(int width, int height) {
        if (width <= 0
                || height <= 0
                || width > maxImageWidth
                || height > maxImageHeight
                || (long) width * height > maxImagePixels) {
            throw new IllegalArgumentException("图片尺寸超过瓦片资源限制: " + width + "x" + height);
        }
    }
}
