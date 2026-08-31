package cn.geoair.map.tile.forge.core.xyz.storage;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.tile.forge.core.zip.ICompressionHandler;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/** ZIP瓦片存储访问器 */
public class ZipTileStorageAccessor extends AbstractTileStorageAccessor {
    public static GiLogger log = GirLoggerFactory.getLogger();
    private final String zipSource;
    private final String rootPrefix;
    private final ICompressionHandler compressionHandler;
    private final Set<String> supportedFormats = new HashSet<>(Arrays.asList("png", "jpg", "jpeg"));

    /** 构造函数 */
    public ZipTileStorageAccessor(
            String zipSource, String rootPrefix, ICompressionHandler compressionHandler) {
        this.zipSource = zipSource;
        this.rootPrefix = normalizePrefix(rootPrefix);
        this.compressionHandler = compressionHandler;
    }

    /** 标准化前缀 */
    private String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return "";
        }
        String normalized = prefix.replace('\\', '/');
        return normalized.endsWith("/") ? normalized : normalized + "/";
    }

    /** 构建完整路径 */
    private String buildPath(String... parts) {
        return rootPrefix + String.join("/", parts);
    }

    @Override
    protected boolean zLevelExists(String basePath, int z) {
        String zPath = buildPath(String.valueOf(z), "");
        try {
            return compressionHandler.findEntryInCentralDir(
                            compressionHandler.parseEocd(
                                    compressionHandler.getFileSize(zipSource), zipSource),
                            zPath,
                            zipSource)
                    != null;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    protected boolean xLevelExists(String basePath, int z, int x) {
        String xPath = buildPath(String.valueOf(z), String.valueOf(x), "");
        try {
            return compressionHandler.findEntryInCentralDir(
                            compressionHandler.parseEocd(
                                    compressionHandler.getFileSize(zipSource), zipSource),
                            xPath,
                            zipSource)
                    != null;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    protected boolean yTileExists(String basePath, int z, int x, int y, String format) {
        try {
            String path = buildPath(String.valueOf(z), String.valueOf(x), y + "." + format);
            return compressionHandler.readFileFromZip(zipSource, path) != null;
        } catch (IOException e) {
            // 尝试其他格式
            for (String altFormat : supportedFormats) {
                if (!altFormat.equals(format)) {
                    try {
                        String altPath =
                                buildPath(
                                        String.valueOf(z), String.valueOf(x), y + "." + altFormat);
                        return compressionHandler.readFileFromZip(zipSource, altPath) != null;
                    } catch (IOException ex) {
                        continue;
                    }
                }
            }
            return false;
        }
    }

    /** 批量检查Z层级是否存在 */
    public Set<Integer> batchCheckZLevels(String basePath, List<Integer> zLevelsToCheck) {
        Map<Integer, String> pathsToCheck =
                zLevelsToCheck.stream()
                        .collect(Collectors.toMap(z -> z, z -> buildPath(String.valueOf(z), "")));

        try {
            List<String> existingPaths =
                    compressionHandler.checkedPathsInZip(
                            zipSource,
                            pathsToCheck.entrySet().stream()
                                    .map(entry -> entry.getValue())
                                    .collect(Collectors.toList()));

            return pathsToCheck.entrySet().stream()
                    .filter(entry -> existingPaths.contains(entry.getValue()))
                    .map(entry -> entry.getKey())
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            log.error("批量检查Z层级失败", e);
            return Collections.emptySet();
        }
    }

    /** 批量检查X层级是否存在 */
    @Override
    public Set<Integer> batchCheckXLevels(String basePath, int z, List<Integer> xLevelsToCheck) {
        Map<Integer, String> pathsToCheck =
                xLevelsToCheck.stream()
                        .collect(
                                Collectors.toMap(
                                        x -> x,
                                        x -> buildPath(String.valueOf(z), String.valueOf(x), "")));

        try {
            List<String> existingPaths =
                    compressionHandler.checkedPathsInZip(
                            zipSource,
                            pathsToCheck.entrySet().stream()
                                    .map(entry -> entry.getValue())
                                    .collect(Collectors.toList()));

            return pathsToCheck.entrySet().stream()
                    .filter(entry -> existingPaths.contains(entry.getValue()))
                    .map(entry -> entry.getKey())
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            log.error("批量检查X层级失败", e);
            return Collections.emptySet();
        }
    }

    /** 批量检查Y瓦片是否存在 */
    @Override
    public Set<Integer> batchCheckYLevels(
            String basePath, int z, int x, String format, List<Integer> yLevelsToCheck) {
        Map<Integer, String> pathsToCheck =
                yLevelsToCheck.stream()
                        .collect(
                                Collectors.toMap(
                                        y -> y,
                                        y ->
                                                buildPath(
                                                        String.valueOf(z),
                                                        String.valueOf(x),
                                                        y + "." + format)));
        try {
            List<String> existingPaths =
                    compressionHandler.checkedPathsInZip(
                            zipSource,
                            pathsToCheck.entrySet().stream()
                                    .map(entry -> entry.getValue())
                                    .collect(Collectors.toList()));
            return pathsToCheck.entrySet().stream()
                    .filter(entry -> existingPaths.contains(entry.getValue()))
                    .map(entry -> entry.getKey())
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            log.error("批量检查Y层级失败", e);
            return Collections.emptySet();
        }
    }

    /** 获取瓦片数据 */
    public byte[] getTileBytes(int z, int x, int y, String format) {
        try {
            String path = buildPath(String.valueOf(z), String.valueOf(x), y + "." + format);
            return compressionHandler.readFileFromZip(zipSource, path);
        } catch (IOException e) {
            // 尝试其他格式
            for (String altFormat : supportedFormats) {
                if (!altFormat.equals(format)) {
                    try {
                        String altPath =
                                buildPath(
                                        String.valueOf(z), String.valueOf(x), y + "." + altFormat);
                        return compressionHandler.readFileFromZip(zipSource, altPath);
                    } catch (IOException ex) {
                        continue;
                    }
                }
            }
            log.warn("瓦片不存在：{}/{}/{}.{}", z, x, y, format);
            return null;
        }
    }

    /** 批量获取多个瓦片数据 */
    public Map<String, byte[]> batchGetTileBytes(int z, int x, List<Integer> ys, String format) {
        Map<String, byte[]> result = new HashMap<>();

        // 构建所有可能的路径
        List<String> pathsToCheck = new ArrayList<>();
        for (int y : ys) {
            for (String fmt : supportedFormats) {
                pathsToCheck.add(buildPath(String.valueOf(z), String.valueOf(x), y + "." + fmt));
            }
        }

        try {
            // 先检查哪些瓦片存在
            List<String> existingPaths =
                    compressionHandler.checkedPathsInZip(zipSource, pathsToCheck);

            // 批量读取存在的瓦片
            for (String path : existingPaths) {
                try {
                    byte[] data = compressionHandler.readFileFromZip(zipSource, path);
                    String[] parts = path.split("/");
                    String fileName = parts[parts.length - 1];
                    String yStr = fileName.substring(0, fileName.lastIndexOf("."));
                    result.put(yStr, data);
                } catch (IOException e) {
                    log.warn("读取瓦片失败：{}", path, e);
                }
            }
        } catch (IOException e) {
            log.error("批量检查瓦片失败", e);
        }

        return result;
    }
}
