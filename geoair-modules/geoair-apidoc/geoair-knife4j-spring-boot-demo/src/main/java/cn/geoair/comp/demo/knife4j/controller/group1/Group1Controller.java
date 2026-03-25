package cn.geoair.comp.demo.knife4j.controller.group1;

// import io.swagger.annotations.Api;
import cn.geoair.base.api.annotation.GaApi;
import cn.geoair.base.api.annotation.GaApiAction;
import cn.geoair.comp.demo.knife4j.model.DemoVo1;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author ：张俊
 * @date ：Created in 2022/8/23 17:23 @description： 分组1 控制器
 */
@Controller
@GaApi(tags = "分组1第一个 22控制器")
// @Api(tags = "分组1第一个 控制器")
public class Group1Controller {

    // @PostMapping("/demo3post")
    // @ResponseBody
    // @GaApiAction(text = "demo1post 控制器")
    // public DemoVo3 demo3post(DemoVo3 demoVo) {
    // return demoVo;
    // }

    @PostMapping("/demo1post")
    @ResponseBody
    @GaApiAction(text = "demo1post 控制器")
    public DemoVo1 demo1post(DemoVo1 demoVo) {
        return demoVo;
    }

    //
    // @PostMapping("/testGiResult")
    // @ResponseBody
    // @GaApiAction(text = "testGiResult 控制器")
    // public GiResult<DemoVo> testGiResult(@RequestBody DemoVo demoVo) {
    // return GiResult.successValue(demoVo);
    // }
    //
    //
    // @PostMapping("/testGiPiger")
    // @ResponseBody
    // @GaApiAction(text = "testGiPiger 控制器")
    // public GiResult<GiPager<List<DemoVo>>> testGiPiger(@RequestBody DemoVo demoVo) {
    // return GiResult.successValue(new GirPager<>());
    // }
    //
    //
    // @PostMapping("/demo1Get")
    // @ResponseBody
    // @GaApiAction(text = "demo1Get 控制器")
    // public String demo1Get() {
    // return "demo1Get返回成功";
    // }
    //
    //
    // @PostMapping("/file")
    // @ResponseBody
    // @ApiOperation(value = "文件上传 控制器")
    // public GiResult<String> file(@RequestParam MultipartFile file, @RequestParam String
    // aaaa) {
    // Gir.log.info(file.getOriginalFilename());
    // return GiResult.successValue("文件上传t返回成功");
    // }

}
