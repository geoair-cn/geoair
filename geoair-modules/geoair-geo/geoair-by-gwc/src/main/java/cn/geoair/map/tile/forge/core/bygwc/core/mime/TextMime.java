package cn.geoair.map.tile.forge.core.bygwc.core.mime;

import cn.geoair.web.mime.GirTextMime;
import cn.geoair.web.mime.MimeException;

public class TextMime extends MimeType {

    public static final TextMime txt = new TextMime(GirTextMime.txt, true);
    public static final TextMime txtHtml = new TextMime(GirTextMime.txtHtml, true);
    public static final TextMime txtMapml = new TextMime(GirTextMime.txtMapml, true);
    public static final TextMime txtXml = new TextMime(GirTextMime.txtXml, true);
    public static final TextMime txtCss = new TextMime(GirTextMime.txtCss, true);
    public static final TextMime txtJs = new TextMime(GirTextMime.txtJs, true);

    private TextMime(GirTextMime girTextMime, boolean noop) {
        super(
                girTextMime.getMimeType(),
                girTextMime.getFileExtension(),
                girTextMime.getInternalName(),
                girTextMime.getFormat(),
                false);
    }

    protected static TextMime checkForFormat(String formatStr) throws MimeException {
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

    protected static TextMime checkForExtension(String fileExtension) throws MimeException {
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
