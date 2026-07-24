package cn.geoair.comp.code.generator.multi.db;

import cn.geoair.comp.code.generator.multi.domian.GenTable;
import cn.geoair.comp.code.generator.multi.domian.GenTableColumn;
import java.util.List;

/**
 * @author ：张逢吉
 * @date ：Created in 13:24 @description： 数据库配置获取器
 */
public interface ICommonRunner {

    List<GenTableColumn> getTableColumnsByTableName(String tableName);

    List<GenTable> selectDbTableListByNames(String[] tableNames);
}
