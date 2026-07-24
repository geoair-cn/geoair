package cn.geoair.web.test;

import cn.geoair.web.data.result.GirWebResult;

/**
 * GirWebResult 示例
 */
public class GirWebResultExample {

    public static void main(String[] args) {
        GirWebResult<String> result = new GirWebResult<>();
        result.andCode(200).andAlertMsg("ok").andValue("hello-web").andLocation("/index");

        System.out.println("code = " + result.code());
        System.out.println("msg = " + result.alertMsg());
        System.out.println("value = " + result.value());
        System.out.println("location = " + result.location());
    }
}
