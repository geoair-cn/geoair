package com.gtc.comp.knife4j.demo.controller.group2;

import com.gtc.comp.knife4j.demo.model.DemoVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author ：张俊
 * @date ：Created in 2022/8/23 17:23
 * @description： 分组2控制器
 */


@Controller
@Api(description = "分组2 第一个 控制器")
public class Group2Controller {

//    @PostMapping("/demo2post")
//    @ResponseBody
//    @ApiOperation(value = "demo2post 控制器")
//    public DemoVo demo1post(@RequestBody DemoVo demoVo) {
//        return demoVo;
//    }


    @PostMapping("/demo2Get")
    @ResponseBody
    @ApiOperation(value = "demo2Get 控制器")
    public String demo1Get() {
        return "demo2Get返回成功";
    }
}
