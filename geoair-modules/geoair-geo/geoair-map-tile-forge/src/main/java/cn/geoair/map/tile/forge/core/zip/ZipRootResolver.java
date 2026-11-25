package cn.geoair.map.tile.forge.core.zip;

import cn.geoair.map.tile.forge.core.enums.GirMapTileType;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.zip.model.RootPathInfo;
import java.io.IOException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 各格式共用一次中央目录扫描，只保留候选根路径，不收集全部条目。 */
public final class ZipRootResolver {
    private static final Pattern XYZ = Pattern.compile("^(.*?)([0-9]+/[0-9]+/[0-9]+\\.[^/]+)$");
    private static final Pattern COMPACT =
            Pattern.compile(
                    "^(.*?)(L[0-9]+/R[0-9a-f]+C[0-9a-f]+\\.bundle)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern LOOSE =
            Pattern.compile(
                    "^(.*?)(L[0-9]+/R[0-9a-f]+/C[0-9a-f]+\\.[^/]+)$", Pattern.CASE_INSENSITIVE);

    private ZipRootResolver() {}

    public static RootPathInfo resolve(GirLayerConfigContext context, ICompressionHandler reader)
            throws IOException {
        GirMapTileType type = context.getMapTileType();
        String prefix = ZipPaths.prefix(context.getTilePathPrefix());
        String standard = metadata(type);
        String[] best = {null, null, null}; // 前缀内标准元数据、祖先标准元数据、旧名称/瓦片目录。
        boolean[] ambiguous = {false, false, false};
        reader.scanAllEntries(
                context.getObjectKey(),
                (entry, total, current) -> {
                    if (entry.isDirectoryIs()) return true;
                    String name = entry.getName();
                    if (!ZipPaths.safe(name))
                        throw new ZipReadException(
                                ZipReadFailure.INVALID_PATH, "ZIP包含非法资源路径", null);
                    String directory = parent(name);
                    String leaf = name.substring(directory.length());
                    boolean named = standard != null && leaf.equalsIgnoreCase(standard);
                    if (named && name.startsWith(prefix)) consider(best, ambiguous, 0, name, true);
                    else if (named
                            && !prefix.isEmpty()
                            && prefix.startsWith(directory)
                            && (best[1] == null || directory.length() > parent(best[1]).length()))
                        best[1] = name;
                    if (name.startsWith(prefix)) {
                        if (type == GirMapTileType.TILE_3D || type == GirMapTileType.S3M) {
                            String suffix = type == GirMapTileType.S3M ? ".scp" : ".json";
                            if (name.toLowerCase(Locale.ROOT).endsWith(suffix))
                                consider(best, ambiguous, 2, name, false);
                        } else {
                            Pattern pattern =
                                    type == GirMapTileType.XYZ
                                            ? XYZ
                                            : type == GirMapTileType.LOOSE
                                                    ? LOOSE
                                                    : type == GirMapTileType.COMPACT_V1
                                                                    || type
                                                                            == GirMapTileType
                                                                                    .COMPACT_V2
                                                            ? COMPACT
                                                            : null;
                            if (pattern != null) {
                                Matcher match = pattern.matcher(name);
                                if (match.matches())
                                    consider(best, ambiguous, 2, match.group(1), true);
                            }
                        }
                    }
                    // 精确命中指定根文件后无需检查后续海量条目。
                    return !(named && directory.equals(prefix));
                });
        String selected = best[0] != null ? best[0] : best[1] != null ? best[1] : best[2];
        if (selected == null)
            throw new ZipReadException(
                    ZipReadFailure.ROOT_NOT_FOUND, "ZIP中未找到所选格式的数据根目录，请检查目录前缀", null);
        int category = best[0] != null ? 0 : best[1] != null ? 1 : 2;
        if (ambiguous[category])
            throw new ZipReadException(ZipReadFailure.ROOT_NOT_FOUND, "ZIP包含多个同级数据根，请指定目录前缀", null);
        if (standard == null) return RootPathInfo.of().setRootPath(selected);
        String directory = parent(selected);
        return RootPathInfo.of()
                .setRootPath(directory)
                .setRootFilePath(selected)
                .setRootFileName(selected.substring(directory.length()))
                .setRootFileStandardName(standard);
    }

    private static String metadata(GirMapTileType type) {
        if (type == GirMapTileType.TILE_3D) return "tileset.json";
        if (type == GirMapTileType.S3M) return "tilesetS3MB.scp";
        if (type == GirMapTileType.TERRAIN_3D) return "layer.json";
        if (type == GirMapTileType.MVT_TILES) return "style.json";
        return null;
    }

    private static String parent(String value) {
        return value.substring(0, value.lastIndexOf('/') + 1);
    }

    private static void consider(
            String[] best, boolean[] ambiguous, int index, String candidate, boolean strict) {
        String old = best[index];
        if (old == null) {
            best[index] = candidate;
            return;
        }
        int a = old.split("/", -1).length, b = candidate.split("/", -1).length;
        if (b < a) {
            best[index] = candidate;
            ambiguous[index] = false;
        } else if (b == a && !old.equals(candidate)) {
            if (strict) ambiguous[index] = true;
            if (candidate.compareTo(old) < 0) best[index] = candidate;
        }
    }
}
