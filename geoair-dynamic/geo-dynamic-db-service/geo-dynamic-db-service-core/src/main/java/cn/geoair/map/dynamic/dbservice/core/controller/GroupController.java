package cn.geoair.map.dynamic.dbservice.core.controller;

import cn.geoair.base.api.annotation.GaApi;
import cn.geoair.base.api.annotation.GaApiAction;
import cn.geoair.map.dynamic.dbservice.core.DsApiUserInfoHelper;
import cn.geoair.map.dynamic.dbservice.core.basic.apo.GroupApo;
import cn.geoair.map.dynamic.dbservice.core.basic.service.DsGroupService;
import cn.geoair.map.dynamic.dbservice.core.common.ResponseDto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import javax.annotation.Resource;

@RestController
@RequestMapping("/ds_api/group")
@GaApi(tags = "api分组信息")
public class GroupController {

    @Autowired DsGroupService dsGroupService;

    @Resource DsApiUserInfoHelper dsApiUserInfoHelper;

    @PostMapping("/create")
    public void create(GroupApo groupApo) {
        groupApo.setCreateUserId(dsApiUserInfoHelper.getSubjectId());
        dsGroupService.insert(groupApo);
    }

    @GaApiAction(text = "创建API分组")
    @PostMapping("/delete/{id}")
    public ResponseDto delete(@PathVariable String id) {
        return dsGroupService.deleteById(id);
    }

    @GaApiAction(text = "获取所有API分组")
    @PostMapping("/getAll")
    public List<GroupApo> getAll() {
        List<GroupApo> list = dsGroupService.getAll();
        return list;
    }

    @GaApiAction(text = "更新API分组信息")
    @PostMapping("/update")
    public void update(GroupApo groupApo) {
        dsGroupService.update(groupApo);
    }
}
