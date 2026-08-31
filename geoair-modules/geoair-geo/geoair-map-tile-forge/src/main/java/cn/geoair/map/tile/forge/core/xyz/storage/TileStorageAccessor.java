package cn.geoair.map.tile.forge.core.xyz.storage;

import cn.hutool.core.lang.Pair;
import java.util.List;
import java.util.Set;

/** 瓦片存储访问器接口（抽象本地/S3等存储） */
public interface TileStorageAccessor {

    /** 获取Z层级的极值（最小Z和最大Z） */
    Pair<Integer, Integer> getZExtremes(String basePath);

    /** 获取指定Z层级下X的极值（最小X和最大X） */
    Pair<Integer, Integer> getXExtremes(String basePath, int z);

    /** 获取指定Z-X目录下Y的极值（最小Y和最大Y） */
    Pair<Integer, Integer> getYExtremes(String basePath, int z, int x, String format);

    Set<Integer> batchCheckZLevels(String basePath, List<Integer> zLevels);

    Set<Integer> batchCheckXLevels(String basePath, int z, List<Integer> xLevels);

    Set<Integer> batchCheckYLevels(
            String basePath, int z, int x, String format, List<Integer> yLevels);
}
