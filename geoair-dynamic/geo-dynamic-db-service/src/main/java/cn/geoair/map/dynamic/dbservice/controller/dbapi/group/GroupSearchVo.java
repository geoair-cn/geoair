package cn.geoair.map.dynamic.dbservice.controller.dbapi.group;

import cn.geoair.base.data.model.annotation.GaModel;
import cn.geoair.base.data.model.annotation.GaModelField;
import cn.hutool.core.bean.BeanUtil;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * api分组信息对象 dbapi_group
 *
 * @author zhangjun
 * @date 2025-07-31
 */
@GaModel(text = "查询api分组信息")
@JsonPropertyOrder(value = {"queryContent"})
public class GroupSearchVo {
    public static GroupSearchVo empty() {
        return new GroupSearchVo();
    }

    public GroupSearchVo copy() {
        GroupSearchVo copy = new GroupSearchVo();
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

    @GaModelField(text = "分组名称")
    private String name;

    @GaModelField(text = "创建人Id")
    private Integer createUserId;

    @GaModelField(text = "创建时间")
    private String createTime;

    @GaModelField(text = "更新时间")
    private String updateTime;

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

    public Integer getCreateUserId() {
        return createUserId;
    }

    public void setCreateUserId(Integer createUserId) {
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
