package cn.geoair.map.tile.forge.core.bygwc.core.mime;

import cn.geoair.web.mime.GirXMLMime;
import cn.geoair.web.mime.MimeException;

public class XMLMime extends MimeType {

    public static final XMLMime ogcxml = new XMLMime(GirXMLMime.ogcxml, false);
    public static final XMLMime kml = new XMLMime(GirXMLMime.kml, false);
    public static final XMLMime kmz = new XMLMime(GirXMLMime.kmz, false);
    public static final XMLMime gml = new XMLMime(GirXMLMime.gml, false);
    public static final XMLMime gml3 = new XMLMime(GirXMLMime.gml3, false);

    private XMLMime(GirXMLMime xmlMime, boolean noop) {
        super(
                xmlMime.getMimeType(),
                xmlMime.getFileExtension(),
                xmlMime.getInternalName(),
                xmlMime.getFormat(),
                false);
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
