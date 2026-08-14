package cn.geoair.map.dynamic.adv.mybatis.tag;

import cn.geoair.map.dynamic.adv.mybatis.node.MixedSqlNode;
import cn.geoair.map.dynamic.adv.mybatis.node.SqlNode;
import cn.geoair.map.dynamic.adv.mybatis.node.WhereSqlNode;
import java.util.List;
import org.dom4j.Element;

/**
 * {@code <where>} 标签处理器。
 * <p>
 * 递归解析子元素，构建 {@link WhereSqlNode}。
 * 自动去除首部多余的 AND/OR，内容非空时添加 "WHERE " 前缀。
 *
 * @author zhangjun
 */
public class WhereHandler implements TagHandler {

    @Override
    public void handle(Element element, List<SqlNode> targetContents) {
        List<SqlNode> contents = XmlParser.parseElement(element);
        targetContents.add(new WhereSqlNode(new MixedSqlNode(contents)));
    }
}
