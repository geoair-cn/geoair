package cn.geoair.map.dynamic.geoserver.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@EnableAutoConfiguration(exclude = SecurityAutoConfiguration.class)
@ConditionalOnClass(SecurityAutoConfiguration.class)
public class DisableSecurityConfig {

}
