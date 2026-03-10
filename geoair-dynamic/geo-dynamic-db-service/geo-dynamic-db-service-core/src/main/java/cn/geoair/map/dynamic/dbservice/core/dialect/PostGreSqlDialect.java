package cn.geoair.map.dynamic.dbservice.core.dialect;

import cn.hutool.core.util.StrUtil;
import cn.hutool.db.dialect.DriverNamePool;

import org.springframework.stereotype.Component;

/**
 * @author ：张俊
 * @date ：Created in 2025/8/5 09:31 @description： TODO
 */
@Component
public class PostGreSqlDialect implements BaseDialect {

	@Override
	public String getSupportDataBaseType() {
		return DriverNamePool.DRIVER_POSTGRESQL;
	}

	@Override
	public String getPageSql(String sql, int pageNum, int pageSize) {
		return StrUtil.format("select * from ( {}  ) template111  limit {} offset {}", sql, pageSize,
				pageSize * pageNum);
	}

	@Override
	public String getCountSql(String sql) {
		return StrUtil.format("select count(1) from ({}) as t", sql);
	}

}
