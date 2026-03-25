package cn.geoair.comp.db.service.core.dao;

import cn.geoair.comp.db.service.core.basic.apo.ApiConfigApo;
import java.util.List;

public interface GirDsApiConfigDao {

    List<ApiConfigApo> selectBatchIds(List<String> ids);

    ApiConfigApo selectByPathOnline(String path);

    List<ApiConfigApo> search(String name, String note, String path, String groupId);

    Integer selectCountByPath(String path);

    Integer selectCountByPathWhenUpdate(String path, String id);

    int selectCountByGroup(String id);

    List<ApiConfigApo> selectByGroup(String groupId);

    List<ApiConfigApo> searchAll();

    void accessSelective(ApiConfigApo t);

    void updateSelectiveById(ApiConfigApo t);

    void deleteById(String id);

    ApiConfigApo getById(String id);
}
