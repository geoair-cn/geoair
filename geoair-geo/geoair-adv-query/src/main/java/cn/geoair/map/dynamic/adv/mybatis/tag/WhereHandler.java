package cn.geoair.map.dynamic.adv.mybatis.tag;

import cn.geoair.map.dynamic.adv.mybatis.node.MixedSqlNode;
import cn.geoair.map.dynamic.adv.mybatis.node.SqlNode;
import cn.geoair.map.dynamic.adv.mybatis.node.WhereSqlNode;
import org.dom4j.Element;

import java.util.List;

public class WhereHandler implements TagHandler {

	@Override
	public void handle(Element element, List<SqlNode> targetContents) {
		List<SqlNode> contents = XmlParser.parseElement(element);

		WhereSqlNode node = new WhereSqlNode(new MixedSqlNode(contents));
		targetContents.add(node);
	}

}
