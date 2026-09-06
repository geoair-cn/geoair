package cn.geoair.map.dynamic.adv.spring;

import cn.hutool.core.util.StrUtil;
import cn.hutool.db.dialect.DialectName;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 方言提供者的公共实现，统一处理产品名标准化和关键字匹配。
 *
 * @author 张逢吉
 */
public abstract class AbstractAdvDialectProvider implements AdvDialectProvider {

    private final String dialectId;
    private final DialectName dialectName;
    private final Set<String> productNameKeywords;

    protected AbstractAdvDialectProvider(String dialectId, DialectName dialectName, String... productNameKeywords) {
        if (StrUtil.isBlank(dialectId)) {
            throw new IllegalArgumentException("dialectId 不能为空");
        }
        this.dialectId = dialectId;
        this.dialectName = dialectName;
        Set<String> keywords = new LinkedHashSet<>();
        if (productNameKeywords != null) {
            for (String keyword : Arrays.asList(productNameKeywords)) {
                if (StrUtil.isNotBlank(keyword)) {
                    keywords.add(keyword.toUpperCase(Locale.ROOT));
                }
            }
        }
        this.productNameKeywords = Collections.unmodifiableSet(keywords);
    }

    @Override
    public final String getDialectId() {
        return dialectId;
    }

    @Override
    public final DialectName getDialectName() {
        return dialectName;
    }

    @Override
    public boolean supportsProductName(String databaseProductName) {
        if (StrUtil.isBlank(databaseProductName)) {
            return false;
        }
        String normalized = databaseProductName.toUpperCase(Locale.ROOT);
        for (String keyword : productNameKeywords) {
            if (normalized.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
