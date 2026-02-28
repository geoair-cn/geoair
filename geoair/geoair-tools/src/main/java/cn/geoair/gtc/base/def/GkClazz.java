package cn.geoair.gtc.base.def;

/**
 * 用来声明的类
 *
 * @author Ray
 *
 */
public class GkClazz {

	private Class<?> target;

	private String className;

	private GkClazz(String className) {
		this.className = className;
	}

	public static GkClazz forName2(String className) {
		return new GkClazz(className);
	}

	public Class<?> getTargetClass() {
		return target;
	}

	public String getClassName() {
		return className;
	}

}
