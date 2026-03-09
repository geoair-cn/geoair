package cn.geoair.map.dynamic.dbservice.model.dbapi.entity;

import cn.hutool.core.bean.BeanUtil;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonFormat;
import cn.geoair.base.data.common.GemDatePattern;
import cn.geoair.base.data.model.annotation.GaModel;
import cn.geoair.base.data.model.annotation.GaModelField;
import cn.geoair.base.gpa.entity.GiCrudEntity;
import cn.geoair.base.gpa.id.GiEntityIdGenerator;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * api分组信息(DbapiGroup)实体类
 *
 * @author zhangjun
 * @date 2025-07-31
 */
@GaModel(text = "api分组信息")
@Table(name = "dbapi_group")
public class DbApiGroupPo implements GiCrudEntity<String>, GiEntityIdGenerator<String> {
    private static final long serialVersionUID = 1753953245114L;

    @Id
    @Column(name = "id")
    @GaModelField(text = "主键", isID = true)
    private String id;

    @Column(name = "name")
    @GaModelField(text = "分组名称")
    private String name;

    @Column(name = "create_user_id")
    @GaModelField(text = "创建人Id")
    private Integer createUserId;

    @Column(name = "create_time")
    @GaModelField(text = "创建时间")
    private String createTime;

    @Column(name = "update_time")
    @GaModelField(text = "更新时间")
    private String updateTime;

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

    public DbApiGroupPo() {}

    public DbApiGroupPo(String id) {
        if (id == null) {
            id = this.generatorId();
        }
        this.id = id;
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

    public void setCreateUserId(Integer createUserId) {
        this.createUserId = createUserId;
    }

    public Integer getCreateUserId() {
        return createUserId;
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

    public String id() {
        return id;
    }

    public String generatorId() {
        return UUID.randomUUID().toString();
    }

    public void setNotDel() {}

    public void setDel() {}

    public void initCreateMeta() {
        setNotDel();
        // setTimeCreate(new Date());
    }

    public void initUpdateMeta() {
        setNotDel();
        // setTimeUpdate(new Date());
    }

    public static DbApiGroupPo empty() {
        return new DbApiGroupPo();
    }

    public DbApiGroupPo copy() {
        DbApiGroupPo copy = new DbApiGroupPo();
        BeanUtil.copyProperties(this, copy);
        return copy;
    }
}
