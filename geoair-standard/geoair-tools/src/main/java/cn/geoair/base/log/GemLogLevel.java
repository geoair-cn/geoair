package cn.geoair.base.log;

/**
 * 日志级别枚举
 * <p>
 * 通过 code 值的大小可以判断日志级别的高低，code 值越小级别越低（记录越详细），
 * code 值越大级别越高（记录越少）。
 * <p>
 * 级别高低顺序：ALL(0) < TRACE(1) < DEBUG(2) < INFO(3) < WARN(4) < ERROR(5) < FATAL(6) < OFF(7)
 *
 * @author Ray
 * @since 1.0
 */
public enum GemLogLevel {

    /**
     * 全部级别，记录所有日志
     */
    ALL(0, "ALL"),

    /**
     * 追踪级别，最详细的日志信息
     */
    TRACE(1, "TRACE"),

    /**
     * 调试级别，用于调试的详细信息
     */
    DEBUG(2, "DEBUG"),

    /**
     * 信息级别，普通运行信息
     */
    INFO(3, "INFO"),

    /**
     * 警告级别，潜在的问题
     */
    WARN(4, "WARN"),

    /**
     * 错误级别，错误事件
     */
    ERROR(5, "ERROR"),

    /**
     * 致命错误级别，严重错误导致程序终止
     */
    FATAL(6, "FATAL"),

    /**
     * 关闭所有日志
     */
    OFF(7, "OFF");

    /**
     * 日志级别代码，数值越小级别越低，数值越大级别越高
     */
    private final int code;

    /**
     * 日志级别名称
     */
    private final String name;

    /**
     * 构造函数
     *
     * @param code 级别代码
     * @param name 级别名称
     */
    GemLogLevel(int code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 获取日志级别代码
     *
     * @return 级别代码
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取日志级别名称
     *
     * @return 级别名称
     */
    public String getName() {
        return name;
    }

    /**
     * 判断当前级别是否高于或等于指定级别
     * <p>
     * 用于日志级别过滤，例如：当前级别为 INFO，则 INFO、WARN、ERROR、FATAL 都会被记录
     *
     * @param level 要比较的日志级别
     * @return {@code true} 如果当前级别 >= 指定级别
     */
    public boolean isGreaterOrEqual(GemLogLevel level) {
        return this.code >= level.code;
    }

    /**
     * 判断当前级别是否低于或等于指定级别
     *
     * @param level 要比较的日志级别
     * @return {@code true} 如果当前级别 <= 指定级别
     */
    public boolean isLessOrEqual(GemLogLevel level) {
        return this.code <= level.code;
    }

    /**
     * 判断当前级别是否高于指定级别
     *
     * @param level 要比较的日志级别
     * @return {@code true} 如果当前级别 > 指定级别
     */
    public boolean isGreaterThan(GemLogLevel level) {
        return this.code > level.code;
    }

    /**
     * 判断当前级别是否低于指定级别
     *
     * @param level 要比较的日志级别
     * @return {@code true} 如果当前级别 < 指定级别
     */
    public boolean isLessThan(GemLogLevel level) {
        return this.code < level.code;
    }

    /**
     * 根据代码值获取对应的日志级别枚举
     *
     * @param code 级别代码
     * @return 对应的日志级别枚举，如果未找到返回 null
     */
    public static GemLogLevel fromCode(int code) {
        for (GemLogLevel level : values()) {
            if (level.code == code) {
                return level;
            }
        }
        return null;
    }

    /**
     * 根据名称获取对应的日志级别枚举
     *
     * @param name 级别名称（不区分大小写）
     * @return 对应的日志级别枚举，如果未找到返回 null
     */
    public static GemLogLevel fromName(String name) {
        if (name == null) {
            return null;
        }
        for (GemLogLevel level : values()) {
            if (level.name.equalsIgnoreCase(name)) {
                return level;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return name;
    }
}
