package cn.geoair.map.tile.forge.core.xyz.storage;

import java.io.File;
import java.util.*;

/**
 * 本地文件存储访问器
 */
public class LocalTileStorageAccessor extends AbstractTileStorageAccessor {

    static LocalTileStorageAccessor instance = null;

    private final Set<String> supportedFormats = new HashSet<>(Arrays.asList("png", "jpg", "jpeg"));

    /**
     * 单例获取
     */
    public static LocalTileStorageAccessor getInstance() {
        if (instance == null) {
            instance = new LocalTileStorageAccessor();
        }
        return instance;
    }

    /**
     * 构建文件路径
     */
    private File buildFile(String basePath, String... parts) {
        File parent = new File(basePath);
        for (String part : parts) {
            parent = new File(parent, part);
        }
        return parent;
    }

    @Override
    protected boolean zLevelExists(String basePath, int z) {
        File zDir = buildFile(basePath, String.valueOf(z));
        return zDir.exists() && zDir.isDirectory();
    }

    @Override
    protected boolean xLevelExists(String basePath, int z, int x) {
        File xDir = buildFile(basePath, String.valueOf(z), String.valueOf(x));
        return xDir.exists() && xDir.isDirectory();
    }

    @Override
    protected boolean yTileExists(String basePath, int z, int x, int y, String format) {
        File tileFile = buildFile(basePath, String.valueOf(z), String.valueOf(x), y + "." + format.toLowerCase());
        if (tileFile.exists() && tileFile.isFile()) {
            return true;
        }

        // 尝试其他格式
        for (String altFormat : supportedFormats) {
            if (!altFormat.equalsIgnoreCase(format)) {
                File altFile = buildFile(basePath, String.valueOf(z), String.valueOf(x), y + "." + altFormat);
                if (altFile.exists() && altFile.isFile()) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 批量检查文件是否存在
     */
    public Set<Integer> batchCheckFilesExist(String dirPath, int z, int x, int[] ys, String suffix) {
        Set<Integer> exists = new HashSet<>();
        File xDir = buildFile(dirPath, String.valueOf(z), String.valueOf(x));

        for (int y : ys) {
            File file = new File(xDir, y + suffix);
            if (file.exists()) {
                exists.add(y);
            }
        }

        return exists;
    }

    /**
     * 批量检查Z层级是否存在
     */
    @Override
    public Set<Integer> batchCheckZLevels(String basePath, List<Integer> zLevels) {
        Set<Integer> existingZs = new HashSet<>();
        File baseDir = new File(basePath);

        for (int z : zLevels) {
            File zDir = new File(baseDir, String.valueOf(z));
            if (zDir.exists() && zDir.isDirectory()) {
                existingZs.add(z);
            }
        }

        return existingZs;
    }

    /**
     * 批量检查X层级是否存在
     */
    @Override
    public Set<Integer> batchCheckXLevels(String basePath, int z, List<Integer> xLevels) {
        Set<Integer> existingXs = new HashSet<>();
        File zDir = new File(basePath, String.valueOf(z));

        if (zDir.exists() && zDir.isDirectory()) {
            for (int x : xLevels) {
                File xDir = new File(zDir, String.valueOf(x));
                if (xDir.exists() && xDir.isDirectory()) {
                    existingXs.add(x);
                }
            }
        }

        return existingXs;
    }

    /**
     * 批量检查Y瓦片是否存在
     */
    @Override
    public Set<Integer> batchCheckYLevels(String basePath, int z, int x, String format, List<Integer> yLevels) {
        Set<Integer> existingYs = new HashSet<>();
        File xDir = new File(new File(basePath, String.valueOf(z)), String.valueOf(x));

        if (xDir.exists() && xDir.isDirectory()) {
            // 支持的格式
            List<String> formats = new ArrayList<>();
            formats.add(format.toLowerCase());
            if (!format.equalsIgnoreCase("png")) formats.add("png");
            if (!format.equalsIgnoreCase("jpg")) formats.add("jpg");
            if (!format.equalsIgnoreCase("jpeg")) formats.add("jpeg");

            for (int y : yLevels) {
                boolean exists = false;

                for (String fmt : formats) {
                    File tileFile = new File(xDir, y + "." + fmt);
                    if (tileFile.exists() && tileFile.isFile()) {
                        exists = true;
                        break;
                    }
                }

                if (exists) {
                    existingYs.add(y);
                }
            }
        }

        return existingYs;
    }
}
