package cn.geoair.map.dynamic.tools.grid.bing;

import cn.geoair.map.dynamic.tools.grid.GirBingMapQuadKeyOpt;
import cn.geoair.map.dynamic.tools.grid.dto.TileZxyApo;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Arrays;

/**
 * 必应地图QuadKey服务实现类
 */
public class BingMapQuadKeyUtils implements GirBingMapQuadKeyOpt {

    // 1. 私有静态实例（volatile保证可见性，防止指令重排）
    private static volatile BingMapQuadKeyUtils INSTANCE;

    // 2. 私有构造方法（禁止外部实例化）
    private BingMapQuadKeyUtils() {
    }

    // 3. 公开静态方法获取单例（双重校验锁）
    public static BingMapQuadKeyUtils getInstance() {
        if (INSTANCE == null) { // 第一次校验（减少锁竞争）
            synchronized (BingMapQuadKeyUtils.class) { // 类锁
                if (INSTANCE == null) { // 第二次校验（防止多线程并发创建）
                    INSTANCE = new BingMapQuadKeyUtils();
                }
            }
        }
        return INSTANCE;
    }


    @Override
    public String xyzToQuadKey(int x, int y, int z) {
        if (z < 0 || z > 23) {
            throw new IllegalArgumentException("缩放级别z必须在0~23之间");
        }
        int maxTileNum = (1 << z) - 1; // 2^z - 1
        if (x < 0 || x > maxTileNum || y < 0 || y > maxTileNum) {
            throw new IllegalArgumentException(String.format("X/Y坐标超出范围（0~%d）", maxTileNum));
        }

        StringBuilder quadKey = new StringBuilder();
        for (int i = z; i > 0; i--) {
            char digit = '0';
            int mask = 1 << (i - 1);

            if ((x & mask) != 0) {
                digit++;
            }
            if ((y & mask) != 0) {
                digit++;
                digit++;
            }
            quadKey.append(digit);
        }
        return quadKey.toString();
    }

    @Override
    public TileZxyApo quadKeyToXyz(String quadKey) {
        if (quadKey == null || quadKey.isEmpty() || quadKey.length() > 23) {
            throw new IllegalArgumentException("QuadKey不能为空且长度需在1~23之间");
        }
        if (!quadKey.matches("[0-3]+")) {
            throw new IllegalArgumentException("QuadKey只能包含0/1/2/3字符");
        }

        int z = quadKey.length();
        int x = 0, y = 0;

        for (int i = 0; i < z; i++) {
            int mask = 1 << (z - 1 - i);
            char digit = quadKey.charAt(i);

            switch (digit) {
                case '0':
                    break;
                case '1':
                    x |= mask;
                    break;
                case '2':
                    y |= mask;
                    break;
                case '3':
                    x |= mask;
                    y |= mask;
                    break;
                default:
                    throw new IllegalArgumentException("无效的QuadKey字符：" + digit);
            }
        }
        return new TileZxyApo(z, x, y);
    }

    @Override
    public String getParentQuadKey(String quadKey) {
        // 改为调用返回实体的方法
        TileZxyApo zxy = quadKeyToXyz(quadKey);
        int z = zxy.getZ();
        if (z == 1) {
            return "";
        }
        return quadKey.substring(0, z - 1);
    }

    @Override
    public String[] getChildQuadKeys(String quadKey) {
        // 改为调用返回实体的方法
        TileZxyApo zxy = quadKeyToXyz(quadKey);
        int z = zxy.getZ();
        if (z >= 23) {
            throw new IllegalArgumentException("当前QuadKey已达最高缩放级别23，无子级");
        }
        return new String[]{quadKey + "0", quadKey + "1", quadKey + "2", quadKey + "3"};
    }

    @Override
    public int getQuadKeyZLevel(String quadKey) {
        // 改为调用返回实体的方法
        TileZxyApo zxy = quadKeyToXyz(quadKey);
        return zxy.getZ();
    }

    @Override
    public String[] getCommonParentQuadKey(int targetLevel, String... quadKeys) {
        if (quadKeys == null || quadKeys.length == 0) {
            throw new IllegalArgumentException("至少需要传入1个QuadKey");
        }
        if (targetLevel < 0 || targetLevel > 23) {
            throw new IllegalArgumentException("指定级别需在0~23之间");
        }

        LinkedHashSet<String> parentKeys = new LinkedHashSet<>();
        for (String quadKey : quadKeys) {
            int currentZ = getQuadKeyZLevel(quadKey);
            if (targetLevel > currentZ) {
                throw new IllegalArgumentException(String.format("QuadKey[%s]级别为%d，小于指定截断级别%d", quadKey, currentZ, targetLevel));
            }

            String truncatedKey = targetLevel == 0 ? "" : quadKey.substring(0, targetLevel);
            parentKeys.add(truncatedKey);
        }
        return parentKeys.toArray(new String[0]);
    }

    @Override
    public String[] getTargetLevelQuadKey(String sourceQuadKey, int targetLevel) {
        // 校验源QuadKey合法性（改为实体）
        TileZxyApo sourceZxy = quadKeyToXyz(sourceQuadKey);
        int currentLevel = sourceZxy.getZ();

        // 校验目标级别
        if (targetLevel < 0 || targetLevel > 23) {
            throw new IllegalArgumentException("目标级别需在0~23之间");
        }

        // 场景1：向上聚合（目标级别 < 当前级别）
        if (targetLevel < currentLevel) {
            String parentKey = targetLevel == 0 ? "" : sourceQuadKey.substring(0, targetLevel);
            return new String[]{parentKey};
        }

        // 场景2：同级转换
        if (targetLevel == currentLevel) {
            return new String[]{sourceQuadKey};
        }

        // 场景3：向下细化（目标级别 > 当前级别）→ 递归生成所有子节点
        int levelDiff = targetLevel - currentLevel;
        return generateAllChildKeys(sourceQuadKey, levelDiff);
    }

    @Override
    public String[] getTargetLevelQuadKey(int x, int y, int currentZ, int targetLevel) {
        String sourceQuadKey = xyzToQuadKey(x, y, currentZ);
        return getTargetLevelQuadKey(sourceQuadKey, targetLevel);
    }
    @Override
    public String[] getTargetLevelQuadKeyRange(String sourceQuadKey, int targetLevel) {
        // 校验源QuadKey合法性
        TileZxyApo sourceZxy = quadKeyToXyz(sourceQuadKey);
        int currentLevel = sourceZxy.getZ();

        // 校验目标级别
        if (targetLevel < 0 || targetLevel > 23) {
            throw new IllegalArgumentException("目标级别需在0~23之间");
        }

        // 场景1：向上聚合（目标级别 < 当前级别）→ 最小值和最大值都是父级Key
        if (targetLevel < currentLevel) {
            String parentKey = targetLevel == 0 ? "" : sourceQuadKey.substring(0, targetLevel);
            return new String[]{parentKey, parentKey};
        }

        // 场景2：同级转换 → 最小值和最大值都是自身
        if (targetLevel == currentLevel) {
            return new String[]{sourceQuadKey, sourceQuadKey};
        }

        // 场景3：向下细化（目标级别 > 当前级别）→ 生成最小值（补0）和最大值（补3）
        int levelDiff = targetLevel - currentLevel;
        StringBuilder minKey = new StringBuilder(sourceQuadKey);
        StringBuilder maxKey = new StringBuilder(sourceQuadKey);

        // 补0到目标级别（最小值）
        for (int i = 0; i < levelDiff; i++) {
            minKey.append('0');
        }

        // 补3到目标级别（最大值）
        for (int i = 0; i < levelDiff; i++) {
            maxKey.append('3');
        }

        return new String[]{minKey.toString(), maxKey.toString()};
    }

    @Override
    public String[] getTargetLevelQuadKeyRange(int x, int y, int currentZ, int targetLevel) {
        String sourceQuadKey = xyzToQuadKey(x, y, currentZ);
        return getTargetLevelQuadKeyRange(sourceQuadKey, targetLevel);
    }

    /**
     * 重载方法：通过TileZxyApo实体获取目标级别QuadKey的范围
     *
     * @param sourceZxy   源瓦片坐标实体
     * @param targetLevel 目标级别
     * @return 长度为2的数组，[0]是最小值，[1]是最大值
     */
    public String[] getTargetLevelQuadKeyRange(TileZxyApo sourceZxy, int targetLevel) {
        String sourceQuadKey = xyzToQuadKey(sourceZxy.getX(), sourceZxy.getY(), sourceZxy.getZ());
        return getTargetLevelQuadKeyRange(sourceQuadKey, targetLevel);
    }

    // ===================== 私有辅助方法 =====================

    /**
     * 递归生成指定QuadKey向下n级的所有子节点QuadKey
     *
     * @param parentKey 父级QuadKey
     * @param levelDiff 向下生成的级别数（n≥1）
     * @return 所有子节点QuadKey数组
     */
    private String[] generateAllChildKeys(String parentKey, int levelDiff) {
        // 递归终止条件：级别差为0 → 返回自身
        if (levelDiff == 0) {
            return new String[]{parentKey};
        }

        // 先获取下一级子节点
        String[] directChildren = getChildQuadKeys(parentKey);
        List<String> allChildren = new ArrayList<>();

        // 递归生成后续级别子节点
        for (String child : directChildren) {
            String[] grandChildren = generateAllChildKeys(child, levelDiff - 1);
            allChildren.addAll(Arrays.asList(grandChildren));
        }

        return allChildren.toArray(new String[0]);
    }

    // ===================== 测试示例 =====================
    public static void main(String[] args) {
        // 获取单例实例
        GirBingMapQuadKeyOpt quadKeyService = BingMapQuadKeyUtils.getInstance();

        // 测试1：XYZ转QuadKey
        String quadKey = quadKeyService.xyzToQuadKey(3, 5, 3);
        System.out.println("XYZ(3,5,3) → QuadKey: " + quadKey); // 输出 112

        // 测试2：QuadKey转TileZxyApo实体
        TileZxyApo zxy = quadKeyService.quadKeyToXyz(quadKey);
        System.out.println("QuadKey(112) → TileZxyApo: " + zxy); // 输出 z=3, x=3, y=5

        // 测试3：向下细化（Z3→Z5）
        String[] targetKeys = quadKeyService.getTargetLevelQuadKey(quadKey, 5);
        System.out.println("QuadKey(112) 转换到Z=5（共" + targetKeys.length + "个）：" + String.join(", ", targetKeys));

        // 测试4：多QuadKey获取指定级别父级
        String[] parentKeys = quadKeyService.getCommonParentQuadKey(3, "11200", "11233", "11300");
        System.out.println("多QuadKey截断到Z=3的父级：" + String.join(", ", parentKeys)); // 输出 112, 113

        // 测试5：通过TileZxyApo获取指定级别QuadKey
        TileZxyApo sourceZxy = new TileZxyApo(3, 3, 5);
        String[] targetKeysByZxy = quadKeyService.getTargetLevelQuadKey(sourceZxy, 4);
        System.out.println("TileZxyApo(3,3,5) 转换到Z=4：" + String.join(", ", targetKeysByZxy));
    }
}
