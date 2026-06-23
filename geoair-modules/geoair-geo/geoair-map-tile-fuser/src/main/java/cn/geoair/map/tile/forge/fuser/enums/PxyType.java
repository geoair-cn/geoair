package cn.geoair.map.tile.forge.fuser.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 瓦片获取器类型枚举
 * <p>
 * 定义支持的所有瓦片获取方式
 * </p>
 *
 * @author 张俊
 * @date Created in 2026/6/15 11:01
 * @description 瓦片获取器类型枚举，用于标识不同的瓦片数据源类型
 */
@Getter
public enum PxyType {

    /**
     * Web 远程方式
     * <p>
     * 从网络 HTTP/HTTPS 服务获取瓦片
     * </p>
     */
    WEB("web", "Web远程服务", true, false),

    /**
     * 本地文件方式
     * <p>
     * 从本地文件系统读取瓦片文件
     * </p>
     */
    LOCAL("local", "本地文件", false, true),

    /**
     * MBTiles 数据库方式
     * <p>
     * 从 MBTiles 格式的 SQLite 数据库读取瓦片
     * </p>
     */
    MBTILES("mbtiles", "MBTiles数据库", false, true),

    /**
     * 自定义实现方式
     * <p>
     * 用户自定义的瓦片获取实现
     * </p>
     */
    CUSTOM("custom", "自定义实现", false, true),

    /**
     * ArcGIS V1 接口（未实现）
     * @deprecated 尚未实现，请使用其他类型
     */
    @Deprecated
    ARCGISV1("arcgisv1", "ArcGIS V1接口", true, false),

    /**
     * ArcGIS V2 接口（未实现）
     * @deprecated 尚未实现，请使用其他类型
     */
    @Deprecated
    ARCGISV2("arcgisv2", "ArcGIS V2接口", true, false);

    /**
     * 类型标识码
     */
    private final String code;

    /**
     * 类型描述
     */
    private final String description;

    /**
     * 是否为网络类型
     */
    private final boolean webType;

    /**
     * 是否为本地类型
     */
    private final boolean localType;

    /**
     * 所有枚举值的映射缓存
     */
    private static final Map<String, PxyType> CODE_MAP;

    static {
        CODE_MAP = Arrays.stream(values())
                .collect(Collectors.toMap(
                        PxyType::getCode,
                        type -> type,
                        (existing, replacement) -> existing
                ));
    }

    /**
     * 构造函数
     *
     * @param code        类型标识码
     * @param description 类型描述
     * @param webType     是否为网络类型
     * @param localType   是否为本地类型
     */
    PxyType(String code, String description, boolean webType, boolean localType) {
        this.code = code;
        this.description = description;
        this.webType = webType;
        this.localType = localType;
    }

    // ==================== 查询方法 ====================

    /**
     * 根据 code 获取枚举
     *
     * @param code 类型标识码
     * @return 对应的枚举，如果未找到返回默认值 WEB
     */
    public static PxyType fromCode(String code) {
        return fromCode(code, WEB);
    }

    /**
     * 根据 code 获取枚举，支持自定义默认值
     *
     * @param code         类型标识码
     * @param defaultValue 默认值
     * @return 对应的枚举，如果未找到返回默认值
     */
    public static PxyType fromCode(String code, PxyType defaultValue) {
        if (code == null || code.trim().isEmpty()) {
            return defaultValue;
        }
        return CODE_MAP.getOrDefault(code.trim().toLowerCase(), defaultValue);
    }

    /**
     * 判断是否为有效的类型标识码
     *
     * @param code 类型标识码
     * @return 是否有效
     */
    public static boolean isValidCode(String code) {
        return code != null && CODE_MAP.containsKey(code.trim().toLowerCase());
    }

    /**
     * 获取所有网络类型的枚举
     *
     * @return 网络类型枚举数组
     */
    public static PxyType[] getWebTypes() {
        return Arrays.stream(values())
                .filter(PxyType::isWebType)
                .toArray(PxyType[]::new);
    }

    /**
     * 获取所有本地类型的枚举
     *
     * @return 本地类型枚举数组
     */
    public static PxyType[] getLocalTypes() {
        return Arrays.stream(values())
                .filter(PxyType::isLocalType)
                .toArray(PxyType[]::new);
    }

    /**
     * 获取所有已实现的类型（排除已废弃的）
     *
     * @return 已实现的枚举数组
     */
    public static PxyType[] getImplementedTypes() {
        return Arrays.stream(values())
                .filter(type -> !type.isDeprecated())
                .toArray(PxyType[]::new);
    }

    // ==================== 判断方法 ====================

    /**
     * 判断是否为网络类型
     *
     * @return true 表示网络类型
     */
    public boolean isWebType() {
        return webType;
    }

    /**
     * 判断是否为本地类型
     *
     * @return true 表示本地类型
     */
    public boolean isLocalType() {
        return localType;
    }

    /**
     * 判断是否为已废弃的类型
     *
     * @return true 表示已废弃
     */
    public boolean isDeprecated() {
        return this == ARCGISV1 || this == ARCGISV2;
    }

    /**
     * 判断是否为网络类型的快捷方法（兼容旧代码）
     */
    public boolean isWeb() {
        return isWebType();
    }

    /**
     * 判断是否为本地类型的快捷方法（兼容旧代码）
     */
    public boolean isLocal() {
        return isLocalType();
    }

    /**
     * 判断是否为 MBTiles 类型
     */
    public boolean isMbtiles() {
        return this == MBTILES;
    }

    /**
     * 判断是否为自定义类型
     */
    public boolean isCustom() {
        return this == CUSTOM;
    }

    /**
     * 判断是否需要网络请求
     *
     * @return true 表示需要网络请求
     */
    public boolean requiresNetwork() {
        return webType;
    }

    /**
     * 判断是否需要本地文件访问
     *
     * @return true 表示需要本地文件访问
     */
    public boolean requiresLocalFile() {
        return localType && this != CUSTOM;
    }

    // ==================== 重写方法 ====================

    @Override
    public String toString() {
        return code;
    }

    /**
     * 获取完整的描述信息
     *
     * @return 格式化的描述字符串
     */
    public String toFullString() {
        return String.format("%s(%s) - %s", code, description,
                webType ? "网络" : "本地");
    }

    /**
     * 获取用于显示的名称
     *
     * @return 显示名称
     */
    public String getDisplayName() {
        return description;
    }

    /**
     * 获取类型分类
     *
     * @return 分类名称
     */
    public String getCategory() {
        if (webType) {
            return "网络";
        } else if (localType) {
            return "本地";
        } else {
            return "其他";
        }
    }
}
