package cn.geoair.gtc.base;

import java.lang.reflect.Type;
import java.util.Map;

import cn.geoair.gtc.base.data.page.support.GirPageConfig;
import cn.geoair.gtc.base.env.GiEnvironmenter;
import cn.geoair.gtc.base.env.GirEnvironmentHelper;
import cn.geoair.gtc.base.env.property.GiPropertier;
import cn.geoair.gtc.base.env.property.GirPropertyHelper;
import cn.geoair.gtc.base.json.GirJSON;
import cn.geoair.gtc.base.lang.caller.GkCallerUtil;
import cn.geoair.gtc.base.log.GiLogger;
import cn.geoair.gtc.base.log.GirLogger;
import cn.geoair.gtc.base.tool.GkConsole;
import cn.geoair.gtc.base.tool.GkConsoleTable;
import cn.geoair.gtc.base.bean.GirBeanException;
import cn.geoair.gtc.base.bean.GiBeanFactory;
import cn.geoair.gtc.base.bean.GirBeanDefinitionStoreException;
import cn.geoair.gtc.base.bean.GirBeanHelper;
import cn.geoair.gtc.base.bean.GirNoSuchBeanException;

/**
 * 基础开发库</br>
 * 提供统一的编程接口和常用工具方法，简化开发流程</br>
 *
 * 命名规范：</br>
 * Ga* annotation 注解</br>
 * Gi* interface 接口</br>
 * Gir* 实现类</br>
 * Gutil* util 工具类</br>
 * Gfun* function 函数接口</br>
 * Gk* kit 内建工具类</br>
 *
 * @author Ray
 *
 */

public abstract class Gir {

	/**
	 * 属性对象，用于获取配置属性值 提供对应用配置文件中属性的访问能力
	 */
	public static final GiPropertier property = new GiPropertier() {

		@Override
		public boolean containsProperty(String key) {
			return GirPropertyHelper.getPropertier().containsProperty(key);
		}

		@Override
		public String getProperty(String key) {

			return GirPropertyHelper.getPropertier().getProperty(key);
		}

		@Override
		public String getProperty(String key, String defaultValue) {

			return GirPropertyHelper.getPropertier().getProperty(key, defaultValue);
		}

		@Override
		public <T> T getProperty(String key, Class<T> targetType) {

			return GirPropertyHelper.getPropertier().getProperty(key, targetType);
		}

		@Override
		public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {

			return GirPropertyHelper.getPropertier().getProperty(key, targetType, defaultValue);
		}

		@Override
		public String getRequiredProperty(String key) throws IllegalStateException {

			return GirPropertyHelper.getPropertier().getRequiredProperty(key);
		}

		@Override
		public <T> T getRequiredProperty(String key, Class<T> targetType) throws IllegalStateException {

			return GirPropertyHelper.getPropertier().getRequiredProperty(key, targetType);
		}

		@Override
		public String resolvePlaceholders(String text) {

			return GirPropertyHelper.getPropertier().resolvePlaceholders(text);
		}

		@Override
		public String resolveRequiredPlaceholders(String text) throws IllegalArgumentException {

			return GirPropertyHelper.getPropertier().resolvePlaceholders(text);
		}
	};

	/**
	 * 环境对象，用于获取运行环境相关信息 提供对应用运行环境状态的访问能力，如开发环境、调试模式等
	 */
	public static final GiEnvironmenter env = new GiEnvironmenter() {

		@Override
		public String[] getActiveProfiles() {

			return GirEnvironmentHelper.getEnvironmenter().getActiveProfiles();
		}

		@Override
		public String[] getDefaultProfiles() {

			return GirEnvironmentHelper.getEnvironmenter().getDefaultProfiles();
		}

		@Override
		public boolean isDev() {

			return GirEnvironmentHelper.getEnvironmenter().isDev();
		}

		@Override
		public boolean isDebugger() {

			return GirEnvironmentHelper.getEnvironmenter().isDebugger();
		}
	};

	/**
	 * 日志对象，提供统一的日志记录功能 封装了底层日志实现，支持不同级别的日志输出
	 */
	public static final GiLogger log = new GiLogger() {

		@Override
		public boolean isFatalEnabled() {
			return GirLogger.getLoger(GkCallerUtil.getCallerCallerName()).isFatalEnabled();
		}

		@Override
		public boolean isErrorEnabled() {

			return GirLogger.getLoger(GkCallerUtil.getCallerCallerName()).isErrorEnabled();
		}

		@Override
		public boolean isWarnEnabled() {

			return GirLogger.getLoger(GkCallerUtil.getCallerCallerName()).isWarnEnabled();
		}

		@Override
		public boolean isInfoEnabled() {

			return GirLogger.getLoger(GkCallerUtil.getCallerCallerName()).isInfoEnabled();
		}

		@Override
		public boolean isDebugEnabled() {

			return GirLogger.getLoger(GkCallerUtil.getCallerCallerName()).isDebugEnabled();
		}

		@Override
		public boolean isTraceEnabled() {

			return GirLogger.getLoger(GkCallerUtil.getCallerCallerName()).isTraceEnabled();
		}

		@Override
		public void fatal(String format, Object... arguments) {
			GirLogger.getLoger(GkCallerUtil.getCallerCallerName()).fatal(format, arguments);
		}

		@Override
		public void fatal(Throwable t) {
			GirLogger.getLoger(GkCallerUtil.getCallerCallerName()).fatal(t);

		}

		@Override
		public void fatal(Throwable t, String format, Object... arguments) {
			GirLogger.getLoger(GkCallerUtil.getCallerCallerName()).fatal(t, format, arguments);
		}

		@Override
		public void error(String format, Object... arguments) {
			GirLogger.getLoger(GkCallerUtil.getCallerCallerName()).error(format, arguments);
		}

		@Override
		public void error(Throwable t) {
			GirLogger.getLoger(GkCallerUtil.getCallerCallerName()).error(t);
		}

		@Override
		public void error(Throwable t, String format, Object... arguments) {
			GirLogger.getLoger(GkCallerUtil.getCallerCallerName()).error(t, format, arguments);
		}

		@Override
		public void warn(String format, Object... arguments) {
			GirLogger.getLoger(GkCallerUtil.getCallerCallerName()).warn(format, arguments);
		}

		@Override
		public void warn(Throwable t) {
			GirLogger.getLoger(GkCallerUtil.getCallerCallerName()).warn(t);
		}

		@Override
		public void warn(Throwable t, String format, Object... arguments) {
			GirLogger.getLoger(GkCallerUtil.getCallerCallerName()).warn(t, format, arguments);
		}

		@Override
		public void info(String format, Object... arguments) {
			GirLogger.getLoger(GkCallerUtil.getCallerCallerName()).info(format, arguments);
		}

		@Override
		public void info(Throwable t) {
			GirLogger.getLoger(GkCallerUtil.getCallerCallerName()).info(t);
		}

		@Override
		public void info(Throwable t, String format, Object... arguments) {
			GirLogger.getLoger(GkCallerUtil.getCallerCallerName()).info(t, format, arguments);
		}

		@Override
		public void debug(String format, Object... arguments) {
			GirLogger.getLoger(GkCallerUtil.getCallerCallerName()).debug(format, arguments);
		}

		@Override
		public void debug(Throwable t) {
			GirLogger.getLoger(GkCallerUtil.getCallerCallerName()).debug(t);
		}

		@Override
		public void debug(Throwable t, String format, Object... arguments) {
			GirLogger.getLoger(GkCallerUtil.getCallerCallerName()).debug(t, format, arguments);
		}

		@Override
		public void trace(String format, Object... arguments) {
			GirLogger.getLoger(GkCallerUtil.getCallerCallerName()).trace(format, arguments);
		}

		@Override
		public void trace(Throwable t) {
			GirLogger.getLoger(GkCallerUtil.getCallerCallerName()).trace(t);
		}

		@Override
		public void trace(Throwable t, String format, Object... arguments) {
			GirLogger.getLoger(GkCallerUtil.getCallerCallerName()).trace(t, format, arguments);
		}

	};

	/**
	 * Bean容器对象，提供对Spring Bean的访问能力 支持获取、注册和管理各种Bean实例
	 */
	public static final GiBeanFactory beans = new GiBeanFactory() {

		@Override
		public Object getBean(String name) throws GirBeanException {
			return GirBeanHelper.getProvider().getBean(name);
		}

		@Override
		public <T> T getBean(String name, Class<T> requiredType) throws GirBeanException {

			return GirBeanHelper.getProvider().getBean(name, requiredType);
		}

		@Override
		public Object getBean(String name, Object... args) throws GirBeanException {

			return GirBeanHelper.getProvider().getBean(name, args);
		}

		@Override
		public <T> T getBean(Class<T> requiredType, Object... args) throws GirBeanException {

			return GirBeanHelper.getProvider().getBean(requiredType, args);
		}

		@Override
		public <T> T getBean(Class<T> requiredType) throws GirBeanException {

			return GirBeanHelper.getProvider().getBean(requiredType);
		}

		@Override
		public <T> T getBean(Class<T> requiredType, Type[] genericType) throws GirBeanException {

			return GirBeanHelper.getProvider().getBean(requiredType, genericType);
		}

		@Override
		public <T> Map<String, T> getBeans(Class<T> clazz, Type[] genericType) throws GirBeanException {

			return GirBeanHelper.getProvider().getBeans(clazz, genericType);
		}

		@Override
		public <T> Map<String, T> getBeans(Class<T> clazz) throws GirBeanException {

			return GirBeanHelper.getProvider().getBeans(clazz);
		}

		@Override
		public boolean containsBean(String name) {

			return GirBeanHelper.getProvider().containsBean(name);
		}

		@Override
		public boolean isSingleton(String name) throws GirNoSuchBeanException {

			return GirBeanHelper.getProvider().isSingleton(name);
		}

		@Override
		public boolean isPrototype(String name) throws GirNoSuchBeanException {

			return GirBeanHelper.getProvider().isPrototype(name);
		}

		@Override
		public boolean isTypeMatch(String name, Class<?> typeToMatch) throws GirNoSuchBeanException {

			return GirBeanHelper.getProvider().isTypeMatch(name, typeToMatch);
		}

		@Override
		public Class<?> getType(String name) throws GirNoSuchBeanException {

			return GirBeanHelper.getProvider().getType(name);
		}

		@Override
		public String[] getAliases(String name) {

			return GirBeanHelper.getProvider().getAliases(name);
		}

		@Override
		public void register(String name, Class<?> beanClass) throws GirBeanDefinitionStoreException {

			GirBeanHelper.getProvider().register(name, beanClass);
		}

		@Override
		public void register(String name, Class<?> beanClass, boolean singleton)
				throws GirBeanDefinitionStoreException {

			GirBeanHelper.getProvider().register(name, beanClass, singleton);
		}

	};

	/**
	 * 打印对象到控制台（不换行）
	 * @param obj 要打印的主要对象
	 * @param otherObjs 其他要打印的对象
	 */
	public static void print(Object obj, Object... otherObjs) {
		GkConsole.print(obj, otherObjs);
	}

	/**
	 * 打印对象到控制台（带换行）
	 * @param obj 要打印的主要对象
	 * @param otherObjs 其他要打印的对象
	 */
	public static void println(Object obj, Object... otherObjs) {
		GkConsole.log(obj, otherObjs);
	}

	/**
	 * 以表格形式打印数据
	 * @param values 要打印的字符串值数组
	 */
	public static void printTable(String... values) {
		GkConsoleTable.create().addHeader(values).addBody(values).print();
	}

	/**
	 * 将对象转换为JSON格式
	 * @param json 要转换的对象
	 * @return GirJSON对象，可用于进一步操作
	 */
	public static GirJSON toJson(Object json) {
		return GirJSON.toJson(json);
	}

	public static void main(String[] args) {
		Gir.log.error(" gtc");
		Gir.println(new GirPageConfig(), "b", "c");
		Gir.printTable("a", "b", "c");
		GkConsole.where();
	}

}
