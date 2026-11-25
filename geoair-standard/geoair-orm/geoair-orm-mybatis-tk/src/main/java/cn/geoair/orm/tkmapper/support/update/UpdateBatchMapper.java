package cn.geoair.orm.tkmapper.support.update;

import org.apache.ibatis.annotations.UpdateProvider;

import tk.mybatis.mapper.annotation.RegisterMapper;

import java.util.List;

/**
 * @author ：zhangjun
 * @date ：Created in 2022/6/21 16:12 @description： TODO
 */
@RegisterMapper
public interface UpdateBatchMapper<T> {

    /** 根据主键选择性批量更新(一次发送多条update语句),mysql数据库url需要设置&allowMultiQueries=true */
    @UpdateProvider(type = UpdateBatchProvider.class, method = "dynamicSQL")
    int batchUpdateByPKSelective(List<T> recordList);

    @UpdateProvider(type = UpdateBatchProvider.class, method = "dynamicSQL")
    int batchUpdateByPK(List<T> recordList);
}
