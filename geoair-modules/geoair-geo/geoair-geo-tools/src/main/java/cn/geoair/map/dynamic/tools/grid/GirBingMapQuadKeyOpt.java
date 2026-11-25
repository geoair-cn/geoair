package cn.geoair.map.dynamic.tools.grid;

import cn.geoair.map.dynamic.tools.grid.dto.TileZxyApo;
import cn.hutool.core.collection.ListUtil;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Bing Maps QuadKey 的生成、解析与层级聚合契约。
 *
 * <p>QuadKey 的 Y 行号采用 Bing/Google/XYZ 顶部原点约定，并非 TMS 行号。
 *
 * @author 张逢吉
 */
public interface GirBingMapQuadKeyOpt {

    /**
     * 根据 XYZ 瓦片坐标生成 QuadKey
     *
     * @param x 瓦片列号
     * @param y 瓦片行号
     * @param z 缩放级别（0~23）
     * @return QuadKey 字符串
     * @throws IllegalArgumentException 入参不合法时抛出
     */
    String xyzToQuadKey(int x, int y, int z);

    /**
     * 解析 QuadKey 为 XYZ 瓦片坐标实体
     *
     * @param quadKey 四叉键字符串
     * @return TileZxyApo 实体（包含z/x/y）
     * @throws IllegalArgumentException QuadKey 不合法时抛出
     */
    TileZxyApo quadKeyToXyz(String quadKey);

    /**
     * 获取指定 QuadKey 的父级别 QuadKey
     *
     * @param quadKey 原四叉键
     * @return 父级 QuadKey（若为0级则返回空字符串）
     * @throws IllegalArgumentException QuadKey 不合法时抛出
     */
    String getParentQuadKey(String quadKey);

    /**
     * 获取指定 QuadKey 的所有子级别 QuadKey（下一级）
     *
     * @param quadKey 原四叉键
     * @return 子级 QuadKey 数组（共4个：0/1/2/3后缀）
     * @throws IllegalArgumentException QuadKey 不合法或已达最高级时抛出
     */
    String[] getChildQuadKeys(String quadKey);

    /**
     * 获取QuadKey对应的缩放级别Z
     *
     * @param quadKey 四叉键字符串
     * @return 缩放级别Z
     * @throws IllegalArgumentException QuadKey不合法时抛出
     */
    int getQuadKeyZLevel(String quadKey);

    /**
     * 多个QuadKey向上截断到指定级别，返回所有不同的父级QuadKey（去重）
     *
     * @param targetLevel 指定要截断到的级别Z
     * @param quadKeys 多个QuadKey字符串（至少1个）
     * @return 指定级别下所有不同的父级QuadKey数组（按输入顺序去重）
     * @throws IllegalArgumentException 入参不合法时抛出
     */
    String[] getCommonParentQuadKey(int targetLevel, String... quadKeys);

    /**
     * 根据QuadKey获取指定级别的所有QuadKey（向上聚合/向下细化） 规则： 1. 目标级别 < 当前级别：向上聚合 → 返回1个父级QuadKey 2. 目标级别 =
     * 当前级别：返回原QuadKey（数组长度1） 3. 目标级别 > 当前级别：向下细化 → 返回4ⁿ个QuadKey（n=级别差）
     *
     * @param sourceQuadKey 源QuadKey
     * @param targetLevel 目标级别（0~23）
     * @return 指定级别的所有QuadKey数组
     * @throws IllegalArgumentException 入参不合法时抛出
     */
    String[] getTargetLevelQuadKey(String sourceQuadKey, int targetLevel);

    /**
     * 根据XYZ坐标获取指定级别的所有QuadKey（重载方法）
     *
     * @param x 源X坐标
     * @param y 源Y坐标
     * @param currentZ 源级别
     * @param targetLevel 目标级别（0~23）
     * @return 指定级别的所有QuadKey数组
     * @throws IllegalArgumentException 入参不合法时抛出
     */
    String[] getTargetLevelQuadKey(int x, int y, int currentZ, int targetLevel);

    /**
     * 获取源QuadKey转换到目标级别后的QuadKey范围（最小值和最大值）
     *
     * @param sourceQuadKey 源QuadKey
     * @param targetLevel 目标级别
     * @return 长度为2的数组，[0]是最小值，[1]是最大值
     */
    String[] getTargetLevelQuadKeyRange(String sourceQuadKey, int targetLevel);

    /**
     * 重载方法：通过XYZ坐标获取目标级别QuadKey的范围
     *
     * @param x 瓦片X坐标
     * @param y 瓦片Y坐标
     * @param currentZ 当前级别
     * @param targetLevel 目标级别
     * @return 长度为2的数组，[0]是最小值，[1]是最大值
     */
    String[] getTargetLevelQuadKeyRange(int x, int y, int currentZ, int targetLevel);

    /**
     * 重载方法：通过TileZxyApo实体获取目标级别QuadKey的范围
     *
     * @param sourceZxy 源瓦片坐标实体
     * @param targetLevel 目标级别
     * @return 长度为2的数组，[0]是最小值，[1]是最大值
     */
    String[] getTargetLevelQuadKeyRange(TileZxyApo sourceZxy, int targetLevel);

    /**
     * 批量根据 XYZ 瓦片坐标实体列表生成 QuadKey 列表
     *
     * @param apoList 瓦片坐标实体列表
     * @return QuadKey 字符串列表
     */
    default List<String> xyzToQuadKeyBatch(Collection<TileZxyApo> apoList) {
        return xyzToQuadKeyBatch(apoList.toArray(new TileZxyApo[0]));
    }

    /**
     * 批量根据 XYZ 瓦片坐标实体数组生成 QuadKey 列表
     *
     * @param apoArray 瓦片坐标实体数组
     * @return QuadKey 字符串列表
     */
    default List<String> xyzToQuadKeyBatch(TileZxyApo[] apoArray) {
        if (apoArray == null || apoArray.length == 0) {
            return ListUtil.empty();
        }
        return Arrays.stream(apoArray)
                .filter(Objects::nonNull)
                .map(apo -> xyzToQuadKey(apo.getX(), apo.getY(), apo.getZ()))
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 将多个 QuadKey 截断到指定层级，并以列表形式返回去重后的父级 QuadKey。
     *
     * @param targetLevel 要截断到的层级
     * @param quadKeys 待聚合的 QuadKey
     * @return 按输入出现顺序去重的父级 QuadKey 列表
     * @throws IllegalArgumentException 入参不合法时抛出
     */
    default List<String> getCommonParentQuadKeyList(int targetLevel, String... quadKeys) {
        String[] commonParentQuadKey = getCommonParentQuadKey(targetLevel, quadKeys);
        return Arrays.asList(commonParentQuadKey);
    }

    /**
     * 将集合中的 QuadKey 截断到指定层级，并返回去重后的父级 QuadKey 数组。
     *
     * @param targetLevel 要截断到的层级
     * @param quadKeyList 待聚合的 QuadKey 集合
     * @return 父级 QuadKey 数组
     * @throws IllegalArgumentException 入参不合法时抛出
     */
    default String[] getCommonParentQuadKey(int targetLevel, Collection<String> quadKeyList) {
        return getCommonParentQuadKey(targetLevel, quadKeyList.toArray(new String[0]));
    }

    /**
     * 根据TileZxyApo实体获取指定级别的所有QuadKey
     *
     * @param sourceZxy 源XYZ实体
     * @param targetLevel 目标级别（0~23）
     * @return 指定级别的所有QuadKey数组
     * @throws IllegalArgumentException 入参不合法时抛出
     */
    default String[] getTargetLevelQuadKey(TileZxyApo sourceZxy, int targetLevel) {
        return getTargetLevelQuadKey(
                sourceZxy.getX(), sourceZxy.getY(), sourceZxy.getZ(), targetLevel);
    }

    /**
     * 根据 XYZ 瓦片坐标实体生成 QuadKey
     *
     * @param tileZxyApo 瓦片坐标实体（包含z/x/y）
     * @return QuadKey 字符串
     * @throws IllegalArgumentException 入参不合法时抛出
     */
    default String xyzToQuadKey(TileZxyApo tileZxyApo) {
        return xyzToQuadKey(tileZxyApo.getX(), tileZxyApo.getY(), tileZxyApo.getZ());
    }

    /**
     * 获取指定 QuadKey 的所有子级别 QuadKey（下一级）
     *
     * @param quadKey 原四叉键
     * @return 子级 QuadKey 数组（共4个：0/1/2/3后缀）
     * @throws IllegalArgumentException QuadKey 不合法或已达最高级时抛出
     */
    default List<String> getChildQuadKeysList(String quadKey) {
        return Arrays.stream(getChildQuadKeys(quadKey)).collect(Collectors.toList());
    }
}
