package cn.geoair.comp.knife4j.ext.aspect;

import cn.geoair.gtc.base.data.page.GiPageParam;
import cn.geoair.gtc.base.data.page.GiPager;
import cn.geoair.gtc.base.data.page.support.GirPageParam;
import cn.geoair.gtc.base.data.result.GiResult;
import cn.geoair.gtc.base.util.GutilClass;
import cn.geoair.gtc.web.data.result.GiWebResult;
import cn.geoair.gtc.web.data.result.GirWebResult;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeBindings;
import com.fasterxml.classmate.TypeResolver;
import com.fasterxml.classmate.types.ResolvedInterfaceType;
import com.fasterxml.classmate.types.ResolvedObjectType;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.reflections.Reflections;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author ：张俊
 * @date ：Created in 2022/12/29 16:33 @description：
 * com.gtc.comp.knife4j.ext.aspect.TypeResolverAspect,\
 */
@Aspect
public class TypeResolverAspect implements ApplicationContextAware {

	// @Resource
	TypeResolver typeResolver;

	Map<String, ResolvedObjectType> resolvedTypeMap = new HashMap<>();

	Reflections reflections = null;

	private ResolvedObjectType getResolvedTypeOrLoad(Class<?> classa) {
		ResolvedObjectType resolvedObjectType = resolvedTypeMap.get(classa.getName());
		if (resolvedObjectType == null) {
			ResolvedType resolve = typeResolver.resolve(classa);
			if (resolve instanceof ResolvedObjectType) {
				resolvedTypeMap.put(classa.getName(), (ResolvedObjectType) resolve);
				return (ResolvedObjectType) resolve;
			}
			else {
				return null;
			}

		}
		return resolvedObjectType;
	}

	@Around("execution(* com.fasterxml.classmate.TypeResolver.resolve(..))")
	public Object typeResolverAspect(ProceedingJoinPoint joinPoint) throws Throwable {
		Object[] args = joinPoint.getArgs();
		Object proceed = joinPoint.proceed(args);
		if (proceed instanceof ResolvedInterfaceType) {
			ResolvedInterfaceType resolvedInterfaceType = (ResolvedInterfaceType) proceed;
			Class<?> erasedType = resolvedInterfaceType.getErasedType();
			if (GutilClass.isAssignable(erasedType, GiResult.class)) {
				// Class<? extends GiWebResult> aClass =
				// GiWebResult.getResult(null).getClass();
				Class<? extends GiWebResult> aClass = GirWebResult.class;
				ResolvedObjectType resolve = getResolvedTypeOrLoad(aClass);
				TypeBindings typeBindings = resolvedInterfaceType.getTypeBindings();
				resolve = setSuperClassTypeBind(resolve, typeBindings);
				return ResolvedObjectType.create(aClass, ((ResolvedInterfaceType) proceed).getTypeBindings()
				// , ((ResolvedInterfaceType) proceed).getParentClass()
						, resolve.getParentClass(), resolve.getImplementedInterfaces());
			}
			if (GutilClass.isAssignable(erasedType, GiPager.class)) {
				Class<? extends GiPager> aClass = GiPager.ofClass(null).getClass();
				ResolvedObjectType resolve = getResolvedTypeOrLoad(aClass);
				return ResolvedObjectType.create(aClass, ((ResolvedInterfaceType) proceed).getTypeBindings(),
						((ResolvedInterfaceType) proceed).getParentClass(), resolve.getImplementedInterfaces());
			}

			if (GutilClass.isAssignable(erasedType, GiPageParam.class)) {
				ResolvedObjectType resolve = getResolvedTypeOrLoad(GirPageParam.class);
				return ResolvedObjectType.create(GirPageParam.class,
						((ResolvedInterfaceType) proceed).getTypeBindings(),
						((ResolvedInterfaceType) proceed).getParentClass(), resolve.getImplementedInterfaces());
			}

		}

		return proceed;
	}

	/**
	 * 获取 GiResult 的子类
	 * @return
	 */
	private Class<?> getGiResultSubClass() {
		Set<Class<? extends GiResult>> subTypesOf = reflections.getSubTypesOf(GiResult.class);
		// Set<Class<? extends GiResult>>

		for (Class<? extends GiResult> aClass : subTypesOf) {
			GutilClass.isNormalClass(aClass);
		}
		return null;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		typeResolver = applicationContext.getBean(TypeResolver.class);
		reflections = applicationContext.getBean(Reflections.class);
	}

	/**
	 * 设置父类的泛型
	 */
	private ResolvedObjectType setSuperClassTypeBind(ResolvedObjectType resolvedObjectType, TypeBindings typeBindings) {
		ResolvedObjectType parentClass = resolvedObjectType.getParentClass();
		if (parentClass == null)
			return resolvedObjectType;
		if (GutilClass.isNormalClass(parentClass.getErasedType())) {
			ResolvedObjectType newparentClass = ResolvedObjectType.create(parentClass.getErasedType(), typeBindings,
					parentClass.getParentClass(), parentClass.getImplementedInterfaces());
			// 递归设置该父类的父类的泛型，直到到达getParentClass（）的值为null为止
			ResolvedObjectType newNewParentClass = setSuperClassTypeBind(newparentClass, typeBindings);
			return ResolvedObjectType.create(resolvedObjectType.getErasedType(), typeBindings, newNewParentClass,
					resolvedObjectType.getImplementedInterfaces());
		}
		return resolvedObjectType;
	}

}
