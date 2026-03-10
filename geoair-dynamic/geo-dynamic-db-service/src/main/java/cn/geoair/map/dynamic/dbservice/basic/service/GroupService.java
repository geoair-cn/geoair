package cn.geoair.map.dynamic.dbservice.basic.service;

import cn.geoair.map.dynamic.dbservice.DbApiUserInfoHelper;
import cn.geoair.map.dynamic.dbservice.basic.dao.ApiConfigMapper;
import cn.geoair.map.dynamic.dbservice.basic.domain.Group;
import cn.geoair.map.dynamic.dbservice.basic.util.UUIDUtil;
import cn.geoair.map.dynamic.dbservice.common.ResponseDto;
import cn.geoair.map.dynamic.dbservice.dao.dbapi.DbApiGroupDao;
import cn.geoair.map.dynamic.dbservice.model.dbapi.entity.DbApiGroupPo;

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

	@Autowired
	DbApiGroupDao dbApiGroupDao;

	@Autowired
	ApiConfigMapper apiConfigMapper;

	@Resource
	DbApiUserInfoHelper dbApiUserInfoHelper;

	public void insert(Group group) {
		group.setId(UUIDUtil.id());
		group.setCreateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
		group.setUpdateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
		DbApiGroupPo po = group.toPo();
		po.initCreateMeta();
		po.setNameCreate(dbApiUserInfoHelper.getSubjectName());
		dbApiGroupDao.gtcAccessSelective(po);
	}

	@Transactional
	public ResponseDto deleteById(String id) {
		int size = apiConfigMapper.selectCountByGroup(id);
		if (size > 0) {
			return ResponseDto.fail("Group is not empty, can not delete");
		}
		else {
			dbApiGroupDao.gtcDeleteByPK(id);
			return ResponseDto.successWithMsg("Group delete success");
		}
	}

	public List<Group> getAll() {
		List<DbApiGroupPo> dbApiGroupPos = dbApiGroupDao.gtcSearchAll();
		List<Group> groups = Group.fromPos(dbApiGroupPos);
		return groups;
	}

	public List<Group> selectBatch(List<String> ids) {
		return Group.fromPos(dbApiGroupDao.selectBatchIds(ids));
	}

	@Transactional
	public void insertBatch(List<Group> configs) {
		configs.stream().forEach(t -> {
			DbApiGroupPo po = t.toPo();
			po.setNameCreate(dbApiUserInfoHelper.getSubjectName());
			po.initCreateMeta();
			dbApiGroupDao.gtcAccessSelective(po);
		});
	}

	@Transactional
	public void update(Group group) {
		group.setUpdateTime(DateFormatUtils.format(new Date(), "yyyy-MM-dd hh:mm:ss"));
		DbApiGroupPo po = group.toPo();
		po.setNameUpdate(dbApiUserInfoHelper.getSubjectName());
		po.initUpdateMeta();
		dbApiGroupDao.gtcUpdateByPKSelective(po);
	}

}
