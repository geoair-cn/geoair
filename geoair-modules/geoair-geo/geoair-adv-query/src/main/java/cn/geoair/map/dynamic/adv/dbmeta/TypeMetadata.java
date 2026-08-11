package cn.geoair.map.dynamic.adv.dbmeta;

import cn.hutool.core.util.StrUtil;

/** 数据库类型的统一元数据接口（合并了旧的DataType接口） */
public interface TypeMetadata {

    /** 忽略策略枚举 */
    enum IgnorePolicy {
        NOT_SET(-1),
        KEEP(0),
        IGNORE(1),
        CONDITIONAL(2),
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

    /** 字段类型的大分组 */
    enum CATEGORY_GROUP {
        STRING,
        NUMBER,
        BOOLEAN,
        BYTES,
        DATETIME,
        COLLECTION,
        GEOMETRY,
        INTERVAL,
        OTHER,
        NONE
    }

    /** 基于长度/精度/小数位特性的二级分组 */
    enum CATEGORY {
        CHAR(CATEGORY_GROUP.STRING, IgnorePolicy.KEEP, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE),
        TEXT(CATEGORY_GROUP.STRING, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE),
        BOOLEAN(CATEGORY_GROUP.BOOLEAN, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE),
        BYTES(CATEGORY_GROUP.BYTES, IgnorePolicy.KEEP, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE),
        BLOB(CATEGORY_GROUP.BYTES, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE),
        INT(CATEGORY_GROUP.NUMBER, IgnorePolicy.KEEP, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE),
        FLOAT(CATEGORY_GROUP.NUMBER, IgnorePolicy.IGNORE, IgnorePolicy.KEEP, IgnorePolicy.KEEP),
        DATE(CATEGORY_GROUP.DATETIME, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE),
        TIME(CATEGORY_GROUP.DATETIME, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE),
        DATETIME(CATEGORY_GROUP.DATETIME, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE),
        TIMESTAMP(CATEGORY_GROUP.DATETIME, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE),
        COLLECTION(CATEGORY_GROUP.COLLECTION, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE),
        GEOMETRY(CATEGORY_GROUP.GEOMETRY, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE),
        INTERVAL(CATEGORY_GROUP.INTERVAL, IgnorePolicy.IGNORE, IgnorePolicy.CONDITIONAL, IgnorePolicy.MUTUAL_DEPENDENT),
        OTHER(CATEGORY_GROUP.OTHER, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE),
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

        public CATEGORY_GROUP group() {
            return group;
        }

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

    /** 判断两个TypeMetadata是否代表同一类型 */
    default boolean isSameType(TypeMetadata other) {
        if (null == other) {
            return false;
        }
        if (this.getOrigin() == other) {
            return true;
        }
        if (this == other) {
            return true;
        }
        if (this == other.getOrigin()) {
            return true;
        }
        if (this.getOrigin() == other.getOrigin()) {
            return true;
        }
        return false;
    }

    /** 不识别的类型，原样输出 */
    TypeMetadata NONE = new TypeMetadata() {
        @Override
        public CATEGORY getCategory() {
            return CATEGORY.NONE;
        }

        @Override
        public String getName() {
            return "NONE";
        }

        @Override
        public int ignoreLength() {
            return -1;
        }

        @Override
        public int ignorePrecision() {
            return -1;
        }

        @Override
        public int ignoreScale() {
            return -1;
        }

        @Override
        public boolean support() {
            return true;
        }

        @Override
        public Class<?> supportClass() {
            return Object.class;
        }

        @Override
        public Config config() {
            return new Config();
        }

        @Override
        public CATEGORY_GROUP getCategoryGroup() {
            return CATEGORY_GROUP.NONE;
        }
    };

    // ========== 方法声明 ==========

    CATEGORY getCategory();

    CATEGORY_GROUP getCategoryGroup();

    String getName();

    default TypeMetadata getOrigin() {
        return this;
    }

    int ignoreLength();

    int ignorePrecision();

    int ignoreScale();

    /** 是否支持该类型 */
    boolean support();

    /** 该类型对应的Java类 */
    Class<?> supportClass();

    default String formula() {
        return null;
    }

    default boolean isArray() {
        return false;
    }

    Config config();

    // ========== Config 内部类 ==========

    class Config {

        /** SQL 数据类型，用于比较数据类型是否相同。不提供则根据NAME生成 */
        private String meta;

        /** SQL生成公式，如 INTERVAL DAY({p}) TO HOUR。不提供则根据NAME生成 */
        private String formula;

        /**
         * 是否忽略长度：-1未设置(继承上级)，0不忽略，1忽略，2视情况，3精度和小数位互依赖
         */
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

        public int ignoreLength() { return ignoreLength; }
        public Config setIgnoreLength(int ignoreLength) { this.ignoreLength = ignoreLength; return this; }

        public int ignorePrecision() { return ignorePrecision; }
        public Config setIgnorePrecision(int ignorePrecision) { this.ignorePrecision = ignorePrecision; return this; }

        public int ignoreScale() { return ignoreScale; }
        public Config setIgnoreScale(int ignoreScale) { this.ignoreScale = ignoreScale; return this; }

        public String[] getLengthRefers() { return lengthRefers; }
        public String getLengthRefer() {
            return (null != lengthRefers && lengthRefers.length > 0) ? lengthRefers[0] : null;
        }
        public Config setLengthRefers(String[] lengthRefers) { this.lengthRefers = lengthRefers; return this; }
        public Config setLengthRefer(String lengthRefer) {
            this.lengthRefers = StrUtil.isNotEmpty(lengthRefer) ? lengthRefer.split(",") : null;
            return this;
        }

        public String[] getPrecisionRefers() { return precisionRefers; }
        public String getPrecisionRefer() {
            return (null != precisionRefers && precisionRefers.length > 0) ? precisionRefers[0] : null;
        }
        public Config setPrecisionRefers(String[] precisionRefers) { this.precisionRefers = precisionRefers; return this; }
        public Config setPrecisionRefer(String precisionRefer) {
            this.precisionRefers = StrUtil.isNotEmpty(precisionRefer) ? precisionRefer.split(",") : null;
            return this;
        }

        public String[] getScaleRefers() { return scaleRefers; }
        public String getScaleRefer() {
            return (null != scaleRefers && scaleRefers.length > 0) ? scaleRefers[0] : null;
        }
        public Config setScaleRefers(String[] scaleRefers) { this.scaleRefers = scaleRefers; return this; }
        public Config setScaleRefer(String scaleRefer) {
            this.scaleRefers = StrUtil.isNotEmpty(scaleRefer) ? scaleRefer.split(",") : null;
            return this;
        }

        public String getFormula() { return formula; }
        public void setFormula(String formula) { this.formula = formula; }

        public String getMeta() { return meta; }
        public void setMeta(String meta) { this.meta = meta; }

        /** 合并非空且!= -1的属性 */
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
