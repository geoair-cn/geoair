package cn.geoair.map.dynamic.adv.mybatis;

import cn.geoair.map.dynamic.adv.mybatis.context.Context;
import cn.geoair.map.dynamic.adv.mybatis.node.SqlNode;
import cn.geoair.map.dynamic.adv.mybatis.tag.XmlParser;
import cn.geoair.map.dynamic.adv.mybatis.token.TokenHandler;
import cn.geoair.map.dynamic.adv.mybatis.token.TokenParser;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 动态 SQL 引擎，将 MyBatis 风格的动态 SQL 模板解析为最终的 SQL 文本和 JDBC 参数。
 * <p>
 * 支持的动态 SQL 语法：
 * <ul>
 *   <li>{@code <if test="...">} — 条件判断</li>
 *   <li>{@code <foreach collection="..." ...>} — 循环展开</li>
 *   <li>{@code <where>} — WHERE 子句自动修剪</li>
 *   <li>{@code <set>} — SET 子句自动修剪</li>
 *   <li>{@code <trim prefix="..." ...>} — 自定义修剪</li>
 *   <li>{@code ${expression}} — 常量替换（直接拼接，<b>注意 SQL 注入风险</b>）</li>
 *   <li>{@code #{expression}} — 参数占位符（替换为 {@code ?}，参数值收集到列表中）</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>
 * DynamicSqlEngine engine = SqlEngineUtil.getEngine();
 * String template = "SELECT * FROM user WHERE 1=1"
 *     + "&lt;if test='name != null'&gt; AND name = #{name}&lt;/if&gt;"
 *     + "&lt;if test='age != null'&gt; AND age = #{age}&lt;/if&gt;";
 * Map&lt;String, Object&gt; params = new HashMap&lt;&gt;();
 * params.put("name", "张三");
 * SqlMeta result = engine.parse(template, params);
 * // result.getSql() → "SELECT * FROM user WHERE 1=1 AND name = ?"
 * // result.getJdbcParamValues() → ["张三"]
 * </pre>
 *
 * @author zhangjun
 * @see SqlEngineUtil
 * @see SqlMeta
 */
public class DynamicSqlEngine {

    private final Cache cache = new Cache();

    /**
     * 解析动态 SQL 模板，生成最终 SQL 和 JDBC 参数。
     *
     * @param text   SQL 模板文本（不含外层 XML 根标签，引擎会自动包裹 {@code <root>...</root>}）
     * @param params 参数上下文（变量名 → 值的映射），用于 OGNL 表达式求值
     * @return 解析结果，包含最终 SQL 和参数值列表
     */
    public SqlMeta parse(String text, Map<String, Object> params) {
        String xmlText = String.format("<root>%s</root>", text);
        SqlNode sqlNode = parseXml2SqlNode(xmlText);
        Context context = new Context(params);
        sqlNode.apply(context);
        replaceHashPlaceholders(context);
        return new SqlMeta(context.getSql(), context.getJdbcParameters());
    }

    /**
     * 静态分析 SQL 模板，提取所有引用的参数名（不执行条件判断）。
     * <p>
     * 用于提前获知模板依赖哪些参数，例如用于参数校验或元数据查询。
     *
     * @param text SQL 模板文本
     * @return 参数名集合
     */
    public Set<String> extractParameterNames(String text) {
        String xmlText = String.format("<root>%s</root>", text);
        SqlNode sqlNode = parseXml2SqlNode(xmlText);
        Set<String> set = new HashSet<>();
        sqlNode.applyParameter(set);
        return set;
    }

    /**
     * 从缓存获取或解析 XML 为 SqlNode 树。
     */
    private SqlNode parseXml2SqlNode(String text) {
        SqlNode node = cache.get(text);
        if (node == null) {
            node = XmlParser.parseXml2SqlNode(text);
            cache.put(text, node);
        }
        return node;
    }

    /**
     * 将 SQL 中的 #{expression} 替换为 ? 占位符，并将表达式求值结果收集到参数列表。
     */
    private void replaceHashPlaceholders(Context context) {
        TokenParser tokenParser = new TokenParser("#{", "}", new TokenHandler() {
            @Override
            public String handleToken(String content) {
                Object value = context.getOgnlValue(content);
                if (value == null) {
                    throw new RuntimeException("could not found value : " + content);
                }
                context.addParameter(value);
                return "?";
            }
        });
        context.setSql(tokenParser.parse(context.getSql()));
    }
}
