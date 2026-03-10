package cn.geoair.map.dynamic.dbservice.core.controller;

import cn.geoair.base.api.annotation.GaApi;
import cn.geoair.base.api.annotation.GaApiAction;
import cn.geoair.map.dynamic.dbservice.core.basic.util.IPUtil;
import cn.geoair.map.dynamic.dbservice.core.config.GirDsServiceProperties;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@Slf4j
@RequestMapping("/ds_api/system")
@GaApi(tags = "系统相关")
public class SystemController {

    @Value("${server.servlet.context-path:}")
    String apiContext;

    @Resource GirDsServiceProperties girDsServiceProperties;

    //
    // @Value("${dbapi.server.port:6106}")
    // String serverPort;

    @Value("${spring.profiles.active:default}")
    String activeProfile;

    @GetMapping("/version")
    @GaApiAction(text = "获取系统版本")
    public String getVersion() {
        log.info("查询系统版本信息");
        return girDsServiceProperties.getVersion();
    }

    @GetMapping("/mode")
    @GaApiAction(text = "获取系统运行模式")
    public String mode() {
        log.info("查询系统运行模式");
        return activeProfile;
    }

    @PostMapping("/getIPPort")
    @GaApiAction(text = "获取服务器IP、端口及上下文路径")
    public String getIPPort(HttpServletRequest request) {
        try {
            int originPort = IPUtil.getOriginPort(request);
            String realApiContext1 = girDsServiceProperties.getRealApiContext();
            String realApiContext2 = StrUtil.removePrefix(realApiContext1, "/");
            String result =
                    request.getServerName() + ":" + originPort + apiContext + "/" + realApiContext2;
            log.info("获取服务器地址信息: {}", result);
            return result;
        } catch (Exception e) {
            log.error("获取服务器地址信息失败", e);
            return "获取服务器信息失败";
        }
    }

    @PostMapping("/getIP")
    @GaApiAction(text = "获取服务器IP和端口")
    public String getIP(HttpServletRequest request) {
        try {
            int originPort = IPUtil.getOriginPort(request);
            String result = request.getServerName() + ":" + originPort;
            log.info("获取服务器IP和端口: {}", result);
            return result;
        } catch (Exception e) {
            log.error("获取服务器IP和端口失败", e);
            return "获取服务器IP和端口失败";
        }
    }

    @GetMapping("/context")
    public Map<String, String> context() {
        // 动态获取上下文路径对应的完整基础URL
        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        Map<String, String> config = MapUtil.newHashMap();
        config.put("baseUrl", baseUrl);
        config.put("loginPage", "");
        return config;
    }

    @GetMapping("/health")
    @GaApiAction(text = "系统健康检查")
    public String healthCheck() {
        log.info("系统健康检查");
        return "OK";
    }
}
