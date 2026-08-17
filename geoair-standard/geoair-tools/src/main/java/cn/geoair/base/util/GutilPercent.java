package cn.geoair.base.util;

/**
 * 进度条工具类
 * 支持 int, long, float, double 等多种数值类型
 */
public class GutilPercent {

    // 默认步长：每10%更新一次
    public static final int DEFAULT_STEP = 10;
    public static final double DEFAULT_STEP_DOUBLE = 10.0;


    /**
     * 计算下一次需要发布的进度值（带步长节流）。
     * <p>
     * 纯函数，不保存任何状态：调用方需自行保存并传入上次已发布的进度，
     * 返回值即"本次需要发布的进度"；返回 -1.0 表示未跨过步长、无需发布。
     *
     * @param current     当前完成量
     * @param total       总量（须 > 0，否则返回 -1.0）
     * @param step        步长（百分比），<= 0 表示每次变化都发布
     * @param lastPercent 上次已发布的进度（0 ~ 100）
     * @return 需要发布的进度（保留两位小数），或 -1.0 表示无需发布
     */
    public static double getNextPercent(double current, double total, double step, double lastPercent) {
        if (total <= 0) {
            return -1.0;
        }

        // 当前值超过总数时限制为总数
        double currentClamped = Math.min(current, total);

        // 计算当前百分比（保留两位小数）
        double percent = Math.round(Math.min(currentClamped / total * 100, 100.0) * 100.0) / 100.0;

        // 判断是否需要更新
        boolean shouldUpdate;
        if (step <= 0) {
            // 步长<=0：进度有变化就更新
            shouldUpdate = (Math.abs(percent - lastPercent) > 0.001);
        } else if (Math.abs(percent - 0.0) < 0.001 || Math.abs(percent - 100.0) < 0.001) {
            // 特殊处理边界：0%和100%必须更新
            shouldUpdate = (Math.abs(percent - lastPercent) > 0.001);
        } else {
            // 按步长更新：计算当前在第几个步长区间
            shouldUpdate = (Math.floor(percent / step) > Math.floor(lastPercent / step));
        }

        return shouldUpdate ? percent : -1.0;
    }

    public static double getUpdatePercentDouble(int current, int total) {
        if (total <= 0) {
            return 0.0;
        }
        double percent = Math.min((double) current / total * 100, 100.0);
        return Math.round(percent * 100.0) / 100.0;
    }


    public static double getUpdatePercentDouble(long current, long total) {
        if (total <= 0) {
            return 0.0;
        }
        double percent = Math.min((double) current / total * 100, 100.0);
        return Math.round(percent * 100.0) / 100.0;
    }


    public static double getUpdatePercentDouble(float current, float total) {
        if (total <= 0) {
            return 0.0;
        }
        double percent = Math.min((double) current / total * 100, 100.0);
        return Math.round(percent * 100.0) / 100.0;
    }

    public static double getUpdatePercentDouble(double current, double total) {
        if (total <= 0) {
            return 0.0;
        }
        double percent = Math.min(current / total * 100, 100.0);
        return Math.round(percent * 100.0) / 100.0;
    }


    public static double getUpdatePercentDouble(Number current, Number total) {
        if (total == null || current == null) {
            return 0.0;
        }
        double totalDouble = total.doubleValue();
        if (totalDouble <= 0) {
            return 0.0;
        }
        double currentDouble = current.doubleValue();
        double percent = Math.min(currentDouble / totalDouble * 100, 100.0);
        return Math.round(percent * 100.0) / 100.0;
    }


    public static String getProgressBar(double percent) {
        return getProgressBar(percent, 50);
    }


    public static String getProgressBar(double percent, int width) {
        if (width <= 0) width = 50;
        int filled = (int) Math.min(Math.round(percent * width / 100.0), width);
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


    public static String getProgressDisplay(double percent) {
        return getProgressBar(percent) + String.format(" %5.2f%%", percent);
    }


    public static String getProgressDisplay(double percent, int width) {
        return getProgressBar(percent, width) + String.format(" %5.2f%%", percent);
    }


    public static int getUpdatePercentInt(int current, int total) {
        if (total <= 0) {
            return 0;
        }
        return (int) Math.min((double) current / total * 100, 100);
    }


    public static int getUpdatePercentInt(long current, long total) {
        if (total <= 0) {
            return 0;
        }
        return (int) Math.min((double) current / total * 100, 100);
    }


    public static int getUpdatePercentInt(float current, float total) {
        if (total <= 0) {
            return 0;
        }
        return (int) Math.min((double) current / total * 100, 100);
    }


    public static int getUpdatePercentInt(double current, double total) {
        if (total <= 0) {
            return 0;
        }
        return (int) Math.min(current / total * 100, 100);
    }


    public static int getUpdatePercentInt(Number current, Number total) {
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


}
