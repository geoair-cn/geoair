package cn.geoair.map.tile.forge.core.xyz.storage;

import cn.hutool.core.lang.Pair;
import cn.hutool.core.map.MapUtil;

import java.util.*;

/**
 * 瓦片存储访问器抽象基类
 */
public abstract class AbstractTileStorageAccessor implements TileStorageAccessor {

    // 缓存结果
    protected Integer maxZ;
    protected Integer minZ;
    protected final Map<String, Map<Integer, Pair<Integer, Integer>>> xExtremesCache = new HashMap<>();
    protected final Map<String, Map<String, Pair<Integer, Integer>>> yExtremesCache = new HashMap<>();

    // 默认最大层级
    protected static final int MAX_LEVEL = 30;

    // Z层级参数（固定）
    protected static final int Z_STEP = 5;         // Z层级检查步长
    protected static final int Z_BATCH_SIZE = 5;   // Z层级批量检查数量

    // XY坐标基础参数（动态计算）
    protected static final int XY_BASE_STEP = 8;    // XY基础步长
    protected static final int XY_BASE_BATCH_SIZE = 10; // XY基础批量大小
    protected static final int MAX_BATCH_SIZE = 50; // 最大批量大小限制

    /**
     * 检查Z层级是否存在
     */
    protected abstract boolean zLevelExists(String basePath, int z);


    /**
     * 检查X层级是否存在
     */
    protected abstract boolean xLevelExists(String basePath, int z, int x);

    /**
     * 检查Y瓦片是否存在
     */
    protected abstract boolean yTileExists(String basePath, int z, int x, int y, String format);


    /**
     * 计算XY坐标的动态步长
     */
    protected int calculateXYStep(int z) {
        int maxRange = (int) Math.pow(2, z) - 1;

        // 根据范围大小动态调整步长
        if (maxRange <= 100) {
            return 2;  // 小范围，小步长
        } else if (maxRange <= 1000) {
            return 10; // 中等范围，中等步长
        } else if (maxRange <= 10000) {
            return 50; // 大范围，大步长
        } else {
            return 100; // 超大范围，最大步长
        }
    }

    /**
     * 计算XY坐标的动态批量大小
     */
    protected int calculateXYBatchSize(int z) {
        int maxRange = (int) Math.pow(2, z) - 1;

        // 根据范围大小和步长动态调整批量大小
        int step = calculateXYStep(z);
        int batchSize = Math.min(step * 2, MAX_BATCH_SIZE);

        // 小范围使用更小的批量
        if (maxRange <= 100) {
            batchSize = Math.min(batchSize, 10);
        }

        return batchSize;
    }

    /**
     * 批量二分查找最小值（完整逻辑）
     *
     * @param basePath  基础路径
     * @param minRange  最小值范围
     * @param maxRange  最大值范围
     * @param step      检查步长（间隔）
     * @param batchSize 批量检查数量
     * @param checker   批量检查器
     * @return 找到的最小值，-1表示未找到
     */
    protected int findMinValue(String basePath, int minRange, int maxRange, int step, int batchSize,
                               BatchValueChecker checker) {
        if (minRange > maxRange) {
            return -1;
        }

        // 如果范围很小，直接全量检查
        if (maxRange - minRange + 1 <= batchSize) {
            List<Integer> allValues = new ArrayList<>();
            for (int i = minRange; i <= maxRange; i++) {
                allValues.add(i);
            }
            Set<Integer> exists = checker.batchExists(basePath, allValues);
            return exists.isEmpty() ? -1 : Collections.min(exists);
        }

        // 第一步：按步长快速扫描，找到候选区域
        List<Integer> stepPoints = new ArrayList<>();
        for (int i = minRange; i <= maxRange; i += step) {
            stepPoints.add(i);
        }
        // 确保包含最后一个值
        if (!stepPoints.contains(maxRange)) {
            stepPoints.add(maxRange);
        }

        Set<Integer> stepExists = checker.batchExists(basePath, stepPoints);

        if (stepExists.isEmpty()) {
            return -1; // 没有找到任何值
        }

        int candidateMin = Collections.min(stepExists);
        int searchStart = Math.max(minRange, candidateMin - step);
        int searchEnd = candidateMin;

        // 第二步：在候选区域内批量精确查找
        return findMinInRange(basePath, searchStart, searchEnd, batchSize, checker);
    }

    /**
     * 批量二分查找最大值（完整逻辑）
     *
     * @param basePath  基础路径
     * @param minRange  最小值范围
     * @param maxRange  最大值范围
     * @param step      检查步长（间隔）
     * @param batchSize 批量检查数量
     * @param checker   批量检查器
     * @return 找到的最大值，-1表示未找到
     */
    protected int findMaxValue(String basePath, int minRange, int maxRange, int step, int batchSize,
                               BatchValueChecker checker) {
        if (minRange > maxRange) {
            return -1;
        }

        // 如果范围很小，直接全量检查
        if (maxRange - minRange + 1 <= batchSize) {
            List<Integer> allValues = new ArrayList<>();
            for (int i = minRange; i <= maxRange; i++) {
                allValues.add(i);
            }
            Set<Integer> exists = checker.batchExists(basePath, allValues);
            return exists.isEmpty() ? -1 : Collections.max(exists);
        }

        // 第一步：按步长快速扫描，找到候选区域
        List<Integer> stepPoints = new ArrayList<>();
        for (int i = minRange; i <= maxRange; i += step) {
            stepPoints.add(i);
        }
        // 确保包含最后一个值
        if (!stepPoints.contains(maxRange)) {
            stepPoints.add(maxRange);
        }

        Set<Integer> stepExists = checker.batchExists(basePath, stepPoints);

        if (stepExists.isEmpty()) {
            return -1; // 没有找到任何值
        }

        int candidateMax = Collections.max(stepExists);
        int searchStart = candidateMax;
        int searchEnd = Math.min(maxRange, candidateMax + step);

        // 第二步：在候选区域内批量精确查找
        return findMaxInRange(basePath, searchStart, searchEnd, batchSize, checker);
    }

    /**
     * 在指定范围内精确查找最小值
     */
    private int findMinInRange(String basePath, int start, int end, int batchSize, BatchValueChecker checker) {
        List<Integer> allValues = new ArrayList<>();
        for (int i = start; i <= end; i++) {
            allValues.add(i);
        }

        // 分批处理，找到第一个存在的值就返回
        for (int i = 0; i < allValues.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, allValues.size());
            List<Integer> batch = allValues.subList(i, endIndex);
            Set<Integer> exists = checker.batchExists(basePath, batch);

            if (!exists.isEmpty()) {
                return Collections.min(exists);
            }
        }

        return -1;
    }

    /**
     * 在指定范围内精确查找最大值
     */
    private int findMaxInRange(String basePath, int start, int end, int batchSize, BatchValueChecker checker) {
        List<Integer> allValues = new ArrayList<>();
        for (int i = start; i <= end; i++) {
            allValues.add(i);
        }

        // 从后往前分批处理，找到最后一个存在的值就返回
        int maxFound = -1;
        for (int i = allValues.size() - 1; i >= 0; i -= batchSize) {
            int startIndex = Math.max(i - batchSize + 1, 0);
            List<Integer> batch = allValues.subList(startIndex, i + 1);
            Set<Integer> exists = checker.batchExists(basePath, batch);

            if (!exists.isEmpty()) {
                int currentMax = Collections.max(exists);
                maxFound = Math.max(maxFound, currentMax);
                break;
            }
        }

        return maxFound;
    }

    /**
     * 优化的批量二分查找最小Z层级
     */
    protected int findMinZ(String basePath) {
        if (minZ != null) {
            return minZ;
        }

        int result = findMinValue(basePath, 0, MAX_LEVEL, Z_STEP, Z_BATCH_SIZE,
                (path, values) -> batchCheckZLevels(path, values));

        minZ = result == -1 ? 0 : result;
        return minZ;
    }

    /**
     * 优化的批量二分查找最大Z层级
     */
    protected int findMaxZ(String basePath) {
        if (maxZ != null) {
            return maxZ;
        }

        int result = findMaxValue(basePath, 0, MAX_LEVEL, Z_STEP, Z_BATCH_SIZE,
                (path, values) -> batchCheckZLevels(path, values));

        maxZ = result == -1 ? 0 : result;
        return maxZ;
    }

    /**
     * 优化的批量二分查找最小X层级
     */
    protected int findMinX(String basePath, int z) {
        int maxPossibleX = (int) Math.pow(2, z) - 1;
        int step = calculateXYStep(z);
        int batchSize = calculateXYBatchSize(z);

        int result = findMinValue(basePath, 0, maxPossibleX, step, batchSize,
                (path, values) -> batchCheckXLevels(path, z, values));

        return result == -1 ? 0 : result;
    }

    /**
     * 优化的批量二分查找最大X层级
     */
    protected int findMaxX(String basePath, int z) {
        if (xExtremesCache.containsKey(basePath) && xExtremesCache.get(basePath).containsKey(z)) {
            return xExtremesCache.get(basePath).get(z).getValue();
        }

        int maxPossibleX = (int) Math.pow(2, z) - 1;
        int step = calculateXYStep(z);
        int batchSize = calculateXYBatchSize(z);

        int result = findMaxValue(basePath, 0, maxPossibleX, step, batchSize,
                (path, values) -> batchCheckXLevels(path, z, values));

        int minX = findMinX(basePath, z);
        xExtremesCache.put(basePath, MapUtil.of(z, new Pair<>(minX, result == -1 ? 0 : result)));
        return result == -1 ? 0 : result;
    }

    /**
     * 优化的批量二分查找最小Y层级
     */
    protected int findMinY(String basePath, int z, int x, String format) {
        int maxPossibleY = (int) Math.pow(2, z) - 1;
        int step = calculateXYStep(z);
        int batchSize = calculateXYBatchSize(z);

        int result = findMinValue(basePath, 0, maxPossibleY, step, batchSize,
                (path, values) -> batchCheckYLevels(path, z, x, format, values));

        return result == -1 ? 0 : result;
    }

    /**
     * 优化的批量二分查找最大Y层级
     */
    protected int findMaxY(String basePath, int z, int x, String format) {
        String cacheKey = getCacheKey(z, x, format);
        if (yExtremesCache.containsKey(basePath)&& yExtremesCache.get(basePath).containsKey(cacheKey)) {
            return yExtremesCache.get(basePath).get(cacheKey).getValue();
        }

        int maxPossibleY = (int) Math.pow(2, z) - 1;
        int step = calculateXYStep(z);
        int batchSize = calculateXYBatchSize(z);

        int result = findMaxValue(basePath, 0, maxPossibleY, step, batchSize,
                (path, values) -> batchCheckYLevels(path, z, x, format, values));

        int minY = findMinY(basePath, z, x, format);
        yExtremesCache.put(basePath, MapUtil.of(cacheKey, new Pair<>(minY, result == -1 ? 0 : result)));
        return result == -1 ? 0 : result;
    }

    /**
     * 批量值检查器接口
     */
    @FunctionalInterface
    protected interface BatchValueChecker {
        Set<Integer> batchExists(String basePath, List<Integer> values);
    }

    /**
     * 获取缓存键
     */
    protected String getCacheKey(int z, int x, String format) {
        return z + "/" + x + "/" + format;
    }

    /**
     * 重置缓存
     */
    protected void resetCache() {
        maxZ = null;
        minZ = null;
        xExtremesCache.clear();
        yExtremesCache.clear();
    }

    @Override
    public Pair<Integer, Integer> getZExtremes(String basePath) {
//        resetCache();
        return new Pair<>(findMinZ(basePath), findMaxZ(basePath));
    }

    @Override
    public Pair<Integer, Integer> getXExtremes(String basePath, int z) {
        findMaxX(basePath, z);
        return xExtremesCache.get(basePath).get(z);
    }

    @Override
    public Pair<Integer, Integer> getYExtremes(String basePath, int z, int x, String format) {
        findMaxY(basePath, z, x, format);
        return yExtremesCache.get(basePath).get(getCacheKey(z, x, format));
    }

    /**
     * 判断字符串是否为数字
     */
    protected boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
