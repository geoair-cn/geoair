package cn.geoair.map.dynamic.adv.mybatis.tag;

import java.util.List;

import org.dom4j.Element;

import cn.geoair.map.dynamic.adv.mybatis.node.IfSqlNode;
import cn.geoair.map.dynamic.adv.mybatis.node.MixedSqlNode;
import cn.geoair.map.dynamic.adv.mybatis.node.SqlNode;

public class IfHandler implements TagHandler {

	@Override
	public void handle(Element element, List<SqlNode> targetContents) {
		String test = element.attributeValue("test");
		if (test == null) {
			throw new RuntimeException("<if> tag missing test attribute");
		}

		List<SqlNode> contents = XmlParser.parseElement(element);

		IfSqlNode ifSqlNode = new IfSqlNode(test, new MixedSqlNode(contents));
		targetContents.add(ifSqlNode);

	}

}
