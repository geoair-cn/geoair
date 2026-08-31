package cn.geoair.map.dynamic.adv.mybatis.tag;

import cn.geoair.map.dynamic.adv.mybatis.node.SqlNode;
import java.util.List;
import org.dom4j.Element;

/**
 * XML 标签处理器接口，负责将 dom4j 的 {@link Element} 转换为对应的 {@link SqlNode}。
 *
 * <p>每个支持的动态 SQL 标签（if、foreach、where、set、trim）都有一个对应的处理器实现。 处理器解析标签属性、递归处理子元素，最终构建出 SqlNode 节点。
 *
 * @see IfHandler
 * @see ForeachHandler
 * @see WhereHandler
 * @see SetHandler
 * @see TrimHandler
 * @author zhangjun
 */
public interface TagHandler {

    /**
     * 处理 XML 元素，将解析结果添加到目标列表中。
     *
     * @param element 待处理的 XML 元素
     * @param targetContents 解析结果将添加到此列表
     */
    void handle(Element element, List<SqlNode> targetContents);
}
