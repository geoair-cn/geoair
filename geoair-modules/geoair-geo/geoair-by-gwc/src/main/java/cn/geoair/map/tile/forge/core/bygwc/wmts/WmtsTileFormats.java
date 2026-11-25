package cn.geoair.map.tile.forge.core.bygwc.wmts;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/** WMTS 图层的响应格式与对应的瓦片地址模板。 */
public final class WmtsTileFormats {
    private WmtsTileFormats() {}

    /** 按调用方提供的顺序写入 MIME 类型和模板，适用于在线或静态图层。 */
    public static void append(Document document, Element layer, Map<String, String> templates) {
        Map<String, String> formats =
                new LinkedHashMap<>(Objects.requireNonNull(templates, "瓦片格式不能为空"));
        for (Map.Entry<String, String> format : formats.entrySet()) {
            if (format.getKey() == null
                    || format.getKey().trim().isEmpty()
                    || format.getValue() == null
                    || format.getValue().trim().isEmpty())
                throw new IllegalArgumentException("瓦片 MIME 类型和地址模板不能为空");
        }
        Node before = layer.getFirstChild();
        while (before != null && !isAfterFormat(before)) before = before.getNextSibling();
        for (String mime : formats.keySet()) {
            Element element = document.createElementNS(layer.getNamespaceURI(), "Format");
            element.setTextContent(mime);
            layer.insertBefore(element, before);
        }
        for (Map.Entry<String, String> format : formats.entrySet()) {
            Element resource = document.createElementNS(layer.getNamespaceURI(), "ResourceURL");
            resource.setAttribute("format", format.getKey());
            resource.setAttribute("resourceType", "tile");
            resource.setAttribute("template", format.getValue());
            layer.appendChild(resource);
        }
    }

    private static boolean isAfterFormat(Node node) {
        String name = node.getLocalName() == null ? node.getNodeName() : node.getLocalName();
        return "InfoFormat".equals(name)
                || "Dimension".equals(name)
                || "TileMatrixSetLink".equals(name)
                || "ResourceURL".equals(name);
    }
}
