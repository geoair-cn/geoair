package cn.geoair.comp.dynamic.ds.readwrite.test;

import cn.geoair.comp.dynamic.ds.readwrite.GirReadWriteDataSource;
import cn.geoair.comp.dynamic.ds.readwrite.GirReadWriteDataSourceBuilder;
import cn.geoair.comp.dynamic.ds.readwrite.enums.LoadStrategyType;

import java.sql.SQLException;
import java.util.Arrays;

/**
 * @author ：张俊
 * @date ：Created in 2026/5/28 19:04
 * @description： TODO
 */
public class BuilderTest {
    public static void main(String[] args) throws SQLException {
        //// ========== 方式1：使用已构建的 GirGroupSource ==========
        //        GirGroupSource slaveGroup = new GirGroupSource("slaveGroup", slaveIds);
        //        GirReadWriteDataSource dataSource = GirReadWriteDataSourceBuilder.builder()
        //                .master("master_db")
        //                .slaves(slaveGroup)           // 直接传入 GirGroupSource
        //                .build();
        //
        //// ========== 方式2：使用已构建的 GirGroupByIdDataSource ==========
        //        GirGroupByIdDataSource slaveGroupById = new GirGroupByIdDataSource("slaveGroup",
        // slaveIds);
        //        GirReadWriteDataSource dataSource2 = GirReadWriteDataSourceBuilder.builder()
        //                .master("master_db")
        //                .slaves(slaveGroupById)       // 直接传入 GirGroupByIdDataSource
        //                .build();

        // ========== 方式3：单个 DataSource 作为从库 ==========
        //        DataSource singleSlave = getSlaveDataSource();
        //        GirReadWriteDataSource dataSource3 = GirReadWriteDataSourceBuilder.builder()
        //                .master("master_db")
        //                .slave(singleSlave)           // 单个 DataSource，会自动包装成 Group
        //                .build();

        // ========== 方式4：使用 ID 列表（自动创建 GirGroupByIdDataSource） ==========
        GirReadWriteDataSource dataSource4 =
                GirReadWriteDataSourceBuilder.builder()
                        .master("master_db")
                        .slaves("slave1", "slave2", "slave3") // 批量添加ID
                        .slaveStrategy(LoadStrategyType.ROUND_ROBIN)
                        .slaveGroupName("mySlaveGroup")
                        .build();

        // ========== 方式5：混合使用（先添加ID，再设置策略） ==========
        GirReadWriteDataSource dataSource5 =
                GirReadWriteDataSourceBuilder.builder()
                        .master("master_db")
                        .addSlave("slave1")
                        .addSlave("slave2")
                        .addSlave("slave3")
                        .slaveStrategy(LoadStrategyType.WEIGHT)
                        .build();

        // 设置权重
        //        GirGroupByIdDataSource slaveGroup = (GirGroupByIdDataSource)
        // dataSource5.getSlaveGroup();
        //        slaveGroup.setWeight("slave1", 5);
        //        slaveGroup.setWeight("slave2", 3);
        //        slaveGroup.setWeight("slave3", 2);

        // ========== 静态快速方法 ==========
        // 使用 GirGroupSource
        //        GirReadWriteDataSource dataSource6 = GirReadWriteDataSourceBuilder
        //                .build("master_db", slaveGroup);

        // 使用 GirGroupByIdDataSource
        //        GirReadWriteDataSource dataSource7 = GirReadWriteDataSourceBuilder
        //                .build("master_db", slaveGroupById);

        // 使用 ID 列表
        GirReadWriteDataSource dataSource8 =
                GirReadWriteDataSourceBuilder.build(
                        "master_db", Arrays.asList("slave1", "slave2", "slave3"));
    }
}
