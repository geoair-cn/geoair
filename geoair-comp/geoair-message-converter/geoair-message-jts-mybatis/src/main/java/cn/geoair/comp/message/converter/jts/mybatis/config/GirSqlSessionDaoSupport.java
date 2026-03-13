//package cn.geoair.comp.message.converter.jts.mybatis.config;
//
//import org.apache.ibatis.session.Configuration;
//import org.apache.ibatis.type.TypeHandlerRegistry;
//import org.mybatis.spring.support.SqlSessionDaoSupport;
//import org.springframework.beans.factory.FactoryBean;
//
//
//public class GirSqlSessionDaoSupport<T> extends SqlSessionDaoSupport implements FactoryBean<T> {
//
//    private Class<T> mapperInterface;
//
//    private boolean addToConfig = true;
//
//
//    public GirSqlSessionDaoSupport() {
//        //intentionally empty
//    }
//
//    public GirSqlSessionDaoSupport(Class<T> mapperInterface) {
//        this.mapperInterface = mapperInterface;
//    }
//
//    /**
//     * {@inheritDoc}
//     */
//    @Override
//    protected void checkDaoConfig() {
//
//        Configuration configuration = getSqlSession().getConfiguration();
//        TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
//    }
//
//    /**
//     * Return the mapper interface of the MyBatis mapper
//     *
//     * @return class of the interface
//     */
//    public Class<T> getMapperInterface() {
//        return mapperInterface;
//    }
//
//    /**
//     * Sets the mapper interface of the MyBatis mapper
//     *
//     * @param mapperInterface class of the interface
//     */
//    public void setMapperInterface(Class<T> mapperInterface) {
//        this.mapperInterface = mapperInterface;
//    }
//
//    /**
//     * {@inheritDoc}
//     */
//    @Override
//    public T getObject() throws Exception {
//        return getSqlSession().getMapper(this.mapperInterface);
//    }
//
//    //------------- mutators --------------
//
//    /**
//     * {@inheritDoc}
//     */
//    @Override
//    public Class<T> getObjectType() {
//        return this.mapperInterface;
//    }
//
//    /**
//     * Return the flag for addition into MyBatis config.
//     *
//     * @return true if the mapper will be added to MyBatis in the case it is not already
//     * registered.
//     */
//    public boolean isAddToConfig() {
//        return addToConfig;
//    }
//
//    /**
//     * If addToConfig is false the mapper will not be added to MyBatis. This means
//     * it must have been included in mybatis-config.xml.
//     * <p/>
//     * If it is true, the mapper will be added to MyBatis in the case it is not already
//     * registered.
//     * <p/>
//     * By default addToCofig is true.
//     *
//     * @param addToConfig
//     */
//    public void setAddToConfig(boolean addToConfig) {
//        this.addToConfig = addToConfig;
//    }
//
//    /**
//     * {@inheritDoc}
//     */
//    @Override
//    public boolean isSingleton() {
//        return true;
//    }
//}
