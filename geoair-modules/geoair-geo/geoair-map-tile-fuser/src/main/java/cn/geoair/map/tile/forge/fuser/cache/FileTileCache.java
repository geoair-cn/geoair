package cn.geoair.map.tile.forge.fuser.cache;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.tile.forge.fuser.utils.FuserCacheUtils;
import cn.geoair.web.mime.GiMimeType;
import cn.hutool.core.io.FileUtil;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件系统缓存实现
 *
 * @author 张俊
 * @date Created in 2023/12/4
 * @description 将瓦片缓存到本地文件系统，目录结构: layerName/z/x/y.png 注意： 缓存的结果，全部转换成wmts原点
 */
public class FileTileCache implements TileCache {
    private static GiLogger log = GirLoggerFactory.getLogger();
    // 缓存根目录
    private final String cacheRoot;
    // 缓存过期时间（毫秒），0表示不过期
    private final long expireTime;
    // 是否启用缓存
    private final boolean enabled;
    // 缓存图层 Y 轴翻转配置
    private final ConcurrentHashMap<String, Boolean> layerReverseCache = new ConcurrentHashMap<>();

    public FileTileCache() {
        this(FileUtil.getTmpDirPath() + "/tile_cache/", 7 * 24 * 60 * 60 * 1000L, true);
    }

    public FileTileCache(String cacheRoot) {
        this(cacheRoot, 7 * 24 * 60 * 60 * 1000L, true);
    }

    public FileTileCache(String cacheRoot, long expireTime, boolean enabled) {
        this.cacheRoot = cacheRoot.endsWith("/") ? cacheRoot : cacheRoot + "/";
        this.expireTime = expireTime;
        this.enabled = enabled;

        // 初始化缓存目录
        if (enabled) {
            try {
                Path cachePath = Paths.get(this.cacheRoot);
                if (!Files.exists(cachePath)) {
                    Files.createDirectories(cachePath);
                    log.info("创建缓存根目录: {}", this.cacheRoot);
                }
            } catch (IOException e) {
                log.error("创建缓存根目录失败: {}", this.cacheRoot, e);
            }
        }
    }

    /**
     * 判断图层是否需要翻转 Y
     *
     * @param layerName 图层名称
     * @return true: 需要翻转（Google 坐标系 → TMS 坐标系）
     */
    private boolean isNeedReverseY(String layerName) {
        return layerReverseCache.computeIfAbsent(
                layerName, k -> FuserCacheUtils.fileCheckIsNeedReverseY(layerName));
    }

    /** 获取缓存文件路径（支持 Y 轴翻转） */
    private Path getCachePath(String layerName, int z, int x, int y, GiMimeType format) {
        boolean needReverse = isNeedReverseY(layerName);
        int storeY = FuserCacheUtils.getStoreY(z, y, needReverse);
        // 使用layerName/z/x/目录结构，文件名为storeY.format
        String subDir = layerName + "/" + z + "/" + x;
        return Paths.get(cacheRoot, subDir, storeY + "." + format.getFileExtension());
    }

    @Override
    public byte[] get(String layerName, int z, int x, int y, GiMimeType format) {
        if (!enabled) {
            return null;
        }

        try {
            Path cachePath = getCachePath(layerName, z, x, y, format);
            if (!Files.exists(cachePath)) {
                log.debug("缓存不存在: {}", cachePath);
                return null;
            }

            // 检查是否过期
            if (expireTime > 0) {
                long lastModified = Files.getLastModifiedTime(cachePath).toMillis();
                long now = System.currentTimeMillis();
                if (now - lastModified > expireTime) {
                    log.debug("缓存已过期，删除: {}", cachePath);
                    Files.deleteIfExists(cachePath);
                    return null;
                }
            }

            // 读取缓存文件
            byte[] data = Files.readAllBytes(cachePath);
            log.debug("从缓存读取瓦片成功: {} - ({},{},{})", layerName, z, x, y);
            return data;

        } catch (IOException e) {
            log.error("读取缓存失败: layerName={}, z={}, x={}, y={}", layerName, z, x, y, e);
            return null;
        }
    }

    @Override
    public boolean put(String layerName, int z, int x, int y, byte[] data, GiMimeType format) {
        if (!enabled || data == null || data.length == 0) {
            return false;
        }

        try {
            Path cachePath = getCachePath(layerName, z, x, y, format);

            // 创建父目录
            Path parentDir = cachePath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            // 写入缓存文件
            Files.write(
                    cachePath,
                    data,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            log.debug("保存瓦片到缓存成功: {} - ({},{},{})", layerName, z, x, y);
            return true;

        } catch (IOException e) {
            log.error("保存缓存失败: layerName={}, z={}, x={}, y={}", layerName, z, x, y, e);
            return false;
        }
    }

    @Override
    public boolean deleteLayerCache(String layerName) {
        if (!enabled) {
            return false;
        }

        Path layerPath = Paths.get(cacheRoot, layerName);
        if (!Files.exists(layerPath)) {
            log.debug("图层缓存目录不存在: {}", layerName);
            return false;
        }

        try {
            // 生成临时目录名（原目录名 + 时间戳 + 随机数）
            String tempDirName =
                    layerName
                            + "_deleting_"
                            + System.currentTimeMillis()
                            + "_"
                            + Thread.currentThread().getId();
            Path tempPath = Paths.get(cacheRoot, tempDirName);

            // 原子性的重命名操作
            Files.move(layerPath, tempPath, StandardCopyOption.ATOMIC_MOVE);
            log.info("图层缓存目录已重命名: {} -> {}", layerPath, tempPath);

            // 异步删除临时目录
            asyncDeleteDirectory(tempPath);

            // 清除缓存的翻转配置
            layerReverseCache.remove(layerName);

            return true;

        } catch (IOException e) {
            log.error("重命名图层缓存目录失败: {}", layerName, e);

            // 重命名失败，尝试直接删除（降级方案）
            try {
                log.warn("尝试直接删除图层缓存: {}", layerName);
                deleteDirectorySync(layerPath);
                // 清除缓存的翻转配置
                layerReverseCache.remove(layerName);
                return true;
            } catch (IOException ex) {
                log.error("直接删除图层缓存也失败: {}", layerName, ex);
                return false;
            }
        }
    }

    private void asyncDeleteDirectory(Path path) {
        // 使用线程池异步删除，避免阻塞主线程
        Thread deleteThread =
                new Thread(
                        () -> {
                            try {
                                log.info("开始异步删除临时目录: {}", path);
                                deleteDirectorySync(path);
                                log.info("异步删除临时目录成功: {}", path);
                            } catch (IOException e) {
                                log.error("异步删除临时目录失败: {}", path, e);
                            }
                        });
        deleteThread.setDaemon(true); // 设置为守护线程
        deleteThread.setName("cache-delete-" + System.currentTimeMillis());
        deleteThread.start();
    }

    private void deleteDirectorySync(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.walk(path)
                    .sorted((a, b) -> -a.compareTo(b)) // 先删除文件，再删除目录
                    .forEach(
                            p -> {
                                try {
                                    Files.deleteIfExists(p);
                                } catch (IOException e) {
                                    log.error("删除文件/目录失败: {}", p, e);
                                }
                            });
        }
    }

    @Override
    public boolean delete(String layerName, Integer z, Integer x) {
        if (!enabled) {
            return false;
        }

        Path targetPath;
        if (z == null) {
            // 删除整个图层
            targetPath = Paths.get(cacheRoot, layerName);
        } else if (x == null) {
            // 删除指定层级的所有瓦片
            targetPath = Paths.get(cacheRoot, layerName, String.valueOf(z));
        } else {
            // 删除指定x目录下的所有瓦片
            targetPath = Paths.get(cacheRoot, layerName, String.valueOf(z), String.valueOf(x));
        }

        if (!Files.exists(targetPath)) {
            log.debug("缓存目录不存在: {}", targetPath);
            return false;
        }

        try {
            String tempDirName =
                    targetPath.getFileName().toString()
                            + "_deleting_"
                            + System.currentTimeMillis()
                            + "_"
                            + Thread.currentThread().getId();
            Path tempPath = targetPath.resolveSibling(tempDirName);

            Files.move(targetPath, tempPath, StandardCopyOption.ATOMIC_MOVE);
            log.info("缓存目录已重命名: {} -> {}", targetPath, tempPath);

            asyncDeleteDirectory(tempPath);
            return true;

        } catch (IOException e) {
            log.error("重命名缓存目录失败: {}", targetPath, e);

            try {
                deleteDirectorySync(targetPath);
                return true;
            } catch (IOException ex) {
                log.error("直接删除缓存目录也失败: {}", targetPath, ex);
                return false;
            }
        }
    }

    @Override
    public boolean delete(String layerName, int z, int x, int y, GiMimeType format) {
        if (!enabled) {
            return false;
        }

        try {
            Path cachePath = getCachePath(layerName, z, x, y, format);
            if (Files.exists(cachePath)) {
                Files.delete(cachePath);
                log.debug("删除缓存成功: {} - ({},{},{})", layerName, z, x, y);
                return true;
            }
        } catch (IOException e) {
            log.error("删除缓存失败: layerName={}, z={}, x={}, y={}", layerName, z, x, y, e);
        }
        return false;
    }

    @Override
    public void clearAll() {
        if (!enabled) {
            return;
        }

        Path cacheRootPath = Paths.get(cacheRoot);
        if (!Files.exists(cacheRootPath)) {
            log.debug("缓存根目录不存在: {}", cacheRoot);
            return;
        }

        try {
            // 生成临时目录名（根目录 + 时间戳 + 随机数）
            String tempDirName =
                    "cache_root_deleting_"
                            + System.currentTimeMillis()
                            + "_"
                            + Thread.currentThread().getId();
            Path tempPath = cacheRootPath.resolveSibling(tempDirName);

            // 原子性的重命名操作
            Files.move(cacheRootPath, tempPath, StandardCopyOption.ATOMIC_MOVE);
            log.info("缓存根目录已重命名: {} -> {}", cacheRootPath, tempPath);

            // 重新创建空的缓存根目录
            Files.createDirectories(cacheRootPath);
            log.info("已重新创建缓存根目录: {}", cacheRootPath);

            // 异步删除临时目录
            asyncDeleteDirectory(tempPath);

            // 清除所有翻转配置缓存
            layerReverseCache.clear();

        } catch (IOException e) {
            log.error("重命名缓存根目录失败: {}", cacheRoot, e);

            // 重命名失败，尝试直接清空（降级方案）
            try {
                log.warn("尝试直接清空缓存根目录: {}", cacheRoot);
                clearAllSync(cacheRootPath);
                layerReverseCache.clear();
            } catch (IOException ex) {
                log.error("直接清空缓存根目录也失败: {}", cacheRoot, ex);
            }
        }
    }

    private void clearAllSync(Path path) throws IOException {
        if (Files.exists(path)) {
            // 遍历目录，删除所有子文件和子目录，但保留根目录本身
            Files.walk(path)
                    .filter(p -> !p.equals(path)) // 排除根目录本身
                    .sorted((a, b) -> -a.compareTo(b)) // 先删除文件，再删除目录
                    .forEach(
                            p -> {
                                try {
                                    Files.deleteIfExists(p);
                                } catch (IOException e) {
                                    log.error("删除文件/目录失败: {}", p, e);
                                }
                            });
            log.info("直接清空缓存根目录成功: {}", path);
        }
    }

    @Override
    public long getTotalSize() {
        if (!enabled) {
            return 0;
        }

        try {
            Path cacheRootPath = Paths.get(cacheRoot);
            if (!Files.exists(cacheRootPath)) {
                return 0;
            }

            return Files.walk(cacheRootPath)
                    .filter(Files::isRegularFile)
                    .mapToLong(
                            path -> {
                                try {
                                    return Files.size(path);
                                } catch (IOException e) {
                                    return 0;
                                }
                            })
                    .sum();
        } catch (IOException e) {
            log.error("获取缓存大小失败", e);
            return 0;
        }
    }

    @Override
    public boolean exists(String layerName, int z, int x, int y, GiMimeType format) {
        if (!enabled) {
            return false;
        }

        Path cachePath = getCachePath(layerName, z, x, y, format);
        if (!Files.exists(cachePath)) {
            return false;
        }

        // 检查是否过期
        if (expireTime > 0) {
            try {
                long lastModified = Files.getLastModifiedTime(cachePath).toMillis();
                long now = System.currentTimeMillis();
                if (now - lastModified > expireTime) {
                    return false;
                }
            } catch (IOException e) {
                log.error("检查缓存过期失败", e);
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
