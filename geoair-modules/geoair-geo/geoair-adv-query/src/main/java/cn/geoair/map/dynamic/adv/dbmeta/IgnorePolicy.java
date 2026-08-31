package cn.geoair.map.dynamic.adv.dbmeta;

import cn.geoair.base.data.GiVisualValuable;

/**
 * 长度/精度/小数位的忽略策略枚举。
 *
 * <p>决定在 SQL DDL 生成时，是否需要输出 length、precision、scale 参数。 例如：VARCHAR 需要长度(KEEP)，TEXT 不需要长度(IGNORE)，
 * INTERVAL 的精度和小数位存在互依赖关系(MUTUAL_DEPENDENT)。
 *
 * @author zhangjun
 * @date 2026/8/14
 */
public enum IgnorePolicy implements GiVisualValuable<String> {

    /** 未设置，表示继承上级配置 */
    NOT_SET(-1),
    /** 保留，SQL 生成时必须输出该参数 */
    KEEP(0),
    /** 忽略，SQL 生成时省略该参数 */
    IGNORE(1),
    /** 视情况而定，需要根据上下文判断是否输出 */
    CONDITIONAL(2),
    /** 精度与小数位互依赖，需要联合计算 */
    MUTUAL_DEPENDENT(3);

    final int code;

    IgnorePolicy(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static IgnorePolicy of(int code) {
        for (IgnorePolicy p : values()) {
            if (p.code == code) return p;
        }
        return NOT_SET;
    }

    @Override
    public String value() {
        return this.name();
    }

    @Override
    public String display() {
        return this.name();
    }
}
