package cn.geoair.comp.dynamic.ds.datasource.wrapper;




import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;


public class SpringDiverManagerSourceWrapper extends AbstractDataSourceWrapper {

    public SpringDiverManagerSourceWrapper(DataSource targetDataSource) {
        super(targetDataSource);
    }

    static Boolean canInit = null;

    public static boolean canInit(){
        if (canInit != null) {
            return canInit;
        }
        try {
            Class.forName("org.springframework.jdbc.datasource.DriverManagerDataSource");
            canInit = true;
        } catch (ClassNotFoundException e) {
            return false;
        }
        return canInit;
    }

    @Override
    protected Class<? extends DataSource> getTargetDataSourceClass() {
        return DriverManagerDataSource.class;
    }

    @Override
    public String getSimpleDataSourceName() {
        DriverManagerDataSource dataSource = (DriverManagerDataSource) targetDataSource;
        return dataSource.getUrl();
    }

    @Override
    public String getJdbcUrl() {
        DriverManagerDataSource dataSource = (DriverManagerDataSource) targetDataSource;
        return dataSource.getUrl();
    }


}
