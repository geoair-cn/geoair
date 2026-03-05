package cn.geoair.comp.code.generator.multi.config;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author ：张逢吉
 * @date ：Created in 13:07 @description： TODO
 */
@Data
@Accessors(chain = true)
public class GirGeneratorConfig {

	/**
	 * 源代码生成路径
	 */
	String sourceRootPath = "/";

	public String getSourceRootPath() {
		if (StrUtil.isEmpty(sourceRootPath)) {
			return "/";
		}
		return sourceRootPath;
	}

	OrmType ormType = OrmType.TKMAPPER;

	/**
	 * 代码的包名的根
	 */
	String sourceRootPackage = "";

	/**
	 * 模块名称
	 */
	String moduleName = "";

	/**
	 * 项目名称
	 */
	String projectName = "";

	/**
	 * 生成作者
	 */
	private String author = "geoair";

	/**
	 * 自动去除表前缀，默认是false
	 */
	public boolean removePre;

	/**
	 * 表前缀(类名不会包含表前缀)
	 */
	public String tablePrefix;

	/**
	 * 是否使用springCache生成代码
	 */
	public Boolean springCacheUse = true;

	/**
	 * 是否多模块
	 */
	private Boolean mutiIs = true;

}
