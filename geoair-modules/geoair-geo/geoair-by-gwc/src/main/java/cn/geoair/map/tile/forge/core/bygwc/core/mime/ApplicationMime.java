
package cn.geoair.map.tile.forge.core.bygwc.core.mime;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ApplicationMime extends MimeType {

    public static final String MAPBOX_TILES_LEGACY_MIME =
            "application/x-protobuf;type=mapbox-vector";

    protected boolean vector;

    public static final ApplicationMime bil16 =
            new ApplicationMime("application/bil16", "bil16", "bil16", "application/bil16", false);

    public static final ApplicationMime bil32 =
            new ApplicationMime("application/bil32", "bil32", "bil32", "application/bil32", false);

    public static final ApplicationMime json =
            new ApplicationMime("application/json", "json", "json", "application/json", false);

    /**
     * 超图软件的自定义格式
     */
    public static final ApplicationMime scp =
            new ApplicationMime("application/json", "scp", "json", "application/json;from=supermap;type=scp", false);


    public static final ApplicationMime stream =
            new ApplicationMime("application/octet-stream", "*", "*", "application/octet-stream", false);

    public static final ApplicationMime topojson =
            new ApplicationMime(
                    "application/json",
                    "topojson",
                    "topojson",
                    "application/json;type=topojson",
                    true);

    public static final ApplicationMime geojson =
            new ApplicationMime(
                    "application/json",
                    "geojson",
                    "geojson",
                    "application/json;type=geojson",
                    true);

    public static final ApplicationMime utfgrid =
            new ApplicationMime(
                    "application/json",
                    "utfgrid",
                    "utfgrid",
                    "application/json;type=utfgrid",
                    true);

    public static final ApplicationMime mapboxVector =
            new ApplicationMime(
                    "application/vnd.mapbox-vector-tile",
                    "pbf",
                    "mapbox-vectortile",
                    "application/vnd.mapbox-vector-tile",
                    true);

    static Set<ApplicationMime> ALL =
            Collections.unmodifiableSet(
                    new HashSet<>(Arrays.asList(bil16, bil32, json, topojson, geojson, utfgrid, mapboxVector, scp))
            );


    private static Map<String, ApplicationMime> BY_FORMAT =
            ALL.stream().collect(Collectors.toMap(
                    ApplicationMime::getFormat,  // key 映射
                    Function.identity()          // value 就是元素本身
            ));

    private static Map<String, ApplicationMime> BY_EXTENSION =
            ALL.stream().collect(Collectors.toMap(
                    ApplicationMime::getFileExtension,  // key 映射
                    Function.identity()          // value 就是元素本身
            ));

    private ApplicationMime(
            String mimeType,
            String fileExtension,
            String internalName,
            String format,
            boolean vector) {
        super(mimeType, fileExtension, internalName, format, false);

        this.vector = vector;
    }

    public ApplicationMime(
            String mimeType, String fileExtension, String internalName, String format)
            throws MimeException {
        super(mimeType, fileExtension, internalName, format, false);
    }

    protected static ApplicationMime checkForFormat(String formatStr) throws MimeException {
        ApplicationMime mimeType = BY_FORMAT.get(formatStr);
        if (mimeType == null && formatStr.equals(MAPBOX_TILES_LEGACY_MIME)) {
            return mapboxVector;
        }
        return mimeType;
    }

    protected static ApplicationMime checkForExtension(String fileExtension) throws MimeException {
        ApplicationMime mimeType = BY_EXTENSION.get(fileExtension);
        return mimeType;
    }

    @Override
    public boolean isVector() {
        return vector;
    }


    public static void main(String[] args) {
        ApplicationMime scp1 = checkForExtension("scp");
        System.out.println(scp1);
    }
}
