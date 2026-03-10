package cn.geoair.map.dynamic.dbservice.controller;

import cn.geoair.base.api.annotation.GaApi;
import cn.geoair.base.api.annotation.GaApiAction;
import cn.geoair.base.data.page.GiPageParam;
import cn.geoair.base.data.page.GiPager;
import cn.geoair.base.data.page.support.GirPager;
import cn.geoair.base.data.result.GiResult;
import cn.geoair.base.util.GutilObject;
import cn.geoair.map.dynamic.dbservice.DbApiUserInfoHelper;
import cn.geoair.map.dynamic.dbservice.basic.domain.DataSource;
import cn.geoair.map.dynamic.dbservice.basic.service.DataSourceService;
import cn.geoair.map.dynamic.dbservice.basic.util.JdbcUtil;
import cn.geoair.map.dynamic.dbservice.common.ResponseDto;
import cn.geoair.map.dynamic.dbservice.controller.dbapi.datasource.DatasourceSearchVo;
import cn.geoair.map.dynamic.dbservice.dao.dbapi.DbApiDataSourceDao;
import cn.geoair.map.dynamic.dbservice.model.dbapi.dto.DbApiDataSourceDto;
import cn.geoair.map.dynamic.dbservice.model.dbapi.seo.DbApiDataSourceSeo;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ArrayUtil;

import com.alibaba.fastjson.JSON;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

/**
 * @program: dbApi
 * @description:
 * @author: 武汉刘德华
 * @create: 2021-01-20 10:43
 */
@Slf4j
@RestController
@RequestMapping("/datasource")
@GaApi(tags = "数据源相关")
public class DataSourceController {

	@Autowired
	DataSourceService dataSourceService;

	@Resource
	private DbApiDataSourceDao dbApiDataSourceDao;

	@Resource
	DbApiUserInfoHelper dbApiUserInfoHelper;

	@RequestMapping("/add")
	@GaApiAction(text = "新增数据源")
	public void add(DataSource dataSource) {
		dataSource.setCreateUserId(dbApiUserInfoHelper.getSubjectId());
		dataSourceService.add(dataSource);
	}

	@PostMapping("/getAll")
	@GaApiAction(text = "获取所有数据源")
	public List<DataSource> getAll() {
		return dataSourceService.getAll();
	}

	@GetMapping("/detail/{id}")
	@GaApiAction(text = "更新API分组信息")
	public DataSource detail(@PathVariable String id) {
		return dataSourceService.detail(id);
	}

	@PostMapping("/delete/{id}")
	@GaApiAction(text = "删除数据源")
	public ResponseDto delete(@PathVariable String id) {
		return dataSourceService.delete(id);
	}

	@PostMapping("/update")
	@GaApiAction(text = "更新数据源")
	public DataSource update(DataSource dataSource) {
		dataSourceService.update(dataSource);
		return null;
	}

	@PostMapping("/connect")
	@GaApiAction(text = "测试连接")
	public ResponseDto connect(DataSource dataSource) {
		Connection connection = null;
		try {
			connection = JdbcUtil.getConnection(dataSource);
			return ResponseDto.apiSuccess(null);
		}
		catch (Exception e) {
			log.error(e.getMessage(), e);
			return ResponseDto.fail(e.getMessage());
		}
		finally {
			if (connection != null) {
				try {
					connection.close();
				}
				catch (SQLException e) {
					log.error(e.getMessage());
				}
			}
		}
	}

	@PostMapping("/export")
	@GaApiAction(text = "导出数据源")
	public void export(String ids, HttpServletResponse response) {
		List<String> collect = Arrays.asList(ids.split(","));
		List<DataSource> list = dataSourceService.selectBatch(collect);
		String s = JSON.toJSONString(list);
		response.setContentType("application/x-msdownload;charset=utf-8");
		response.setHeader("Content-Disposition", "attachment; filename=datasource.json");
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

	@RequestMapping(value = "/import", produces = "application/json;charset=UTF-8")
	@GaApiAction(text = "导入数据源")
	public void importDatasource(@RequestParam("file") MultipartFile file) throws IOException {
		String s = IoUtil.read(file.getInputStream(), "utf-8");
		List<DataSource> list = JSON.parseArray(s, DataSource.class);
		list.stream().forEach(t -> {
			t.setCreateUserId(dbApiUserInfoHelper.getSubjectId());
			t.setCreateTime(DateFormatUtils.format(new Date(), "yyyy-MM-dd hh:mm:ss"));
			t.setUpdateTime(DateFormatUtils.format(new Date(), "yyyy-MM-dd hh:mm:ss"));
		});
		dataSourceService.insertBatch(list);
	}

	@GaApiAction(text = "分页列出数据源信息")
	@RequestMapping(value = "/listDbApiDatasourcePage", method = { RequestMethod.POST })
	@ResponseBody
	public GiResult<GiPager<DataSource>> listDbApiDatasourcePage(@Validated @RequestBody DatasourceSearchVo param) {
		DbApiDataSourceSeo seo = new DbApiDataSourceSeo();
		BeanUtils.copyProperties(param, seo);
		seo.setNotDel();
		if (GutilObject.isNotEmpty(param.getQueryContent())) {
			seo.setAndQueryContentIn(ArrayUtil.toArray(ListUtil.of(param.getQueryContent()), String.class));
		}
		GiPager<DbApiDataSourceDto> giPager = dbApiDataSourceDao.searchListPage(seo, GiPageParam.of());
		Iterable<DbApiDataSourceDto> value = giPager.value();
		GirPager<DataSource> reg = new GirPager<>();
		List<DataSource> vdvos = DataSource.fromDtos(ListUtil.toList(value));
		reg.put(vdvos, giPager.total(), giPager.pageParam());
		return GiResult.successValue(reg);
	}

}
