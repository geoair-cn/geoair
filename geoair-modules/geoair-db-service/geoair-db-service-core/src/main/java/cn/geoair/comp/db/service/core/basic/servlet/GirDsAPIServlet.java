package cn.geoair.comp.db.service.core.basic.servlet;

import cn.geoair.base.util.GutilObject;
import cn.geoair.comp.db.service.core.basic.apo.ApiConfigApo;
import cn.geoair.comp.db.service.core.basic.executor.Executor;
import cn.geoair.comp.db.service.core.basic.executor.GirDsSQLExecutor;
import cn.geoair.comp.db.service.core.basic.service.DsApiConfigService;
import cn.geoair.comp.db.service.core.basic.service.DsApiService;
import cn.geoair.comp.db.service.core.basic.util.Constants;
import cn.geoair.comp.db.service.core.basic.util.JacksonUtils;
import cn.geoair.comp.db.service.core.common.ResponseDto;
import cn.geoair.comp.db.service.core.config.GirDsServiceProperties;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GirDsAPIServlet extends HttpServlet {

    @Autowired DsApiConfigService dsApiConfigService;

    @Autowired GirDsServiceProperties girDsServiceProperties;

    @Autowired DsApiService dsApiService;

    @Autowired GirDsSQLExecutor girDsSqlExecutor;

    ApiConfigApo config;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        log.debug("servlet execute");
        String realApiContext1 = girDsServiceProperties.getRealApiContext();
        String realApiContext2 = StrUtil.removePrefix(realApiContext1, "/");
        String property = SpringUtil.getProperty("server.servlet.context-path");
        if (GutilObject.isEmpty(property)) {
            property = "";
        }
        String realApiContext = property + "/" + realApiContext2;
        String servletPath = request.getRequestURI();
        servletPath = servletPath.substring(realApiContext.length() + 1);
        PrintWriter out = null;
        try {
            out = response.getWriter();
            ResponseDto responseDto = process(servletPath, request, response);
            // 全局数据转换
            Object res = globalTransform(responseDto);
            String json = JacksonUtils.toJSONString(res);
            out.append(json);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            ResponseDto responseDto = ResponseDto.fail(e.toString());
            // 全局数据转换
            Object res = globalTransform(responseDto);
            String json = JacksonUtils.toJSONString(res);
            out.append(json);
            log.error(e.toString(), e);
        } finally {
            if (out != null) out.close();
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        doGet(req, resp);
    }

    public ResponseDto process(
            String path, HttpServletRequest request, HttpServletResponse response) {
        // // 校验接口是否存在
        this.config = dsApiConfigService.getConfig(path);
        if (config == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return ResponseDto.fail("Api not exists");
        }
        try {
            Map<String, Object> requestParam = getParams(request, config);
            List<Object> executorResults = new ArrayList<>();
            JSONArray tasks = config.getTaskJson();
            for (int i = 0; i < tasks.size(); i++) {
                JSONObject task = tasks.getJSONObject(i);
                int type = task.getIntValue("taskType");
                Executor executor;

                if (type == Constants.API_EXECUTOR_SQL) executor = girDsSqlExecutor;
                else if (type == Constants.API_EXECUTOR_HTTP) executor = girDsSqlExecutor;
                else if (type == Constants.API_EXECUTOR_ES) executor = girDsSqlExecutor;
                else throw new RuntimeException("Executor type unknown!");
                Object res = executor.execute(task, requestParam);
                executorResults.add(res);
            }
            // 如果只有一个执行器就不返回数组格式的数据，返回对象格式
            Object result = executorResults.size() == 1 ? executorResults.get(0) : executorResults;

            return ResponseDto.apiSuccess(result);

        } catch (Exception e) {
            log.error("API服务调用异常", e);
            throw new RuntimeException(e.getMessage());
        }
    }

    private Map<String, Object> getParams(HttpServletRequest request, ApiConfigApo apiConfigApo) {
        /**
         * Content-Type格式说明: {@see <a href=
         * "https://www.w3.org/Protocols/rfc1341/4_Content-Type.html">Content-Type</a>}
         * type/subtype(;parameter)? type
         */
        String unParseContentType = request.getContentType();

        // 如果是浏览器get请求过来，取出来的contentType是null
        if (unParseContentType == null) {
            unParseContentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE;
        }
        // issues/I57ZG2
        // 解析contentType 格式: appliation/json;charset=utf-8
        String[] contentTypeArr = unParseContentType.split(";");
        String contentType = contentTypeArr[0];

        Map<String, Object> params = null;
        // 如果是application/json请求，不管接口规定的content-type是什么，接口都可以访问，且请求参数都以json body 为准
        if (contentType.equalsIgnoreCase(MediaType.APPLICATION_JSON_VALUE)) {
            JSONObject jo = getHttpJsonBody(request);
            params =
                    JSONObject.parseObject(
                            jo.toJSONString(), new TypeReference<Map<String, Object>>() {});
        }
        // 如果是application/x-www-form-urlencoded请求，先判断接口规定的content-type是不是确实是application/x-www-form-urlencoded
        else if (contentType.equalsIgnoreCase(MediaType.APPLICATION_FORM_URLENCODED_VALUE)) {
            if (MediaType.APPLICATION_FORM_URLENCODED_VALUE.equalsIgnoreCase(
                    apiConfigApo.getContentType())) {
                params = dsApiService.getSqlParam(request, apiConfigApo);
            } else {
                throw new RuntimeException(
                        "This API only supports content-type: "
                                + apiConfigApo.getContentType()
                                + ", but you use: "
                                + contentType);
            }
        } else {
            throw new RuntimeException("Content-type not supported: " + contentType);
        }

        return params;
    }

    private JSONObject getHttpJsonBody(HttpServletRequest request) {
        try {
            InputStreamReader in =
                    new InputStreamReader(request.getInputStream(), StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(in);
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
            JSONObject jsonObject = JSON.parseObject(sb.toString());
            return jsonObject;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {

        }
        return null;
    }

    /**
     * 全局转换数据
     *
     * @param responseDto
     * @return
     */
    private Object globalTransform(ResponseDto responseDto) {
        return responseDto;
    }
}
