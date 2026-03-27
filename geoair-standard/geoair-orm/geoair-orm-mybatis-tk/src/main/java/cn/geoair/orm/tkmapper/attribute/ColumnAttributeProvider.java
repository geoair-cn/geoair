package cn.geoair.orm.tkmapper.attribute;

import org.apache.ibatis.mapping.MappedStatement;
import tk.mybatis.mapper.mapperhelper.MapperHelper;
import tk.mybatis.mapper.mapperhelper.MapperTemplate;

public class ColumnAttributeProvider extends MapperTemplate {

    public ColumnAttributeProvider(Class<?> mapperClass, MapperHelper mapperHelper) {
        super(mapperClass, mapperHelper);
    }

    public String getgtcAttribute(MappedStatement ms) {
        return null;
    }

    public String getgtcAttributes(MappedStatement ms) {
        return null;
    }

    public String setgtcAttribute(MappedStatement ms) {
        /*
         * Class<?> entityClass = getEntityClass(ms); StringBuilder sql = new
         * StringBuilder(); sql.append(SqlHelper.updateTable(entityClass,
         * tableName(entityClass))); sql.append(SqlHelper.updateSetColumns(entityClass,
         * null, true, isNotEmpty())); sql.append(SqlHelper.wherePKColumns(entityClass,
         * true)); return sql.toString();
         */
        return null;
    }

    public String setgtcAttributes(MappedStatement ms) {
        return null;
    }
}
