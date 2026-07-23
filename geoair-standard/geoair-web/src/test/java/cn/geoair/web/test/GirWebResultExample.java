package cn.geoair.web.test;

import cn.geoair.web.data.result.GirWebResult;

/**
 * GirWebResult 最小示例
 */
public class GirWebResultExample {

    public static void main(String[] args) {
        GirWebResult<String> result = new GirWebResult<>();
        result.forSuccess().andAlertMsg("操作成功").andValue("payload").andLocation("/home");

        System.out.println("code = " + result.getCode());
        System.out.println("alertMsg = " + result.getAlertMsg());
        System.out.println("location = " + result.getLocation());
        System.out.println("data = " + result.getData());
    }
}
