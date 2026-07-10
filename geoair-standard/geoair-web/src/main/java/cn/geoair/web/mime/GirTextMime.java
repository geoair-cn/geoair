
package cn.geoair.web.mime;

public class GirTextMime extends BaseMimeType implements IMimeTypeGetter{

    public static final GirTextMime txt = new GirTextMime("text/plain", "txt", "txt", "text/plain");

    public static final GirTextMime txtHtml =
            new GirTextMime("text/html", "txt.html", "html", "text/html");

    public static final GirTextMime txtMapml =
            new GirTextMime("text/mapml", "mapml", "mapml", "text/mapml");

    public static final GirTextMime txtXml = new GirTextMime("text/xml", "xml", "xml", "text/xml");

    public static final GirTextMime txtCss = new GirTextMime("text/css", "css", "css", "text/css");

    public static final GirTextMime txtJs =
            new GirTextMime("text/javascript", "js", "javascript", "text/javascript");

    private GirTextMime(
            String mimeType,
            String fileExtension,
            String internalName,
            String format) {
        super(mimeType, fileExtension, internalName, format);
    }

    @Override
    public GirTextMime checkForFormat(String formatStr) {
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
    public GirTextMime checkForExtension(String fileExtension) {
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
