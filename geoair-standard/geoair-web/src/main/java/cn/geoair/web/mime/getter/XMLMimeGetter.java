package cn.geoair.web.mime.getter;

import static cn.geoair.web.mime.GirXMLMime.*;

import cn.geoair.web.mime.GiMimeType;
import cn.geoair.web.mime.IMimeTypeGetter;

public class XMLMimeGetter implements IMimeTypeGetter {

    @Override
    public GiMimeType checkForFormat(String formatStr) {
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

    @Override
    public GiMimeType checkForExtension(String fileExtension) {
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
