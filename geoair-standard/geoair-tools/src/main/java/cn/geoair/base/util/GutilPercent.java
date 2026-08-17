package cn.geoair.base.util;

/**
 * 进度计算与进度条展示的纯工具类<br>
 * 支持 int、long、float、double、Number 等多种数值类型<br>
 * 本类全部方法均为无状态静态方法，不保存任何成员变量，线程安全，可被并发调用。
 * <p>
 * 方法族语义区别：<br>
 * <pre>
 * 1. getUpdatePercent*（纯计算）：仅将"当前完成量 / 总量"换算为百分比，结果钳制在 0~100，
 *    不涉及步长与发布节奏；
 * 2. getNextPercent（带步长节流的发布判断）：基于上次已发布的进度，仅在跨过步长区间
 *    （或到达 0% / 100% 边界）时返回新进度，否则返回 -1.0 表示无需发布；
 * 3. getProgressBar / getProgressDisplay（展示）：将百分比渲染为进度条字符串或
 *    "进度条 + 百分比"的完整显示字符串。
 * </pre>
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
     * <p>
     * 参数范围约束与负数输入行为：<br>
     * <pre>
     * 1. total 必须 &gt; 0，否则直接返回 -1.0；
     * 2. current 为负数时按 0 处理（钳制下限），超过 total 时按 total 处理（钳制上限）；
     * 3. step 为百分比步长，&lt;= 0 表示进度有变化即发布；
     * 4. lastPercent 应为上次已发布的进度（0 ~ 100），负数或超界值本方法不校验，
     *    仅按 step 区间参与比较，建议调用方自行保证其合法性；
     * 5. 返回 -1.0 的语义：total 非法（&lt;= 0），或本次进度未跨过步长、无需发布。
     * </pre>
     *
     * @param current     当前完成量（负数按 0 处理）
     * @param total       总量（须 > 0，否则返回 -1.0）
     * @param step        步长（百分比），<= 0 表示每次变化都发布
     * @param lastPercent 上次已发布的进度（0 ~ 100）
     * @return 需要发布的进度（保留两位小数，钳制在 0~100），或 -1.0 表示无需发布
     */
    public static double getNextPercent(double current, double total, double step, double lastPercent) {
        if (total <= 0) {
            return -1.0;
        }

        // 当前值为负数时按 0 处理（钳制下限），超过总数时限制为总数
        double currentClamped = Math.min(Math.max(current, 0.0), total);

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

    /**
     * 计算进度百分比（纯计算，无步长节流，节流版本见 {@link #getNextPercent(double, double, double, double)}）
     *
     * @param current 当前完成量（负数按 0 处理）
     * @param total   总量（须 > 0，否则返回 0.0）
     * @return 进度百分比（保留两位小数，钳制在 0~100）
     */
    public static double getUpdatePercentDouble(int current, int total) {
        if (total <= 0) {
            return 0.0;
        }
        double percent = Math.min(Math.max((double) current / total * 100, 0.0), 100.0);
        return Math.round(percent * 100.0) / 100.0;
    }

    /**
     * 计算进度百分比（纯计算，无步长节流，节流版本见 {@link #getNextPercent(double, double, double, double)}）
     *
     * @param current 当前完成量（负数按 0 处理）
     * @param total   总量（须 > 0，否则返回 0.0）
     * @return 进度百分比（保留两位小数，钳制在 0~100）
     */
    public static double getUpdatePercentDouble(long current, long total) {
        if (total <= 0) {
            return 0.0;
        }
        double percent = Math.min(Math.max((double) current / total * 100, 0.0), 100.0);
        return Math.round(percent * 100.0) / 100.0;
    }

    /**
     * 计算进度百分比（纯计算，无步长节流，节流版本见 {@link #getNextPercent(double, double, double, double)}）
     *
     * @param current 当前完成量（负数按 0 处理）
     * @param total   总量（须 > 0，否则返回 0.0）
     * @return 进度百分比（保留两位小数，钳制在 0~100）
     */
    public static double getUpdatePercentDouble(float current, float total) {
        if (total <= 0) {
            return 0.0;
        }
        double percent = Math.min(Math.max((double) current / total * 100, 0.0), 100.0);
        return Math.round(percent * 100.0) / 100.0;
    }

    /**
     * 计算进度百分比（纯计算，无步长节流，节流版本见 {@link #getNextPercent(double, double, double, double)}）
     *
     * @param current 当前完成量（负数按 0 处理）
     * @param total   总量（须 > 0，否则返回 0.0）
     * @return 进度百分比（保留两位小数，钳制在 0~100）
     */
    public static double getUpdatePercentDouble(double current, double total) {
        if (total <= 0) {
            return 0.0;
        }
        double percent = Math.min(Math.max(current / total * 100, 0.0), 100.0);
        return Math.round(percent * 100.0) / 100.0;
    }

    /**
     * 计算进度百分比（纯计算，无步长节流，节流版本见 {@link #getNextPercent(double, double, double, double)}）
     *
     * @param current 当前完成量（负数按 0 处理，null 按 0 处理）
     * @param total   总量（须 > 0，否则返回 0.0，null 返回 0.0）
     * @return 进度百分比（保留两位小数，钳制在 0~100）
     */
    public static double getUpdatePercentDouble(Number current, Number total) {
        if (total == null || current == null) {
            return 0.0;
        }
        double totalDouble = total.doubleValue();
        if (totalDouble <= 0) {
            return 0.0;
        }
        double currentDouble = current.doubleValue();
        double percent = Math.min(Math.max(currentDouble / totalDouble * 100, 0.0), 100.0);
        return Math.round(percent * 100.0) / 100.0;
    }

    /**
     * 获取进度条字符串（默认宽度 50）
     *
     * @param percent 进度百分比（负数按 0 处理，大于 100 按 100 处理）
     * @return 进度条字符串，如 "[██████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░]"
     */
    public static String getProgressBar(double percent) {
        return getProgressBar(percent, 50);
    }

    /**
     * 获取进度条字符串（自定义宽度）
     *
     * @param percent 进度百分比（负数按 0 处理，大于 100 按 100 处理）
     * @param width   进度条宽度（&lt;= 0 时按默认宽度 50 处理）
     * @return 进度条字符串
     */
    public static String getProgressBar(double percent, int width) {
        if (width <= 0) width = 50;
        // 百分比钳制到 0~100，防止 filled 为负导致进度条超宽
        double clamped = Math.min(Math.max(percent, 0.0), 100.0);
        int filled = (int) Math.min(Math.round(clamped * width / 100.0), width);
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
     * 获取完整进度显示（进度条 + 百分比，默认宽度 50）
     *
     * @param percent 进度百分比（负数按 0 处理，大于 100 按 100 处理）
     * @return 进度条与百分比拼接的字符串
     */
    public static String getProgressDisplay(double percent) {
        return getProgressBar(percent) + String.format(" %5.2f%%", percent);
    }

    /**
     * 获取完整进度显示（进度条 + 百分比，自定义宽度）
     *
     * @param percent 进度百分比（负数按 0 处理，大于 100 按 100 处理）
     * @param width   进度条宽度（&lt;= 0 时按默认宽度 50 处理）
     * @return 进度条与百分比拼接的字符串
     */
    public static String getProgressDisplay(double percent, int width) {
        return getProgressBar(percent, width) + String.format(" %5.2f%%", percent);
    }

    /**
     * 计算进度百分比并取整（纯计算，无步长节流，节流版本见 {@link #getNextPercent(double, double, double, double)}）
     *
     * @param current 当前完成量（负数按 0 处理）
     * @param total   总量（须 > 0，否则返回 0）
     * @return 进度百分比整数（钳制在 0~100）
     */
    public static int getUpdatePercentInt(int current, int total) {
        if (total <= 0) {
            return 0;
        }
        return (int) Math.min(Math.max((double) current / total * 100, 0.0), 100);
    }

    /**
     * 计算进度百分比并取整（纯计算，无步长节流，节流版本见 {@link #getNextPercent(double, double, double, double)}）
     *
     * @param current 当前完成量（负数按 0 处理）
     * @param total   总量（须 > 0，否则返回 0）
     * @return 进度百分比整数（钳制在 0~100）
     */
    public static int getUpdatePercentInt(long current, long total) {
        if (total <= 0) {
            return 0;
        }
        return (int) Math.min(Math.max((double) current / total * 100, 0.0), 100);
    }

    /**
     * 计算进度百分比并取整（纯计算，无步长节流，节流版本见 {@link #getNextPercent(double, double, double, double)}）
     *
     * @param current 当前完成量（负数按 0 处理）
     * @param total   总量（须 > 0，否则返回 0）
     * @return 进度百分比整数（钳制在 0~100）
     */
    public static int getUpdatePercentInt(float current, float total) {
        if (total <= 0) {
            return 0;
        }
        return (int) Math.min(Math.max((double) current / total * 100, 0.0), 100);
    }

    /**
     * 计算进度百分比并取整（纯计算，无步长节流，节流版本见 {@link #getNextPercent(double, double, double, double)}）
     *
     * @param current 当前完成量（负数按 0 处理）
     * @param total   总量（须 > 0，否则返回 0）
     * @return 进度百分比整数（钳制在 0~100）
     */
    public static int getUpdatePercentInt(double current, double total) {
        if (total <= 0) {
            return 0;
        }
        return (int) Math.min(Math.max(current / total * 100, 0.0), 100);
    }

    /**
     * 计算进度百分比并取整（纯计算，无步长节流，节流版本见 {@link #getNextPercent(double, double, double, double)}）
     *
     * @param current 当前完成量（负数按 0 处理，null 按 0 处理）
     * @param total   总量（须 > 0，否则返回 0，null 返回 0）
     * @return 进度百分比整数（钳制在 0~100）
     */
    public static int getUpdatePercentInt(Number current, Number total) {
        if (total == null || current == null) {
            return 0;
        }
        double totalDouble = total.doubleValue();
        if (totalDouble <= 0) {
            return 0;
        }
        double currentDouble = current.doubleValue();
        return (int) Math.min(Math.max(currentDouble / totalDouble * 100, 0.0), 100);
    }

    /**
     * 获取进度条字符串（默认宽度 50）
     *
     * @param percent 进度百分比（负数按 0 处理，大于 100 按 100 处理）
     * @return 进度条字符串，如 "[██████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░]"
     */
    public static String getProgressBar(int percent) {
        return getProgressBar(percent, 50);
    }

    /**
     * 获取进度条字符串（自定义宽度）
     *
     * @param percent 进度百分比（负数按 0 处理，大于 100 按 100 处理）
     * @param width   进度条宽度（&lt;= 0 时按默认宽度 50 处理）
     * @return 进度条字符串
     */
    public static String getProgressBar(int percent, int width) {
        if (width <= 0) width = 50;
        // 百分比钳制到 0~100，防止 filled 为负导致进度条超宽
        int clamped = Math.min(Math.max(percent, 0), 100);
        int filled = Math.min(clamped * width / 100, width);
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
     * 获取完整进度显示（进度条 + 百分比，默认宽度 50）
     *
     * @param percent 进度百分比（负数按 0 处理，大于 100 按 100 处理）
     * @return 进度条与百分比拼接的字符串
     */
    public static String getProgressDisplay(int percent) {
        return getProgressBar(percent) + String.format(" %3d%%", percent);
    }

    /**
     * 获取完整进度显示（进度条 + 百分比，自定义宽度）
     *
     * @param percent 进度百分比（负数按 0 处理，大于 100 按 100 处理）
     * @param width   进度条宽度（&lt;= 0 时按默认宽度 50 处理）
     * @return 进度条与百分比拼接的字符串
     */
    public static String getProgressDisplay(int percent, int width) {
        return getProgressBar(percent, width) + String.format(" %3d%%", percent);
    }
}