package cn.geoair.base.util;

import cn.geoair.base.percent.GiPercentUpdateConsumer;

import cn.geoair.base.percent.GirPercentConsumer;

/**
 * 进度条工具类
 * 支持 int, long, float, double 等多种数值类型
 */
public class GutilPercent {

    // 默认步长：每10%更新一次
    public static final int DEFAULT_STEP = 10;
    public static final double DEFAULT_STEP_DOUBLE = 10.0;


    public static double getUpdatePercentDouble(Number current, Number total, double[] lastPercent) {
        return getUpdatePercentDouble(current, total, DEFAULT_STEP_DOUBLE, lastPercent);
    }


    public static double getUpdatePercentDouble(Number current, Number total, double step, double[] lastPercent) {
        // 参数校验
        if (total == null || current == null || lastPercent == null || lastPercent.length == 0) {
            return -1.0;
        }

        double totalDouble = total.doubleValue();
        if (totalDouble <= 0) {
            return -1.0;
        }

        double currentDouble = current.doubleValue();
        // 如果当前值超过总数，限制为总数
        if (currentDouble > totalDouble) {
            currentDouble = totalDouble;
        }

        // 计算当前百分比（保留两位小数）
        double percent = Math.min(currentDouble / totalDouble * 100, 100.0);
        // 四舍五入保留两位小数
        percent = Math.round(percent * 100.0) / 100.0;

        // 判断是否需要更新
        boolean shouldUpdate = false;

        if (step <= 0) {
            // 步长<=0：每次都更新
            shouldUpdate = (Math.abs(percent - lastPercent[0]) > 0.001);
        } else {
            // 特殊处理边界：0%和100%必须更新
            if (Math.abs(percent - 0.0) < 0.001 || Math.abs(percent - 100.0) < 0.001) {
                shouldUpdate = (Math.abs(percent - lastPercent[0]) > 0.001);
            } else {
                // 按步长更新：计算当前在第几个步长区间
                double currentStep = Math.floor(percent / step);
                double lastStep = Math.floor(lastPercent[0] / step);
                shouldUpdate = (currentStep > lastStep);
            }
        }

        // 需要更新则保存并返回
        if (shouldUpdate) {
            lastPercent[0] = percent;
            return percent;
        }

        return -1.0;
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


    public static double getUpdatePercentDouble(int current, int total, double[] lastPercent) {
        return getUpdatePercentDouble((Number) current, (Number) total, DEFAULT_STEP_DOUBLE, lastPercent);
    }

    public static double getUpdatePercentDouble(int current, int total, double step, double[] lastPercent) {
        return getUpdatePercentDouble((Number) current, (Number) total, step, lastPercent);
    }

    public static double getUpdatePercentDouble(long current, long total, double[] lastPercent) {
        return getUpdatePercentDouble((Number) current, (Number) total, DEFAULT_STEP_DOUBLE, lastPercent);
    }

    public static double getUpdatePercentDouble(long current, long total, double step, double[] lastPercent) {
        return getUpdatePercentDouble((Number) current, (Number) total, step, lastPercent);
    }

    public static double getUpdatePercentDouble(float current, float total, double[] lastPercent) {
        return getUpdatePercentDouble((Number) current, (Number) total, DEFAULT_STEP_DOUBLE, lastPercent);
    }

    public static double getUpdatePercentDouble(float current, float total, double step, double[] lastPercent) {
        return getUpdatePercentDouble((Number) current, (Number) total, step, lastPercent);
    }

    public static double getUpdatePercentDouble(double current, double total, double[] lastPercent) {
        return getUpdatePercentDouble((Number) current, (Number) total, DEFAULT_STEP_DOUBLE, lastPercent);
    }

    public static double getUpdatePercentDouble(double current, double total, double step, double[] lastPercent) {
        return getUpdatePercentDouble((Number) current, (Number) total, step, lastPercent);
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


    public static GirPercentConsumer getPercentConsumerDouble(GiPercentUpdateConsumer percentUpdateConsumer, double step) {
        return new GirPercentConsumer(step, percentUpdateConsumer);
    }

    public static GirPercentConsumer getPercentConsumerDouble(GiPercentUpdateConsumer percentUpdateConsumer) {
        return new GirPercentConsumer(percentUpdateConsumer);
    }


    public static int getUpdatePercentInt(Number current, Number total, int[] lastPercent) {
        return getUpdatePercentInt(current, total, DEFAULT_STEP, lastPercent);
    }


    public static int getUpdatePercentInt(Number current, Number total, int step, int[] lastPercent) {
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


    public static int getUpdatePercentInt(int current, int total, int[] lastPercent) {
        return getUpdatePercentInt((Number) current, (Number) total, DEFAULT_STEP, lastPercent);
    }

    public static int getUpdatePercentInt(int current, int total, int step, int[] lastPercent) {
        return getUpdatePercentInt((Number) current, (Number) total, step, lastPercent);
    }

    public static int getUpdatePercentInt(long current, long total, int[] lastPercent) {
        return getUpdatePercentInt((Number) current, (Number) total, DEFAULT_STEP, lastPercent);
    }

    public static int getUpdatePercentInt(long current, long total, int step, int[] lastPercent) {
        return getUpdatePercentInt((Number) current, (Number) total, step, lastPercent);
    }

    public static int getUpdatePercentInt(float current, float total, int[] lastPercent) {
        return getUpdatePercentInt((Number) current, (Number) total, DEFAULT_STEP, lastPercent);
    }

    public static int getUpdatePercentInt(float current, float total, int step, int[] lastPercent) {
        return getUpdatePercentInt((Number) current, (Number) total, step, lastPercent);
    }

    public static int getUpdatePercentInt(double current, double total, int[] lastPercent) {
        return getUpdatePercentInt((Number) current, (Number) total, DEFAULT_STEP, lastPercent);
    }

    public static int getUpdatePercentInt(double current, double total, int step, int[] lastPercent) {
        return getUpdatePercentInt((Number) current, (Number) total, step, lastPercent);
    }


    public static GirPercentConsumer getPercentConsumerInt(GiPercentUpdateConsumer percentUpdateConsumer, int step) {
        return new GirPercentConsumer(step, percentUpdateConsumer);
    }

    public static GirPercentConsumer getPercentConsumerInt(GiPercentUpdateConsumer percentUpdateConsumer) {
        return new GirPercentConsumer(percentUpdateConsumer);
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
