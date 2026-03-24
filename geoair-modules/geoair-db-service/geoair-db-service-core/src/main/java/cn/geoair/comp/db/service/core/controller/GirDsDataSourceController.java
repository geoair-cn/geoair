package cn.geoair.comp.db.service.core.controller;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.fastjson2.JSON;

import cn.geoair.base.api.annotation.GaApi;
import cn.geoair.base.api.annotation.GaApiAction;
import cn.geoair.comp.db.service.core.DsApiUserInfoHelper;
import cn.geoair.comp.db.service.core.basic.apo.DataSourceApo;
import cn.geoair.comp.db.service.core.basic.service.DsDataSourceService;
import cn.geoair.comp.db.service.core.basic.util.JdbcUtil;
import cn.geoair.comp.db.service.core.common.ResponseDto;
import cn.geoair.comp.db.service.core.utils.TokenManager;

import cn.hutool.core.io.IoUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @program: dbApi
 * @description:
 * @author: 武汉刘德华
 * @create: 2021-01-20 10:43
 */
@Slf4j
@RestController
@RequestMapping("/ds_api/datasource")
@GaApi(tags = "GirDs 数据源相关")
public class GirDsDataSourceController {

	@Autowired
	DsDataSourceService dsDataSourceService;

	@Resource
	DsApiUserInfoHelper dsApiUserInfoHelper;

	@RequestMapping("/add")
	@GaApiAction(text = "新增数据源")
	public void add(DataSourceApo dataSourceApo) {
		TokenManager.validateToken();
		dataSourceApo.setCreateUserId(dsApiUserInfoHelper.getSubjectId());
		dsDataSourceService.add(dataSourceApo);
	}

	@PostMapping("/getAll")
	@GaApiAction(text = "获取所有数据源")
	public List<DataSourceApo> getAll() {
		TokenManager.validateToken();
		return dsDataSourceService.getAll();
	}

	@GetMapping("/detail/{id}")
	@GaApiAction(text = "更新API分组信息")
	public DataSourceApo detail(@PathVariable String id) {
		return dsDataSourceService.detail(id);
	}

	@PostMapping("/delete/{id}")
	@GaApiAction(text = "删除数据源")
	public ResponseDto delete(@PathVariable String id) {
		TokenManager.validateToken();
		return dsDataSourceService.delete(id);
	}

	@PostMapping("/update")
	@GaApiAction(text = "更新数据源")
	public DataSourceApo update(DataSourceApo dataSourceApo) {
		TokenManager.validateToken();
		dsDataSourceService.update(dataSourceApo);
		return null;
	}

	@PostMapping("/connect")
	@GaApiAction(text = "测试连接")
	public ResponseDto connect(DataSourceApo dataSourceApo) {
		Connection connection = null;
		try {
			connection = JdbcUtil.getConnection(dataSourceApo);
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
		List<DataSourceApo> list = dsDataSourceService.selectBatch(collect);
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
		List<DataSourceApo> list = JSON.parseArray(s, DataSourceApo.class);
		list.stream().forEach(t -> {
			t.setCreateUserId(dsApiUserInfoHelper.getSubjectId());
			t.setCreateTime(DateFormatUtils.format(new Date(), "yyyy-MM-dd hh:mm:ss"));
			t.setUpdateTime(DateFormatUtils.format(new Date(), "yyyy-MM-dd hh:mm:ss"));
		});
		dsDataSourceService.insertBatch(list);
	}

}
