package cn.geoair.map.dynamic.dbservice.basic.dao;

import cn.geoair.map.dynamic.dbservice.basic.domain.ApiConfig;
import cn.geoair.map.dynamic.dbservice.dao.dbapi.DbApiConfigDao;
import cn.geoair.map.dynamic.dbservice.model.dbapi.entity.DbApiConfigPo;

import org.springframework.stereotype.Component;

import java.util.List;

import javax.annotation.Resource;

@Component
public class ApiConfigMapper {
    @Resource DbApiConfigDao dbApiConfigDao;

    public List<ApiConfig> selectBatchIds(List<String> ids) {
        List<DbApiConfigPo> dbApiConfigPos = dbApiConfigDao.selectBatchIds(ids);
        return ApiConfig.fromPos(dbApiConfigPos);
    }

    public ApiConfig selectByPathOnline(String path) {
        return ApiConfig.fromPo(dbApiConfigDao.selectByPathOnline(path));
    }

    public List<ApiConfig> search(String name, String note, String path, String groupId) {
        return ApiConfig.fromDtos(dbApiConfigDao.search(name, note, path, groupId));
    }

    public Integer selectCountByPath(String path) {
        return dbApiConfigDao.selectCountByPath(path);
    }

    public Integer selectCountByPathWhenUpdate(String path, String id) {
        return dbApiConfigDao.selectCountByPathWhenUpdate(path, id);
    }

    public int selectCountByGroup(String id) {
        return dbApiConfigDao.selectCountByGroup(id);
    }

    public List<ApiConfig> selectByGroup(String groupId) {
        return ApiConfig.fromDtos(dbApiConfigDao.selectByGroup(groupId));
    }
}
