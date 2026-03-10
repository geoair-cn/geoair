package cn.geoair.map.dynamic.dbservice.controller;

import cn.geoair.base.api.annotation.GaApi;
import cn.geoair.base.api.annotation.GaApiAction;
import cn.geoair.base.data.page.GiPageParam;
import cn.geoair.base.data.page.GiPager;
import cn.geoair.base.data.page.support.GirPager;
import cn.geoair.base.data.result.GiResult;
import cn.geoair.base.util.GutilObject;
import cn.geoair.map.dynamic.adv.mybatis.SqlMeta;
import cn.geoair.map.dynamic.dbservice.DbApiUserInfoHelper;
import cn.geoair.map.dynamic.dbservice.basic.domain.ApiConfig;
import cn.geoair.map.dynamic.dbservice.basic.domain.DataSource;
import cn.geoair.map.dynamic.dbservice.basic.domain.Group;
import cn.geoair.map.dynamic.dbservice.basic.service.ApiConfigService;
import cn.geoair.map.dynamic.dbservice.basic.service.DataSourceService;
import cn.geoair.map.dynamic.dbservice.basic.service.GroupService;
import cn.geoair.map.dynamic.dbservice.basic.util.*;
import cn.geoair.map.dynamic.dbservice.common.ResponseDto;
import cn.geoair.map.dynamic.dbservice.controller.dbapi.config.ConfigSearchVo;
import cn.geoair.map.dynamic.dbservice.dao.dbapi.DbApiConfigDao;
import cn.geoair.map.dynamic.dbservice.model.dbapi.dto.DbApiConfigDto;
import cn.geoair.map.dynamic.dbservice.model.dbapi.seo.DbApiConfigSeo;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;

import com.alibaba.druid.pool.DruidPooledConnection;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
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
@RequestMapping("/apiConfig")
@GaApi(tags = "api配置")
public class ApiConfigController {

	@Resource
	DbApiUserInfoHelper dbApiUserInfoHelper;

	@Autowired
	ApiConfigService apiConfigService;

	@Resource
	private DbApiConfigDao dbapiConfigDao;

	@Autowired
	DataSourceService dataSourceService;

	@Autowired
	GroupService groupService;

	@Value("${server.servlet.context-path:}")
	String apiContext;

	@PostMapping("/context")
	public String getContext() {
		String s = StrUtil.replaceFirst(apiContext, "/", "");
		return s;
	}

	@PostMapping("/add")
	@GaApiAction(text = "新增API")
	public ResponseDto add(@RequestBody JSONObject jo) {
		ApiConfig config = new ApiConfig();
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
		config.setCreateUserId(dbApiUserInfoHelper.getSubjectId());
		return apiConfigService.add(config);
	}

	@Deprecated
	@PostMapping("/parseParam")
	@GaApiAction(text = "转换参数")
	public ResponseDto parseParam(String sql) {
		try {
			Set<String> set = SqlEngineUtil.getEngine().parseParameter(sql);
			// 转化成前端需要的格式
			List<JSONObject> list = set.stream().map(t -> {
				JSONObject object = new JSONObject();
				object.put("value", t);
				return object;
			}).collect(Collectors.toList());
			return ResponseDto.successWithData(list);
		}
		catch (Exception e) {
			return ResponseDto.fail(e.getMessage());
		}
	}

	@GetMapping("/getAll")
	@GaApiAction(text = "查询所有的API")
	public List<ApiConfig> getAll() {
		return apiConfigService.getAll();
	}

	// 给前端使用的数据结构
	@PostMapping("/getApiTree")
	@GaApiAction(text = "获取Api树")
	public List<JSONObject> getAllApiTree() {
		return apiConfigService.getAllApiTree();
	}

	@PostMapping("/search")
	@GaApiAction(text = "查询")
	public List<ApiConfig> search(String name, String note, String path, String groupId) {
		return apiConfigService.search(name, note, path, groupId);
	}

	@PostMapping("/detail/{id}")
	@GaApiAction(text = "详情")
	public ApiConfig detail(@PathVariable String id) {
		return apiConfigService.detail(id);
	}

	@PostMapping("/copy/{id}")
	@GaApiAction(text = "复制")
	public ApiConfig copy(@PathVariable String id) {
		return apiConfigService.copy(id);
	}

	@PostMapping("/delete/{id}")
	@GaApiAction(text = "删除API")
	public void delete(@PathVariable String id) {
		apiConfigService.delete(id);
	}

	@PostMapping("/update")
	@GaApiAction(text = "更新API")
	public ResponseDto update(@RequestBody JSONObject jo) {
		ApiConfig config = new ApiConfig();
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

		return apiConfigService.update(config);
	}

	@GetMapping("/online/{id}")
	@GaApiAction(text = "上线")
	public void online(@PathVariable String id) {
		apiConfigService.online(id);
	}

	@GetMapping("/offline/{id}")
	@GaApiAction(text = "下线")
	public void offline(@PathVariable String id) {
		apiConfigService.offline(id);
	}

	@GetMapping("/apiDocs")
	@GaApiAction(text = "api文档")
	public void apiDocs(String ids, HttpServletResponse response) {
		List<String> collect = Arrays.asList(ids.split(","));
		String docs = apiConfigService.apiDocs(collect);
		response.setContentType("application/x-msdownload;charset=utf-8");
		response.setHeader("Content-Disposition", "attachment; filename=API docs.md");
		OutputStream os = null; // 输出流
		try {
			os = response.getOutputStream();
			os.write(docs.getBytes("utf-8"));
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				if (os != null)
					os.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 导出API 配置
	 * @param ids
	 * @param response
	 */
	@PostMapping("/downloadConfig")
	@GaApiAction(text = "下载API配置")
	public void downloadConfig(String ids, HttpServletResponse response) {
		List<String> collect = Arrays.asList(ids.split(","));
		JSONObject jo = apiConfigService.exportAPI(collect);
		String s = jo.toString();
		response.setContentType("application/x-msdownload;charset=utf-8");
		response.setHeader("Content-Disposition", "attachment; filename=api_config.json");
		OutputStream os = null;
		try {
			os = response.getOutputStream();
			os.write(s.getBytes("utf-8"));
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				if (os != null)
					os.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@PostMapping("/downloadGroupConfig")
	@GaApiAction(text = "下载API组配置")
	public void downloadGroupConfig(String ids, HttpServletResponse response) {
		List<String> collect = Arrays.asList(ids.split(","));
		List<Group> list = groupService.selectBatch(collect);
		String s = JSON.toJSONString(list);
		response.setContentType("application/x-msdownload;charset=utf-8");
		// response.setHeader("Content-Disposition", "attachment; filename=api配置.json");
		OutputStream os = null;
		try {
			os = response.getOutputStream();
			os.write(s.getBytes("utf-8"));
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				if (os != null)
					os.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 导入API配置
	 * @param file
	 * @throws IOException
	 */
	@PostMapping(value = "/import", produces = "application/json;charset=UTF-8")
	public void importAPI(@RequestParam("file") MultipartFile file) throws IOException {
		String s = IoUtil.read(file.getInputStream(), "utf-8");
		JSONObject jsonObject = JSON.parseObject(s);
		List<ApiConfig> apis = jsonObject.getJSONArray("api").toJavaList(ApiConfig.class);
		apis.stream().forEach(t -> {
			t.setCreateUserId(dbApiUserInfoHelper.getSubjectId());
			t.setCreateTime(DateFormatUtils.format(new Date(), "yyyy-MM-dd hh:mm:ss"));
			t.setUpdateTime(DateFormatUtils.format(new Date(), "yyyy-MM-dd hh:mm:ss"));
		});

		apiConfigService.importAPI(apis);
	}

	@PostMapping(value = "/importGroup", produces = "application/json;charset=UTF-8")
	public void importGroup(@RequestParam("file") MultipartFile file) throws IOException {
		String s = IoUtil.read(file.getInputStream(), "utf-8");
		List<Group> configs = JSON.parseArray(s, Group.class);
		configs.stream().forEach(t -> {
			t.setCreateUserId(dbApiUserInfoHelper.getSubjectId());
			t.setCreateTime(DateFormatUtils.format(new Date(), "yyyy-MM-dd hh:mm:ss"));
			t.setUpdateTime(DateFormatUtils.format(new Date(), "yyyy-MM-dd hh:mm:ss"));
		});
		groupService.insertBatch(configs);
	}

	@PostMapping("/sql/execute")
	public ResponseDto executeSql(String datasourceId, String sql, String params) {
		DruidPooledConnection connection = null;
		try {
			DataSource dataSource = dataSourceService.detail(datasourceId);
			connection = PoolManager.getPooledConnection(dataSource);
			Map<String, Object> map = JSON.parseObject(params, Map.class);
			SqlMeta sqlMeta = SqlEngineUtil.getEngine().parse(sql, map);
			Object data = JdbcUtil.executeSql(connection, sqlMeta.getSql(), sqlMeta.getJdbcParamValues(), false);
			return ResponseDto.successWithData(data);
		}
		catch (Exception e) {
			return ResponseDto.fail(e.getMessage());
		}
		finally {
			try {
				if (connection != null)
					connection.close();
			}
			catch (SQLException e) {
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
			DataSource dataSource = dataSourceService.detail(datasourceId);
			connection = PoolManager.getPooledConnection(dataSource);
			Map<String, Object> map = JSON.parseObject(params, Map.class);
			SqlMeta sqlMeta = SqlEngineUtil.getEngine().parse(sql, map);
			Object data = JdbcUtil.executeSql(connection, sqlMeta.getSql(), sqlMeta.getJdbcParamValues(), false);
			return ResponseDto.successWithData(data);
		}
		catch (Exception e) {
			return ResponseDto.fail(e.getMessage());
		}
		finally {
			try {
				if (connection != null)
					connection.close();
			}
			catch (SQLException e) {
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
		}
		catch (Exception e) {
			return ResponseDto.fail(e.getMessage());
		}
	}

	@GaApiAction(text = "分页列出api配置信息")
	@RequestMapping(value = "/listDbApiConfigPage", method = { RequestMethod.POST })
	@ResponseBody
	public GiResult<GiPager<ApiConfig>> listDbApiConfigPage(@Validated @RequestBody ConfigSearchVo param) {
		DbApiConfigSeo seo = new DbApiConfigSeo();
		BeanUtils.copyProperties(param, seo);
		seo.setNotDel();
		if (GutilObject.isNotEmpty(param.getQueryContent())) {
			seo.setAndQueryContentIn(ArrayUtil.toArray(ListUtil.of(param.getQueryContent()), String.class));
		}
		GiPager<DbApiConfigDto> giPager = dbapiConfigDao.searchListPage(seo, GiPageParam.of());
		Iterable<DbApiConfigDto> value = giPager.value();
		GirPager<ApiConfig> reg = new GirPager();
		List<ApiConfig> vdvos = ApiConfig.fromDtos(ListUtil.toList(value));
		reg.put(vdvos, giPager.total(), giPager.pageParam());
		return GiResult.successValue(reg);
	}

}
