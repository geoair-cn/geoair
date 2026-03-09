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
 * 数据源信息(DbapiDatasource)实体类
 *
 * @author zhangjun
 * @date 2025-07-31
 */
@GaModel(text = "数据源信息")
@Table(name = "dbapi_datasource")
public class DbApiDataSourcePo implements GiCrudEntity<String>, GiEntityIdGenerator<String> {
    private static final long serialVersionUID = 1753953250845L;

    @Id
    @Column(name = "id")
    @GaModelField(text = "名称", isID = true)
    private String id;

    @Column(name = "name")
    @GaModelField(text = "名称")
    private String name;

    @Column(name = "note")
    @GaModelField(text = "备注信息")
    private String note;

    @Column(name = "type")
    @GaModelField(text = "类型")
    private String type;

    @Column(name = "url")
    @GaModelField(text = "jdbcUrl")
    private String url;

    @Column(name = "username")
    @GaModelField(text = "用户名")
    private String username;

    @Column(name = "password")
    @GaModelField(text = "密码")
    private String password;

    @Column(name = "driver")
    @GaModelField(text = "驱动名称")
    private String driver;

    @Column(name = "table_sql")
    @GaModelField(text = "创建或编辑API的时候，选择数据源，会执行此sql来获取该数据源下的所有表名称")
    private String tableSql;

    @Column(name = "create_user_id")
    @GaModelField(text = "创建人")
    private Integer createUserId;

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

    public DbApiDataSourcePo() {}

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

    public DbApiDataSourcePo(String id) {
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

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getDriver() {
        return driver;
    }

    public void setTableSql(String tableSql) {
        this.tableSql = tableSql;
    }

    public String getTableSql() {
        return tableSql;
    }

    public void setCreateUserId(Integer createUserId) {
        this.createUserId = createUserId;
    }

    public Integer getCreateUserId() {
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
        setTimeCreate(new Date());
        setTimeUpdate(new Date());
    }

    public void initUpdateMeta() {
        setNotDel();
        setTimeUpdate(new Date());
    }

    public static DbApiDataSourcePo empty() {
        return new DbApiDataSourcePo();
    }

    public DbApiDataSourcePo copy() {
        DbApiDataSourcePo copy = new DbApiDataSourcePo();
        BeanUtil.copyProperties(this, copy);
        return copy;
    }
}
