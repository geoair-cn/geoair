package cn.geoair.map.dynamic.adv.mybatis.tag;

import cn.geoair.map.dynamic.adv.mybatis.node.MixedSqlNode;
import cn.geoair.map.dynamic.adv.mybatis.node.SqlNode;
import cn.geoair.map.dynamic.adv.mybatis.node.TextSqlNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.dom4j.*;

/**
 * XML 解析器，将动态 SQL 模板的 XML 文本解析为 {@link SqlNode} 节点树。
 *
 * <p>使用 dom4j 解析 XML，递归遍历元素树，将每个 XML 元素分派给对应的 {@link TagHandler} 处理。 纯文本内容转换为 {@link TextSqlNode}。
 *
 * <p>支持的标签（不区分大小写）：
 *
 * <ul>
 *   <li>{@code <if test="...">} — 条件判断
 *   <li>{@code <foreach collection="..." ...>} — 循环展开
 *   <li>{@code <where>} — WHERE 子句自动修剪
 *   <li>{@code <set>} — SET 子句自动修剪
 *   <li>{@code <trim prefix="..." ...>} — 自定义修剪
 * </ul>
 *
 * @author zhangjun
 */
public class XmlParser {

    private static final Map<String, TagHandler> NODE_HANDLERS =
            new HashMap<String, TagHandler>() {
                {
                    put("foreach", new ForeachHandler());
                    put("if", new IfHandler());
                    put("trim", new TrimHandler());
                    put("where", new WhereHandler());
                    put("set", new SetHandler());
                }
            };

    private XmlParser() {}

    /**
     * 将 XML 文本解析为 SqlNode 节点树。
     *
     * @param text 完整的 XML 文本（需包含根元素）
     * @return 解析后的 SqlNode 树根节点
     * @throws RuntimeException 如果 XML 格式错误或包含不支持的标签
     */
    public static SqlNode parseXml2SqlNode(String text) {
        Document document;
        try {
            document = DocumentHelper.parseText(text);
        } catch (DocumentException e) {
            throw new RuntimeException(e);
        }
        Element rootElement = document.getRootElement();
        List<SqlNode> contents = parseElement(rootElement);
        return new MixedSqlNode(contents);
    }

    /**
     * 递归解析 XML 元素的子内容，转换为 SqlNode 列表。
     *
     * <p>纯文本节点转为 {@link TextSqlNode}，XML 元素分派给对应的 {@link TagHandler}。
     *
     * @param element 待解析的 XML 元素
     * @return 解析后的 SqlNode 列表
     * @throws RuntimeException 如果遇到不支持的标签
     */
    public static List<SqlNode> parseElement(Element element) {
        List<SqlNode> nodes = new ArrayList<>();
        List<Object> children = element.content();
        for (Object node : children) {
            if (node instanceof Text) {
                nodes.add(new TextSqlNode(((Text) node).getText()));
            } else if (node instanceof Element) {
                String nodeName = ((Element) node).getName();
                TagHandler handler = NODE_HANDLERS.get(nodeName.toLowerCase());
                if (handler == null) {
                    throw new RuntimeException("tag not supported: <" + nodeName + ">");
                }
                handler.handle((Element) node, nodes);
            }
        }
        return nodes;
    }
}
