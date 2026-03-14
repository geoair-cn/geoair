package cn.geoair.map.dynamic.dbservice.core.typehander.oracle;

import cn.geoair.map.dynamic.dbservice.core.typehander.BlobTypeHandler;
import cn.hutool.db.Entity;

import java.sql.ResultSet;
import java.util.Map;

/**
 * @author ：张俊
 * @date ：Created in 2024/4/18 17:46 @description： oracle空间字段的解析
 */
public class OracleBlobTypeHandler extends BlobTypeHandler {

	@Override
	public String getResult(Entity entity, String columnName) {

		return String.valueOf("(OracleBlob)");
	}

	@Override
	public String getResult(ResultSet resultSet, String columnName) {
		return String.valueOf("(OracleBlob)");
	}

	@Override
	public String getResult(ResultSet resultSet, Integer columnIndex) {
		return String.valueOf("(OracleBlob)");
	}

	@Override
	public String getResult(Map<String, Object> row, String columnName) {
		return String.valueOf("(OracleBlob)");
	}

	@Override
	public String getResult(Object obj) {
		return String.valueOf("(OracleBlob)");
	}

}
