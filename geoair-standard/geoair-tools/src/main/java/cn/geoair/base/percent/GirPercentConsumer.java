package cn.geoair.base.percent;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.base.util.GutilPercent;

/**
 * @author ：张俊
 * @date ：Created in 2026/7/13 11:24
 * @description： 进度消费者，支持 int 和 double 两种进度类型
 */
public class GirPercentConsumer implements GiPercentConsumer {

    private static final GiLogger log = GirLoggerFactory.getLogger();

    public enum PercentType {
        INT,
        DOUBLE
    }

    /** 上次更新的进度（int类型） */
    private int[] intLastPercent = {0};

    /** 上次更新的进度（double类型） */
    private double[] doubleLastPercent = {0.0};

    /** int类型步长 */
    private int intStep;

    /** double类型步长 */
    private double doubleStep;

    /** 进度类型 */
    private PercentType percentType;

    /** 进度更新回调 */
    private GiPercentUpdateConsumer percentUpdateConsumer;

    private boolean started = false;

    /** 记录的总数 */
    private Number totalCount = null;

    /**
     * 构造函数 - 使用int类型进度（默认步长10）
     *
     * @param percentUpdateConsumer 进度更新回调
     */
    public GirPercentConsumer(GiPercentUpdateConsumer percentUpdateConsumer) {
        this.percentType = PercentType.INT;
        this.intStep = GutilPercent.DEFAULT_STEP;
        this.percentUpdateConsumer = percentUpdateConsumer;
    }

    /**
     * 构造函数 - 使用int类型进度（自定义步长）
     *
     * @param step 步长
     * @param percentUpdateConsumer 进度更新回调
     */
    public GirPercentConsumer(int step, GiPercentUpdateConsumer percentUpdateConsumer) {
        this.percentType = PercentType.INT;
        this.intStep = step;
        this.percentUpdateConsumer = percentUpdateConsumer;
    }

    /**
     * 构造函数 - 使用double类型进度（默认步长10.0）
     *
     * @param percentUpdateConsumer 进度更新回调
     * @param isDouble true表示使用double类型
     */
    public GirPercentConsumer(GiPercentUpdateConsumer percentUpdateConsumer, boolean isDouble) {
        this.percentType = isDouble ? PercentType.DOUBLE : PercentType.INT;
        if (isDouble) {
            this.doubleStep = GutilPercent.DEFAULT_STEP_DOUBLE;
        } else {
            this.intStep = GutilPercent.DEFAULT_STEP;
        }
        this.percentUpdateConsumer = percentUpdateConsumer;
    }

    /**
     * 构造函数 - 使用double类型进度（自定义步长）
     *
     * @param step double类型步长
     * @param percentUpdateConsumer 进度更新回调
     */
    public GirPercentConsumer(double step, GiPercentUpdateConsumer percentUpdateConsumer) {
        this.percentType = PercentType.DOUBLE;
        this.doubleStep = step;
        this.percentUpdateConsumer = percentUpdateConsumer;
    }

    /**
     * 构造函数 - 使用double类型进度（自定义步长）
     *
     * @param step int类型步长（会自动转为double）
     * @param percentUpdateConsumer 进度更新回调
     * @param useDouble 是否使用double类型
     */
    public GirPercentConsumer(
            int step, GiPercentUpdateConsumer percentUpdateConsumer, boolean useDouble) {
        if (useDouble) {
            this.percentType = PercentType.DOUBLE;
            this.doubleStep = step;
        } else {
            this.percentType = PercentType.INT;
            this.intStep = step;
        }
        this.percentUpdateConsumer = percentUpdateConsumer;
    }

    @Override
    public void accept(Long allCount, Long currentCount) {
        if (allCount == null || currentCount == null) {
            log.warn("总数或当前数为null，跳过进度更新");
            return;
        }

        if (allCount <= 0) {
            log.warn("总数 <= 0，跳过进度更新");
            return;
        }

        // 第一次调用时自动触发start
        if (!started) {
            doStart(allCount);
        }

        // 检查总数是否变化（如果变化则重新启动）
        if (totalCount != null && !totalCount.equals(allCount)) {
            log.trace("总数发生变化: {} -> {}, 重新启动进度", totalCount, allCount);
            doStart(allCount);
        }

        try {
            switch (percentType) {
                case INT:
                    handleIntProgress(allCount, currentCount);
                    break;
                case DOUBLE:
                    handleDoubleProgress(allCount, currentCount);
                    break;
                default:
                    log.warn("未知的进度类型: {}", percentType);
            }
        } catch (Exception e) {
            log.error("进度更新异常", e);
        }
    }

    /**
     * 执行启动逻辑
     *
     * @param allCount 总数
     */
    private void doStart(Number allCount) {
        if (percentUpdateConsumer != null) {
            percentUpdateConsumer.start(allCount);
            started = true;
            totalCount = allCount;
            log.trace("进度已启动，总数: {}", allCount);
        } else {
            log.warn("进度更新消费者为null，无法启动");
        }
    }

    /**
     * 手动启动进度（可选，用于提前初始化）
     *
     * @param allCount 总数
     */
    public void start(Number allCount) {
        if (allCount == null) {
            log.warn("启动失败：总数为null");
            return;
        }
        if (allCount.doubleValue() <= 0) {
            log.warn("启动失败：总数 <= 0");
            return;
        }
        doStart(allCount);
    }

    /** 处理int类型进度 */
    private void handleIntProgress(Long allCount, Long currentCount) {
        int updatePercent =
                GutilPercent.getUpdatePercentInt(currentCount, allCount, intStep, intLastPercent);
        if (updatePercent != -1) {
            if (percentUpdateConsumer != null) {
                percentUpdateConsumer.update(updatePercent);
            }
            log.trace("进度更新: {}%", updatePercent);
        }
    }

    /** 处理double类型进度 */
    private void handleDoubleProgress(Long allCount, Long currentCount) {
        double updatePercent =
                GutilPercent.getUpdatePercentDouble(
                        currentCount, allCount, doubleStep, doubleLastPercent);
        if (updatePercent != -1.0) {
            if (percentUpdateConsumer != null) {
                percentUpdateConsumer.update(updatePercent);
            }
            log.trace("进度更新: {}%", updatePercent);
        }
    }

    /** 获取当前进度类型 */
    public PercentType getPercentType() {
        return percentType;
    }

    /** 重置进度状态 */
    public void reset() {
        intLastPercent[0] = 0;
        doubleLastPercent[0] = 0.0;
        started = false;
        totalCount = null;
        log.trace("进度状态已重置");
    }

    /**
     * 重置进度状态并重新启动
     *
     * @param allCount 新的总数
     */
    public void resetAndStart(Number allCount) {
        reset();
        start(allCount);
    }

    /** 获取当前int类型进度值（如果当前是int类型） */
    public int getCurrentIntPercent() {
        return percentType == PercentType.INT ? intLastPercent[0] : -1;
    }

    /** 获取当前double类型进度值（如果当前是double类型） */
    public double getCurrentDoublePercent() {
        return percentType == PercentType.DOUBLE ? doubleLastPercent[0] : -1.0;
    }

    /** 是否已启动 */
    public boolean isStarted() {
        return started;
    }

    /** 获取当前总数 */
    public Number getTotalCount() {
        return totalCount;
    }

    /** 判断是否完成（进度达到100%） */
    public boolean isComplete() {
        if (percentType == PercentType.INT) {
            return intLastPercent[0] >= 100;
        } else {
            return doubleLastPercent[0] >= 100.0;
        }
    }
}
