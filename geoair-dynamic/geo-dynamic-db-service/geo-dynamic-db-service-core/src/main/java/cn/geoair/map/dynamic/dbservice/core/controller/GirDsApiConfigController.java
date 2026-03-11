package cn.geoair.map.dynamic.dbservice.core.controller;

import cn.geoair.base.api.annotation.GaApi;
import cn.geoair.base.api.annotation.GaApiAction;
import cn.geoair.map.dynamic.adv.mybatis.SqlMeta;
import cn.geoair.map.dynamic.dbservice.core.DsApiUserInfoHelper;
import cn.geoair.map.dynamic.dbservice.core.basic.apo.ApiConfigApo;
import cn.geoair.map.dynamic.dbservice.core.basic.apo.DataSourceApo;
import cn.geoair.map.dynamic.dbservice.core.basic.apo.GroupApo;
import cn.geoair.map.dynamic.dbservice.core.basic.service.DsApiConfigService;
import cn.geoair.map.dynamic.dbservice.core.basic.service.DsDataSourceService;
import cn.geoair.map.dynamic.dbservice.core.basic.service.DsGroupService;
import cn.geoair.map.dynamic.dbservice.core.basic.util.*;
import cn.geoair.map.dynamic.dbservice.core.common.ResponseDto;
import cn.geoair.map.dynamic.dbservice.core.utils.TokenManager;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;

import com.alibaba.druid.pool.DruidPooledConnection;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

/**
 * @program: dbApi
 * @description:
 * @author: 武汉刘德华
 * @create: 2021-01-19 17:27
 */
@RestController
@Slf4j
@RequestMapping("/ds_api/apiConfig")
@GaApi(tags = "GirDs api配置")
public class GirDsApiConfigController {

    @Resource DsApiUserInfoHelper dsApiUserInfoHelper;

    @Autowired DsApiConfigService dsApiConfigService;

    @Autowired DsDataSourceService dsDataSourceService;

    @Autowired DsGroupService dsGroupService;

    @Value("${server.servlet.context-path:}")
    String apiContext;

    @PostMapping("/context")
    @GaApiAction(text = "获取上下文")
    public String getContext() {
        String s = StrUtil.replaceFirst(apiContext, "/", "");
        return s;
    }

    @PostMapping("/add")
    @GaApiAction(text = "新增API")
    public ResponseDto add(@RequestBody JSONObject jo) {
        TokenManager.validateToken();
        ApiConfigApo config = new ApiConfigApo();
        config.setName(jo.getString("name"));
        config.setPath(jo.getString("path"));
        config.setNote(jo.getString("note"));
        config.setGroupId(jo.getString("groupId"));
        config.setContentType(jo.getString("contentType"));
        config.setJsonParam(jo.getString("jsonParam"));
        config.setParams(jo.getJSONArray("paramsJson").toString());
        config.setAccess(jo.getInteger("access"));
        config.setTask(jo.getJSONArray("taskJson").toString());
        config.setStatus(Constants.API_STATUS_OFFLINE);
        String id = UUIDUtil.id();
        config.setId(id);
        config.setCreateUserId(dsApiUserInfoHelper.getSubjectId());
        return dsApiConfigService.add(config);
    }

    @Deprecated
    @PostMapping("/parseParam")
    @GaApiAction(text = "转换参数")
    public ResponseDto parseParam(String sql) {
        try {
            Set<String> set = SqlEngineUtil.getEngine().parseParameter(sql);
            // 转化成前端需要的格式
            List<JSONObject> list =
                    set.stream()
                            .map(
                                    t -> {
                                        JSONObject object = new JSONObject();
                                        object.put("value", t);
                                        return object;
                                    })
                            .collect(Collectors.toList());
            return ResponseDto.successWithData(list);
        } catch (Exception e) {
            return ResponseDto.fail(e.getMessage());
        }
    }

    @GetMapping("/getAll")
    @GaApiAction(text = "查询所有的API")
    public List<ApiConfigApo> getAll() {
        TokenManager.validateToken();
        return dsApiConfigService.getAll();
    }

    // 给前端使用的数据结构
    @PostMapping("/getApiTree")
    @GaApiAction(text = "获取Api树")
    public List<JSONObject> getAllApiTree() {
        TokenManager.validateToken();
        return dsApiConfigService.getAllApiTree();
    }

    @PostMapping("/search")
    @GaApiAction(text = "查询")
    public List<ApiConfigApo> search(String name, String note, String path, String groupId) {
        TokenManager.validateToken();
        return dsApiConfigService.search(name, note, path, groupId);
    }

    @PostMapping("/detail/{id}")
    @GaApiAction(text = "详情")
    public ApiConfigApo detail(@PathVariable String id) {
        return dsApiConfigService.detail(id);
    }

    @PostMapping("/copy/{id}")
    @GaApiAction(text = "复制")
    public ApiConfigApo copy(@PathVariable String id) {
        TokenManager.validateToken();
        return dsApiConfigService.copy(id);
    }

    @PostMapping("/delete/{id}")
    @GaApiAction(text = "删除API")
    public void delete(@PathVariable String id) {
        TokenManager.validateToken();
        dsApiConfigService.delete(id);
    }

    @PostMapping("/update")
    @GaApiAction(text = "更新API")
    public ResponseDto update(@RequestBody JSONObject jo) {
        TokenManager.validateToken();
        ApiConfigApo config = new ApiConfigApo();
        config.setId(jo.getString("id"));
        config.setName(jo.getString("name"));
        config.setPath(jo.getString("path"));
        config.setNote(jo.getString("note"));
        config.setGroupId(jo.getString("groupId"));
        config.setContentType(jo.getString("contentType"));
        config.setJsonParam(jo.getString("jsonParam"));
        config.setParams(jo.getJSONArray("paramsJson").toString());
        config.setAccess(jo.getInteger("access"));
        config.setTask(jo.getJSONArray("taskJson").toString());
        // config.setStatus(Constants.API_STATUS_OFFLINE); // 更新后不更新状态

        return dsApiConfigService.update(config);
    }

    @GetMapping("/online/{id}")
    @GaApiAction(text = "上线")
    public void online(@PathVariable String id) {
        TokenManager.validateToken();
        dsApiConfigService.online(id);
    }

    @GetMapping("/offline/{id}")
    @GaApiAction(text = "下线")
    public void offline(@PathVariable String id) {
        TokenManager.validateToken();
        dsApiConfigService.offline(id);
    }

    @GetMapping("/apiDocs")
    @GaApiAction(text = "api文档")
    public void apiDocs(String ids, HttpServletResponse response) {
        List<String> collect = Arrays.asList(ids.split(","));
        String docs = dsApiConfigService.apiDocs(collect);
        response.setContentType("application/x-msdownload;charset=utf-8");
        response.setHeader("Content-Disposition", "attachment; filename=API docs.md");
        OutputStream os = null; // 输出流
        try {
            os = response.getOutputStream();
            os.write(docs.getBytes("utf-8"));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (os != null) os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 导出API 配置
     *
     * @param ids
     * @param response
     */
    @PostMapping("/downloadConfig")
    @GaApiAction(text = "下载API配置")
    public void downloadConfig(String ids, HttpServletResponse response) {
        List<String> collect = Arrays.asList(ids.split(","));
        JSONObject jo = dsApiConfigService.exportAPI(collect);
        String s = jo.toString();
        response.setContentType("application/x-msdownload;charset=utf-8");
        response.setHeader("Content-Disposition", "attachment; filename=api_config.json");
        OutputStream os = null;
        try {
            os = response.getOutputStream();
            os.write(s.getBytes("utf-8"));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (os != null) os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @PostMapping("/downloadGroupConfig")
    @GaApiAction(text = "下载API组配置")
    public void downloadGroupConfig(String ids, HttpServletResponse response) {
        List<String> collect = Arrays.asList(ids.split(","));
        List<GroupApo> list = dsGroupService.selectBatch(collect);
        String s = JSON.toJSONString(list);
        response.setContentType("application/x-msdownload;charset=utf-8");
        // response.setHeader("Content-Disposition", "attachment; filename=api配置.json");
        OutputStream os = null;
        try {
            os = response.getOutputStream();
            os.write(s.getBytes("utf-8"));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (os != null) os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 导入API配置
     *
     * @param file
     * @throws IOException
     */
    @PostMapping(value = "/import", produces = "application/json;charset=UTF-8")
    @GaApiAction(text = "导入API配置")
    public void importAPI(@RequestParam("file") MultipartFile file) throws IOException {
        String s = IoUtil.read(file.getInputStream(), "utf-8");
        JSONObject jsonObject = JSON.parseObject(s);
        List<ApiConfigApo> apis = jsonObject.getJSONArray("api").toJavaList(ApiConfigApo.class);
        apis.stream()
                .forEach(
                        t -> {
                            t.setCreateUserId(dsApiUserInfoHelper.getSubjectId());
                            t.setCreateTime(
                                    DateFormatUtils.format(new Date(), "yyyy-MM-dd hh:mm:ss"));
                            t.setUpdateTime(
                                    DateFormatUtils.format(new Date(), "yyyy-MM-dd hh:mm:ss"));
                        });

        dsApiConfigService.importAPI(apis);
    }

    @PostMapping(value = "/importGroup", produces = "application/json;charset=UTF-8")
    @GaApiAction(text = "导入API配置")
    public void importGroup(@RequestParam("file") MultipartFile file) throws IOException {
        String s = IoUtil.read(file.getInputStream(), "utf-8");
        List<GroupApo> configs = JSON.parseArray(s, GroupApo.class);
        configs.stream()
                .forEach(
                        t -> {
                            t.setCreateUserId(dsApiUserInfoHelper.getSubjectId());
                            t.setCreateTime(
                                    DateFormatUtils.format(new Date(), "yyyy-MM-dd hh:mm:ss"));
                            t.setUpdateTime(
                                    DateFormatUtils.format(new Date(), "yyyy-MM-dd hh:mm:ss"));
                        });
        dsGroupService.insertBatch(configs);
    }

    @PostMapping("/sql/execute")
    @GaApiAction(text = "sql执行")
    public ResponseDto executeSql(String datasourceId, String sql, String params) {
        DruidPooledConnection connection = null;
        try {
            DataSourceApo dataSourceApo = dsDataSourceService.detail(datasourceId);
            connection = PoolManager.getPooledConnection(dataSourceApo);
            Map<String, Object> map = JSON.parseObject(params, Map.class);
            SqlMeta sqlMeta = SqlEngineUtil.getEngine().parse(sql, map);
            Object data =
                    JdbcUtil.executeSql(
                            connection, sqlMeta.getSql(), sqlMeta.getJdbcParamValues(), false);
            return ResponseDto.successWithData(data);
        } catch (Exception e) {
            return ResponseDto.fail(e.getMessage());
        } finally {
            try {
                if (connection != null) connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @PostMapping("/sql/executeV2")
    public ResponseDto executeSql(@RequestBody JSONObject jo) {
        String datasourceId = jo.getString("datasourceId");
        String params = jo.getString("params");
        String sql = jo.getString("sql");
        DruidPooledConnection connection = null;
        try {
            DataSourceApo dataSourceApo = dsDataSourceService.detail(datasourceId);
            connection = PoolManager.getPooledConnection(dataSourceApo);
            Map<String, Object> map = JSON.parseObject(params, Map.class);
            SqlMeta sqlMeta = SqlEngineUtil.getEngine().parse(sql, map);
            Object data =
                    JdbcUtil.executeSql(
                            connection, sqlMeta.getSql(), sqlMeta.getJdbcParamValues(), false);
            return ResponseDto.successWithData(data);
        } catch (Exception e) {
            return ResponseDto.fail(e.getMessage());
        } finally {
            try {
                if (connection != null) connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @PostMapping("/parseDynamicSql")
    public ResponseDto parseDynamicSql(String sql, String params) {
        try {
            Map<String, Object> map = JSON.parseObject(params, Map.class);
            SqlMeta sqlMeta = SqlEngineUtil.getEngine().parse(sql, map);
            return ResponseDto.successWithData(sqlMeta);
        } catch (Exception e) {
            return ResponseDto.fail(e.getMessage());
        }
    }
}
