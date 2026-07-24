package cn.geoair.web.mime.getter;

import static cn.geoair.web.mime.GirTextMime.*;

import cn.geoair.web.mime.GiMimeType;
import cn.geoair.web.mime.IMimeTypeGetter;

public class TextMimeGetter implements IMimeTypeGetter {

    @Override
    public GiMimeType checkForFormat(String formatStr) {
        if (formatStr.toLowerCase().startsWith("text")) {
            if (formatStr.equalsIgnoreCase("text/plain")) {
                return txt;
            } else if (formatStr.startsWith("text/html")) {
                return txtHtml;
            } else if (formatStr.startsWith("text/mapml")) {
                return txtMapml;
            } else if (formatStr.startsWith("text/xml")) {
                return txtXml;
            } else if (formatStr.startsWith("text/css")) {
                return txtCss;
            } else if (formatStr.startsWith("text/javscript")) {
                return txtJs;
            }
        }

        return null;
    }

    @Override
    public GiMimeType checkForExtension(String fileExtension) {
        if (fileExtension.equalsIgnoreCase("txt")) {
            return txt;
        } else if (fileExtension.equalsIgnoreCase("txt.html")) {
            return txtHtml;
        } else if (fileExtension.equalsIgnoreCase("html")) {
            return txtHtml;
        } else if (fileExtension.equalsIgnoreCase("mapml")) {
            return txtMapml;
        } else if (fileExtension.equalsIgnoreCase("xml")) {
            return txtXml;
        } else if (fileExtension.equalsIgnoreCase("css")) {
            return txtCss;
        } else if (fileExtension.equalsIgnoreCase("js")) {
            return txtJs;
        }

        return null;
    }
}
