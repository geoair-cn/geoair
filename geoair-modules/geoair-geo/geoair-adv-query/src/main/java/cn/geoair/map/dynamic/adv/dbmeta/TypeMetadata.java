package cn.geoair.map.dynamic.adv.dbmeta;

import cn.hutool.core.util.StrUtil;

/** 数据库类型的枚举 */
public interface TypeMetadata {

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

    // 基于以下特性区分的二级分组
    // 要用来区分 length/precision
    // BLOB不需要长度 BYTES需要长度
    // TIMESTAMP在有些数据库中支持SCALE需要在单独的alias中设置如TIMESTAMP(6)
    enum CATEGORY {
        CHAR(CATEGORY_GROUP.STRING, 0, 1, 1),
        TEXT(CATEGORY_GROUP.STRING, 1, 1, 1),
        BOOLEAN(CATEGORY_GROUP.BOOLEAN, 1, 1, 1),
        BYTES(CATEGORY_GROUP.BYTES, 0, 1, 1),
        BLOB(CATEGORY_GROUP.BYTES, 1, 1, 1),
        INT(CATEGORY_GROUP.NUMBER, 0, 1, 1),
        FLOAT(CATEGORY_GROUP.NUMBER, 1, 0, 0),
        DATE(CATEGORY_GROUP.DATETIME, 1, 1, 1),
        TIME(CATEGORY_GROUP.DATETIME, 1, 1, 1),
        DATETIME(CATEGORY_GROUP.DATETIME, 1, 1, 1),
        TIMESTAMP(CATEGORY_GROUP.DATETIME, 1, 1, 1),
        COLLECTION(CATEGORY_GROUP.COLLECTION, 1, 1, 1),
        GEOMETRY(CATEGORY_GROUP.GEOMETRY, 1, 1, 1),
        INTERVAL(CATEGORY_GROUP.INTERVAL, 1, 2, 3),
        OTHER(CATEGORY_GROUP.OTHER, 1, 1, 1),
        NONE(CATEGORY_GROUP.NONE, 1, 1, 1);

        private final CATEGORY_GROUP group;

        private final int ignoreLength;

        private final int ignorePrecision;

        private final int ignoreScale;

        private Config config;

        CATEGORY(CATEGORY_GROUP group, int ignoreLength, int ignorePrecision, int ignoreScale) {
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
                config.setIgnoreLength(ignoreLength)
                        .setIgnorePrecision(ignorePrecision)
                        .setIgnoreScale(ignoreScale);
            }
            return config;
        }
    }

    default boolean equals(TypeMetadata metadata) {
        if (null == metadata) {
            return false;
        }
        if (this.getOrigin() == metadata) {
            return true;
        }
        if (this == metadata) {
            return true;
        }
        if (this == metadata.getOrigin()) {
            return true;
        }
        if (this.getOrigin() == metadata.getOrigin()) {
            return true;
        }
        return false;
    }

    TypeMetadata ILLEGAL =
            new TypeMetadata() {

                @Override
                public CATEGORY getCategory() {
                    return CATEGORY.NONE;
                }

                @Override
                public CATEGORY_GROUP getCategoryGroup() {
                    return CATEGORY_GROUP.NONE;
                }

                @Override
                public String getName() {
                    return "ILLEGAL";
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
                    return false;
                }

                @Override
                public Config config() {
                    return new Config();
                }
            };

    // 不识别的类型 原样输出
    TypeMetadata NONE =
            new TypeMetadata() {

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
                public Config config() {
                    return new Config();
                }

                @Override
                public CATEGORY_GROUP getCategoryGroup() {
                    return CATEGORY_GROUP.NONE;
                }
            };

    CATEGORY getCategory();

    CATEGORY_GROUP getCategoryGroup();

    String getName();

    default TypeMetadata getOrigin() {
        return this;
    }

    int ignoreLength();

    int ignorePrecision();

    int ignoreScale();

    boolean support();

    default String formula() {
        return null;
    }

    default boolean isArray() {
        return false;
    }

    Config config();

    class Config {

        /** SQL 数据类型(用来比较数据类型是否相同) INTERVAL DAY TO HOUR 不提供则根据NAME 生成 */
        private String meta;

        /** SQL生成公式如INTERVAL DAY(｛p｝) TO HOUR 不提供则根据NAME 生成 */
        private String formula;

        /**
         * 是否忽略长度，创建和比较时忽略，但元数据中可能会有对应的列也有值 -1:未设置可以继承上级 0:不忽略 1:忽略 2:根据情况(是否提供)
         * 3:用来处理precision和scale相互依赖的情况,只有同时有值才生效,其中一个没值就全忽略
         */
        private int ignoreLength = -1;

        private int ignorePrecision = -1;

        private int ignoreScale = -1;

        /**
         * 读取元数据时 字符类型长度对应的列<br>
         * 正常情况下只有一列<br>
         * 如果需要取多列以,分隔
         */
        private String[] lengthRefers;

        /**
         * 读取元数据时 数字类型长度对应的列<br>
         * 正常情况下只有一列<br>
         * 如果需要取多列以,分隔
         */
        private String[] precisionRefers;

        /**
         * 读取元数据时 小数位对应的列<br>
         * 正常情况下只有一列<br>
         * 如果需要取多列以,分隔
         */
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
            if (null != lengthRefers && lengthRefers.length > 0) {
                return lengthRefers[0];
            }
            return null;
        }

        public Config setLengthRefers(String[] lengthRefers) {
            this.lengthRefers = lengthRefers;
            return this;
        }

        public Config setLengthRefer(String lengthRefer) {
            if (StrUtil.isNotEmpty(lengthRefer)) {
                this.lengthRefers = lengthRefer.split(",");
            } else {

                this.lengthRefers = null;
            }
            return this;
        }

        public String[] getPrecisionRefers() {
            return precisionRefers;
        }

        public String getPrecisionRefer() {
            if (null != precisionRefers && precisionRefers.length > 0) {
                return precisionRefers[0];
            }
            return null;
        }

        public Config setPrecisionRefers(String[] precisionRefers) {
            this.precisionRefers = precisionRefers;
            return this;
        }

        public Config setPrecisionRefer(String precisionRefer) {
            if (StrUtil.isNotEmpty(precisionRefer)) {
                this.precisionRefers = precisionRefer.split(",");
            } else {
                this.precisionRefers = null;
            }
            return this;
        }

        public String[] getScaleRefers() {
            return scaleRefers;
        }

        public String getScaleRefer() {
            if (null != scaleRefers && scaleRefers.length > 0) {
                return scaleRefers[0];
            }
            return null;
        }

        public Config setScaleRefers(String[] scaleRefers) {
            this.scaleRefers = scaleRefers;
            return this;
        }

        public Config setScaleRefer(String scaleRefer) {
            if (StrUtil.isNotEmpty(scaleRefer)) {
                this.scaleRefers = scaleRefer.split(",");
            } else {

                this.scaleRefers = null;
            }
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
         * 合并copy的属性(非空并且!=-1的属性)
         *
         * @param copy 复本
         * @return Config
         */
        public Config merge(Config copy) {
            if (null != copy) {
                String meta = copy.getMeta();
                String formula = copy.getFormula();
                int ignoreLength = copy.ignoreLength();
                int ignorePrecision = copy.ignorePrecision;
                int ignoreScale = copy.ignoreScale();
                if (StrUtil.isNotEmpty(meta)) {
                    this.meta = meta;
                }
                if (StrUtil.isNotEmpty(formula)) {
                    this.formula = formula;
                }
                if (-1 != ignoreLength) {
                    this.ignoreLength = ignoreLength;
                }
                if (-1 != ignorePrecision) {
                    this.ignorePrecision = ignorePrecision;
                }
                if (-1 != ignoreScale) {
                    this.ignoreScale = ignoreScale;
                }
                String[] lengthRefers = copy.getLengthRefers();
                ;
                String[] precisionRefers = copy.getPrecisionRefers();
                String[] scaleRefers = copy.getScaleRefers();
                if (null != lengthRefers && lengthRefers.length > 0) {
                    this.lengthRefers = lengthRefers;
                }
                if (null != precisionRefers && precisionRefers.length > 0) {
                    this.precisionRefers = precisionRefers;
                }
                if (null != scaleRefers && scaleRefers.length > 0) {
                    this.scaleRefers = scaleRefers;
                }
            }
            return this;
        }
    }
}
