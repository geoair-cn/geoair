package cn.geoair.map.dynamic.dbservice.core.basic.service;

import cn.geoair.map.dynamic.dbservice.core.DsApiUserInfoHelper;
import cn.geoair.map.dynamic.dbservice.core.basic.apo.GroupApo;
import cn.geoair.map.dynamic.dbservice.core.basic.util.UUIDUtil;
import cn.geoair.map.dynamic.dbservice.core.common.ResponseDto;
import cn.geoair.map.dynamic.dbservice.core.dao.ApiConfigDao;
import cn.geoair.map.dynamic.dbservice.core.dao.ApiGroupDao;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

@Service
public class GroupService {

    @Autowired ApiGroupDao apiGroupDao;

    @Autowired ApiConfigDao apiConfigDao;

    @Resource DsApiUserInfoHelper dsApiUserInfoHelper;

    public void insert(GroupApo groupApo) {
        groupApo.setId(UUIDUtil.id());
        groupApo.setCreateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        groupApo.setUpdateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        // DbApiGroupPo po = group.toPo();
        // po.initCreateMeta();
        groupApo.setCreateUserName(dsApiUserInfoHelper.getSubjectName());
        groupApo.setCreateUserId(dsApiUserInfoHelper.getSubjectId());
        apiGroupDao.accessSelective(groupApo);
    }

    @Transactional
    public ResponseDto deleteById(String id) {
        int size = apiConfigDao.selectCountByGroup(id);
        if (size > 0) {
            return ResponseDto.fail("Group is not empty, can not delete");
        } else {
            apiGroupDao.deleteByPK(id);
            return ResponseDto.successWithMsg("Group delete success");
        }
    }

    public List<GroupApo> getAll() {
        // List<DbApiGroupPo> dbApiGroupPos = apiGroupDao.searchAll();
        // List<Group> groups = Group.fromPos(dbApiGroupPos);
        return apiGroupDao.searchAll();
    }

    public List<GroupApo> selectBatch(List<String> ids) {
        // return Group.fromPos();
        return apiGroupDao.selectBatchIds(ids);
    }

    @Transactional
    public void insertBatch(List<GroupApo> configs) {
        configs.stream()
                .forEach(
                        t -> {
                            // DbApiGroupPo po = t.toPo();
                            //
                            // po.setNameCreate(dbApiUserInfoHelper.getSubjectName());
                            // po.initCreateMeta();
                            t.setCreateUserName(dsApiUserInfoHelper.getSubjectName());
                            t.setCreateUserId(dsApiUserInfoHelper.getSubjectId());
                            // t.setId(UUIDUtil.id());
                            apiGroupDao.accessSelective(t);
                        });
    }

    @Transactional
    public void update(GroupApo groupApo) {
        groupApo.setUpdateTime(DateFormatUtils.format(new Date(), "yyyy-MM-dd hh:mm:ss"));
        // DbApiGroupPo po = group.toPo();
        // po.setNameUpdate(dbApiUserInfoHelper.getSubjectName());
        // po.initUpdateMeta();
        apiGroupDao.updateSelectiveById(groupApo);
    }
}
