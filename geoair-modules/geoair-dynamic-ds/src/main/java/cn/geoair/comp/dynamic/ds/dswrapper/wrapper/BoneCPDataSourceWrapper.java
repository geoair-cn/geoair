package cn.geoair.comp.dynamic.ds.dswrapper.wrapper;

import com.jolbox.bonecp.BoneCPDataSource;

import javax.sql.DataSource;

/** BoneCP 数据源包装器（轻量级高性能连接池） */
public class BoneCPDataSourceWrapper extends GirAbstractDataSourceWrapper {

    private static Boolean canInit = null;

    public BoneCPDataSourceWrapper(DataSource targetDataSource) {
        super(targetDataSource);
    }

    @Override
    protected Class<? extends DataSource> getTargetDataSourceClass() {
        return BoneCPDataSource.class;
    }

    public static boolean canInit() {
        if (canInit != null) {
            return canInit;
        }
        try {
            Class.forName("com.jolbox.bonecp.BoneCPDataSource");
            canInit = true;
        } catch (ClassNotFoundException e) {
            canInit = false;
        }
        return canInit;
    }

    @Override
    public boolean close() {
        getBoneCPDataSource().close();
        return true;
    }

    @Override
    public String getSimpleDataSourceName() {
        return targetDataSource.getClass().getSimpleName() + "@" + targetDataSource.hashCode();
    }

    @Override
    public String getJdbcUrl() {
        BoneCPDataSource boneCPDataSource = (BoneCPDataSource) targetDataSource;
        return boneCPDataSource.getJdbcUrl();
    }

    public BoneCPDataSource getBoneCPDataSource() {
        if (isSupport()) {
            return (BoneCPDataSource) targetDataSource;
        }
        throw new IllegalArgumentException("当前数据源不是BoneCP数据源");
    }

    @Override
    public Integer getActiveCount() {
        BoneCPDataSource bcp = getBoneCPDataSource();
        if (bcp == null) {
            return 0;
        }
        Integer activeCount = null;
        try {
            activeCount = bcp.getPool().getTotalLeased();
        } catch (Exception e) {

        }
        return activeCount;
    }
}
