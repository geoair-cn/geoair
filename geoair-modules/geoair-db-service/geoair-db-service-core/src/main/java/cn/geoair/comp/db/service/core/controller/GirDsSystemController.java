package cn.geoair.comp.db.service.core.controller;

import cn.geoair.base.api.annotation.GaApi;
import cn.geoair.base.api.annotation.GaApiAction;
import cn.geoair.base.util.GutilObject;
import cn.geoair.comp.db.service.core.basic.util.IPUtil;
import cn.geoair.comp.db.service.core.config.GirDsServiceProperties;
import cn.geoair.comp.db.service.core.utils.TokenManager;
import cn.geoair.map.dynamic.tools.simple.GirServletUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson2.JSONObject;
import java.util.Map;
import javax.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@Slf4j
@RequestMapping("/ds_api/system")
@GaApi(tags = "GirDs系统相关")
public class GirDsSystemController {

    @Resource GirDsServiceProperties girDsServiceProperties;

    //
    // @Value("${dbapi.server.port:6106}")
    // String serverPort;

    @Value("${spring.profiles.active:default}")
    String activeProfile;

    @GetMapping("/version")
    @GaApiAction(text = "获取系统版本")
    public String getVersion() {
        log.debug("查询系统版本信息");
        return girDsServiceProperties.getVersion();
    }

    @GetMapping("/login")
    @GaApiAction(text = "登录")
    public String login(@RequestParam String username, @RequestParam String password) {
        JSONObject result = new JSONObject();
        boolean enableLogin = girDsServiceProperties.isEnableLogin();

        if (enableLogin) {
            if (username.equals(girDsServiceProperties.getDefaultUser())
                    && password.equals(girDsServiceProperties.getDefaultPassword())) {
                String token = TokenManager.generateToken(username, password);
                // 组装返回结果
                result.put("success", true);
                result.put("message", "登录成功");
                JSONObject data = new JSONObject();
                data.put("token", token);
                data.put("username", username);
                result.put("data", data);
            } else {
                result.put("success", false);
                result.put("message", "用户名或密码错误");
                result.put("data", new JSONObject());
            }
        } else {
            String token =
                    TokenManager.generateToken(
                            girDsServiceProperties.getDefaultUser(),
                            girDsServiceProperties.getDefaultPassword());
            result.put("success", true);
            result.put("message", "免登录成功");
            JSONObject data = new JSONObject();
            data.put("token", token);
            data.put("username", username);
            result.put("data", data);
        }
        return result.toString();
    }

    @GetMapping("/validateToken")
    @GaApiAction(text = "验证token")
    public String validateToken(String token) {
        JSONObject result = new JSONObject();
        result.put("success", TokenManager.validateToken(token));
        return result.toString();
    }

    @GetMapping("/logout")
    @GaApiAction(text = "登出")
    public String logout(String token) {
        JSONObject result = new JSONObject();
        result.put("success", true);
        result.put("message", "登出成功");
        return result.toString();
    }

    @GetMapping("/mode")
    @GaApiAction(text = "获取系统运行模式")
    public String mode() {
        log.debug("查询系统运行模式");
        return activeProfile;
    }

    @PostMapping("/getIPPort")
    @GaApiAction(text = "获取服务器IP、端口及上下文路径")
    public String getIPPort(HttpServletRequest request) {
        try {
            int originPort = IPUtil.getOriginPort(request);
            String property = SpringUtil.getProperty("server.servlet.context-path");
            if (GutilObject.isEmpty(property)) {
                property = "";
            }
            String realApiContext1 = girDsServiceProperties.getRealApiContext();
            String realApiContext2 = StrUtil.removePrefix(realApiContext1, "/");
            String result =
                    request.getServerName() + ":" + originPort + property + "/" + realApiContext2;
            log.debug("获取服务器地址信息: {}", result);
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
            log.debug("获取服务器IP和端口: {}", result);
            return result;
        } catch (Exception e) {
            log.error("获取服务器IP和端口失败", e);
            return "获取服务器IP和端口失败";
        }
    }

    @GetMapping("/context")
    @GaApiAction(text = "获取上下文")
    public Map<String, Object> context(HttpServletRequest request) {
        String baseUrl = null;
        String serviceUrl = girDsServiceProperties.getServiceUrl();
        if (StrUtil.isNotBlank(serviceUrl)) {
            baseUrl = serviceUrl;
        } else {
            // 动态获取上下文路径对应的完整基础URL
            baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        }

        Map<String, Object> config = MapUtil.newHashMap();
        config.put("baseUrl", baseUrl);
        config.put("byGirServlet", GirServletUtil.getClientIP(request));
        config.put("byServerInfoByRequest", GirServletUtil.getServerInfoByRequest());
        config.put(
                "byCurrentContextPath",
                ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString());
        config.put("loginPage", "");
        return config;
    }

    @GetMapping("/health")
    @GaApiAction(text = "系统健康检查")
    public String healthCheck() {
        log.debug("系统健康检查");
        return "OK";
    }
}
