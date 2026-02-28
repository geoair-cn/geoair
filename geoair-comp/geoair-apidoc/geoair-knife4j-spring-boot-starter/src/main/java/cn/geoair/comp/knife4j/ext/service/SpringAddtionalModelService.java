package cn.geoair.comp.knife4j.ext.service;

import com.fasterxml.classmate.TypeResolver;
import com.github.xiaoymin.knife4j.core.io.ResourceUtil;
import cn.geoair.comp.knife4j.ext.model.SpringAddtionalModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * @author ：张俊
 * @date ：Created in 2022/8/23 16:16 @description： controller 扫描服务类
 */

@Component
public class SpringAddtionalModelService {

	@Autowired
	private TypeResolver typeResolver;

	/***
	 * 扫描包,获取对象
	 * @param basePackage 扫描包路径
	 * @return SpringAddtionModel实例
	 */
	public SpringAddtionalModel scan(String... basePackage) {
		if (basePackage == null || basePackage.length == 0) {
			throw new IllegalArgumentException("basePackage can't be empty!!!");
		}
		SpringAddtionalModel springAddtionalModel = new SpringAddtionalModel();
		ResourceUtil resourceUtil = new ResourceUtil();
		Set<Class<?>> classSets = resourceUtil.find(basePackage).getClasses();
		if (classSets == null || classSets.isEmpty()) {
			throw new IllegalArgumentException("can't find any Models in basePackage");
		}
		int a = 0;
		for (Class<?> clazz : classSets) {
			if (a == 0) {
				springAddtionalModel.setFirst(typeResolver.resolve(clazz));
			}
			else {
				springAddtionalModel.add(typeResolver.resolve(clazz));
			}
			a++;
		}
		return springAddtionalModel;
	}

	/***
	 * 根据对象集合组成SpringAddtionModel
	 * @param modelClassList clss集合
	 * @return SpringAddtionModel实例
	 */
	public SpringAddtionalModel listClass(List<Class> modelClassList) {
		if (modelClassList == null || modelClassList.size() == 0) {
			throw new IllegalArgumentException("modelClassList can't be empty!!!");
		}
		SpringAddtionalModel springAddtionalModel = new SpringAddtionalModel();
		int a = 0;
		for (Class<?> clazz : modelClassList) {
			if (a == 0) {
				springAddtionalModel.setFirst(typeResolver.resolve(clazz));
			}
			else {
				springAddtionalModel.add(typeResolver.resolve(clazz));
			}
			a++;
		}
		return springAddtionalModel;
	}

	/***
	 * 根据对象集合组成SpringAddtionModel
	 * @param modelClassList clss集合
	 * @return SpringAddtionModel实例
	 */
	public SpringAddtionalModel scanAndClassList(List<Class> modelClassList, String... basePackage) {

		if (basePackage == null || basePackage.length == 0) {
			throw new IllegalArgumentException("basePackage can't be empty!!!");
		}
		if (modelClassList == null || modelClassList.size() == 0) {
			throw new IllegalArgumentException("modelClassList can't be empty!!!");
		}
		SpringAddtionalModel springAddtionalModel = new SpringAddtionalModel();
		ResourceUtil resourceUtil = new ResourceUtil();
		resourceUtil.find(basePackage);
		Set<Class<?>> classSets = resourceUtil.getClasses();
		if (classSets == null || classSets.isEmpty()) {
			throw new IllegalArgumentException("can't find any Models in basePackage");
		}
		int a = 0;
		for (Class<?> clazz : classSets) {
			if (a == 0) {
				springAddtionalModel.setFirst(typeResolver.resolve(clazz));
			}
			else {
				springAddtionalModel.add(typeResolver.resolve(clazz));
			}
			a++;
		}
		for (Class<?> clazz : modelClassList) {
			springAddtionalModel.add(typeResolver.resolve(clazz));
		}
		return springAddtionalModel;
	}

}
