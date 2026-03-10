package cn.geoair.map.dynamic.dbservice.core.mapper.dbapi;

import cn.geoair.base.data.page.GfunPageExcute;
import cn.geoair.base.data.page.GiPageParam;
import cn.geoair.base.data.page.GiPager;
import cn.geoair.map.dynamic.dbservice.core.basic.domain.Group;
import cn.geoair.map.dynamic.dbservice.core.dao.dbapi.DbApiDataSourceDao;
import cn.geoair.map.dynamic.dbservice.core.model.dbapi.dto.DbApiDataSourceDto;
import cn.geoair.map.dynamic.dbservice.core.model.dbapi.entity.DbApiDataSourcePo;
import cn.geoair.map.dynamic.dbservice.core.model.dbapi.seo.DbApiDataSourceSeo;
import cn.geoair.orm.tkmapper.impls.TkEntityMapper;

import org.apache.ibatis.annotations.Param;

import tk.mybatis.mapper.entity.Example;

import java.util.List;

/**
 * 数据源信息Mapper接口
 *
 * @author zhangjun
 * @date 2025-07-31
 */
public interface DbApiDataSourceMapper
        extends DbApiDataSourceDao, TkEntityMapper<DbApiDataSourcePo, String> {

    @Override
    List<DbApiDataSourceDto> searchList(@Param("param") DbApiDataSourceSeo dbapiDatasourceSeo);

    default List<DbApiDataSourcePo> selectBatchIds(List<String> ids) {
        Example example = new Example(Group.class);
        example.and().andIn("id", ids);
        return selectByExample(example);
    }

    @Override
    default GiPager<DbApiDataSourceDto> searchListPage(
            @Param("param") DbApiDataSourceSeo dbapiDatasourceSeo, GiPageParam pageParam) {

        GfunPageExcute<DbApiDataSourceDto> exec =
                new GfunPageExcute<DbApiDataSourceDto>() {
                    @Override
                    public Iterable<DbApiDataSourceDto> excute() {
                        return searchList(dbapiDatasourceSeo);
                    }
                };

        return pageExcuter().excutePage(exec, pageParam);
    }
}
