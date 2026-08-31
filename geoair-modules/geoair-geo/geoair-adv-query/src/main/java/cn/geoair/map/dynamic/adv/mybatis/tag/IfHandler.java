package cn.geoair.map.dynamic.adv.mybatis.tag;

import cn.geoair.map.dynamic.adv.mybatis.node.IfSqlNode;
import cn.geoair.map.dynamic.adv.mybatis.node.MixedSqlNode;
import cn.geoair.map.dynamic.adv.mybatis.node.SqlNode;

import org.dom4j.Element;

import java.util.List;

/**
 * {@code <if>} 标签处理器。
 *
 * <p>从 XML 元素中提取 {@code test} 属性，递归解析子元素，构建 {@link IfSqlNode}。
 *
 * @author zhangjun
 */
public class IfHandler implements TagHandler {

    @Override
    public void handle(Element element, List<SqlNode> targetContents) {
        String test = element.attributeValue("test");
        if (test == null) {
            throw new RuntimeException("<if> tag missing test attribute");
        }
        List<SqlNode> contents = XmlParser.parseElement(element);
        targetContents.add(new IfSqlNode(test, new MixedSqlNode(contents)));
    }
}
