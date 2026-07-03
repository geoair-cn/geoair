package cn.geoair.base.util;

/**
 * 进度条工具类
 * 支持 int, long, float, double 等多种数值类型
 */
public class GutilPercent {

    // 默认步长：每10%更新一次
    private static final int DEFAULT_STEP = 10;


    /**
     * 获取需要更新的进度百分比（使用默认步长10%）
     *
     * @param current     当前数量（支持 int, long, float, double）
     * @param total       总数（支持 int, long, float, double）
     * @param lastPercent 上次更新的百分比（使用数组保存状态）
     * @return 需要更新的百分比，不需要更新返回-1
     */
    public static int getUpdatePercent(Number current, Number total, int[] lastPercent) {
        return getUpdatePercent(current, total, DEFAULT_STEP, lastPercent);
    }


    /**
     * 获取需要更新的进度百分比（自定义步长）
     *
     * @param current     当前数量（支持 int, long, float, double）
     * @param total       总数（支持 int, long, float, double）
     * @param step        更新步长（如：10表示每10%更新一次）
     * @param lastPercent 上次更新的百分比（使用数组保存状态）
     * @return 需要更新的百分比，不需要更新返回-1
     */
    public static int getUpdatePercent(Number current, Number total, int step, int[] lastPercent) {
        // 参数校验
        if (total == null || current == null || lastPercent == null || lastPercent.length == 0) {
            return -1;
        }

        double totalDouble = total.doubleValue();
        if (totalDouble <= 0) {
            return -1;
        }

        double currentDouble = current.doubleValue();
        // 如果当前值超过总数，限制为总数
        if (currentDouble > totalDouble) {
            currentDouble = totalDouble;
        }

        // 计算当前百分比
        int percent = (int) Math.min(currentDouble / totalDouble * 100, 100);

        // 判断是否需要更新
        boolean shouldUpdate = false;

        if (step <= 0) {
            // 步长<=0：每次都更新
            shouldUpdate = (percent != lastPercent[0]);
        } else {
            // 特殊处理边界：0%和100%必须更新
            if (percent == 0 || percent == 100) {
                shouldUpdate = (percent != lastPercent[0]);
            } else {
                // 按步长更新
                shouldUpdate = (percent / step > lastPercent[0] / step);
            }
        }

        // 需要更新则保存并返回
        if (shouldUpdate) {
            lastPercent[0] = percent;
            return percent;
        }

        return -1;
    }

    /**
     * 计算进度百分比（仅计算，不涉及步长控制）
     *
     * @param current 当前数量
     * @param total   总数
     * @return 进度百分比 0-100
     */
    public static int getUpdatePercent(int current, int total) {
        if (total <= 0) {
            return 0;
        }
        return (int) Math.min((double) current / total * 100, 100);
    }

    /**
     * 计算进度百分比（仅计算，不涉及步长控制）
     *
     * @param current 当前数量
     * @param total   总数
     * @return 进度百分比 0-100
     */
    public static int getUpdatePercent(long current, long total) {
        if (total <= 0) {
            return 0;
        }
        return (int) Math.min((double) current / total * 100, 100);
    }

    /**
     * 计算进度百分比（仅计算，不涉及步长控制）
     *
     * @param current 当前数量
     * @param total   总数
     * @return 进度百分比 0-100
     */
    public static int getUpdatePercent(float current, float total) {
        if (total <= 0) {
            return 0;
        }
        return (int) Math.min((double) current / total * 100, 100);
    }

    /**
     * 计算进度百分比（仅计算，不涉及步长控制）
     *
     * @param current 当前数量
     * @param total   总数
     * @return 进度百分比 0-100
     */
    public static int getUpdatePercent(double current, double total) {
        if (total <= 0) {
            return 0;
        }
        return (int) Math.min(current / total * 100, 100);
    }

    /**
     * 计算进度百分比（仅计算，不涉及步长控制）
     *
     * @param current 当前数量
     * @param total   总数
     * @return 进度百分比 0-100
     */
    public static int getUpdatePercent(Number current, Number total) {
        if (total == null || current == null) {
            return 0;
        }
        double totalDouble = total.doubleValue();
        if (totalDouble <= 0) {
            return 0;
        }
        double currentDouble = current.doubleValue();
        return (int) Math.min(currentDouble / totalDouble * 100, 100);
    }


    public static int getUpdatePercent(int current, int total, int[] lastPercent) {
        return getUpdatePercent((Number) current, (Number) total, DEFAULT_STEP, lastPercent);
    }

    public static int getUpdatePercent(int current, int total, int step, int[] lastPercent) {
        return getUpdatePercent((Number) current, (Number) total, step, lastPercent);
    }

    public static int getUpdatePercent(long current, long total, int[] lastPercent) {
        return getUpdatePercent((Number) current, (Number) total, DEFAULT_STEP, lastPercent);
    }

    public static int getUpdatePercent(long current, long total, int step, int[] lastPercent) {
        return getUpdatePercent((Number) current, (Number) total, step, lastPercent);
    }

    public static int getUpdatePercent(float current, float total, int[] lastPercent) {
        return getUpdatePercent((Number) current, (Number) total, DEFAULT_STEP, lastPercent);
    }

    public static int getUpdatePercent(float current, float total, int step, int[] lastPercent) {
        return getUpdatePercent((Number) current, (Number) total, step, lastPercent);
    }

    public static int getUpdatePercent(double current, double total, int[] lastPercent) {
        return getUpdatePercent((Number) current, (Number) total, DEFAULT_STEP, lastPercent);
    }

    public static int getUpdatePercent(double current, double total, int step, int[] lastPercent) {
        return getUpdatePercent((Number) current, (Number) total, step, lastPercent);
    }

    // ==================== 进度条显示 ====================

    /**
     * 获取进度条字符串（默认宽度50）
     */
    public static String getProgressBar(int percent) {
        return getProgressBar(percent, 50);
    }

    /**
     * 获取进度条字符串（自定义宽度）
     */
    public static String getProgressBar(int percent, int width) {
        if (width <= 0) width = 50;
        int filled = Math.min(percent * width / 100, width);
        int empty = width - filled;

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < filled; i++) {
            sb.append("█");
        }
        for (int i = 0; i < empty; i++) {
            sb.append("░");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * 获取完整进度显示（进度条+百分比）
     */
    public static String getProgressDisplay(int percent) {
        return getProgressBar(percent) + String.format(" %3d%%", percent);
    }

    /**
     * 获取完整进度显示（自定义宽度）
     */
    public static String getProgressDisplay(int percent, int width) {
        return getProgressBar(percent, width) + String.format(" %3d%%", percent);
    }

    // ==================== 测试 ====================

    public static void main(String[] args) {
        System.out.println("=== 测试各种数据类型 ===");

        int[] lastPercent = {0};

        // 测试 double 类型
        double totalDouble = 100.5;
        System.out.println("\n--- double 类型 ---");
        for (double current = 0; current <= totalDouble; current += 0.5) {
            int percent = GutilPercent.getUpdatePercent(current, totalDouble, 5, lastPercent);
            if (percent != -1) {
                System.out.printf("当前进度: %d%%  %s%n",
                        percent, GutilPercent.getProgressDisplay(percent));
            }
        }

        // 重置状态
        int[] lastPercent2 = {0};

        // 测试 float 类型
        float totalFloat = 100.0f;
        System.out.println("\n--- float 类型 ---");
        for (float current = 0; current <= totalFloat; current += 1) {
            int percent = GutilPercent.getUpdatePercent(current, totalFloat, 1, lastPercent2);
            if (percent != -1) {
                System.out.printf("当前进度: %d%%  %s%n",
                        percent, GutilPercent.getProgressDisplay(percent));
            }
        }

        // 重置状态
        int[] lastPercent3 = {0};

        // 测试 int 类型
        System.out.println("\n--- int 类型 ---");
        for (int current = 0; current <= 100; current += 1) {
            int percent = GutilPercent.getUpdatePercent(current, 100, 10, lastPercent3);
            if (percent != -1) {
                System.out.printf("当前进度: %d%%  %s%n",
                        percent, GutilPercent.getProgressDisplay(percent));
            }
        }

        // 测试边界情况
        System.out.println("\n=== 边界测试 ===");
        int[] last = {0};
        System.out.println("测试0: " + GutilPercent.getUpdatePercent(0, 100.5, last));
        System.out.println("测试1: " + GutilPercent.getUpdatePercent(1, 100.5, last));
        System.out.println("测试10: " + GutilPercent.getUpdatePercent(10, 100.5, last));
        System.out.println("测试99: " + GutilPercent.getUpdatePercent(99, 100.5, last));
        System.out.println("测试100: " + GutilPercent.getUpdatePercent(100, 100.5, last));
        System.out.println("测试100.5: " + GutilPercent.getUpdatePercent(100.5, 100.5, last));

        // 测试总数为0的情况
        System.out.println("\n=== 异常情况测试 ===");
        int[] last2 = {0};
        System.out.println("总数为0: " + GutilPercent.getUpdatePercent(50, 0, last2));
        System.out.println("当前值超过总数: " + GutilPercent.getUpdatePercent(150, 100, last2));
    }
}
