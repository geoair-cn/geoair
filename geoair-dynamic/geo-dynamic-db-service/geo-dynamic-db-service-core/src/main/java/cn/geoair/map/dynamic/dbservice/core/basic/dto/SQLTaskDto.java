package cn.geoair.map.dynamic.dbservice.core.basic.dto;

import java.util.List;

public class SQLTaskDto {

    String datasourceId;

    Integer transaction;

    Integer pageIs = 0;

    Integer humpIs = 0;

    List<ApiSqlDto> sqlList;

    public String getDatasourceId() {
        return datasourceId;
    }

    public void setDatasourceId(String datasourceId) {
        this.datasourceId = datasourceId;
    }

    public Integer getTransaction() {
        return transaction;
    }

    public boolean transactionIs() {
        return transaction == 1;
    }

    public boolean pageIs() {
        return pageIs == 1;
    }

    public boolean humpIs() {
        return humpIs == 1;
    }

    public void setTransaction(Integer transaction) {
        this.transaction = transaction;
    }

    public List<ApiSqlDto> getSqlList() {
        return sqlList;
    }

    public void setSqlList(List<ApiSqlDto> sqlList) {
        this.sqlList = sqlList;
    }

    public Integer getPageIs() {
        return pageIs;
    }

    public void setPageIs(Integer pageIs) {
        this.pageIs = pageIs;
    }

    public Integer getHumpIs() {
        return humpIs;
    }

    public void setHumpIs(Integer humpIs) {
        this.humpIs = humpIs;
    }
}
