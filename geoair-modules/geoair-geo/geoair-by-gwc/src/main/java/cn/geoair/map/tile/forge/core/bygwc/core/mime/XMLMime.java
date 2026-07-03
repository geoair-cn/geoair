
package cn.geoair.map.tile.forge.core.bygwc.core.mime;

public class XMLMime extends MimeType {

    public static final XMLMime ogcxml =
            new XMLMime(
                    "application/vnd.ogc.se_xml",
                    "ogc-xml",
                    "ogc-xml",
                    "application/vnd.ogc.se_xml",
                    false);

    public static final XMLMime kml =
            new XMLMime(
                    "application/vnd.google-earth.kml+xml",
                    "kml",
                    "kml",
                    "application/vnd.google-earth.kml+xml",
                    false);

    public static final XMLMime kmz =
            new XMLMime(
                    "application/vnd.google-earth.kmz",
                    "kmz",
                    "kmz",
                    "application/vnd.google-earth.kmz",
                    false);

    public static final XMLMime gml =
            new XMLMime("application/vnd.ogc.gml", "gml", "gml", "application/vnd.ogc.gml", false);

    public static final XMLMime gml3 =
            new XMLMime(
                    "application/vnd.ogc.gml/3.1.1",
                    "gml3",
                    "gml3",
                    "application/vnd.ogc.gml/3.1.1",
                    false);

    private XMLMime(
            String mimeType,
            String fileExtension,
            String internalName,
            String format,
            boolean noop) {
        super(mimeType, fileExtension, internalName, format, false);
    }

    protected static XMLMime checkForFormat(String formatStr) throws MimeException {
        if (formatStr.equalsIgnoreCase("application/vnd.google-earth.kml+xml")) {
            return kml;
        } else if (formatStr.equalsIgnoreCase("application/vnd.google-earth.kmz")) {
            return kmz;
        } else if (formatStr.equalsIgnoreCase("application/vnd.ogc.se_xml")) {
            return ogcxml;
        } else if (formatStr.equalsIgnoreCase("application/vnd.ogc.gml")) {
            return gml;
        } else if (formatStr.equalsIgnoreCase("application/vnd.ogc.gml/3.1.1")) {
            return gml3;
        }

        return null;
    }

    protected static XMLMime checkForExtension(String fileExtension) throws MimeException {
        if (fileExtension.equalsIgnoreCase("kml")) {
            return kml;
        } else if (fileExtension.equalsIgnoreCase("kmz")) {
            return kmz;
        } else if (fileExtension.equalsIgnoreCase("gml")) {
            return gml;
        } else if (fileExtension.equalsIgnoreCase("gml3")) {
            return gml3;
        }

        return null;
    }
}
