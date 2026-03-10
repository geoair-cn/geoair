package cn.geoair.map.dynamic.dbservice.core.controller;

import cn.geoair.base.api.annotation.GaApi;
import cn.geoair.base.api.annotation.GaApiAction;
import cn.geoair.base.data.page.GiPageParam;
import cn.geoair.base.data.page.GiPager;
import cn.geoair.base.data.page.support.GirPager;
import cn.geoair.base.data.result.GiResult;
import cn.geoair.base.util.GutilObject;
import cn.geoair.map.dynamic.dbservice.core.DbApiUserInfoHelper;
import cn.geoair.map.dynamic.dbservice.core.basic.domain.Group;
import cn.geoair.map.dynamic.dbservice.core.basic.service.GroupService;
import cn.geoair.map.dynamic.dbservice.core.common.ResponseDto;
import cn.geoair.map.dynamic.dbservice.core.controller.dbapi.group.GroupSearchVo;
import cn.geoair.map.dynamic.dbservice.core.dao.dbapi.DbApiGroupDao;
import cn.geoair.map.dynamic.dbservice.core.model.dbapi.dto.DbApiGroupDto;
import cn.geoair.map.dynamic.dbservice.core.model.dbapi.seo.DbApiGroupSeo;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ArrayUtil;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import javax.annotation.Resource;

@RestController
@RequestMapping("/group")
@GaApi(tags = "api分组信息")
public class GroupController {

    @Autowired GroupService groupService;

    @Resource private DbApiGroupDao dbApiGroupDao;

    @Resource DbApiUserInfoHelper dbApiUserInfoHelper;

    @PostMapping("/create")
    public void create(Group group) {
        group.setCreateUserId(dbApiUserInfoHelper.getSubjectId());
        groupService.insert(group);
    }

    @GaApiAction(text = "创建API分组")
    @PostMapping("/delete/{id}")
    public ResponseDto delete(@PathVariable String id) {
        return groupService.deleteById(id);
    }

    @GaApiAction(text = "获取所有API分组")
    @PostMapping("/getAll")
    public List<Group> getAll() {
        List<Group> list = groupService.getAll();
        return list;
    }

    @GaApiAction(text = "更新API分组信息")
    @PostMapping("/update")
    public void update(Group group) {
        groupService.update(group);
    }

    @GaApiAction(text = "分页列出api分组信息")
    @RequestMapping(
            value = "/listDbApiGroupPage",
            method = {RequestMethod.POST})
    @ResponseBody
    public GiResult<GiPager<Group>> listDbApiGroupPage(
            @Validated @RequestBody GroupSearchVo param) {
        DbApiGroupSeo seo = new DbApiGroupSeo();
        BeanUtils.copyProperties(param, seo);
        seo.setNotDel();
        if (GutilObject.isNotEmpty(param.getQueryContent())) {
            seo.setAndQueryContentIn(
                    ArrayUtil.toArray(ListUtil.of(param.getQueryContent()), String.class));
        }
        GiPager<DbApiGroupDto> giPager = dbApiGroupDao.searchListPage(seo, GiPageParam.of());
        Iterable<DbApiGroupDto> value = giPager.value();
        GirPager<Group> reg = new GirPager<>();
        List<Group> vdvos = Group.fromDtos(ListUtil.toList(value));
        reg.put(vdvos, giPager.total(), giPager.pageParam());
        return GiResult.successValue(reg);
    }
}
