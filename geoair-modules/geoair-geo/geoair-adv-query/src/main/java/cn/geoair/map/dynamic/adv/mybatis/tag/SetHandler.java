package cn.geoair.map.dynamic.adv.mybatis.tag;

import cn.geoair.map.dynamic.adv.mybatis.node.MixedSqlNode;
import cn.geoair.map.dynamic.adv.mybatis.node.SetSqlNode;
import cn.geoair.map.dynamic.adv.mybatis.node.SqlNode;
import java.util.List;
import org.dom4j.Element;

public class SetHandler implements TagHandler {

    @Override
    public void handle(Element element, List<SqlNode> targetContents) {
        List<SqlNode> contents = XmlParser.parseElement(element);

        SetSqlNode node = new SetSqlNode(new MixedSqlNode(contents));
        targetContents.add(node);
    }
}
