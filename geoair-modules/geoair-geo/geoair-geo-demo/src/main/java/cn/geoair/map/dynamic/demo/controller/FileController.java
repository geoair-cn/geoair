package cn.geoair.map.dynamic.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import cn.geoair.base.api.annotation.GaApi;
import cn.geoair.base.api.annotation.GaApiAction;
import cn.geoair.base.data.result.GiResult;

@Controller
@GaApi(tags = "文件")
public class FileController {

	@PostMapping("/file")
	@ResponseBody
	@GaApiAction(text = "文件 控制器")
	public GiResult<?> demo1post(MultipartFile file) {
		return GiResult.success();
	}

}
