package cn.geoair.gtc.base.data.page;

import java.lang.reflect.Type;
import java.util.List;

import cn.geoair.gtc.base.util.GutilClass;
import cn.geoair.gtc.base.util.GutilGenericType;

/**
 * @author Ray
 **/
@FunctionalInterface
public interface GfunParamPageExcute<P, R> {

	List<R> excute(P p);

	@SuppressWarnings("unchecked")
	public default Class<R> getReturnClass() {
		Class<?> myClass = this.getClass();
		if (!myClass.getName().contains(GutilClass.LAMBDA_CLASS_SIGN)) {
			myClass = GutilClass.getUserClass(this);
		}
		Type[] types = GutilGenericType.resolveTypeArguments(myClass, GfunParamPageExcute.class);
		if (types != null && types.length > 1) {
			Type type = types[1];
			if (type instanceof Class) {
				return (Class<R>) type;
			}
		}
		return null;
	}

}
