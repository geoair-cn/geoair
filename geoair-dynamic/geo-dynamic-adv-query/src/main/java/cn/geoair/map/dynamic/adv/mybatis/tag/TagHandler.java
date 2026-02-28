package cn.geoair.map.dynamic.adv.mybatis.tag;

import cn.geoair.map.dynamic.adv.mybatis.node.SqlNode;
import org.dom4j.Element;

import java.util.List;

public interface TagHandler {

	void handle(Element element, List<SqlNode> contents);

}
