package cn.geoair.map.dynamic.adv.config;

import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandler;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

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
     * 启用 插入的sql日志
     */
    boolean enableAccessLog = true;
    /**
     * 启用删除的sql日志
     */
    boolean enableDelLog = true;

    /**
     * 执行异常的日志是否展示
     */
    boolean enableErrorLog = true;

    /**
     * 用户自定义类型处理器列表，优先级高于 SPI 默认处理器。
     * 每个 Executor 创建时会读取此列表，注册到自己的 AdvTypeHandlerRegistry 中。
     */
    private List<AdvTypeHandler<?>> typeHandlers = new ArrayList<>();

    /**
     * 添加一个自定义类型处理器
     */
    public AdvQueryGlobalConfig addTypeHandler(AdvTypeHandler<?> handler) {
        if (this.typeHandlers == null) {
            this.typeHandlers = new ArrayList<>();
        }
        this.typeHandlers.add(handler);
        return this;
    }

    /**
     * 设置自定义类型处理器列表
     */
    public AdvQueryGlobalConfig setTypeHandlers(List<AdvTypeHandler<?>> typeHandlers) {
        this.typeHandlers = typeHandlers;
        return this;
    }

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
