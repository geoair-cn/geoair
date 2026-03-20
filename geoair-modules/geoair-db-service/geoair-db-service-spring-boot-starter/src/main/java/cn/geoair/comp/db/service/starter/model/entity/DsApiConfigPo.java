package cn.geoair.comp.db.service.starter.model.entity;

import cn.geoair.base.data.common.GemDatePattern;
import cn.geoair.base.data.model.annotation.GaModel;
import cn.geoair.base.data.model.annotation.GaModelField;
import cn.geoair.base.gpa.entity.GiCrudEntity;
import cn.geoair.base.gpa.id.GiEntityIdGenerator;
import cn.hutool.core.bean.BeanUtil;

import com.alibaba.fastjson2.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * api配置信息(DbapiConfig)实体类
 *
 * @author zhangjun
 * @date 2025-07-31
 */
@GaModel(text = "api配置信息")
@Table(name = "dsapi_config")
public class DsApiConfigPo implements GiCrudEntity<String>, GiEntityIdGenerator<String> {

	private static final long serialVersionUID = 1753953255397L;

	@Id
	@Column(name = "id")
	@GaModelField(text = "主键", isID = true)
	private String id;

	@Column(name = "name")
	@GaModelField(text = "名称")
	private String name;

	@Column(name = "note")
	@GaModelField(text = "备注")
	private String note;

	@Column(name = "path")
	@GaModelField(text = "接口定义的路径")
	private String path;

	@Column(name = "params")
	@GaModelField(text = "参数信息")
	private String params;

	@Column(name = "json_param")
	@GaModelField(text = "入参信息")
	private String jsonParam;

	@Column(name = "status")
	@GaModelField(text = "状态，停用与否")
	private Integer status;

	@Column(name = "access")
	@GaModelField(text = "api类型")
	private Integer access;

	@Column(name = "group_id")
	@GaModelField(text = "分组Id")
	private String groupId;

	@Column(name = "content_type")
	@GaModelField(text = "请求头")
	private String contentType;

	@Column(name = "task")
	@GaModelField(text = "该任务信息")
	private String task;

	@Column(name = "create_user_id")
	@GaModelField(text = "创建人Id")
	private String createUserId;

	@Column(name = "create_time")
	@GaModelField(text = "创建时间")
	private String createTime;

	@Column(name = "update_time")
	@GaModelField(text = "更新时间")
	private String updateTime;

	@Column(name = "del_is")
	@GaModelField(text = "是否删除标识")
	private String delIs;

	@Column(name = "time_create")
	@GaModelField(text = "创建时间", datePattern = GemDatePattern.ISO8601Long)
	@JSONField(format = "yyyy-MM-dd HH:mm:ss")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
	private Date timeCreate;

	@Column(name = "time_update")
	@GaModelField(text = "更新时间", datePattern = GemDatePattern.ISO8601Long)
	@JSONField(format = "yyyy-MM-dd HH:mm:ss")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
	private Date timeUpdate;

	@Column(name = "name_create")
	@GaModelField(text = "创建人名称")
	private String nameCreate;

	@Column(name = "name_update")
	@GaModelField(text = "更新人名称")
	private String nameUpdate;

	public DsApiConfigPo() {
	}

	public DsApiConfigPo(String id) {
		if (id == null) {
			id = this.generatorId();
		}
		this.id = id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public String getNote() {
		return note;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getPath() {
		return path;
	}

	public void setParams(String params) {
		this.params = params;
	}

	public String getParams() {
		return params;
	}

	public void setJsonParam(String jsonParam) {
		this.jsonParam = jsonParam;
	}

	public String getJsonParam() {
		return jsonParam;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public Integer getStatus() {
		return status;
	}

	public void setAccess(Integer access) {
		this.access = access;
	}

	public Integer getAccess() {
		return access;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public String getGroupId() {
		return groupId;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getContentType() {
		return contentType;
	}

	public void setTask(String task) {
		this.task = task;
	}

	public String getTask() {
		return task;
	}

	public void setCreateUserId(String createUserId) {
		this.createUserId = createUserId;
	}

	public String getCreateUserId() {
		return createUserId;
	}

	public void setDelIs(String delIs) {
		this.delIs = delIs;
	}

	public String getDelIs() {
		return delIs;
	}

	public void setTimeCreate(Date timeCreate) {
		this.timeCreate = timeCreate;
	}

	public Date getTimeCreate() {
		return timeCreate;
	}

	public void setTimeUpdate(Date timeUpdate) {
		this.timeUpdate = timeUpdate;
	}

	public Date getTimeUpdate() {
		return timeUpdate;
	}

	public void setNameCreate(String nameCreate) {
		this.nameCreate = nameCreate;
	}

	public String getNameCreate() {
		return nameCreate;
	}

	public void setNameUpdate(String nameUpdate) {
		this.nameUpdate = nameUpdate;
	}

	public String getNameUpdate() {
		return nameUpdate;
	}

	public String getCreateTime() {
		return createTime;
	}

	public void setCreateTime(String createTime) {
		this.createTime = createTime;
	}

	public String getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(String updateTime) {
		this.updateTime = updateTime;
	}

	public String id() {
		return id;
	}

	public String generatorId() {
		return UUID.randomUUID().toString();
	}

	public void setNotDel() {
	}

	public void setDel() {
	}

	public void initCreateMeta() {
		setNotDel();
		setTimeCreate(new Date());
		setTimeUpdate(new Date());
	}

	public void initUpdateMeta() {
		setNotDel();
		setTimeUpdate(new Date());
	}

	public static DsApiConfigPo empty() {
		return new DsApiConfigPo();
	}

	public DsApiConfigPo copy() {
		DsApiConfigPo copy = new DsApiConfigPo();
		BeanUtil.copyProperties(this, copy);
		return copy;
	}

}
