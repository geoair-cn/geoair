
package cn.geoair.web.mime;

public class GirApplicationMime extends BaseMimeType {



    public static final GirApplicationMime bil16 =
            new GirApplicationMime("application/bil16", "bil16", "bil16", "application/bil16");

    public static final GirApplicationMime bil32 =
            new GirApplicationMime("application/bil32", "bil32", "bil32", "application/bil32");

    public static final GirApplicationMime json =
            new GirApplicationMime("application/json", "json", "json", "application/json");

    /**
     * 超图软件的自定义格式
     */
    public static final GirApplicationMime scp =
            new GirApplicationMime("application/json", "scp", "json", "application/json;from=supermap;type=scp");


    public static final GirApplicationMime stream =
            new GirApplicationMime("application/octet-stream", "*", "*", "application/octet-stream");

    public static final GirApplicationMime topojson =
            new GirApplicationMime(
                    "application/json",
                    "topojson",
                    "topojson",
                    "application/json;type=topojson");

    public static final GirApplicationMime geojson =
            new GirApplicationMime(
                    "application/json",
                    "geojson",
                    "geojson",
                    "application/json;type=geojson");

    public static final GirApplicationMime utfgrid =
            new GirApplicationMime(
                    "application/json",
                    "utfgrid",
                    "utfgrid",
                    "application/json;type=utfgrid");

    public static final GirApplicationMime mapboxVector =
            new GirApplicationMime(
                    "application/vnd.mapbox-vector-tile",
                    "pbf",
                    "mapbox-vectortile",
                    "application/vnd.mapbox-vector-tile");


    private GirApplicationMime(
            String mimeType,
            String fileExtension,
            String internalName,
            String format
    ) {
        super(mimeType, fileExtension, internalName, format);
    }


}
