package cn.geoair.map.dynamic.adv.query.dialect.mysql.base;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.stream.Collectors;

import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvBaseAccessOpt;
import cn.geoair.map.dynamic.adv.query.dialect.mysql.MysqlDialectTableNameUtil;

import cn.hutool.core.util.StrUtil;

/**
 * MySQL插入操作实现类 仅实现MySQL专属的差异化语法，复用父类所有通用逻辑
 */
public class MysqlAdvBaseAccessOpt extends AbstractExecAdvBaseAccessOpt {

	public MysqlAdvBaseAccessOpt() {
		// 绑定MySQL专属的表名处理器
		this.dialectTableNameProcessor = MysqlDialectTableNameUtil.getInstance();
	}

	// MySQL默认主键字段
	private static final String MYSQL_DEFAULT_PRIMARY_KEY = "id";

	// ========== 实现差异化抽象方法 ==========
	@Override
	protected String buildInsertReturnIdSql(String tableName, String fields, String placeholders) {
		// MySQL：基础INSERT语法（主键返回通过PreparedStatement获取）
		return buildInsertSql(tableName, fields, placeholders);
	}

	@Override
	protected Long executeInsertReturnId(Connection connection, String execSql, Object... params) throws SQLException {
		// MySQL：通过PreparedStatement.getGeneratedKeys()获取自增主键
		try (PreparedStatement pstmt = connection.prepareStatement(execSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
			// 设置参数
			for (int i = 0; i < params.length; i++) {
				pstmt.setObject(i + 1, params[i]);
			}
			pstmt.executeUpdate();

			// 获取主键
			try (ResultSet rs = pstmt.getGeneratedKeys()) {
				if (rs.next()) {
					return rs.getLong(1);
				}
				else {
					throw new SQLException("插入成功但未获取到主键");
				}
			}
		}
	}

	@Override
	protected String buildInsertIgnoreSql(String tableName, String fields, String placeholders) {
		return StrUtil.format("INSERT IGNORE INTO {} ({}) VALUES ({})", tableName, fields, placeholders);
	}

	@Override
	protected String buildInsertOrUpdateSql(String tableName, String fields, String placeholders,
			Set<String> updateFields) {
		// MySQL：ON DUPLICATE KEY UPDATE 语法
		String updateClause = updateFields.stream().map(field -> StrUtil.format("{} = VALUES({})", field, field))
				.collect(Collectors.joining(","));

		return StrUtil.format("INSERT INTO {} ({}) VALUES ({}) ON DUPLICATE KEY UPDATE {}", tableName, fields,
				placeholders, updateClause);
	}

}
