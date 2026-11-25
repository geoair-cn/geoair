package cn.geoair.map.dynamic.adv.mybatis.tag;

import cn.geoair.map.dynamic.adv.mybatis.node.MixedSqlNode;
import cn.geoair.map.dynamic.adv.mybatis.node.SqlNode;
import cn.geoair.map.dynamic.adv.mybatis.node.TrimSqlNode;

import org.dom4j.Element;

import java.util.Arrays;
import java.util.List;

/**
 * {@code <trim>} 标签处理器。
 *
 * <p>从 XML 元素中提取修剪属性（prefix、suffix、prefixesToOverride、suffixesToOverride）， 递归解析子元素，构建 {@link
 * TrimSqlNode}。
 *
 * <p>{@code prefixesToOverride} 和 {@code suffixesToOverride} 使用 {@code |} 分隔多个值， 例如 {@code
 * prefixesToOverride="AND |OR "}。
 *
 * @author zhangjun
 */
public class TrimHandler implements TagHandler {

    @Override
    public void handle(Element element, List<SqlNode> targetContents) {
        String prefix = element.attributeValue("prefix");
        String suffix = element.attributeValue("suffix");

        String prefixesToOverride = element.attributeValue("prefixesToOverride");
        List<String> prefixesOverride =
                prefixesToOverride == null ? null : Arrays.asList(prefixesToOverride.split("\\|"));

        String suffixesToOverride = element.attributeValue("suffixesToOverride");
        List<String> suffixesOverride =
                suffixesToOverride == null ? null : Arrays.asList(suffixesToOverride.split("\\|"));

        List<SqlNode> contents = XmlParser.parseElement(element);
        targetContents.add(
                new TrimSqlNode(
                        new MixedSqlNode(contents),
                        prefix,
                        suffix,
                        prefixesOverride,
                        suffixesOverride));
    }
}
