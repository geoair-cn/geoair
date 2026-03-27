package cn.geoair.map.dynamic.adv.mybatis.tag;

import cn.geoair.map.dynamic.adv.mybatis.node.SqlNode;
import java.util.List;
import org.dom4j.Element;

public interface TagHandler {

    void handle(Element element, List<SqlNode> contents);
}
