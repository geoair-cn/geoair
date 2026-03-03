package cn.geoair.comp.knife4j.demo.controller.group1;

import cn.geoair.base.api.annotation.GaApi;
import cn.geoair.base.api.annotation.GaApiAction;
import cn.geoair.base.data.result.GiResult;
import cn.geoair.comp.knife4j.demo.model.DemoVo1;
import io.swagger.annotations.Api;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;


@Controller
@GaApi(tags = "文件" )
public class FileController {
    @PostMapping("/file" )
    @ResponseBody
    @GaApiAction(text = "文件 控制器" )
    public GiResult<DemoVo1> demo1post(MultipartFile file) {
        return GiResult.successValue(new DemoVo1());
    }

}
