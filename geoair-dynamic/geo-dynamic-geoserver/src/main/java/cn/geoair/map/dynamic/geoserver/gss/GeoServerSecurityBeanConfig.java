package cn.geoair.map.dynamic.geoserver.gss;

import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.security.*;
import org.geoserver.security.filter.*;
import org.geoserver.security.impl.*;
import org.geoserver.security.password.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/** 替代 GeoServer 原生 applicationSecurityContext.xml 的配置类 手动定义安全相关 Bean，无需引入原生 XML */
@Configuration
public class GeoServerSecurityBeanConfig {

    //    @Bean
    //    @DependsOn("extensions") // 保证 extensions 先初始化
    //    public GeoServerSecurityManager authenticationManager(GeoServerDataDirectory
    // dataDirectory)
    //            throws Exception {
    //        GeoServerSecurityManager securityManager = new
    // GeoServerSecurityManager(dataDirectory);
    //
    //        return securityManager;
    //    }

    // 别名：对应 XML 中的 <alias name="authenticationManager" alias="geoServerSecurityManager"/>
    //    @Bean
    //    public GeoServerSecurityManager geoServerSecurityManager(
    //            GeoServerSecurityManager authenticationManager) {
    //        return authenticationManager;
    //    }

    // ===================== 过滤链代理 =====================
    //    @Bean
    //    public GeoServerSecurityFilterChainProxy filterChainProxy(
    //            GeoServerSecurityManager authenticationManager) {
    //        return new GeoServerSecurityFilterChainProxy(authenticationManager);
    //    }

    //    // ===================== 密码编码器 =====================
    //    @Bean
    //    public GeoServerEmptyPasswordEncoder emptyPasswordEncoder() {
    //        GeoServerEmptyPasswordEncoder encoder = new GeoServerEmptyPasswordEncoder();
    //        encoder.setPrefix("empty");
    //        return encoder;
    //    }
    //
    //    @Bean
    //    public GeoServerPlainTextPasswordEncoder plainTextPasswordEncoder() {
    //        GeoServerPlainTextPasswordEncoder encoder = new GeoServerPlainTextPasswordEncoder();
    //        encoder.setPrefix("plain");
    //        return encoder;
    //    }
    //
    //    @Bean
    //    @Scope("prototype") // 对应 XML 中的 scope="prototype"
    //    public GeoServerPBEPasswordEncoder pbePasswordEncoder() {
    //        GeoServerPBEPasswordEncoder encoder = new GeoServerPBEPasswordEncoder();
    //        encoder.setPrefix("crypt1");
    //        encoder.setAlgorithm("PBEWITHMD5ANDDES");
    //        return encoder;
    //    }
    //
    //    @Bean
    //    @Scope("prototype")
    //    public GeoServerPBEPasswordEncoder strongPbePasswordEncoder() {
    //        GeoServerPBEPasswordEncoder encoder = new GeoServerPBEPasswordEncoder();
    //        encoder.setPrefix("crypt2");
    //        encoder.setProviderName("BC");
    //        encoder.setAlgorithm("PBEWITHSHA256AND256BITAES-CBC-BC");
    //        encoder.setAvailableWithoutStrongCryptogaphy(false);
    //        return encoder;
    //    }

    //    @Bean
    //    @Scope("prototype")
    //    public GeoServerDigestPasswordEncoder digestPasswordEncoder() {
    //        GeoServerDigestPasswordEncoder encoder = new GeoServerDigestPasswordEncoder();
    //        encoder.setPrefix("digest1");
    //        return encoder;
    //    }

    // ===================== 规则 DAO =====================
    @Bean
    public DataAccessRuleDAO accessRulesDao(
            GeoServerDataDirectory dataDirectory, Catalog rawCatalog) throws IOException {
        return new GssDataRuleDAO(dataDirectory, rawCatalog);
    }

    @Bean
    public ServiceAccessRuleDAO serviceRulesDao(
            GeoServerDataDirectory dataDirectory, Catalog rawCatalog) throws IOException {
        return new GssServiceDAO(dataDirectory, rawCatalog);
    }

    @Bean
    public RESTAccessRuleDAO restRulesDao(GeoServerDataDirectory dataDirectory) throws IOException {
        return new GssRESTAccessRuleDAO(dataDirectory);
    }

    // ===================== 安全提供者 =====================
    //    @Bean
    //    public XMLSecurityProvider xmlSecurityProvider() {
    //        return new XMLSecurityProvider();
    //    }

    //    @Bean
    //    public J2eeSecurityProvider j2eeSecurityProvider() {
    //        return new J2eeSecurityProvider();
    //    }

    //    @Bean
    //    public GeoServerBasicAuthenticationProvider basicAuthSecurityProvider() {
    //        return new GeoServerBasicAuthenticationProvider();
    //    }

    //    @Bean
    //    public GeoServerDigestAuthenticationProvider digestAuthSecurityProvider() {
    //        return new GeoServerDigestAuthenticationProvider();
    //    }

    // 其他 Provider 按此方式定义...

    // ===================== 辅助 Bean =====================
    //    @Bean
    //    public GeoServerRoleConverterImpl roleConverter() {
    //        GeoServerRoleConverterImpl converter = new GeoServerRoleConverterImpl();
    //        converter.setRoleDelimiterString(";");
    //        converter.setRoleParameterDelimiterString(",");
    //        converter.setRoleParameterStartString("(");
    //        converter.setRoleParameterEndString(")");
    //        converter.setRoleParameterAssignmentString("=");
    //        return converter;
    //    }

    //    @Bean
    //    public BruteForceListener bruteForceListener(
    //            GeoServerSecurityManager geoServerSecurityManager) {
    //        return new BruteForceListener(geoServerSecurityManager);
    //    }

    //    @Bean
    //    public RememberMeServicesFactoryBean rememberMeServices(
    //            GeoServerSecurityManager geoServerSecurityManager) {
    //        return new RememberMeServicesFactoryBean(geoServerSecurityManager);
    //    }
}
