package cn.geoair.map.dynamic.dbservice.dao.dbapi;

import cn.geoair.base.data.page.GiPageParam;
import cn.geoair.base.data.page.GiPager;
import cn.geoair.base.gpa.dao.GiEntityDao;
import cn.geoair.map.dynamic.dbservice.model.dbapi.dto.DbApiConfigDto;
import cn.geoair.map.dynamic.dbservice.model.dbapi.entity.DbApiConfigPo;
import cn.geoair.map.dynamic.dbservice.model.dbapi.seo.DbApiConfigSeo;

import java.util.List;

/**
 * api配置信息Dao接口
 *
 * @author zhangjun
 * @date 2025-07-31
 */
public interface DbApiConfigDao extends GiEntityDao<DbApiConfigPo, String> {
    List<DbApiConfigDto> searchList(DbApiConfigSeo dbapiConfigSeo);

    List<DbApiConfigPo> selectBatchIds(List<String> ids);

    DbApiConfigDto selectByPathOnline(String path);

    List<DbApiConfigDto> search(String name, String note, String path, String groupId);

    Integer selectCountByPath(String path);

    Integer selectCountByPathWhenUpdate(String path, String id);

    int selectCountByGroup(String id);

    List<DbApiConfigDto> selectByGroup(String groupId);

    GiPager<DbApiConfigDto> searchListPage(DbApiConfigSeo dbapiConfigSeo, GiPageParam pageParam);
}
