package cn.geoair.orm.tkmapper.support.insert;

import org.apache.ibatis.annotations.InsertProvider;

import tk.mybatis.mapper.annotation.RegisterMapper;

/**
 * tkmapper 原本的InserMapper的KeySql存在缺陷，改写一下
 *
 * @author ray
 * @param <T>
 */
@RegisterMapper
public interface GirInsertMapper<T> {

    /**
     * 保存一个实体，null的属性也会保存，不会使用数据库默认值
     *
     * @param record
     * @return
     */
    @InsertProvider(type = GirInsertProvider.class, method = "dynamicSQL")
    int insert(T record);

    /**
     * 保存一个实体，null的属性不会保存，会使用数据库默认值
     *
     * @param record
     * @return
     */
    @InsertProvider(type = GirInsertProvider.class, method = "dynamicSQL")
    int insertSelective(T record);
}
