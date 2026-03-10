package cn.geoair.map.dynamic.dbservice.core.controller.dbapi.config;

import cn.geoair.base.data.model.annotation.GaModel;
import cn.geoair.base.data.model.annotation.GaModelField;
import cn.hutool.core.bean.BeanUtil;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * api配置信息对象 dbapi_config
 *
 * @author zhangjun
 * @date 2025-07-31
 */
@GaModel(text = "查询api配置信息")
@JsonPropertyOrder(value = {"queryContent"})
public class ConfigSearchVo {

    public static ConfigSearchVo empty() {
        return new ConfigSearchVo();
    }

    public ConfigSearchVo copy() {
        ConfigSearchVo copy = new ConfigSearchVo();
        BeanUtil.copyProperties(this, copy);
        return copy;
    }

    @GaModelField(text = "查询的字符串")
    private String queryContent;

    public String getQueryContent() {
        return queryContent;
    }

    public void setQueryContent(String queryContent) {
        this.queryContent = queryContent;
    }

    @GaModelField(text = "主键")
    private String id;

    @GaModelField(text = "名称")
    private String name;

    @GaModelField(text = "备注")
    private String note;

    @GaModelField(text = "接口定义的路径")
    private String path;

    @GaModelField(text = "参数信息")
    private String params;

    @GaModelField(text = "入参信息")
    private String jsonParam;

    @GaModelField(text = "状态，停用与否")
    private Integer status;

    @GaModelField(text = "api类型")
    private Integer access;

    @GaModelField(text = "分组Id")
    private String groupId;

    @GaModelField(text = "请求头")
    private String contentType;

    @GaModelField(text = "未知")
    private String task;

    @GaModelField(text = "创建人Id")
    private String createUserId;

    @GaModelField(text = "创建时间")
    private String createTime;

    @GaModelField(text = "更新时间")
    private String updateTime;

    @GaModelField(text = "是否删除标识")
    private String delIs;

    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @GaModelField(text = "创建时间")
    private Date timeCreate;

    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @GaModelField(text = "更新时间")
    private Date timeUpdate;

    @GaModelField(text = "创建人名称")
    private String nameCreate;

    @GaModelField(text = "更新人名称")
    private String nameUpdate;

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

    public String getJsonParam() {
        return jsonParam;
    }

    public void setJsonParam(String jsonParam) {
        this.jsonParam = jsonParam;
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

    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public String getCreateUserId() {
        return createUserId;
    }

    public void setCreateUserId(String createUserId) {
        this.createUserId = createUserId;
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

    public String getDelIs() {
        return delIs;
    }

    public void setDelIs(String delIs) {
        this.delIs = delIs;
    }

    public Date getTimeCreate() {
        return timeCreate;
    }

    public void setTimeCreate(Date timeCreate) {
        this.timeCreate = timeCreate;
    }

    public Date getTimeUpdate() {
        return timeUpdate;
    }

    public void setTimeUpdate(Date timeUpdate) {
        this.timeUpdate = timeUpdate;
    }

    public String getNameCreate() {
        return nameCreate;
    }

    public void setNameCreate(String nameCreate) {
        this.nameCreate = nameCreate;
    }

    public String getNameUpdate() {
        return nameUpdate;
    }

    public void setNameUpdate(String nameUpdate) {
        this.nameUpdate = nameUpdate;
    }
}
