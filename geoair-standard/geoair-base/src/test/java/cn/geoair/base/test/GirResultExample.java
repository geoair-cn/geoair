package cn.geoair.base.test;

import cn.geoair.base.data.result.support.GirResult;

/**
 * GirResult 示例
 */
public class GirResultExample {

    public static void main(String[] args) {
        GirResult<String> result = new GirResult<>();
        result.andCode(200).andAlertMsg("ok").andValue("hello-base");

        System.out.println("code = " + result.code());
        System.out.println("msg = " + result.alertMsg());
        System.out.println("value = " + result.value());
    }
}
