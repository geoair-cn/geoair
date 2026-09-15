package cn.geoair.map.dynamic.adv.dbmeta;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * SQLite 声明类型到五种类型亲和性的映射。
 *
 * <p>SQLite 允许任意声明类型，因此不能按固定类型名做穷举匹配。</p>
 */
public enum SqliteType implements DataBaseFieldType {
    INTEGER("INTEGER", DefaultJavaType.JAVA_LONG, CategoryEnum.INT),
    TEXT("TEXT", DefaultJavaType.JAVA_TEXT, CategoryEnum.TEXT),
    BLOB("BLOB", DefaultJavaType.JAVA_BLOB, CategoryEnum.BLOB),
    REAL("REAL", DefaultJavaType.JAVA_DOUBLE, CategoryEnum.FLOAT),
    NUMERIC("NUMERIC", DefaultJavaType.JAVA_NUMERIC, CategoryEnum.FLOAT);

    private final String standardName;
    private final DefaultJavaType javaType;
    private final CategoryEnum category;

    SqliteType(String standardName, DefaultJavaType javaType, CategoryEnum category) {
        this.standardName = standardName;
        this.javaType = javaType;
        this.category = category;
    }

    /** 按 SQLite 官方的声明类型亲和性顺序解析。 */
    public static SqliteType getByUdtName(String declaredType) {
        if (declaredType == null || declaredType.trim().isEmpty()) {
            return BLOB;
        }
        String type = declaredType.toUpperCase(Locale.ROOT);
        if (type.contains("INT")) {
            return INTEGER;
        }
        if (type.contains("CHAR") || type.contains("CLOB") || type.contains("TEXT")) {
            return TEXT;
        }
        if (type.contains("BLOB")) {
            return BLOB;
        }
        if (type.contains("REAL") || type.contains("FLOA") || type.contains("DOUB")) {
            return REAL;
        }
        return NUMERIC;
    }

    @Override
    public List<String> getUdtNames() {
        return Collections.singletonList(standardName.toLowerCase(Locale.ROOT));
    }

    @Override
    public String getStandardName() {
        return standardName;
    }

    @Override
    public DefaultJavaType getJavaType() {
        return javaType;
    }

    @Override
    public CategoryEnum getCategory() {
        return category;
    }

    @Override
    public CategoryGroupEnum getCategoryGroup() {
        return category.group();
    }

    @Override
    public String getName() {
        return standardName;
    }

    @Override
    public int ignoreLength() {
        return javaType.ignoreLength();
    }

    @Override
    public int ignorePrecision() {
        return javaType.ignorePrecision();
    }

    @Override
    public int ignoreScale() {
        return javaType.ignoreScale();
    }

    @Override
    public boolean support() {
        return javaType.support();
    }

    @Override
    public Class<?> supportClass() {
        return javaType.supportClass();
    }

    @Override
    public Config config() {
        return category.config();
    }
}
