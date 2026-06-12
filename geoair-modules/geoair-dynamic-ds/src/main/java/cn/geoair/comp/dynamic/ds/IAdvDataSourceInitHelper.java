package cn.geoair.comp.dynamic.ds;

import cn.geoair.comp.dynamic.ds.apo.DataSourceApo;

import javax.sql.DataSource;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/10/9 10:32
 * @description： 由上游进行实现 通过
 */
public interface IAdvDataSourceInitHelper {

    /** 根据数据源Apo配置创建并返回连接池实例 不到万不得已，不要去创建一个新的实例 */
    DataSource getDbDataSourceByApo(DataSourceApo dataSourceApo);
}
