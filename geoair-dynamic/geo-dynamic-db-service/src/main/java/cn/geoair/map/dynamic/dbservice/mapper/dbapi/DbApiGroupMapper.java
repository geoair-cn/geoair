package cn.geoair.map.dynamic.dbservice.mapper.dbapi;

import cn.geoair.base.data.page.GfunPageExcute;
import cn.geoair.base.data.page.GiPageParam;
import cn.geoair.base.data.page.GiPager;
import cn.geoair.map.dynamic.dbservice.dao.dbapi.DbApiGroupDao;
import cn.geoair.map.dynamic.dbservice.model.dbapi.dto.DbApiGroupDto;
import cn.geoair.map.dynamic.dbservice.model.dbapi.entity.DbApiGroupPo;
import cn.geoair.map.dynamic.dbservice.model.dbapi.seo.DbApiGroupSeo;
import cn.geoair.orm.tkmapper.impls.TkEntityMapper;

import org.apache.ibatis.annotations.Param;

import tk.mybatis.mapper.entity.Example;

import java.util.List;

/**
 * api分组信息Mapper接口
 *
 * @author zhangjun
 * @date 2025-07-31
 */
public interface DbApiGroupMapper extends DbApiGroupDao, TkEntityMapper<DbApiGroupPo, String> {
    @Override
    List<DbApiGroupDto> searchList(@Param("param") DbApiGroupSeo dbapiGroupSeo);

    default List<DbApiGroupPo> selectBatchIds(List<String> ids) {
        Example example = new Example(DbApiGroupPo.class);
        example.and().andIn("id", ids);
        return selectByExample(example);
    }

    @Override
    default GiPager<DbApiGroupDto> searchListPage(
            @Param("param") DbApiGroupSeo dbapiGroupSeo, GiPageParam pageParam) {

        GfunPageExcute<DbApiGroupDto> exec =
                new GfunPageExcute<DbApiGroupDto>() {
                    @Override
                    public Iterable<DbApiGroupDto> excute() {
                        return searchList(dbapiGroupSeo);
                    }
                };

        return pageExcuter().excutePage(exec, pageParam);
    }
}
