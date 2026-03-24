package cn.geoair.comp.knife4j.ext.springfox.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.springframework.util.StringUtils;

import cn.geoair.comp.knife4j.ext.core.model.ApiModelInfo;
import cn.geoair.comp.knife4j.ext.core.model.DocketInfo;
import cn.geoair.comp.knife4j.ext.springfox.model.SpringAddtionalModel;
import cn.geoair.comp.knife4j.ext.springfox.service.SpringAddtionalModelUtils;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

/**
 * @author ：张俊
 * @date ：Created in 2022/8/23 16:15 @description： 创建Docket 工具类
 */
public class CreateApiUtil {

	public static Docket createGroup(ApiModelInfo apiModelInfo, DocketInfo docketInfo) {

		ApiInfo apiInfo = new ApiInfoBuilder().title(apiModelInfo.getTitle())
				.contact(new Contact(apiModelInfo.getAuthor(), "", "")).termsOfServiceUrl("/apidoc.html")
				.description(apiModelInfo.getDescription()).version(apiModelInfo.getVersion()).build();
		SpringAddtionalModel springAddtionalModel = null;
		if (docketInfo.getModelscan() != null && !docketInfo.getModelscan().equals("")
				&& (docketInfo.getModelClassList() == null || docketInfo.getModelClassList().equals(""))) {
			// 如果存在扫描参数 modelscan 则添加扫描additionalModels
			springAddtionalModel = SpringAddtionalModelUtils.scan(docketInfo.getModelscan());
		}
		else if (docketInfo.getModelClassList() != null && !docketInfo.getModelClassList().equals("")
				&& (docketInfo.getModelscan() == null && docketInfo.getModelscan().equals(""))) {
			springAddtionalModel = SpringAddtionalModelUtils.listClass(docketInfo.getModelClassList());
		}
		else if (docketInfo.getModelClassList() != null && !docketInfo.getModelClassList().equals("")
				&& docketInfo.getModelscan() != null && !docketInfo.getModelscan().equals("")) {
			springAddtionalModel = SpringAddtionalModelUtils.scanAndClassList(docketInfo.getModelClassList(),
					docketInfo.getModelscan());
		}
		Predicate<String> specifyScan = getPathSelectors(docketInfo.getSpecifyScan());
		if (springAddtionalModel == null) {
			return new Docket(DocumentationType.SWAGGER_2).apiInfo(apiInfo).groupName(docketInfo.getGroupName())
					.useDefaultResponseMessages(false).select()
					.apis(RequestHandlerSelectors.basePackage(docketInfo.getBasePackage())).paths(specifyScan).build();
		}
		else {
			return new Docket(DocumentationType.SWAGGER_2).apiInfo(apiInfo).groupName(docketInfo.getGroupName())
					.useDefaultResponseMessages(false)
					.additionalModels(springAddtionalModel.getFirst(), springAddtionalModel.getRemaining()).select()
					.apis(RequestHandlerSelectors.basePackage(docketInfo.getBasePackage())).paths(specifyScan).build();
		}
	}

	private static Predicate<String> getPathSelectors(String specifyScan) {
		if (StringUtils.isEmpty(specifyScan)) {
			// 任何路径都满足这个条件
			return PathSelectors.any();
		}
		// 如果不为空 逗号分隔取出每个指定action
		String[] regex = specifyScan.split(",", -1);
		List<Predicate<String>> predicates = new ArrayList<>();
		for (int i = 0; i < regex.length; i++) {
			predicates.add(PathSelectors.regex("." + regex[i] + ".*"));
		}
		return combinePredicates(predicates);
	}

	private static Predicate<String> combinePredicates(List<Predicate<String>> predicates) {
		// 空列表则返回"不匹配任何路径"
		if (predicates.isEmpty()) {
			return PathSelectors.none();
		}

		// 初始值为第一个Predicate，后续依次用or()组合
		Predicate<String> combined = predicates.get(0);
		for (int i = 1; i < predicates.size(); i++) {
			combined = combined.or(predicates.get(i));
		}
		return combined;
	}

}
