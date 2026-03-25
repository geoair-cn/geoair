package cn.geoair.comp.demo.knife4j.controller.group1;

import cn.geoair.base.api.annotation.GaApi;
import cn.geoair.base.api.annotation.GaApiAction;
import cn.geoair.base.data.result.GiResult;
import cn.geoair.comp.demo.knife4j.model.DemoVo1;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

@Controller
@GaApi(tags = "文件")
public class FileController {

    @PostMapping("/file")
    @ResponseBody
    @GaApiAction(text = "文件 控制器")
    public GiResult<DemoVo1> demo1post(MultipartFile file) {
        return GiResult.successValue(new DemoVo1());
    }
}
