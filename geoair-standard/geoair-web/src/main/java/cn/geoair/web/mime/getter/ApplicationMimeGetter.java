package cn.geoair.web.mime.getter;

import static cn.geoair.web.mime.GirApplicationMime.*;

import cn.geoair.web.mime.GiMimeType;
import cn.geoair.web.mime.GirApplicationMime;
import cn.geoair.web.mime.IMimeTypeGetter;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ApplicationMimeGetter implements IMimeTypeGetter {
    public static final String MAPBOX_TILES_LEGACY_MIME =
            "application/x-protobuf;type=mapbox-vector";
    static Set<GirApplicationMime> ALL =
            Collections.unmodifiableSet(
                    new HashSet<>(
                            Arrays.asList(
                                    bil16,
                                    bil32,
                                    json,
                                    topojson,
                                    geojson,
                                    utfgrid,
                                    mapboxVector,
                                    scp)));

    protected static Map<String, GirApplicationMime> BY_FORMAT =
            ALL.stream()
                    .collect(
                            Collectors.toMap(
                                    GirApplicationMime::getFormat, // key 映射
                                    Function.identity() // value 就是元素本身
                                    ));

    protected static Map<String, GirApplicationMime> BY_EXTENSION =
            ALL.stream()
                    .collect(
                            Collectors.toMap(
                                    GirApplicationMime::getFileExtension, // key 映射
                                    Function.identity() // value 就是元素本身
                                    ));

    @Override
    public GiMimeType checkForFormat(String formatStr) {
        GiMimeType mimeType = BY_FORMAT.get(formatStr);
        if (mimeType == null && formatStr.equals(MAPBOX_TILES_LEGACY_MIME)) {
            return mapboxVector;
        }
        return mimeType;
    }

    @Override
    public GiMimeType checkForExtension(String fileExtension) {
        GiMimeType mimeType = BY_EXTENSION.get(fileExtension);
        return mimeType;
    }
}
