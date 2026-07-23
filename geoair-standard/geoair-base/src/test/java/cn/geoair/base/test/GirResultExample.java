package cn.geoair.base.test;

import cn.geoair.base.data.result.support.GirResult;

/**
 * GirResult 最小示例
 */
public class GirResultExample {

    public static void main(String[] args) {
        GirResult<String> result = new GirResult<>();
        result.forSuccess().andAlertMsg("操作成功").andValue("data");

        System.out.println("code = " + result.getCode());
        System.out.println("alertMsg = " + result.getAlertMsg());
        System.out.println("data = " + result.getData());
    }
}
