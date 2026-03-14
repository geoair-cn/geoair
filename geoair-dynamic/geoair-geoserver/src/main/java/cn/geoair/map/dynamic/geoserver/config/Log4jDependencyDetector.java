package cn.geoair.map.dynamic.geoserver.config;

import cn.geoair.base.Gir;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/** 内置在三方Jar中的日志依赖检测初始化器 检测log4j-to-slf4j/log4j-api是否存在，缺失则抛出友好提示 */
public class Log4jDependencyDetector implements ApplicationContextInitializer<ConfigurableApplicationContext> {

	// 需检测的核心类（对应两个依赖）
	private static final String[] REQUIRED_CLASSES = {
			// log4j-to-slf4j 的核心类
			"org.apache.logging.slf4j.Log4jLoggerFactory",
			// log4j-api 的核心类
			"org.apache.logging.log4j.LogManager" };

	// 缺失类对应的依赖提示
	private static final String DEPENDENCY_TIP = "\n===== 【GeoServer三方Jar日志依赖缺失提醒】 =====\n"
			+ "检测到缺失以下日志依赖，导致GeoServer日志无法输出！\n" + "请在您的项目pom.xml中添加：\n" + "<dependency>\n"
			+ "    <groupId>org.apache.logging.log4j</groupId>\n" + "    <artifactId>log4j-to-slf4j</artifactId>\n"
			+ "    <version>2.17.2</version>\n" + "</dependency>\n" + "<dependency>\n"
			+ "    <groupId>org.apache.logging.log4j</groupId>\n" + "    <artifactId>log4j-api</artifactId>\n"
			+ "    <version>2.17.2</version>\n" + "</dependency>\n" + "==============================================";

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		// 检测每个核心类是否存在
		for (String className : REQUIRED_CLASSES) {
			try {
				// 加载类，检测是否存在
				Class.forName(className);
			}
			catch (ClassNotFoundException e) {
				// 缺失则抛出包含解决方案的异常
				throw new IllegalStateException(DEPENDENCY_TIP, e);
			}
		}
		// 检测通过，打印成功日志
		Gir.log.info("✅ Log4j 日志依赖检测通过（log4j-to-slf4j/log4j-api 均存在）");
	}

}
