package cn.geoair.base.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public abstract class GutilDigest {

	private static final String MD5_ALGORITHM_NAME = "MD5";

	private static final char[] HEX_CHARS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e',
			'f' };

	/**
	 * Calculate the MD5 digest of the given bytes.
	 * @param bytes the bytes to calculate the digest over
	 * @return the digest
	 */
	public static byte[] md5Digest(byte[] bytes) {
		return digest(MD5_ALGORITHM_NAME, bytes);
	}

	/**
	 * Return a hexadecimal string representation of the MD5 digest of the given bytes.
	 * @param bytes the bytes to calculate the digest over
	 * @return a hexadecimal digest string
	 */
	public static String md5DigestAsHex(byte[] bytes) {
		return digestAsHexString(MD5_ALGORITHM_NAME, bytes);
	}

	/**
	 * Append a hexadecimal string representation of the MD5 digest of the given bytes to
	 * the given {@link StringBuilder}.
	 * @param bytes the bytes to calculate the digest over
	 * @param builder the string builder to append the digest to
	 * @return the given string builder
	 */
	public static StringBuilder appendMd5DigestAsHex(byte[] bytes, StringBuilder builder) {
		return appendDigestAsHex(MD5_ALGORITHM_NAME, bytes, builder);
	}

	/**
	 * Create a new {@link MessageDigest} with the given algorithm.
	 *
	 * <p>
	 * Necessary because {@code MessageDigest} is not thread-safe.
	 */
	private static MessageDigest getDigest(String algorithm) {
		try {
			return MessageDigest.getInstance(algorithm);
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("Could not find MessageDigest with algorithm \"" + algorithm + "\"", ex);
		}
	}

	private static byte[] digest(String algorithm, byte[] bytes) {
		return getDigest(algorithm).digest(bytes);
	}

	private static String digestAsHexString(String algorithm, byte[] bytes) {
		char[] hexDigest = digestAsHexChars(algorithm, bytes);
		return new String(hexDigest);
	}

	private static StringBuilder appendDigestAsHex(String algorithm, byte[] bytes, StringBuilder builder) {
		char[] hexDigest = digestAsHexChars(algorithm, bytes);
		return builder.append(hexDigest);
	}

	private static char[] digestAsHexChars(String algorithm, byte[] bytes) {
		byte[] digest = digest(algorithm, bytes);
		return encodeHex(digest);
	}

	private static char[] encodeHex(byte[] bytes) {
		char[] chars = new char[32];
		for (int i = 0; i < chars.length; i = i + 2) {
			byte b = bytes[i / 2];
			chars[i] = HEX_CHARS[(b >>> 0x4) & 0xf];
			chars[i + 1] = HEX_CHARS[b & 0xf];
		}
		return chars;
	}

	public static String calFileMd5(File file) {
		FileInputStream fileInputStream = null;
		try {
			MessageDigest MD5 = MessageDigest.getInstance("MD5");
			fileInputStream = new FileInputStream(file);
			byte[] buffer = new byte[8192];
			int length;
			while ((length = fileInputStream.read(buffer)) != -1) {
				MD5.update(buffer, 0, length);
			}

			return new String(encodeHex(MD5.digest()));
		}
		catch (Exception e) {
			e.printStackTrace();
			return "";
		}
		finally {
			try {
				if (fileInputStream != null)
					fileInputStream.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static String calFileMd5ByByte(byte[] bytes) {
		try {
			MessageDigest MD5 = MessageDigest.getInstance("MD5");
			MD5.update(bytes);

			return new String(encodeHex(MD5.digest()));
		}
		catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}

	public static void main(String[] args) throws Exception {

		byte[] ts = "050318".getBytes();
		MessageDigest MD5 = MessageDigest.getInstance("MD5");
		MD5.update(ts);

		byte[] tt = MD5.digest();

		System.out.println(new String(encodeHex(tt)));
		System.out.println(new BigInteger(1, tt).toString(16));
		System.out.println(UUID.randomUUID().toString());

		List<String> list = Arrays.asList(new String[] { "a", "b", "c", "d", "e", "f", "g", "h" });
		System.out.println(list.subList(0, 3));
		System.out.println(list.subList(3, list.size()));
	}

}
