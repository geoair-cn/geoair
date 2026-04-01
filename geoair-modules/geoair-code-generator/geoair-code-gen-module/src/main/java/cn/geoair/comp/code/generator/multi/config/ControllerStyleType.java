package cn.geoair.comp.code.generator.multi.config;

/**
 * @author ：张逢吉
 * @date ：Created in 10:05 @description： TODO
 */
public enum ControllerStyleType {

    rest("1", "接口命名为rest风格  简洁为 add ,update,list,del"),
    hasType("2", "接口命名为功能加上类型  例如 addXXX ,updateXXX,listXXX,delXXX");

    private String remark;
    private String code;

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    ControllerStyleType(String code, String remark) {
        this.remark = remark;
        this.code = code;
    }
}
