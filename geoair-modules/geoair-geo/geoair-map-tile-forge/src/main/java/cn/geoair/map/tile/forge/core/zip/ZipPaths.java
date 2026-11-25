package cn.geoair.map.tile.forge.core.zip;

import java.nio.file.Path;
import java.nio.file.InvalidPathException;
import java.io.IOException;
import java.util.Locale;

/** 请求路径已由 Servlet 解码，此处不重复 URLDecode，保留加号与字面百分号。 */
public final class ZipPaths {
    private ZipPaths() { }

    public static String relative(String value) {
        if (value == null || value.isEmpty() || value.startsWith("/") || value.indexOf('\\') >= 0
                || value.indexOf(':') >= 0 || value.indexOf('\0') >= 0)
            throw new ZipReadException(ZipReadFailure.INVALID_PATH, "ZIP资源路径无效", null);
        for (String part : value.split("/", -1)) {
            if (part.isEmpty() || part.equals(".") || part.equals(".."))
                throw new ZipReadException(ZipReadFailure.INVALID_PATH, "ZIP资源路径包含非法目录", null);
        }
        return value;
    }

    public static String prefix(String value) {
        if (value == null || value.trim().isEmpty()) return "";
        return relative(value.replace('\\', '/').replaceAll("/+$", "")) + "/";
    }

    public static boolean safe(String value) {
        try { relative(value); return true; }
        catch (ZipReadException e) { return false; }
    }

    public static String format(String value) {
        String format = value == null || value.isEmpty() ? "png" : value.toLowerCase(Locale.ROOT);
        if (!format.matches("[a-z0-9]+"))
            throw new ZipReadException(ZipReadFailure.INVALID_PATH, "瓦片格式无效", null);
        return format;
    }

    public static String number(String value) {
        if (value == null || !value.matches("[0-9]+"))
            throw new ZipReadException(ZipReadFailure.INVALID_PATH, "瓦片行列号无效", null);
        return value;
    }

    public static Path inside(Path root, String relative) {
        try {
            Path result = root.resolve(relative(relative)).normalize();
            if (!result.toFile().getCanonicalFile().toPath().startsWith(root.toRealPath()))
                throw new ZipReadException(ZipReadFailure.INVALID_PATH, "ZIP资源路径越出临时目录", null);
            return result;
        } catch (InvalidPathException e) {
            throw new ZipReadException(ZipReadFailure.INVALID_PATH, "当前系统不支持此ZIP资源路径", e);
        } catch (IOException e) {
            throw new ZipReadException(ZipReadFailure.READ_FAILED, "无法检查ZIP临时目录", e);
        }
    }
}
