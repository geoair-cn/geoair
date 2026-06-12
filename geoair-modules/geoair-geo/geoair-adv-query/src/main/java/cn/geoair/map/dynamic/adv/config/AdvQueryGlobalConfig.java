package cn.geoair.map.dynamic.adv.config;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author ：张俊
 * @date ：Created in 2026/5/17 16:40
 * @description： 高级查询器的配置
 */
@Data
@Accessors(chain = true)
public class AdvQueryGlobalConfig {

    public static AdvQueryGlobalConfig of() {
        return new AdvQueryGlobalConfig();
    }

    /** 启用查询的sql日志 */
    boolean enableQueryLog = true;

    /** 启用ddl操作sql日志 */
    boolean enableDdlLog = true;

    /** 启用 更新的sql日志 */
    boolean enableUpdateLog = true;

    /** 启用 插入的sql日志 */
    boolean enableAccessLog = true;

    /** 启用删除的sql日志 */
    boolean enableDelLog = true;

    /** 执行异常的日志是否展示 */
    boolean enableErrorLog = true;

    /**
     * 打开日志
     *
     * @return
     */
    public AdvQueryGlobalConfig turnOnLog() {
        enableQueryLog = true;
        enableDdlLog = true;
        enableUpdateLog = true;
        enableDelLog = true;
        enableAccessLog = true;
        enableErrorLog = true;
        return this;
    }

    /**
     * 关闭日志
     *
     * @return
     */
    public AdvQueryGlobalConfig turnOffLog() {
        enableQueryLog = false;
        enableDdlLog = false;
        enableUpdateLog = false;
        enableDelLog = false;
        enableAccessLog = false;
        enableErrorLog = false;
        return this;
    }
}
