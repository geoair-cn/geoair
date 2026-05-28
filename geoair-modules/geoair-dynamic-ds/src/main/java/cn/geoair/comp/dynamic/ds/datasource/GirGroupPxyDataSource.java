package cn.geoair.comp.dynamic.ds.datasource;

import cn.geoair.comp.dynamic.ds.readwrite.GirGroupByIdDataSource;
import cn.geoair.comp.dynamic.ds.readwrite.enums.LoadStrategyType;

import java.util.List;

/**
 * @author ：张俊
 * @date ：Created in 2026/5/28 18:26
 * @description： 兼容原有旧API，当时由于命名指向不明确，故废弃
 */
@Deprecated
public class GirGroupPxyDataSource extends GirGroupByIdDataSource {


    public GirGroupPxyDataSource(String groupName, List<String> dataSourceIds) {
        super(GirGroupByIdDataSource
                .builderById().
                dataSourceIds(dataSourceIds).
                groupName(groupName));
    }

    public GirGroupPxyDataSource(String groupName, List<String> dataSourceIds, LoadStrategyType strategyType) {
        super(GirGroupByIdDataSource
                .builderById().
                dataSourceIds(dataSourceIds).
                groupName(groupName).
                strategy(strategyType));
    }
}
