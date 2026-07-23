package cn.geoair.comp.db.service.core.test;

import cn.geoair.comp.db.service.core.basic.apo.DsDataSourceApo;
import cn.geoair.comp.db.service.core.basic.dto.SQLTaskDto;
import cn.geoair.comp.db.service.core.common.ResponseDto;

/**
 * db-service 基础 DTO 示例
 */
public class DbServiceDtoExample {

    public static void main(String[] args) {
        DsDataSourceApo ds = new DsDataSourceApo();
        ds.setId("gis_ds");
        ds.setName("GIS 数据源");
        ds.setUrl("jdbc:postgresql://127.0.0.1:5432/postgis");
        ds.setUsername("postgres");
        ds.setPassword("encrypted-password");
        ds.setDriver("org.postgresql.Driver");

        SQLTaskDto task = new SQLTaskDto();
        task.setDatasourceId("gis_ds");
        task.setTransaction(1);
        task.setPageIs(1);
        task.setHumpIs(1);

        ResponseDto<Object> response = ResponseDto.successWithData("ok");

        System.out.println("ds.id = " + ds.getId());
        System.out.println("task.transactionIs = " + task.transactionIs());
        System.out.println("task.pageIs = " + task.pageIs());
        System.out.println("response.success = " + response.getSuccess());
        System.out.println("response.data = " + response.getData());
    }
}
