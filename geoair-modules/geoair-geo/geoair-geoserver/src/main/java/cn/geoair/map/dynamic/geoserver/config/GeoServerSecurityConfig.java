package cn.geoair.map.dynamic.geoserver.config;

import cn.geoair.base.Gir;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

// import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

/** 自定义 Spring Security 配置，消除默认密码警告 */
@Configuration
// @EnableWebSecurity
public class GeoServerSecurityConfig {

    public GeoServerSecurityConfig() {
        Gir.log.info("GeoServer 密码会覆盖系统原来的SpringSecurity，如果需要，可以直接移除这个bean");
    }

    // // 自定义用户服务，覆盖默认逻辑（避免自动生成密码）
    @Bean
    @ConditionalOnMissingBean(UserDetailsService.class)
    public UserDetailsService userDetailsService() {
        // 可根据 GeoServer 配置动态加载用户，此处为示例
        UserDetails defaultUser =
                User.withUsername("admin")
                        .passwordEncoder(new BCryptPasswordEncoder()::encode)
                        .password("geoserver")
                        .roles("ADMIN")
                        .build();
        return new InMemoryUserDetailsManager(defaultUser);
    }

    //
    // @Bean
    // @ConditionalOnMissingBean(PasswordEncoder.class)
    // public org.springframework.security.crypto.password.PasswordEncoder
    // passwordEncoder() {
    // return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    // }

    // 自定义安全过滤链，适配 GeoServer 接口
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 精准配置路径权限：仅 /geoserver/** 无需登录，其他路径也匿名访问
                .authorizeHttpRequests(
                        auth ->
                                auth.antMatchers("/geoserver/**")
                                        .permitAll() //  /geoserver/**
                                        // 无需登录
                                        .anyRequest()
                                        .permitAll() // 其他路径也允许匿名
                        )
                .formLogin()
                .disable()
                .httpBasic()
                .disable()
                .csrf()
                .disable()
                .logout()
                .disable()
                .sessionManagement()
                .disable();

        return http.build();
    }
}
