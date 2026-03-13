//package cn.geoair.comp.message.converter.jts.mybatis.config;
//
//import cn.geoair.base.log.GiLogger;
//import cn.geoair.base.log.GirLogger;
//import cn.geoair.comp.message.converter.jts.mybatis.typehander.NetPgGeometryTypeHandler;
//import cn.geoair.comp.message.converter.jts.mybatis.typehander.OrgPgGeometryTypeHandler;
//import cn.geoair.map.dynamic.tools.convert.GirPostGisTran;
//import org.apache.ibatis.session.Configuration;
//import org.apache.ibatis.type.TypeHandlerRegistry;
//import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
//import org.springframework.stereotype.Component;
//
///**
// * @author ：张逢吉
// * @date ：Created in   16:30
// * @description： TODO
// */
//
//@Component
//public class GirMyBatisConfigurationCustomizer implements ConfigurationCustomizer {
//    GiLogger log = GirLogger.getLoger(GirMyBatisConfigurationCustomizer.class);
//
//    @Override
//    public void customize(Configuration configuration) {
//        TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
//        if (GirPostGisTran.isNetConvert()) {
//            NetPgGeometryTypeHandler.register(typeHandlerRegistry);
//            log.debug("NetPGGeometryModule 注册" );
//        }
//        if (GirPostGisTran.isOrgConvert()) {
//            OrgPgGeometryTypeHandler.register(typeHandlerRegistry);
//            log.debug("OrgPGGeometryModule 注册" );
//        }
//    }
//}
