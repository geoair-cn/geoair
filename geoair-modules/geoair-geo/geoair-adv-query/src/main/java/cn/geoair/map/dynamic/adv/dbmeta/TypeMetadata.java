package cn.geoair.map.dynamic.adv.dbmeta;

import cn.hutool.core.util.StrUtil;

/**
 * 数据库类型的统一元数据接口，描述一个数据库字段类型的完整语义。
 *
 * <p>本接口是类型体系的核心契约，为 SQL DDL 生成、字段类型判断、Java 类型映射提供统一基础。 主要实现者：
 *
 * <ul>
 *   <li>{@link DefaultJavaType} — Java 类型枚举，描述 Java 侧的类型语义
 *   <li>{@link DataBaseFieldType} — 继承本接口，增加数据库特定的 UDT 名称映射能力
 * </ul>
 *
 * <h3>类型分类体系</h3>
 *
 * <pre>
 * CategoryGroupEnum (大分组: STRING / NUMBER / DATETIME / ...)
 *   └── CategoryEnum (二级分组: CHAR / TEXT / INT / FLOAT / DATE / TIMESTAMP / ...)
 *         └── IgnorePolicy (长度/精度/小数位的忽略策略)
 * </pre>
 *
 * @author zhangjun
 * @date 2026/8/14
 */
public interface TypeMetadata {

    // ========== 方法声明 ==========

    /**
     * 获取该类型的二级分类（基于长度/精度/小数位特性）。
     *
     * @return {@link CategoryEnum} 枚举值，如 CHAR、INT、FLOAT、GEOMETRY 等
     */
    CategoryEnum getCategory();

    /**
     * 获取该类型的大分组。
     *
     * @return {@link CategoryGroupEnum} 枚举值
     */
    CategoryGroupEnum getCategoryGroup();

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
     *
     * <p>例如：VARCHAR → String.class，NUMERIC → BigDecimal.class，GEOMETRY → byte[].class
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
     *
     * <p>主要包含两部分：
     *
     * <ul>
     *   <li><b>忽略策略</b> — ignoreLength/ignorePrecision/ignoreScale，决定 SQL 生成时是否输出长度/精度/小数位参数
     *   <li><b>列引用</b> — lengthRefers/precisionRefers/scaleRefers，从数据库元数据读取时对应的实际列名
     * </ul>
     *
     * <p>通常通过 {@link CategoryEnum#config()} 获取实例，而非直接构造。
     */
    class Config {

        /** SQL 数据类型，用于比较数据类型是否相同。不提供则根据NAME生成 */
        private String meta;

        /** SQL生成公式，如 INTERVAL DAY({p}) TO HOUR。不提供则根据NAME生成 */
        private String formula;

        /** 是否忽略长度：-1未设置(继承上级)，0不忽略，1忽略，2视情况，3精度和小数位互依赖 */
        private int ignoreLength = -1;

        private int ignorePrecision = -1;
        private int ignoreScale = -1;

        /** 从元数据读取时，字符类型长度对应的列。多列以,分隔 */
        private String[] lengthRefers;

        /** 从元数据读取时，数字类型长度对应的列 */
        private String[] precisionRefers;

        /** 从元数据读取时，小数位对应的列 */
        private String[] scaleRefers;

        public Config() {}

        public Config(
                String meta,
                String formula,
                String lengthRefer,
                String precisionRefer,
                String scaleRefer,
                int ignoreLength,
                int ignorePrecision,
                int ignoreScale) {
            setMeta(meta);
            setFormula(formula);
            setLengthRefer(lengthRefer);
            setScaleRefer(scaleRefer);
            setPrecisionRefer(precisionRefer);
            this.ignoreLength = ignoreLength;
            this.ignorePrecision = ignorePrecision;
            this.ignoreScale = ignoreScale;
        }

        public Config(
                String lengthRefer,
                String precisionRefer,
                String scaleRefer,
                int ignoreLength,
                int ignorePrecision,
                int ignoreScale) {
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
            return (null != precisionRefers && precisionRefers.length > 0)
                    ? precisionRefers[0]
                    : null;
        }

        public Config setPrecisionRefers(String[] precisionRefers) {
            this.precisionRefers = precisionRefers;
            return this;
        }

        public Config setPrecisionRefer(String precisionRefer) {
            this.precisionRefers =
                    StrUtil.isNotEmpty(precisionRefer) ? precisionRefer.split(",") : null;
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
