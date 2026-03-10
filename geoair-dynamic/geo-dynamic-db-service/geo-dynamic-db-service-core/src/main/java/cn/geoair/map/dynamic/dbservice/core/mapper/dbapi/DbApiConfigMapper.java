package cn.geoair.map.dynamic.dbservice.core.mapper.dbapi;

import cn.geoair.base.data.page.GfunPageExcute;
import cn.geoair.base.data.page.GiPageParam;
import cn.geoair.base.data.page.GiPager;
import cn.geoair.map.dynamic.dbservice.core.dao.dbapi.DbApiConfigDao;
import cn.geoair.map.dynamic.dbservice.core.model.dbapi.dto.DbApiConfigDto;
import cn.geoair.map.dynamic.dbservice.core.model.dbapi.entity.DbApiConfigPo;
import cn.geoair.map.dynamic.dbservice.core.model.dbapi.seo.DbApiConfigSeo;
import cn.geoair.orm.tkmapper.impls.TkEntityMapper;

import org.apache.ibatis.annotations.Param;

import tk.mybatis.mapper.entity.Example;

import java.util.List;

/**
 * api配置信息Mapper接口
 *
 * @author zhangjun
 * @date 2025-07-31
 */
public interface DbApiConfigMapper extends DbApiConfigDao, TkEntityMapper<DbApiConfigPo, String> {

    @Override
    List<DbApiConfigDto> searchList(@Param("param") DbApiConfigSeo dbapiConfigSeo);

    default List<DbApiConfigPo> selectBatchIds(List<String> ids) {
        Example example = new Example(DbApiConfigPo.class);
        example.and().andIn("id", ids);
        return selectByExample(example);
    }

    DbApiConfigDto selectByPathOnline(String path);

    List<DbApiConfigDto> search(
            @Param("name") String name,
            @Param("note") String note,
            @Param("path") String path,
            @Param("groupId") String groupId);

    Integer selectCountByPath(String path);

    Integer selectCountByPathWhenUpdate(@Param("path") String path, @Param("id") String id);

    int selectCountByGroup(String id);

    List<DbApiConfigDto> selectByGroup(String groupId);

    @Override
    default GiPager<DbApiConfigDto> searchListPage(
            @Param("param") DbApiConfigSeo dbapiConfigSeo, GiPageParam pageParam) {

        GfunPageExcute<DbApiConfigDto> exec =
                new GfunPageExcute<DbApiConfigDto>() {
                    @Override
                    public Iterable<DbApiConfigDto> excute() {
                        return searchList(dbapiConfigSeo);
                    }
                };

        return pageExcuter().excutePage(exec, pageParam);
    }
}
