package cn.geoair.map.dynamic.adv.mybatis.tag;

import cn.geoair.map.dynamic.adv.mybatis.node.MixedSqlNode;
import cn.geoair.map.dynamic.adv.mybatis.node.SetSqlNode;
import cn.geoair.map.dynamic.adv.mybatis.node.SqlNode;
import java.util.List;
import org.dom4j.Element;

/**
 * {@code <set>} 标签处理器。
 *
 * <p>递归解析子元素，构建 {@link SetSqlNode}。 自动去除尾部多余的逗号，内容非空时添加 "SET " 前缀。
 *
 * @author zhangjun
 */
public class SetHandler implements TagHandler {

    @Override
    public void handle(Element element, List<SqlNode> targetContents) {
        List<SqlNode> contents = XmlParser.parseElement(element);
        targetContents.add(new SetSqlNode(new MixedSqlNode(contents)));
    }
}
