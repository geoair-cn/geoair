package cn.geoair.map.dynamic.adv.mybatis.tag;

import java.util.List;

import org.dom4j.Element;

import cn.geoair.map.dynamic.adv.mybatis.node.SqlNode;

public interface TagHandler {

	void handle(Element element, List<SqlNode> contents);

}
