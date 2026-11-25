package cn.geoair.orm.tkmapper.support.update;

import org.apache.ibatis.mapping.MappedStatement;

import tk.mybatis.mapper.entity.EntityColumn;
import tk.mybatis.mapper.mapperhelper.EntityHelper;
import tk.mybatis.mapper.mapperhelper.MapperHelper;
import tk.mybatis.mapper.mapperhelper.MapperTemplate;
import tk.mybatis.mapper.mapperhelper.SqlHelper;

import java.util.Set;

/**
 * @author ：张俊
 * @date ：Created in 2022/6/21 16:12 @description： 批量更新实现类
 */
public class UpdateBatchProvider extends MapperTemplate {

    public UpdateBatchProvider(Class<?> mapperClass, MapperHelper mapperHelper) {
        super(mapperClass, mapperHelper);
    }

    /**
     * 根据主键选择性批量更新 <foreach collection="list" item="record" separator=";"> update t_product_base
     * <set> <if test="record.productSku != null and record.productSku != ''"> product_sku =
     * #{record.productSku,jdbcType=VARCHAR}, </if> </set> where id = #{record.id,jdbcType=INTEGER}
     * </foreach>
     */
    public String batchUpdateByPKSelective(MappedStatement ms) {
        final Class<?> entityClass = getEntityClass(ms);
        // 开始拼sql
        StringBuilder sql = new StringBuilder();
        // 循环开始
        sql.append("<foreach collection=\"list\" item=\"record\" separator=\";\">");
        // update语句
        sql.append(SqlHelper.updateTable(entityClass, tableName(entityClass)));
        // set语句
        sql.append(SqlHelper.updateSetColumns(entityClass, "record", true, isNotEmpty()));
        // where语句
        sql.append("<where>");
        // 获取全部列
        Set<EntityColumn> columnList = EntityHelper.getPKColumns(entityClass);
        // 当某个列有主键策略时，不需要考虑他的属性是否为空，因为如果为空，一定会根据主键策略给他生成一个值
        for (EntityColumn column : columnList) {
            // if (column.isId() && column.isUpdatable()) {
            if (column.isId() /* && column.isUpdatable() */) {
                sql.append(" and " + column.getColumnEqualsHolder("record"));
                // break;
            }
        }
        sql.append("</where>");
        // 循环结束
        sql.append("</foreach>");
        return sql.toString();
    }

    public String batchUpdateByPK(MappedStatement ms) {
        final Class<?> entityClass = getEntityClass(ms);
        // 开始拼sql
        StringBuilder sql = new StringBuilder();
        // 循环开始
        sql.append("<foreach collection=\"list\" item=\"record\" separator=\";\">");
        // update语句
        sql.append(SqlHelper.updateTable(entityClass, tableName(entityClass)));
        // set语句
        sql.append(SqlHelper.updateSetColumns(entityClass, "record", false, isNotEmpty()));
        // where语句
        sql.append("<where>");
        // 获取全部列
        Set<EntityColumn> columnList = EntityHelper.getPKColumns(entityClass);
        // 当某个列有主键策略时，不需要考虑他的属性是否为空，因为如果为空，一定会根据主键策略给他生成一个值
        for (EntityColumn column : columnList) {
            // if (column.isId() && column.isUpdatable()) {
            if (column.isId() /* && column.isUpdatable() */) {
                sql.append(" and " + column.getColumnEqualsHolder("record"));
                // break;
            }
        }
        sql.append("</where>");
        // 循环结束
        sql.append("</foreach>");
        return sql.toString();
    }
}
