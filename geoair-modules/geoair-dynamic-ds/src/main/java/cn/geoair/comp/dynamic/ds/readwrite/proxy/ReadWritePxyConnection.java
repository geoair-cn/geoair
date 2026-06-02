package cn.geoair.comp.dynamic.ds.readwrite.proxy;

import cn.geoair.comp.dynamic.ds.dswrapper.ConnectionWrapper;
import lombok.Getter;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * @author ：张俊
 * @date ：Created in 2026/6/1 16:04
 * @description： 读写的数据库连接的包装
 */
@Getter
public class ReadWritePxyConnection extends ConnectionWrapper {

    /**
     * 是否是从节点的链接
     */
    boolean slaveIs;

    /**
     * 来自于那个数据源
     */
    DataSource fromDataSource;


    public ReadWritePxyConnection(Connection connection, boolean slaveIs, DataSource fromDataSource) {
        super(connection);
        this.slaveIs = slaveIs;
        this.fromDataSource = fromDataSource;
    }

    @Override
    public String toString() {
        return "ReadWritePxyConnection{" +
                "slaveIs=" + slaveIs +
                ", fromDataSource=" + fromDataSource.getClass().getSimpleName() +
                "} ";
    }
}
