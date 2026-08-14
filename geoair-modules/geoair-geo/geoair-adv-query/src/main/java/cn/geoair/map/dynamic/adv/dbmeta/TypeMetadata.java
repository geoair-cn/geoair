package cn.geoair.map.dynamic.adv.dbmeta;

import cn.hutool.core.util.StrUtil;

/**
 * 数据库类型的统一元数据接口，描述一个数据库字段类型的完整语义。
 * <p>
 * 本接口是类型体系的核心契约，为 SQL DDL 生成、字段类型判断、Java 类型映射提供统一基础。
 * 主要实现者：
 * <ul>
 *   <li>{@link DefaultJavaType} — Java 类型枚举，描述 Java 侧的类型语义</li>
 *   <li>{@link DataBaseFieldType} — 继承本接口，增加数据库特定的 UDT 名称映射能力</li>
 * </ul>
 *
 * <h3>类型分类体系</h3>
 * <pre>
 * CATEGORY_GROUP (大分组: STRING / NUMBER / DATETIME / ...)
 *   └── CATEGORY (二级分组: CHAR / TEXT / INT / FLOAT / DATE / TIMESTAMP / ...)
 *         └── IgnorePolicy (长度/精度/小数位的忽略策略)
 * </pre>
 *
 * @author zhangjun
 * @date 2026/8/14
 */
public interface TypeMetadata {

    /**
     * 长度/精度/小数位的忽略策略枚举。
     * <p>
     * 决定在 SQL DDL 生成时，是否需要输出 length、precision、scale 参数。
     * 例如：VARCHAR 需要长度(KEEP)，TEXT 不需要长度(IGNORE)，
     * INTERVAL 的精度和小数位存在互依赖关系(MUTUAL_DEPENDENT)。
     */
    enum IgnorePolicy {
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
    }

    /**
     * 字段类型的大分组，按语义聚合为一级分类。
     * <p>
     * 每个 {@link CATEGORY} 属于一个 CATEGORY_GROUP。
     * 用于快速判断字段属于哪一大类（如判断是否为数值型、日期型等）。
     */
    enum CATEGORY_GROUP {
        /** 字符串类型（CHAR、TEXT 等） */
        STRING,
        /** 数值类型（INT、FLOAT 等） */
        NUMBER,
        /** 布尔类型 */
        BOOLEAN,
        /** 二进制类型（BYTES、BLOB） */
        BYTES,
        /** 日期时间类型（DATE、TIME、TIMESTAMP、DATETIME） */
        DATETIME,
        /** 集合类型（如 PostgreSQL 数组） */
        COLLECTION,
        /** 空间几何类型（GEOMETRY、GEOGRAPHY） */
        GEOMETRY,
        /** 时间间隔类型（INTERVAL） */
        INTERVAL,
        /** 其他未分类类型 */
        OTHER,
        /** 无类型（兜底） */
        NONE
    }

    /**
     * 基于长度/精度/小数位特性的二级类型分组。
     * <p>
     * 每个值关联一个 {@link CATEGORY_GROUP}（大分组）和三个 {@link IgnorePolicy}（长度/精度/小数位策略）。
     * 例如：{@code CHAR} 属于 {@code STRING} 大分组，长度需要保留(KEEP)，精度和小数位忽略(IGNORE)。
     */
    enum CATEGORY {
        /** 定长/变长字符串，如 CHAR、VARCHAR — 需要长度，忽略精度和小数位 */
        CHAR(CATEGORY_GROUP.STRING, IgnorePolicy.KEEP, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE),
        /** 大文本类型，如 TEXT、CLOB — 长度/精度/小数位全部忽略 */
        TEXT(CATEGORY_GROUP.STRING, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE),
        /** 布尔类型 — 全部忽略 */
        BOOLEAN(CATEGORY_GROUP.BOOLEAN, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE),
        /** 定长二进制，如 RAW、BINARY — 需要长度 */
        BYTES(CATEGORY_GROUP.BYTES, IgnorePolicy.KEEP, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE),
        /** 大二进制对象，如 BLOB — 全部忽略 */
        BLOB(CATEGORY_GROUP.BYTES, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE),
        /** 整数类型，如 INT、BIGINT — 需要长度（显示宽度），忽略精度和小数位 */
        INT(CATEGORY_GROUP.NUMBER, IgnorePolicy.KEEP, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE),
        /** 浮点/精确数值，如 FLOAT、NUMERIC、DECIMAL — 忽略长度，保留精度和小数位 */
        FLOAT(CATEGORY_GROUP.NUMBER, IgnorePolicy.IGNORE, IgnorePolicy.KEEP, IgnorePolicy.KEEP),
        /** 日期类型，如 DATE — 全部忽略 */
        DATE(CATEGORY_GROUP.DATETIME, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE),
        /** 时间类型，如 TIME — 全部忽略 */
        TIME(CATEGORY_GROUP.DATETIME, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE),
        /** 日期时间类型（无时区），如 DATETIME — 全部忽略 */
        DATETIME(CATEGORY_GROUP.DATETIME, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE),
        /** 时间戳类型，如 TIMESTAMP — 全部忽略 */
        TIMESTAMP(CATEGORY_GROUP.DATETIME, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE),
        /** 集合/数组类型 — 全部忽略 */
        COLLECTION(CATEGORY_GROUP.COLLECTION, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE),
        /** 空间几何类型，如 GEOMETRY、GEOGRAPHY — 全部忽略 */
        GEOMETRY(CATEGORY_GROUP.GEOMETRY, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE),
        /** 时间间隔类型，如 INTERVAL — 精度视情况，精度与小数位互依赖 */
        INTERVAL(CATEGORY_GROUP.INTERVAL, IgnorePolicy.IGNORE, IgnorePolicy.CONDITIONAL, IgnorePolicy.MUTUAL_DEPENDENT),
        /** 其他未分类类型 — 全部忽略 */
        OTHER(CATEGORY_GROUP.OTHER, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE),
        /** 无类型兜底 — 全部忽略 */
        NONE(CATEGORY_GROUP.NONE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE);

        private final CATEGORY_GROUP group;
        private final IgnorePolicy ignoreLength;
        private final IgnorePolicy ignorePrecision;
        private final IgnorePolicy ignoreScale;
        private Config config;

        CATEGORY(CATEGORY_GROUP group, IgnorePolicy ignoreLength, IgnorePolicy ignorePrecision, IgnorePolicy ignoreScale) {
            this.group = group;
            this.ignoreLength = ignoreLength;
            this.ignorePrecision = ignorePrecision;
            this.ignoreScale = ignoreScale;
        }

        /** 获取该分类所属的大分组 */
        public CATEGORY_GROUP group() {
            return group;
        }

        /**
         * 获取该分类的默认配置（懒加载，首次调用时从 IgnorePolicy 初始化）。
         *
         * @return 包含忽略策略的 {@link Config} 实例
         */
        public Config config() {
            if (null == config) {
                config = new Config();
                config.setIgnoreLength(ignoreLength.code())
                        .setIgnorePrecision(ignorePrecision.code())
                        .setIgnoreScale(ignoreScale.code());
            }
            return config;
        }
    }

    // ========== 方法声明 ==========

    /**
     * 获取该类型的二级分类（基于长度/精度/小数位特性）。
     *
     * @return {@link CATEGORY} 枚举值，如 CHAR、INT、FLOAT、GEOMETRY 等
     */
    CATEGORY getCategory();

    /**
     * 获取该类型的大分组。
     *
     * @return {@link CATEGORY_GROUP} 枚举值
     */
    CATEGORY_GROUP getCategoryGroup();

    /**
     * 获取该类型的名称，用于 SQL 生成和显示。
     *
     * @return 类型名称，如 "VARCHAR"、"NUMERIC"、"TIMESTAMP"
     */
    String getName();

    /**
     * 获取长度参数的忽略策略值。
     *
     * @return {@link IgnorePolicy#code()} 的值：-1 未设置，0 保留，1 忽略，2 视情况，3 互依赖
     */
    int ignoreLength();

    /**
     * 获取精度参数的忽略策略值。
     *
     * @return 同 {@link #ignoreLength()}
     */
    int ignorePrecision();

    /**
     * 获取小数位参数的忽略策略值。
     *
     * @return 同 {@link #ignoreLength()}
     */
    int ignoreScale();

    /**
     * 判断当前类型是否受支持（可安全映射到 Java 类型）。
     *
     * @return {@code true} 表示支持，{@code false} 表示该类型需要特殊处理
     */
    boolean support();

    /**
     * 获取该类型对应的 Java 类。
     * <p>
     * 例如：VARCHAR → String.class，NUMERIC → BigDecimal.class，GEOMETRY → byte[].class
     *
     * @return 对应的 Java 类
     */
    Class<?> supportClass();

    /**
     * 获取该类型的运行时配置。
     *
     * @return {@link Config} 实例，包含忽略策略、长度/精度/小数位的列引用等配置
     */
    Config config();

    // ========== Config 内部类 ==========

    /**
     * 类型的运行时配置，封装 SQL DDL 生成所需的元信息。
     * <p>
     * 主要包含两部分：
     * <ul>
     *   <li><b>忽略策略</b> — ignoreLength/ignorePrecision/ignoreScale，决定 SQL 生成时是否输出长度/精度/小数位参数</li>
     *   <li><b>列引用</b> — lengthRefers/precisionRefers/scaleRefers，从数据库元数据读取时对应的实际列名</li>
     * </ul>
     *
     * <p>通常通过 {@link CATEGORY#config()} 获取实例，而非直接构造。
     */
    class Config {

        /**
         * SQL 数据类型，用于比较数据类型是否相同。不提供则根据NAME生成
         */
        private String meta;

        /**
         * SQL生成公式，如 INTERVAL DAY({p}) TO HOUR。不提供则根据NAME生成
         */
        private String formula;

        /**
         * 是否忽略长度：-1未设置(继承上级)，0不忽略，1忽略，2视情况，3精度和小数位互依赖
         */
        private int ignoreLength = -1;
        private int ignorePrecision = -1;
        private int ignoreScale = -1;

        /**
         * 从元数据读取时，字符类型长度对应的列。多列以,分隔
         */
        private String[] lengthRefers;

        /**
         * 从元数据读取时，数字类型长度对应的列
         */
        private String[] precisionRefers;

        /**
         * 从元数据读取时，小数位对应的列
         */
        private String[] scaleRefers;

        public Config() {
        }

        public Config(String meta, String formula, String lengthRefer, String precisionRefer,
                      String scaleRefer, int ignoreLength, int ignorePrecision, int ignoreScale) {
            setMeta(meta);
            setFormula(formula);
            setLengthRefer(lengthRefer);
            setScaleRefer(scaleRefer);
            setPrecisionRefer(precisionRefer);
            this.ignoreLength = ignoreLength;
            this.ignorePrecision = ignorePrecision;
            this.ignoreScale = ignoreScale;
        }

        public Config(String lengthRefer, String precisionRefer, String scaleRefer,
                      int ignoreLength, int ignorePrecision, int ignoreScale) {
            setLengthRefer(lengthRefer);
            setScaleRefer(scaleRefer);
            setPrecisionRefer(precisionRefer);
            this.ignoreLength = ignoreLength;
            this.ignorePrecision = ignorePrecision;
            this.ignoreScale = ignoreScale;
        }

        public Config(String lengthRefer, String precisionRefer, String scaleRefer) {
            setLengthRefer(lengthRefer);
            setScaleRefer(scaleRefer);
            setPrecisionRefer(precisionRefer);
        }

        public Config(int ignoreLength, int ignorePrecision, int ignoreScale) {
            this.ignoreLength = ignoreLength;
            this.ignorePrecision = ignorePrecision;
            this.ignoreScale = ignoreScale;
        }

        public int ignoreLength() {
            return ignoreLength;
        }

        public Config setIgnoreLength(int ignoreLength) {
            this.ignoreLength = ignoreLength;
            return this;
        }

        public int ignorePrecision() {
            return ignorePrecision;
        }

        public Config setIgnorePrecision(int ignorePrecision) {
            this.ignorePrecision = ignorePrecision;
            return this;
        }

        public int ignoreScale() {
            return ignoreScale;
        }

        public Config setIgnoreScale(int ignoreScale) {
            this.ignoreScale = ignoreScale;
            return this;
        }

        public String[] getLengthRefers() {
            return lengthRefers;
        }

        public String getLengthRefer() {
            return (null != lengthRefers && lengthRefers.length > 0) ? lengthRefers[0] : null;
        }

        public Config setLengthRefers(String[] lengthRefers) {
            this.lengthRefers = lengthRefers;
            return this;
        }

        public Config setLengthRefer(String lengthRefer) {
            this.lengthRefers = StrUtil.isNotEmpty(lengthRefer) ? lengthRefer.split(",") : null;
            return this;
        }

        public String[] getPrecisionRefers() {
            return precisionRefers;
        }

        public String getPrecisionRefer() {
            return (null != precisionRefers && precisionRefers.length > 0) ? precisionRefers[0] : null;
        }

        public Config setPrecisionRefers(String[] precisionRefers) {
            this.precisionRefers = precisionRefers;
            return this;
        }

        public Config setPrecisionRefer(String precisionRefer) {
            this.precisionRefers = StrUtil.isNotEmpty(precisionRefer) ? precisionRefer.split(",") : null;
            return this;
        }

        public String[] getScaleRefers() {
            return scaleRefers;
        }

        public String getScaleRefer() {
            return (null != scaleRefers && scaleRefers.length > 0) ? scaleRefers[0] : null;
        }

        public Config setScaleRefers(String[] scaleRefers) {
            this.scaleRefers = scaleRefers;
            return this;
        }

        public Config setScaleRefer(String scaleRefer) {
            this.scaleRefers = StrUtil.isNotEmpty(scaleRefer) ? scaleRefer.split(",") : null;
            return this;
        }

        public String getFormula() {
            return formula;
        }

        public void setFormula(String formula) {
            this.formula = formula;
        }

        public String getMeta() {
            return meta;
        }

        public void setMeta(String meta) {
            this.meta = meta;
        }

        /**
         * 合并非空且!= -1的属性
         *
         * @deprecated 当前无调用者，保留供未来扩展使用
         */
        @Deprecated
        public Config merge(Config copy) {
            if (null != copy) {
                if (StrUtil.isNotEmpty(copy.getMeta())) this.meta = copy.getMeta();
                if (StrUtil.isNotEmpty(copy.getFormula())) this.formula = copy.getFormula();
                if (-1 != copy.ignoreLength()) this.ignoreLength = copy.ignoreLength();
                if (-1 != copy.ignorePrecision()) this.ignorePrecision = copy.ignorePrecision();
                if (-1 != copy.ignoreScale()) this.ignoreScale = copy.ignoreScale();
                if (null != copy.getLengthRefers() && copy.getLengthRefers().length > 0)
                    this.lengthRefers = copy.getLengthRefers();
                if (null != copy.getPrecisionRefers() && copy.getPrecisionRefers().length > 0)
                    this.precisionRefers = copy.getPrecisionRefers();
                if (null != copy.getScaleRefers() && copy.getScaleRefers().length > 0)
                    this.scaleRefers = copy.getScaleRefers();
            }
            return this;
        }
    }
}
