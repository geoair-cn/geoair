package cn.geoair.comp.db.service.core.dialect;

import cn.geoair.base.Gir;
import cn.geoair.base.util.GutilObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author ：zhangjun
 * @date ：Created in 2025/8/5 09:30 @description： TODO
 */
public interface BaseDialect {

	List<BaseDialect> instances = new ArrayList<BaseDialect>();

	static BaseDialect getInstance(String dataBaseType) {
		if (GutilObject.isEmpty(instances)) {
			Map<String, BaseDialect> beans = Gir.beans.getBeans(BaseDialect.class);
			for (BaseDialect dialect : beans.values()) {
				instances.add(dialect);
			}
		}
		for (BaseDialect dialect : instances) {
			if (dialect.getSupportDataBaseType().equals(dataBaseType)) {
				return dialect;
			}
		}
		return null;
	}

	String getSupportDataBaseType();

	String getPageSql(String sql, int pageNum, int pageSize);

	String getCountSql(String sql);

}
