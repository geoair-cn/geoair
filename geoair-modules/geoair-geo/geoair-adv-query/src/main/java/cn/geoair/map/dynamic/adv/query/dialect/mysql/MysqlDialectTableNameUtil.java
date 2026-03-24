package cn.geoair.map.dynamic.adv.query.dialect.mysql;

import cn.geoair.map.dynamic.adv.query.dialect.AbstractDialectTableNameUtil;

import cn.hutool.core.util.StrUtil;

/**
 * MySQL方言表名处理器 仅实现MySQL专属的差异化逻辑
 */
public class MysqlDialectTableNameUtil extends AbstractDialectTableNameUtil {

	// 单例实例
	private static final MysqlDialectTableNameUtil INSTANCE = new MysqlDialectTableNameUtil();

	// MySQL专属常量
	private static final String MYSQL_DEFAULT_DATABASE = ""; // MySQL无默认库名

	private static final String MYSQL_QUOTE_CHAR = "`";

	private static final String FIELD_QUOTE_PREFIX = "`";

	private static final String FIELD_QUOTE_SUFFIX = "`";

	public static MysqlDialectTableNameUtil getInstance() {
		return INSTANCE;
	}

	// ========== 实现差异化抽象方法 ==========
	@Override
	protected String getQuoteChar() {
		return MYSQL_QUOTE_CHAR;
	}

	@Override
	protected String getDefaultSchemaName() {
		return MYSQL_DEFAULT_DATABASE;
	}

	@Override
	public String tbQuoteFieldName(String fieldName) {
		if (StrUtil.isEmpty(fieldName)) {
			return fieldName;
		}
		if (fieldName.startsWith(FIELD_QUOTE_PREFIX) && fieldName.endsWith(FIELD_QUOTE_SUFFIX)) {
			return fieldName;
		}
		return FIELD_QUOTE_PREFIX + fieldName + FIELD_QUOTE_SUFFIX;
	}

}
