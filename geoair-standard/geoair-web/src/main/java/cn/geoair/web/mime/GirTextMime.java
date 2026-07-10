
package cn.geoair.web.mime;

public class GirTextMime extends BaseMimeType {

    public static final GirTextMime txt = new GirTextMime("text/plain", "txt", "txt", "text/plain");

    public static final GirTextMime txtHtml = new GirTextMime("text/html", "txt.html", "html", "text/html");

    public static final GirTextMime txtMapml = new GirTextMime("text/mapml", "mapml", "mapml", "text/mapml");

    public static final GirTextMime txtXml = new GirTextMime("text/xml", "xml", "xml", "text/xml");

    public static final GirTextMime txtCss = new GirTextMime("text/css", "css", "css", "text/css");

    public static final GirTextMime txtJs = new GirTextMime("text/javascript", "js", "javascript", "text/javascript");

    private GirTextMime(
            String mimeType,
            String fileExtension,
            String internalName,
            String format) {
        super(mimeType, fileExtension, internalName, format);
    }

}
