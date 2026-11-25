package cn.geoair.map.tile.forge.fuser.encoding;

import static org.junit.Assert.*;

import cn.geoair.map.tile.forge.core.bygwc.wmts.WmtsTileFormats;
import cn.geoair.map.tile.forge.fuser.utils.TileImageUtils;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class TileResponseFormatsTest {
    @Test
    public void jpegResponseCanFlattenTransparencyWhileCacheStillProtectsIt() throws Exception {
        BufferedImage transparent = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        byte[] png = TileImageUtils.writeImage(transparent, "png");
        transparent.flush();
        assertSame(png, TileImageCodec.encodeForResponse(png, "png", 95, Color.WHITE));
        assertThrows(IOException.class, () -> TileImageCodec.normalize(png, "jpg"));
        byte[] jpeg = TileImageCodec.encodeForResponse(png, "jpg", 95, Color.WHITE);
        assertEquals("jpeg", TileImageCodec.detect(jpeg));
        BufferedImage flattened = TileImageUtils.readImage(jpeg);
        try {
            assertEquals(Color.WHITE.getRGB(), flattened.getRGB(16, 16));
        } finally {
            flattened.flush();
        }
        assertSame(jpeg, TileImageCodec.encodeForResponse(jpeg, "jpeg", 95, Color.WHITE));
    }

    @Test
    public void wmtsFormatsMatchTheirOwnResourceTemplates() throws Exception {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element layer = doc.createElementNS("http://www.opengis.net/wmts/1.0", "Layer");
        layer.appendChild(doc.createElement("TileMatrixSetLink"));
        Map<String, String> formats = new LinkedHashMap<>();
        formats.put("image/png", "/{TileMatrix}/{TileCol}/{TileRow}.png");
        formats.put("image/jpeg", "/{TileMatrix}/{TileCol}/{TileRow}.jpg");
        WmtsTileFormats.append(doc, layer, formats);
        assertEquals("Format", layer.getFirstChild().getNodeName());
        Element png = (Element) layer.getElementsByTagName("ResourceURL").item(0);
        Element jpg = (Element) layer.getElementsByTagName("ResourceURL").item(1);
        assertEquals("image/png", png.getAttribute("format"));
        assertTrue(png.getAttribute("template").endsWith(".png"));
        assertEquals("image/jpeg", jpg.getAttribute("format"));
        assertTrue(jpg.getAttribute("template").endsWith(".jpg"));
    }
}
