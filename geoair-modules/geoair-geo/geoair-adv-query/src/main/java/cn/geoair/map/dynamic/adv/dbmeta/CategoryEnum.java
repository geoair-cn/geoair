package cn.geoair.map.dynamic.adv.dbmeta;

import cn.geoair.base.data.GiVisualValuable;

/**
 * 基于长度/精度/小数位特性的二级类型分组。
 *
 * <p>每个值关联一个 {@link CategoryGroupEnum}（大分组）和三个 {@link IgnorePolicy}（长度/精度/小数位策略）。 例如：{@code CHAR}
 * 属于 {@code STRING} 大分组，长度需要保留(KEEP)，精度和小数位忽略(IGNORE)。
 *
 * @author zhangjun
 * @date 2026/8/14
 */
public enum CategoryEnum implements GiVisualValuable<String> {

    /** 定长/变长字符串，如 CHAR、VARCHAR — 需要长度，忽略精度和小数位 */
    CHAR(CategoryGroupEnum.STRING, IgnorePolicy.KEEP, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE),
    /** 大文本类型，如 TEXT、CLOB — 长度/精度/小数位全部忽略 */
    TEXT(CategoryGroupEnum.STRING, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE),
    /** 布尔类型 — 全部忽略 */
    BOOLEAN(
            CategoryGroupEnum.BOOLEAN,
            IgnorePolicy.IGNORE,
            IgnorePolicy.IGNORE,
            IgnorePolicy.IGNORE),
    /** 定长二进制，如 RAW、BINARY — 需要长度 */
    BYTES(CategoryGroupEnum.BYTES, IgnorePolicy.KEEP, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE),
    /** 大二进制对象，如 BLOB — 全部忽略 */
    BLOB(CategoryGroupEnum.BYTES, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE),
    /** 整数类型，如 INT、BIGINT — 需要长度（显示宽度），忽略精度和小数位 */
    INT(CategoryGroupEnum.NUMBER, IgnorePolicy.KEEP, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE),
    /** 浮点/精确数值，如 FLOAT、NUMERIC、DECIMAL — 忽略长度，保留精度和小数位 */
    FLOAT(CategoryGroupEnum.NUMBER, IgnorePolicy.IGNORE, IgnorePolicy.KEEP, IgnorePolicy.KEEP),
    /** 日期类型，如 DATE — 全部忽略 */
    DATE(CategoryGroupEnum.DATETIME, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE),
    /** 时间类型，如 TIME — 全部忽略 */
    TIME(CategoryGroupEnum.DATETIME, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE),
    /** 日期时间类型（无时区），如 DATETIME — 全部忽略 */
    DATETIME(
            CategoryGroupEnum.DATETIME,
            IgnorePolicy.IGNORE,
            IgnorePolicy.IGNORE,
            IgnorePolicy.IGNORE),
    /** 时间戳类型，如 TIMESTAMP — 全部忽略 */
    TIMESTAMP(
            CategoryGroupEnum.DATETIME,
            IgnorePolicy.IGNORE,
            IgnorePolicy.IGNORE,
            IgnorePolicy.IGNORE),
    /** 集合/数组类型 — 全部忽略 */
    COLLECTION(
            CategoryGroupEnum.COLLECTION,
            IgnorePolicy.IGNORE,
            IgnorePolicy.IGNORE,
            IgnorePolicy.IGNORE),
    /** 空间几何类型，如 GEOMETRY、GEOGRAPHY — 全部忽略 */
    GEOMETRY(
            CategoryGroupEnum.GEOMETRY,
            IgnorePolicy.IGNORE,
            IgnorePolicy.IGNORE,
            IgnorePolicy.IGNORE),
    /** 时间间隔类型，如 INTERVAL — 精度视情况，精度与小数位互依赖 */
    INTERVAL(
            CategoryGroupEnum.INTERVAL,
            IgnorePolicy.IGNORE,
            IgnorePolicy.CONDITIONAL,
            IgnorePolicy.MUTUAL_DEPENDENT),
    /** 其他未分类类型 — 全部忽略 */
    OTHER(CategoryGroupEnum.OTHER, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE),
    /** 无类型兜底 — 全部忽略 */
    NONE(CategoryGroupEnum.NONE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE);

    private final CategoryGroupEnum group;
    private final IgnorePolicy ignoreLength;
    private final IgnorePolicy ignorePrecision;
    private final IgnorePolicy ignoreScale;
    private TypeMetadata.Config config;

    CategoryEnum(
            CategoryGroupEnum group,
            IgnorePolicy ignoreLength,
            IgnorePolicy ignorePrecision,
            IgnorePolicy ignoreScale) {
        this.group = group;
        this.ignoreLength = ignoreLength;
        this.ignorePrecision = ignorePrecision;
        this.ignoreScale = ignoreScale;
    }

    /** 获取该分类所属的大分组 */
    public CategoryGroupEnum group() {
        return group;
    }

    /**
     * 获取该分类的默认配置（懒加载，首次调用时从 IgnorePolicy 初始化）。
     *
     * @return 包含忽略策略的 {@link TypeMetadata.Config} 实例
     */
    public TypeMetadata.Config config() {
        if (null == config) {
            config = new TypeMetadata.Config();
            config.setIgnoreLength(ignoreLength.code())
                    .setIgnorePrecision(ignorePrecision.code())
                    .setIgnoreScale(ignoreScale.code());
        }
        return config;
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
