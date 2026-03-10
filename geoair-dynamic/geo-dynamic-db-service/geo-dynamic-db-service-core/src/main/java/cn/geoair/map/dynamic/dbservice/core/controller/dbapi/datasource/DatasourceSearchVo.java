package cn.geoair.map.dynamic.dbservice.core.controller.dbapi.datasource;

import cn.geoair.base.data.model.annotation.GaModel;
import cn.geoair.base.data.model.annotation.GaModelField;
import cn.hutool.core.bean.BeanUtil;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * 数据源信息对象 dbapi_datasource
 *
 * @author zhangjun
 * @date 2025-07-31
 */
@GaModel(text = "查询数据源信息")
@JsonPropertyOrder(value = {"queryContent"})
public class DatasourceSearchVo {

    public static DatasourceSearchVo empty() {
        return new DatasourceSearchVo();
    }

    public DatasourceSearchVo copy() {
        DatasourceSearchVo copy = new DatasourceSearchVo();
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

    @GaModelField(text = "名称")
    private String id;

    @GaModelField(text = "名称")
    private String name;

    @GaModelField(text = "备注信息")
    private String note;

    @GaModelField(text = "类型")
    private String type;

    @GaModelField(text = "jdbcUrl")
    private String url;

    @GaModelField(text = "用户名")
    private String username;

    @GaModelField(text = "密码")
    private String password;

    @GaModelField(text = "驱动名称")
    private String driver;

    @GaModelField(text = "创建或编辑API的时候，选择数据源，会执行此sql来获取该数据源下的所有表名称")
    private String tableSql;

    @GaModelField(text = "创建人")
    private Integer createUserId;

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getTableSql() {
        return tableSql;
    }

    public void setTableSql(String tableSql) {
        this.tableSql = tableSql;
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
