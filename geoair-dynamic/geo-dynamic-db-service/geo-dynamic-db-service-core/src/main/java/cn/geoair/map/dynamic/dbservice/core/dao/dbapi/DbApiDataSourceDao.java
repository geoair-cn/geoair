package cn.geoair.map.dynamic.dbservice.core.dao.dbapi;

import cn.geoair.base.data.page.GiPageParam;
import cn.geoair.base.data.page.GiPager;
import cn.geoair.base.gpa.dao.GiEntityDao;
import cn.geoair.map.dynamic.dbservice.core.model.dbapi.dto.DbApiDataSourceDto;
import cn.geoair.map.dynamic.dbservice.core.model.dbapi.entity.DbApiDataSourcePo;
import cn.geoair.map.dynamic.dbservice.core.model.dbapi.seo.DbApiDataSourceSeo;

import java.util.List;

/**
 * 数据源信息Dao接口
 *
 * @author zhangjun
 * @date 2025-07-31
 */
public interface DbApiDataSourceDao extends GiEntityDao<DbApiDataSourcePo, String> {

    List<DbApiDataSourceDto> searchList(DbApiDataSourceSeo dbapiDatasourceSeo);

    List<DbApiDataSourcePo> selectBatchIds(List<String> ids);

    GiPager<DbApiDataSourceDto> searchListPage(
            DbApiDataSourceSeo dbapiDatasourceSeo, GiPageParam pageParam);
}
