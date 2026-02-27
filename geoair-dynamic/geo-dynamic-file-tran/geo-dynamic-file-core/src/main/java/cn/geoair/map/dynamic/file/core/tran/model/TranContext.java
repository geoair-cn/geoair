package cn.geoair.map.dynamic.file.core.tran.model;

import cn.geoair.map.dynamic.file.core.tran.TranPostProcessor;
import cn.geoair.map.dynamic.file.core.tran.TranPreProcessor;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

/**
 * 转换上下文
 * 用于传递自定义参数、配置、临时数据，支持扩展
 */
@Data
@Accessors(chain = true)
public class TranContext {

    // 基础配置
    private int batchLogThreshold = 1000; // 批量日志阈值
    private boolean skipErrorRecord = true; // 是否跳过错误记录
    private boolean autoCloseResource = true; // 是否自动关闭资源
    private long timeout = 30 * 60 * 1000; // 转换超时时间（ms）

    // 自定义扩展参数（键值对）
    private Map<String, Object> extParams = new HashMap<>();

    // 预处理/后处理钩子
    private TranPreProcessor preProcessor;
    private TranPostProcessor postProcessor;

    // 获取自定义参数
    @SuppressWarnings("unchecked")
    public <T> T getExtParam(String key) {
        return (T) extParams.get(key);
    }

    // 设置自定义参数
    public TranContext putExtParam(String key, Object value) {
        this.extParams.put(key, value);
        return this;
    }
}
