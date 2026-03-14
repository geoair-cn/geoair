package cn.geoair.comp.db.service.core.utils;

import cn.geoair.base.Gir;
import cn.geoair.comp.db.service.core.config.GirDsServiceProperties;
import cn.geoair.map.dynamic.tools.GirService;
import cn.geoair.map.dynamic.tools.simple.GirServletUtil;
import cn.geoair.web.util.GirHttpServletHelper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/** Token管理工具类（MD5版：用户名+密码+dsApi拼接后MD5作为Token，无需存储，多实例直接校验） */
public class TokenManager {

	// 固定拼接字符串（你指定的dsApi）
	private static final String FIXED_STR = "dsApi";

	/**
	 * 生成Token：用户名 + 密码 + dsApi 拼接后MD5加密
	 * @param username 用户名
	 * @param password 密码
	 * @return 32位小写MD5 Token
	 */
	public static String generateToken(String username, String password) {
		if (username == null || password == null) {
			throw new IllegalArgumentException("用户名/密码不能为空");
		}
		// 拼接字符串：用户名 + 密码 + dsApi
		String source = username + password + FIXED_STR;
		// 生成MD5并返回
		return md5Encrypt(source);
	}

	/**
	 * 校验Token有效性：重新拼接用户名+密码+dsApi生成MD5，与传入Token比对
	 * @param token 待校验的Token
	 * @param username 用户名（校验时需传入，用于重新生成MD5）
	 * @param password 密码（校验时需传入，用于重新生成MD5）
	 * @return true-有效，false-无效
	 */
	public static boolean validateToken(String token, String username, String password) {
		if (token == null || token.isEmpty() || username == null || password == null) {
			return false;
		}
		// 重新生成Token，与传入的Token比对
		String generatedToken = generateToken(username, password);
		return generatedToken.equals(token);
	}

	public static boolean validateToken(String token) {
		GirDsServiceProperties girDsServiceProperties = GirService.getPxyBeanC(GirDsServiceProperties.class);
		if (!girDsServiceProperties.isEnableLogin()) {
			return true;
		}
		else {
			String username = girDsServiceProperties.getDefaultUser();
			String defaultPassword = girDsServiceProperties.getDefaultPassword();
			return validateToken(token, username, defaultPassword);
		}
	}

	public static boolean validateToken() {
		Map<String, String> paramMap = GirServletUtil.getParamMap(GirHttpServletHelper.getRequest());
		String token = paramMap.get("dsToken");
		boolean b = validateToken(token);
		if (!b) {
			GirServletUtil.setNoCacheHeaders();
			GirServletUtil.toResponse(GirHttpServletHelper.getResponse(),
					"{\"code\":401,\"message\":\"Token无效\"}".getBytes(), "application/json");
			throw new RuntimeException("Token无效");
		}
		return b;
	}

	/**
	 * MD5加密核心方法（32位小写）
	 * @param source 待加密字符串
	 * @return 32位小写MD5结果
	 */
	private static String md5Encrypt(String source) {
		try {
			// 获取MD5加密实例
			MessageDigest md = MessageDigest.getInstance("MD5");
			// 加密源字符串
			byte[] bytes = md.digest(source.getBytes(StandardCharsets.UTF_8));
			// 转为32位小写十六进制字符串
			StringBuilder sb = new StringBuilder();
			for (byte b : bytes) {
				String hex = Integer.toHexString(0xff & b);
				if (hex.length() == 1) {
					sb.append('0');
				}
				sb.append(hex);
			}
			return sb.toString();
		}
		catch (NoSuchAlgorithmException e) {
			// MD5算法不存在时抛出运行时异常（理论上不会发生）
			throw new RuntimeException("MD5加密算法不存在", e);
		}
	}

	// 测试示例（可选）
	public static void main(String[] args) {
		// 测试生成Token
		String token = TokenManager.generateToken("admin", "123456");
		Gir.log.info("生成的Token：" + token);

		// 测试校验Token
		boolean valid = TokenManager.validateToken(token, "admin", "123456");
		Gir.log.info("Token是否有效：" + valid); // 输出：true

		// 错误密码校验
		boolean invalid = TokenManager.validateToken(token, "admin", "654321");
		Gir.log.info("错误密码校验结果：" + invalid); // 输出：false
	}

}
