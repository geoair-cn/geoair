package cn.geoair.web.mime;

public class GirXMLMime extends BaseMimeType {

    public static final GirXMLMime xml =
            new GirXMLMime("application/xml", "xml", "xml", "application/xml");

    public static final GirXMLMime ogcxml =
            new GirXMLMime(
                    "application/vnd.ogc.se_xml",
                    "ogc-xml",
                    "ogc-xml",
                    "application/vnd.ogc.se_xml");

    public static final GirXMLMime kml =
            new GirXMLMime(
                    "application/vnd.google-earth.kml+xml",
                    "kml",
                    "kml",
                    "application/vnd.google-earth.kml+xml");

    public static final GirXMLMime kmz =
            new GirXMLMime(
                    "application/vnd.google-earth.kmz",
                    "kmz",
                    "kmz",
                    "application/vnd.google-earth.kmz");

    public static final GirXMLMime gml =
            new GirXMLMime("application/vnd.ogc.gml", "gml", "gml", "application/vnd.ogc.gml");

    public static final GirXMLMime gml3 =
            new GirXMLMime(
                    "application/vnd.ogc.gml/3.1.1",
                    "gml3",
                    "gml3",
                    "application/vnd.ogc.gml/3.1.1");

    private GirXMLMime(String mimeType, String fileExtension, String internalName, String format) {
        super(mimeType, fileExtension, internalName, format);
    }
}
