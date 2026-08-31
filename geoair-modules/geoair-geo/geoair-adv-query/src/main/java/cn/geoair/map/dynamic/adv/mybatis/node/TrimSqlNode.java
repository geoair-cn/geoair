package cn.geoair.map.dynamic.adv.mybatis.node;

import cn.geoair.map.dynamic.adv.mybatis.context.Context;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Set;

/**
 * 修剪节点，用于在子节点 SQL 前后添加前缀/后缀，并去除子节点 SQL 首尾的指定模式。
 *
 * <p>这是 {@code <where>} 和 {@code <set>} 标签的基类，也可直接用于 {@code <trim>} 标签。
 *
 * <p>处理流程：
 *
 * <ol>
 *   <li>在临时上下文中执行子节点，获取原始 SQL
 *   <li>去除 SQL 首部匹配 {@code prefixesToOverride} 的前缀（如 "AND "、"OR "）
 *   <li>去除 SQL 尾部匹配 {@code suffixesToOverride} 的后缀（如 ","）
 *   <li>在非空 SQL 前后添加 {@code prefix} / {@code suffix}
 * </ol>
 *
 * @author zhangjun
 * @see WhereSqlNode
 * @see SetSqlNode
 */
public class TrimSqlNode implements SqlNode {

    private final SqlNode contents;
    private final String prefix;
    private final String suffix;
    private final List<String> prefixesToOverride;
    private final List<String> suffixesToOverride;

    public TrimSqlNode(
            SqlNode contents,
            String prefix,
            String suffix,
            List<String> prefixesToOverride,
            List<String> suffixesToOverride) {
        this.contents = contents;
        this.prefix = prefix;
        this.suffix = suffix;
        this.prefixesToOverride = prefixesToOverride;
        this.suffixesToOverride = suffixesToOverride;
    }

    /** 执行子节点、修剪首尾模式、添加前后缀。 */
    @Override
    public void apply(Context context) {
        context.appendSql(" ");
        Context proxy = new Context(context.getData());
        contents.apply(proxy);
        String sql = proxy.getSql().trim();

        if (sql.length() > 0) {
            if (prefixesToOverride != null) {
                for (String key : prefixesToOverride) {
                    if (sql.startsWith(key)) {
                        sql = sql.substring(key.length());
                        break;
                    }
                }
            }
            if (suffixesToOverride != null) {
                for (String key : suffixesToOverride) {
                    if (sql.endsWith(key)) {
                        sql = sql.substring(0, sql.length() - key.length());
                        break;
                    }
                }
            }
        }

        if (StringUtils.isNotBlank(sql) && StringUtils.isNotBlank(prefix)) {
            context.appendSql(prefix);
        }
        context.appendSql(sql);
        if (StringUtils.isNotBlank(sql) && StringUtils.isNotBlank(suffix)) {
            context.appendSql(suffix);
        }
    }

    @Override
    public void applyParameter(Set<String> set) {
        contents.applyParameter(set);
    }
}
