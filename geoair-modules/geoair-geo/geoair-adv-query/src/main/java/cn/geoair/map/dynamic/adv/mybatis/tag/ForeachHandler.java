package cn.geoair.map.dynamic.adv.mybatis.tag;

import cn.geoair.map.dynamic.adv.mybatis.node.ForeachSqlNode;
import cn.geoair.map.dynamic.adv.mybatis.node.MixedSqlNode;
import cn.geoair.map.dynamic.adv.mybatis.node.SqlNode;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Element;

import java.util.List;

/**
 * {@code <foreach>} 标签处理器。
 *
 * <p>从 XML 元素中提取循环属性（collection、open、close、separator、item、index）， 递归解析子元素，构建 {@link
 * ForeachSqlNode}。
 *
 * <ul>
 *   <li>{@code collection} — 必填，集合变量名
 *   <li>{@code item} — 可选，循环变量名，默认 "item"
 *   <li>{@code index} — 可选，索引变量名，默认 "index"
 * </ul>
 *
 * @author zhangjun
 */
public class ForeachHandler implements TagHandler {

    @Override
    public void handle(Element element, List<SqlNode> targetContents) {
        List<SqlNode> contents = XmlParser.parseElement(element);

        String open = element.attributeValue("open");
        String close = element.attributeValue("close");
        String collection = element.attributeValue("collection");
        String separator = element.attributeValue("separator");
        String item = element.attributeValue("item");
        String index = element.attributeValue("index");

        if (StringUtils.isBlank(collection)) {
            throw new RuntimeException("<foreach> attribute missing : collection");
        }
        if (StringUtils.isBlank(item)) {
            item = "item";
        }
        if (StringUtils.isBlank(index)) {
            index = "index";
        }

        targetContents.add(
                new ForeachSqlNode(
                        collection,
                        open,
                        close,
                        separator,
                        item,
                        index,
                        new MixedSqlNode(contents)));
    }
}
