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
public class ConfigAdvQuery {

    public static ConfigAdvQuery of() {
        return new ConfigAdvQuery();
    }

    /**
     * 启用查询的sql日志
     */
    boolean enableQueryLog = true;
    /**
     * 启用ddl操作sql日志
     */
    boolean enableDdlLog = true;

    /**
     * 启用 更新的sql日志
     */
    boolean enableUpdateLog = true;
    /**
     * 启用删除的sql日子
     */
    boolean enableDelLog = true;

}
