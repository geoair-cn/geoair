package cn.geoair.map.dynamic.dbservice.core.dao.dbapi;

import cn.geoair.base.data.page.GiPageParam;
import cn.geoair.base.data.page.GiPager;
import cn.geoair.base.gpa.dao.GiEntityDao;
import cn.geoair.map.dynamic.dbservice.core.model.dbapi.dto.DbApiGroupDto;
import cn.geoair.map.dynamic.dbservice.core.model.dbapi.entity.DbApiGroupPo;
import cn.geoair.map.dynamic.dbservice.core.model.dbapi.seo.DbApiGroupSeo;

import java.util.List;

/**
 * api分组信息Dao接口
 *
 * @author zhangjun
 * @date 2025-07-31
 */
public interface DbApiGroupDao extends GiEntityDao<DbApiGroupPo, String> {

    List<DbApiGroupDto> searchList(DbApiGroupSeo dbapiGroupSeo);

    List<DbApiGroupPo> selectBatchIds(List<String> ids);

    GiPager<DbApiGroupDto> searchListPage(DbApiGroupSeo dbapiGroupSeo, GiPageParam pageParam);
}
