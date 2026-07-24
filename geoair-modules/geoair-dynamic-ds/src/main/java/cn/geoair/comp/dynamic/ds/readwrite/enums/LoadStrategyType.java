package cn.geoair.comp.dynamic.ds.readwrite.enums;

import lombok.Getter;

/**
 * @author ：张俊
 * @date ：Created in 2026/5/28 16:38
 * @description：负载策略
 */
@Getter
public enum LoadStrategyType {

    /** 轮询策略 按顺序轮流分配请求到各个从库，使负载均匀分布 */
    ROUND_ROBIN("round_robin", "轮询策略"),

    /** 随机策略 随机选择一个从库处理请求 */
    RANDOM("random", "随机策略"),

    /** 权重策略 根据配置的权重值分配请求，权重高的从库处理更多请求 */
    WEIGHT("weight", "权重策略"),

    /** 最少连接数策略 选择当前活跃连接数最少的从库 */
    LEAST_ACTIVE("least_active", "最少连接数策略");

    /** 策略代码 */
    private final String code;

    /** 策略描述 */
    private final String description;

    LoadStrategyType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据code获取枚举
     *
     * @param code 策略代码
     * @return 对应的枚举，未找到返回null
     */
    public static LoadStrategyType fromCode(String code) {
        for (LoadStrategyType type : LoadStrategyType.values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 根据code获取枚举（带默认值）
     *
     * @param code 策略代码
     * @param defaultValue 默认枚举
     * @return 对应的枚举
     */
    public static LoadStrategyType fromCode(String code, LoadStrategyType defaultValue) {
        LoadStrategyType type = fromCode(code);
        return type != null ? type : defaultValue;
    }

    @Override
    public String toString() {
        return this.code;
    }
}
