package cn.geoair.comp.dynamic.ds.dswrapper;

import cn.geoair.comp.dynamic.ds.dswrapper.wrapper.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

public class DataSourceWrapperRegistry {

    private static final List<Class<? extends AdvDataSourceWrapper>> WRAPPER_CLASSES =
            new ArrayList<>();

    //  注册所有常用包装器
    static {
        if (DruidDataSourceWrapper.canInit()) registerWrapper(DruidDataSourceWrapper.class);
        if (HikariDataSourceWrapper.canInit()) registerWrapper(HikariDataSourceWrapper.class);
        if (DBCP2DataSourceWrapper.canInit()) registerWrapper(DBCP2DataSourceWrapper.class);
        if (C3P0DataSourceWrapper.canInit()) registerWrapper(C3P0DataSourceWrapper.class);
        if (BoneCPDataSourceWrapper.canInit()) registerWrapper(BoneCPDataSourceWrapper.class);
        if (SpringDiverManagerSourceWrapper.canInit())
            registerWrapper(SpringDiverManagerSourceWrapper.class);
        if (AdvSimpleDataSourceWrapper.canInit()) registerWrapper(AdvSimpleDataSourceWrapper.class);
        if (DiverManagerSourceWrapper.canInit()) registerWrapper(DiverManagerSourceWrapper.class);
        if (CommonSourceWrapper.canInit()) registerWrapper(CommonSourceWrapper.class);
    }

    public static void registerWrapper(Class<? extends AdvDataSourceWrapper> wrapperClass) {
        if (!WRAPPER_CLASSES.contains(wrapperClass)) {
            WRAPPER_CLASSES.add(wrapperClass);
        }
    }

    public static Optional<AdvDataSourceWrapper> getWrapper(DataSource dataSource) {
        for (Class<? extends AdvDataSourceWrapper> wrapperClass : WRAPPER_CLASSES) {
            try {
                AdvDataSourceWrapper wrapper =
                        wrapperClass.getConstructor(DataSource.class).newInstance(dataSource);
                if (wrapper.isSupport()) {
                    return Optional.of(wrapper);
                }
            } catch (Exception e) {
                continue;
            }
        }
        return Optional.empty();
    }

    public static String getSimpleDataSourceName(DataSource dataSource) {
        return getWrapper(dataSource)
                .map(AdvDataSourceWrapper::getSimpleDataSourceName)
                .orElse("unknown");
    }

    public static String getJdbcUrl(DataSource dataSource) {
        return getWrapper(dataSource).map(AdvDataSourceWrapper::getJdbcUrl).orElse("unknown");
    }
}
