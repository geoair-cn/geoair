package cn.geoair.base.data.model.suggest;


import cn.geoair.base.data.model.annotation.GaModelField;

import java.util.Date;

/**
 * 简易的审计字段模型，推荐使用，不做要求
 */

public class BaseModelSuggest {


    @GaModelField(text = "创建人ID", columnName = "id_create")
    private String idCreate;


    @GaModelField(text = "创建人名称", columnName = "name_create")
    private String nameCreate;


    @GaModelField(text = "创建时间", columnName = "time_create")
    private Date timeCreate;


    @GaModelField(text = "更新人名称", columnName = "name_update")
    private String nameUpdate;


    @GaModelField(text = "更新人ID", columnName = "id_update")
    private String idUpdate;


    @GaModelField(text = "更新时间", columnName = "time_update")
    private Date timeUpdate;


    @GaModelField(text = "备注", columnName = "remark")
    private String remark;


    @GaModelField(text = "是否删除#em=YES:已删除;NO:未删除", columnName = "del_is", em = DelIsEnum.class)
    private String delIs;

    public String getIdCreate() {
        return idCreate;
    }

    public BaseModelSuggest setIdCreate(String idCreate) {
        this.idCreate = idCreate;
        return this;
    }

    public String getNameCreate() {
        return nameCreate;
    }

    public BaseModelSuggest setNameCreate(String nameCreate) {
        this.nameCreate = nameCreate;
        return this;
    }

    public Date getTimeCreate() {
        return timeCreate;
    }

    public BaseModelSuggest setTimeCreate(Date timeCreate) {
        this.timeCreate = timeCreate;
        return this;
    }

    public String getNameUpdate() {
        return nameUpdate;
    }

    public BaseModelSuggest setNameUpdate(String nameUpdate) {
        this.nameUpdate = nameUpdate;
        return this;
    }

    public String getIdUpdate() {
        return idUpdate;
    }

    public BaseModelSuggest setIdUpdate(String idUpdate) {
        this.idUpdate = idUpdate;
        return this;
    }

    public Date getTimeUpdate() {
        return timeUpdate;
    }

    public BaseModelSuggest setTimeUpdate(Date timeUpdate) {
        this.timeUpdate = timeUpdate;
        return this;
    }

    public String getRemark() {
        return remark;
    }

    public BaseModelSuggest setRemark(String remark) {
        this.remark = remark;
        return this;
    }

    public String getDelIs() {
        return delIs;
    }

    public BaseModelSuggest setDelIs(String delIs) {
        this.delIs = delIs;
        return this;
    }

    public void setNotDel() {
        setDelIs("NO");
    }

    public void setDel() {
        setDelIs("YES");
    }
}
