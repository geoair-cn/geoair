package cn.geoair.comp.code.generator.multi.domian;

import java.util.List;

import lombok.Data;

/**
 * 业务表 gen_table
 *
 * @author ray
 */
@Data
public class GenTable {

	private static final long serialVersionUID = 1L;

	/**
	 * 项目名称
	 */
	private String projectName;

	/**
	 * 表名称
	 */

	private String tableName;

	/**
	 * 表描述
	 */

	private String tableComment;

	/**
	 * 实体类名称(首字母大写)
	 */

	private String className;

	/**
	 * 生成包路径
	 */

	private String packageName;

	/**
	 * 生成模块名
	 */
	private String moduleName;

	/**
	 * 生成业务名
	 */
	private String businessName;

	/**
	 * 生成功能名
	 */
	private String functionName;

	/**
	 * 生成作者
	 */
	private String functionAuthor;

	/**
	 * 生成路径（不填默认项目路径）
	 */
	private String genPath;

	/**
	 * 主键信息
	 */
	private GenTableColumn pkColumn;

	private List<GenTableColumn> columns;

	/**
	 * 模板来源 (MC:MC,Ray:KLF)
	 */
	private String templateSource;

}
