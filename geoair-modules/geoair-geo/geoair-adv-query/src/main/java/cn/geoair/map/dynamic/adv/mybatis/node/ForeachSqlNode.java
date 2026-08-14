package cn.geoair.map.dynamic.adv.mybatis.node;

import cn.geoair.map.dynamic.adv.mybatis.context.Context;
import cn.geoair.map.dynamic.adv.mybatis.token.TokenHandler;
import cn.geoair.map.dynamic.adv.mybatis.token.TokenParser;
import cn.geoair.map.dynamic.adv.mybatis.util.OgnlUtil;
import cn.geoair.map.dynamic.adv.mybatis.util.RegexUtil;
import java.util.ArrayList;
import java.util.Set;

/**
 * 循环节点，对应 {@code <foreach>} 标签。
 * <p>
 * 将集合展开为 SQL 片段，支持自定义：
 * <ul>
 *   <li>{@code open} / {@code close} — 循环体的前后包裹字符（如括号）</li>
 *   <li>{@code separator} — 每次迭代之间的分隔符（如逗号）</li>
 *   <li>{@code item} — 循环变量名，默认 "item"</li>
 *   <li>{@code index} — 索引变量名，默认 "index"</li>
 * </ul>
 *
 * <p>执行流程：
 * <ol>
 *   <li>通过 OGNL 获取集合的可迭代对象</li>
 *   <li>遍历集合，每次迭代将 {@code item} 替换为 {@code collection[i]}，{@code index} 替换为 {@code __index_collection[i]}</li>
 *   <li>将展开后的 SQL 片段追加到上下文</li>
 * </ol>
 *
 * @author zhangjun
 */
public class ForeachSqlNode implements SqlNode {

    private final String collection;
    private final String open;
    private final String close;
    private final String separator;
    private final String item;
    private final String index;
    private final SqlNode contents;
    private final String indexDataName;

    public ForeachSqlNode(String collection, String open, String close,
                          String separator, String item, String index, SqlNode contents) {
        this.collection = collection;
        this.open = open;
        this.close = close;
        this.separator = separator;
        this.item = item;
        this.index = index;
        this.contents = contents;
        this.indexDataName = String.format("__index_%s", collection);
    }

    /**
     * 展开循环：遍历集合，生成带索引的 SQL 片段。
     */
    @Override
    public void apply(Context context) {
        context.appendSql(" ");
        Iterable<?> iterable = OgnlUtil.getIterable(collection, context.getData());
        int currentIndex = 0;

        ArrayList<Integer> indexes = new ArrayList<>();
        context.getData().put(indexDataName, indexes);
        context.appendSql(open);

        for (Object o : iterable) {
            ((ArrayList<Integer>) context.getData().get(indexDataName)).add(currentIndex);
            if (currentIndex != 0) {
                context.appendSql(separator);
            }
            Context proxy = new Context(context.getData());
            String childSqlText = getChildText(proxy, currentIndex);
            context.appendSql(childSqlText);
            currentIndex++;
        }

        context.appendSql(close);
    }

    /**
     * 提取参数名：将集合名和子节点的参数名都加入集合。
     */
    @Override
    public void applyParameter(Set<String> set) {
        set.add(collection);
        contents.applyParameter(set);
    }

    /**
     * 处理单次迭代的 SQL 片段：将 item/index 变量替换为实际的集合索引访问表达式。
     *
     * @param proxy       临时上下文，用于获取子节点生成的 SQL 文本
     * @param currentIndex 当前迭代索引
     * @return 替换变量名后的 SQL 片段
     */
    public String getChildText(Context proxy, int currentIndex) {
        String newItem = String.format("%s[%d]", collection, currentIndex);
        String newIndex = String.format("%s[%d]", indexDataName, currentIndex);
        this.contents.apply(proxy);
        String sql = proxy.getSql();
        TokenParser tokenParser = new TokenParser("#{", "}", new TokenHandler() {
            @Override
            public String handleToken(String content) {
                String replace = RegexUtil.replace(content, item, newItem);
                if (replace.equals(content)) {
                    replace = RegexUtil.replace(content, index, newIndex);
                }
                return "#{" + replace + "}";
            }
        });
        return tokenParser.parse(sql);
    }
}
