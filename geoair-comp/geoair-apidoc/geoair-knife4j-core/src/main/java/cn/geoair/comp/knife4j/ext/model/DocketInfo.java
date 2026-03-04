package cn.geoair.comp.knife4j.ext.model;

import java.util.List;

/**
 * @author ：张俊
 * @date ：Created in 2022/8/23 16:14 @description： Docket 收集模型
 */
public class DocketInfo {

	public static final DocketInfo DEFAULT;

	private final String groupName;

	private final String basePackage;

	// .paths(PathSelectors.ant(“/sys/**”));
	private String specifyScan;

	private String modelscan;

	private List<Class> modelClassList;

	/**
	 * 基础实现
	 * @param groupName
	 * @param basePackage
	 */
	public DocketInfo(String groupName, String basePackage) {
		this.groupName = groupName;
		this.basePackage = basePackage;
		this.specifyScan = "";
	}

	/**
	 * 基础实现
	 * @param groupName
	 * @param basePackage
	 * @param specifyScan 指定扫描某些controller 过滤 多个用逗号分隔 例如：api/task,api/layer
	 */
	public DocketInfo(String groupName, String basePackage, String specifyScan) {
		this.groupName = groupName;
		this.basePackage = basePackage;
		this.specifyScan = specifyScan;
	}

	/**
	 * 增强实现
	 * @param groupName
	 * @param basePackage
	 * @param specifyScan 指定的扫描controller 过滤 多个用逗号分隔 例如：/task,/layer
	 * @param modelscan 扫描bean model 例如：com.ktw.api.model
	 */
	public DocketInfo(String groupName, String basePackage, String specifyScan, String modelscan) {
		this.groupName = groupName;
		this.basePackage = basePackage;
		this.specifyScan = specifyScan;
		this.modelscan = modelscan;
	}

	/**
	 * 增强实现
	 * @param groupName
	 * @param basePackage
	 * @param specifyScan 指定的扫描controller 过滤 多个用逗号分隔 例如：/task,/layer
	 * @param modelClassList model Class集合
	 */
	public DocketInfo(String groupName, String basePackage, String specifyScan, List<Class> modelClassList) {
		this.groupName = groupName;
		this.basePackage = basePackage;
		this.specifyScan = specifyScan;
		this.modelClassList = modelClassList;
	}

	/**
	 * 增强实现
	 * @param groupName
	 * @param basePackage
	 * @param specifyScan 指定的扫描controller 过滤 多个用逗号分隔 例如：/task,/layer
	 * @param modelscan 扫描bean model 例如：com.ktw.api.model
	 * @param modelClassList model Class集合
	 */
	public DocketInfo(String groupName, String basePackage, String specifyScan, String modelscan,
			List<Class> modelClassList) {
		this.groupName = groupName;
		this.basePackage = basePackage;
		this.specifyScan = specifyScan;
		this.modelscan = modelscan;
		this.modelClassList = modelClassList;
	}

	public String getGroupName() {
		return this.groupName;
	}

	public String getBasePackage() {
		return this.basePackage;
	}

	public String getModelscan() {
		return this.modelscan;
	}

	public String getSpecifyScan() {
		return this.specifyScan;
	}

	public List<Class> getModelClassList() {
		return this.modelClassList;
	}

	static {
		DEFAULT = new DocketInfo("geoair", "cn.geoair.apidoc.controller", "");
	}

}
