package cn.geoair.base.util;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5 摘要工具类，提供字节数组/字符串/文件的 MD5 摘要计算能力。
 *
 * <p>注意：MD5 已不再被视为密码学安全算法，碰撞攻击已可行。密码存储等安全敏感场景请使用带盐的 加盐哈希算法（如
 * PBKDF2、bcrypt、scrypt）；本工具适用于文件完整性校验、幂等键生成等非安全场景。
 */
public abstract class GutilDigest {

    private static final String MD5_ALGORITHM_NAME = "MD5";

    private static final char[] HEX_CHARS = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    /**
     * 计算给定字节数组的 MD5 摘要。
     *
     * <p>注意：MD5 已不再被视为密码学安全算法，密码存储场景请使用带盐的加盐哈希算法 （如 PBKDF2、bcrypt、scrypt）。
     *
     * @param bytes 要计算摘要的字节数组，不可为 null
     * @return 摘要字节数组
     * @throws NullPointerException 如果字节数组为 null
     */
    public static byte[] md5Digest(byte[] bytes) {
        return digest(MD5_ALGORITHM_NAME, bytes);
    }

    /**
     * 返回给定字节数组 MD5 摘要的十六进制字符串表示（小写）。
     *
     * <p>注意：MD5 已不再被视为密码学安全算法，密码存储场景请使用带盐的加盐哈希算法 （如 PBKDF2、bcrypt、scrypt）。
     *
     * @param bytes 要计算摘要的字节数组，不可为 null
     * @return 十六进制摘要字符串
     * @throws NullPointerException 如果字节数组为 null
     */
    public static String md5DigestAsHex(byte[] bytes) {
        return digestAsHexString(MD5_ALGORITHM_NAME, bytes);
    }

    /**
     * 计算给定字符串的 MD5 摘要并返回十六进制字符串表示（小写）。
     *
     * <p><b>编码一致性：</b>本方法固定使用 UTF-8 编码将字符串转为字节。摘要结果取决于编码方式，
     * 签名与校验双方必须使用相同编码，否则结果不一致；跨系统或与其它语言交互时建议显式使用 {@link #md5(String, Charset)} 指定字符集。
     *
     * <p>注意：MD5 已不再被视为密码学安全算法，密码存储场景请使用带盐的加盐哈希算法 （如 PBKDF2、bcrypt、scrypt）。
     *
     * @param text 要计算摘要的字符串，不可为 null
     * @return 十六进制摘要字符串
     * @throws NullPointerException 如果字符串为 null
     * @see #md5(String, Charset)
     */
    public static String md5(String text) {
        return md5(text, Charset.forName("UTF-8"));
    }

    /**
     * 计算给定字符串（按指定字符集编码）的 MD5 摘要并返回十六进制字符串表示（小写）。
     *
     * <p><b>编码一致性：</b>摘要结果取决于字符串按 {@code charset} 编码后的字节，签名与校验双方
     * 必须使用相同字符集，否则结果不一致；建议显式指定字符集而非依赖默认值。
     *
     * <p>注意：MD5 已不再被视为密码学安全算法，密码存储场景请使用带盐的加盐哈希算法 （如 PBKDF2、bcrypt、scrypt）。
     *
     * @param text 要计算摘要的字符串，不可为 null
     * @param charset 将字符串编码为字节所使用的字符集，不可为 null
     * @return 十六进制摘要字符串
     * @throws IllegalArgumentException 如果字符集为 null
     * @throws NullPointerException 如果字符串为 null
     * @see #md5(String)
     */
    public static String md5(String text, Charset charset) {
        if (charset == null) {
            throw new IllegalArgumentException("The charset must not be null");
        }
        return md5DigestAsHex(text.getBytes(charset));
    }

    /**
     * 将给定字节数组的 MD5 摘要的十六进制字符串表示追加到指定 {@link StringBuilder}。
     *
     * <p>注意：MD5 已不再被视为密码学安全算法，密码存储场景请使用带盐的加盐哈希算法 （如 PBKDF2、bcrypt、scrypt）。
     *
     * @param bytes 要计算摘要的字节数组，不可为 null
     * @param builder 要追加摘要的字符串构建器，不可为 null
     * @return 给定的字符串构建器
     * @throws IllegalArgumentException 如果字符串构建器为 null
     * @throws NullPointerException 如果字节数组为 null
     */
    public static StringBuilder appendMd5DigestAsHex(byte[] bytes, StringBuilder builder) {
        if (builder == null) {
            throw new IllegalArgumentException("The builder must not be null");
        }
        return appendDigestAsHex(MD5_ALGORITHM_NAME, bytes, builder);
    }

    /**
     * 创建指定算法的 {@link MessageDigest} 实例。
     *
     * <p>每次调用都创建新实例，因为 {@code MessageDigest} 不是线程安全的。
     *
     * @param algorithm 摘要算法名称，如 "MD5"
     * @return 新的 {@link MessageDigest} 实例
     * @throws IllegalStateException 如果 JVM 不支持该算法
     */
    private static MessageDigest getDigest(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(
                    "Could not find MessageDigest with algorithm \"" + algorithm + "\"", ex);
        }
    }

    /**
     * 按指定算法计算字节数组的摘要。
     *
     * @param algorithm 摘要算法名称
     * @param bytes 要计算摘要的字节数组
     * @return 摘要字节数组
     */
    private static byte[] digest(String algorithm, byte[] bytes) {
        return getDigest(algorithm).digest(bytes);
    }

    /**
     * 按指定算法计算摘要并返回十六进制字符串。
     *
     * @param algorithm 摘要算法名称
     * @param bytes 要计算摘要的字节数组
     * @return 十六进制摘要字符串
     */
    private static String digestAsHexString(String algorithm, byte[] bytes) {
        char[] hexDigest = digestAsHexChars(algorithm, bytes);
        return new String(hexDigest);
    }

    /**
     * 按指定算法计算摘要并将十六进制字符串追加到字符串构建器。
     *
     * @param algorithm 摘要算法名称
     * @param bytes 要计算摘要的字节数组
     * @param builder 要追加摘要的字符串构建器
     * @return 给定的字符串构建器
     */
    private static StringBuilder appendDigestAsHex(
            String algorithm, byte[] bytes, StringBuilder builder) {
        char[] hexDigest = digestAsHexChars(algorithm, bytes);
        return builder.append(hexDigest);
    }

    /**
     * 按指定算法计算摘要并返回十六进制字符数组。
     *
     * @param algorithm 摘要算法名称
     * @param bytes 要计算摘要的字节数组
     * @return 十六进制摘要字符数组
     */
    private static char[] digestAsHexChars(String algorithm, byte[] bytes) {
        byte[] digest = digest(algorithm, bytes);
        return encodeHex(digest);
    }

    /**
     * 将字节数组编码为小写十六进制字符数组，长度自适应（每个字节对应 2 个字符）， 可正确处理任意摘要算法（MD5/SHA-1/SHA-256 等）的输出长度。
     *
     * @param bytes 要编码的字节数组
     * @return 十六进制字符数组
     */
    private static char[] encodeHex(byte[] bytes) {
        char[] chars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            chars[i * 2] = HEX_CHARS[(b >>> 0x4) & 0xf];
            chars[i * 2 + 1] = HEX_CHARS[b & 0xf];
        }
        return chars;
    }

    /**
     * 计算文件的 MD5 摘要并返回十六进制字符串表示（小写）。
     *
     * <p>文件按 8KB 缓冲分块读取更新摘要。文件不存在、读取失败或计算异常时返回 null （null 表示失败，调用方需自行区分）。
     *
     * <p>注意：MD5 已不再被视为密码学安全算法，密码存储场景请使用带盐的加盐哈希算法 （如 PBKDF2、bcrypt、scrypt）。
     *
     * @param file 要计算摘要的文件，不可为 null
     * @return 十六进制摘要字符串；文件为 null 或计算失败时返回 null
     * @see #calFileMd5ByByte(byte[])
     */
    public static String calFileMd5(File file) {
        if (file == null) {
            return null;
        }
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            MessageDigest md5 = MessageDigest.getInstance(MD5_ALGORITHM_NAME);
            byte[] buffer = new byte[8192];
            int length;
            while ((length = fileInputStream.read(buffer)) != -1) {
                md5.update(buffer, 0, length);
            }
            return new String(encodeHex(md5.digest()));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 计算字节数组的 MD5 摘要并返回十六进制字符串表示（小写）。
     *
     * <p>字节数组为 null 或计算异常时返回 null（null 表示失败，调用方需自行区分）。
     *
     * <p>注意：MD5 已不再被视为密码学安全算法，密码存储场景请使用带盐的加盐哈希算法 （如 PBKDF2、bcrypt、scrypt）。
     *
     * @param bytes 要计算摘要的字节数组，可为 null
     * @return 十六进制摘要字符串；字节数组为 null 或计算失败时返回 null
     * @see #md5DigestAsHex(byte[])
     */
    public static String calFileMd5ByByte(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        try {
            MessageDigest md5 = MessageDigest.getInstance(MD5_ALGORITHM_NAME);
            md5.update(bytes);
            return new String(encodeHex(md5.digest()));
        } catch (Exception e) {
            return null;
        }
    }
}
