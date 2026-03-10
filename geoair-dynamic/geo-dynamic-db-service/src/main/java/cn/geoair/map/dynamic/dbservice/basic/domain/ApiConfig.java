package cn.geoair.map.dynamic.dbservice.basic.domain;

import cn.geoair.map.dynamic.dbservice.common.ApiPluginConfig;
import cn.geoair.map.dynamic.dbservice.model.dbapi.dto.DbApiConfigDto;
import cn.geoair.map.dynamic.dbservice.model.dbapi.entity.DbApiConfigPo;

import com.alibaba.fastjson.JSONArray;

import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @program: dbApi
 * @description:
 * @author: 武汉刘德华
 * @create: 2021-01-20 09:50
 */
public class ApiConfig implements Serializable {

	public DbApiConfigPo toPo() {
		DbApiConfigPo thisPo = new DbApiConfigPo();
		BeanUtils.copyProperties(this, thisPo);
		return thisPo;
	}

	public static ApiConfig fromPo(DbApiConfigPo po) {
		ApiConfig thisVo = new ApiConfig();
		BeanUtils.copyProperties(po, thisVo);
		return thisVo;
	}

	public static List<ApiConfig> fromPos(List<DbApiConfigPo> pos) {
		List<ApiConfig> list = new ArrayList<>();
		for (DbApiConfigPo po : pos) {
			ApiConfig thisVo = fromPo(po);
			list.add(thisVo);
		}
		return list;
	}

	public static ApiConfig fromDto(DbApiConfigDto dto) {
		return fromPo(dto);
	}

	public static List<ApiConfig> fromDtos(List<DbApiConfigDto> dtos) {
		List<ApiConfig> list = new ArrayList<>();
		for (DbApiConfigDto dto : dtos) {
			ApiConfig thisVo = fromDto(dto);
			list.add(thisVo);
		}
		return list;
	}

	String id;

	String name;

	String note;

	String path;

	/** application/x-www-form-urlencoded 类API对应的参数 */
	String params;

	Integer status;

	Integer access;

	String groupId;

	String contentType;

	/** application/json 类API对应的json参数示例 */
	String jsonParam;

	String task;

	String createTime;

	String updateTime;

	String createUserId;

	JSONArray paramsJson; // params的json格式

	JSONArray taskJson; // task的json格式

	List<ApiPluginConfig> alarmPlugins;

	ApiPluginConfig cachePlugin;

	ApiPluginConfig globalTransformPlugin;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getParams() {
		return params;
	}

	public void setParams(String params) {
		this.params = params;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public Integer getAccess() {
		return access;
	}

	public void setAccess(Integer access) {
		this.access = access;
	}

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getJsonParam() {
		return jsonParam;
	}

	public void setJsonParam(String jsonParam) {
		this.jsonParam = jsonParam;
	}

	public String getTask() {
		return task;
	}

	public void setTask(String task) {
		this.task = task;
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

	public String getCreateUserId() {
		return createUserId;
	}

	public void setCreateUserId(String createUserId) {
		this.createUserId = createUserId;
	}

	public JSONArray getParamsJson() {
		return paramsJson;
	}

	public void setParamsJson(JSONArray paramsJson) {
		this.paramsJson = paramsJson;
	}

	public JSONArray getTaskJson() {
		return taskJson;
	}

	public void setTaskJson(JSONArray taskJson) {
		this.taskJson = taskJson;
	}

	public List<ApiPluginConfig> getAlarmPlugins() {
		return alarmPlugins;
	}

	public void setAlarmPlugins(List<ApiPluginConfig> alarmPlugins) {
		this.alarmPlugins = alarmPlugins;
	}

	public ApiPluginConfig getCachePlugin() {
		return cachePlugin;
	}

	public void setCachePlugin(ApiPluginConfig cachePlugin) {
		this.cachePlugin = cachePlugin;
	}

	public ApiPluginConfig getGlobalTransformPlugin() {
		return globalTransformPlugin;
	}

	public void setGlobalTransformPlugin(ApiPluginConfig globalTransformPlugin) {
		this.globalTransformPlugin = globalTransformPlugin;
	}

}
