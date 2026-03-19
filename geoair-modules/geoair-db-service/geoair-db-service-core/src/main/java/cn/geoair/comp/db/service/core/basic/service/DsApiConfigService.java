package cn.geoair.comp.db.service.core.basic.service;

import cn.geoair.comp.db.service.core.DsApiUserInfoHelper;
import cn.geoair.comp.db.service.core.basic.apo.ApiConfigApo;
import cn.geoair.comp.db.service.core.basic.apo.GroupApo;
import cn.geoair.comp.db.service.core.basic.util.Constants;
import cn.geoair.comp.db.service.core.basic.util.UUIDUtil;
import cn.geoair.comp.db.service.core.common.ResponseDto;
import cn.geoair.comp.db.service.core.dao.GirDsApiConfigDao;
import cn.geoair.comp.db.service.core.dao.GirDsApiGroupDao;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.ListUtil;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @program: dbApi
 * @description:
 * @author: 武汉刘德华
 * @create: 2021-01-19 17:27
 */
@Slf4j
@Service
public class DsApiConfigService {

	@Autowired
	GirDsApiGroupDao girDsApiGroupDao;

	@Autowired
	GirDsApiConfigDao girDsApiConfigDao;

	// @Autowired
	// CacheManager cacheManager;

	@Value("${server.servlet.context-path}")
	String apiContext;

	@Resource
	DsApiUserInfoHelper dsApiUserInfoHelper;

	@Transactional
	public ResponseDto add(ApiConfigApo apiConfigApo) {
		int size = girDsApiConfigDao.selectCountByPath(apiConfigApo.getPath());
		if (size > 0) {
			return ResponseDto.fail("Path has been used!");
		}
		else {

			if (MediaType.APPLICATION_JSON_VALUE.equals(apiConfigApo.getContentType())) {
				apiConfigApo.setParams("[]"); // 不能设置null 前端使用会报错
			}
			else if (MediaType.APPLICATION_FORM_URLENCODED_VALUE.equals(apiConfigApo.getContentType())) {
				apiConfigApo.setJsonParam(null);
			}
			String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
			apiConfigApo.setCreateTime(now);
			apiConfigApo.setUpdateTime(now);
			girDsApiConfigDao.accessSelective(apiConfigApo);
			return ResponseDto.successWithMsg("Create API success");
		}
	}

	@Transactional
	public ResponseDto update(ApiConfigApo apiConfigApo) {

		int size = girDsApiConfigDao.selectCountByPathWhenUpdate(apiConfigApo.getPath(), apiConfigApo.getId());
		if (size > 0) {
			return ResponseDto.fail("Path has been used");
		}
		else {

			// clean data cache if cache plugin configured before
			ApiConfigApo oldConfig = detail(apiConfigApo.getId());
			cleanDataCacheAndMetaCache(oldConfig);

			if (MediaType.APPLICATION_JSON_VALUE.equals(apiConfigApo.getContentType())) {
				apiConfigApo.setParams("[]"); // 不能设置null 前端使用会报错
			}
			else if (MediaType.APPLICATION_FORM_URLENCODED_VALUE.equals(apiConfigApo.getContentType())) {
				apiConfigApo.setJsonParam(null);
			}
			String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
			apiConfigApo.setUpdateTime(now);
			// DbApiConfigPo updatePo = apiConfig.toPo();
			// updatePo.initUpdateMeta();
			// updatePo.setNameUpdate(dbApiUserInfoHelper.getSubjectName());
			girDsApiConfigDao.updateSelectiveById(apiConfigApo);

			return ResponseDto.successWithMsg("Update API Success");
		}
	}

	@Transactional
	public void delete(String id) {
		ApiConfigApo oldConfig = detail(id);
		cleanDataCacheAndMetaCache(oldConfig);
		girDsApiConfigDao.deleteById(id);
	}

	/**
	 * 刪除API相关的元数据缓存和 API配置的插件对应的数据缓存
	 * @param apiConfigApo
	 */
	private void cleanDataCacheAndMetaCache(ApiConfigApo apiConfigApo) {
		// 清除API相关的元数据缓存
		// cacheManager.getCache("api").evictIfPresent(apiConfigApo.getPath());
	}

	/**
	 * get API full detail
	 * @param id
	 * @return
	 */
	public ApiConfigApo detail(String id) {
		ApiConfigApo apiConfigApo = girDsApiConfigDao.getById(id);
		// ApiConfig apiConfig = ApiConfig.fromPo(dbApiConfigPo);
		enhanceApiConfig(apiConfigApo);
		return apiConfigApo;
	}

	/**
	 * get API full detail
	 * @param id
	 * @return
	 */
	public ApiConfigApo copy(String id) {
		ApiConfigApo apiConfigApo = girDsApiConfigDao.getById(id);
		// ApiConfig apiConfig = ApiConfig.fromPo(dbApiConfigPo);
		enhanceApiConfig(apiConfigApo);
		ApiConfigApo copy = new ApiConfigApo();
		BeanUtil.copyProperties(apiConfigApo, copy);
		String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
		copy.setCreateTime(now);
		copy.setUpdateTime(now);
		// DbApiConfigPo insertPo = apiConfig.toPo();
		// insertPo.initCreateMeta();
		String id1 = UUIDUtil.id();
		copy.setId(id1);
		copy.setPath(MessageFormat.format("{0}_{1}", apiConfigApo.getPath(), id1));
		copy.setName(MessageFormat.format("{0}_copy_{1}", apiConfigApo.getName(), id1));
		girDsApiConfigDao.accessSelective(copy);
		return copy;
	}

	private void enhanceApiConfig(ApiConfigApo apiConfigApo) {
		if (apiConfigApo != null) {
			apiConfigApo.setTaskJson(JSONArray.parseArray(apiConfigApo.getTask()));
			apiConfigApo.setParamsJson(JSONArray.parseArray(apiConfigApo.getParams()));
			apiConfigApo.setAlarmPlugins(ListUtil.empty());
			apiConfigApo.setCachePlugin(null);
			apiConfigApo.setGlobalTransformPlugin(null);
		}
	}

	public List<ApiConfigApo> getAll() {
		// DbApiConfigSeo dbApiConfigSeo = new DbApiConfigSeo();
		// dbApiConfigSeo.setDelIs("NO");
		// List<DbApiConfigDto> dtos = apiConfigDao.searchList(dbApiConfigSeo);
		// List<ApiConfig> list = ApiConfig.fromDtos(dtos);
		// List<ApiConfig> collect =
		// list.stream()
		// .sorted(Comparator.comparing(ApiConfig::getUpdateTime).reversed())
		// .collect(Collectors.toList());
		return girDsApiConfigDao.searchAll();
	}

	/**
	 * 给前端使用的数据格式
	 * @return
	 */
	public List<JSONObject> getAllApiTree() {

		List<GroupApo> groupApos = girDsApiGroupDao.searchAll();
		// apiConfigDao.selectAll().stream()
		// .filter(t -> t.getGroupId() != null)
		// .collect(Collectors.toList());
		List<JSONObject> list = groupApos.stream().sorted(Comparator.comparing(GroupApo::getUpdateTime)).map(g -> {
			List<ApiConfigApo> apiConfigApos = girDsApiConfigDao.selectByGroup(g.getId());
			List<JSONObject> children = apiConfigApos.stream().sorted(Comparator.comparing(ApiConfigApo::getUpdateTime))
					.map(t -> {
						JSONObject jo = new JSONObject();
						jo.put("name", t.getName());
						jo.put("id", t.getId());
						jo.put("type", "api");
						jo.put("access", t.getAccess());
						jo.put("status", t.getStatus());
						return jo;
					}).collect(Collectors.toList());

			JSONObject jsonObject = new JSONObject();
			jsonObject.put("name", g.getName());
			jsonObject.put("id", g.getId());
			jsonObject.put("type", "group");
			jsonObject.put("children", children);
			return jsonObject;
		}).collect(Collectors.toList());

		return list;
	}

	public List<ApiConfigApo> search(String name, String note, String path, String groupId) {
		if (StringUtils.isNoneBlank(name)) {
			name = "%" + name + "%";
		}
		if (StringUtils.isNoneBlank(note)) {
			note = "%" + note + "%";
		}
		if (StringUtils.isNoneBlank(path)) {
			path = "%" + path + "%";
		}
		return girDsApiConfigDao.search(name, note, path, groupId);
	}

	/** servlet 从这获取API元数据 */
	// @Cacheable(value = "api", key = "#path", unless = "#result == null")
	public ApiConfigApo getConfig(String path) {
		ApiConfigApo apiConfigApo = girDsApiConfigDao.selectByPathOnline(path);
		enhanceApiConfig(apiConfigApo);
		return apiConfigApo;
	}

	public void online(String id) {
		ApiConfigApo apiConfigApo = girDsApiConfigDao.getById(id);
		apiConfigApo.setStatus(Constants.API_STATUS_ONLINE);
		girDsApiConfigDao.updateSelectiveById(apiConfigApo);
	}

	public void offline(String id) {
		ApiConfigApo apiConfigApo = detail(id);
		cleanDataCacheAndMetaCache(apiConfigApo);
		apiConfigApo.setStatus(Constants.API_STATUS_OFFLINE);
		girDsApiConfigDao.updateSelectiveById(apiConfigApo);
	}

	public String apiDocs(List<String> ids) {
		StringBuffer temp = new StringBuffer("# 接口文档\n---\n");
		List<ApiConfigApo> list = girDsApiConfigDao.selectBatchIds(ids);
		list.forEach(t -> {
			String templ = "## {0}\n- 接口地址： /{1}/{2}\n- 接口备注：{3}\n- Content-Type：{4}\n";
			temp.append(
					MessageFormat.format(templ, t.getName(), apiContext, t.getPath(), t.getNote(), t.getContentType()));
			temp.append("\n- 请求参数：");
			if (MediaType.APPLICATION_FORM_URLENCODED_VALUE.equalsIgnoreCase(t.getContentType())) {
				String params = t.getParams();
				JSONArray array = JSONArray.parseArray(params);

				if (!array.isEmpty()) {
					StringBuilder buffer = new StringBuilder();
					buffer.append("\n\n| 参数名称 | 参数类型 | 参数说明 |\n");
					buffer.append("| :----: | :----: | :----: |\n");

					for (int i = 0; i < array.size(); i++) {
						JSONObject jsonObject = array.getJSONObject(i);
						String name = jsonObject.getString("name");
						String type = jsonObject.getString("type");
						String note = jsonObject.getString("note");
						buffer.append(MessageFormat.format("| {0} | {1} | {2} |\n", name, type, note));
					}

					temp.append(buffer);
				}
				else {
					temp.append("无参数\n");
				}
			}
			else if (MediaType.APPLICATION_JSON_VALUE.equalsIgnoreCase(t.getContentType())) {
				temp.append("\n```json\n").append(t.getJsonParam()).append("\n```\n");
			}
			temp.append("\n---\n");
		});

		temp.append("\n导出日期：" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
		return temp.toString();
	}

	/**
	 * 导出API配置
	 * @param ids
	 * @return
	 */
	public JSONObject exportAPI(List<String> ids) {
		List<ApiConfigApo> list = girDsApiConfigDao.selectBatchIds(ids);

		JSONObject jsonObject = new JSONObject();
		jsonObject.put("api", list);
		jsonObject.put("plugins", ListUtil.empty());
		return jsonObject;
	}

	/**
	 * 导入API配置
	 * @param configs
	 */
	@Transactional
	public void importAPI(List<ApiConfigApo> configs) {
		configs.stream().forEach(t -> {
			t.setCreateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
			t.setUpdateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
			t.setStatus(Constants.API_STATUS_OFFLINE);
			girDsApiConfigDao.accessSelective(t);
		});
	}

}
