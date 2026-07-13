package cn.geoair.base.percent;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.base.util.GutilPercent;

import java.util.function.Consumer;

/**
 * @author ：张俊
 * @date ：Created in 2026/7/13 11:24
 * @description： 进度的消费者
 */
public class GirDoublePercentConsumer implements GiPercentConsumer {

    GiLogger log = GirLoggerFactory.getLogger();

    /**
     * 上次更新的进度
     */
    double[] lastPercent = {0};

    //    默认步长
    double step = 10;


    Consumer<Double> percentUpdateConsumer;

    public GirDoublePercentConsumer(Double step, Consumer<Double> percentUpdateConsumer) {
        this.step = step;
        this.percentUpdateConsumer = percentUpdateConsumer;
    }

    public GirDoublePercentConsumer(Consumer<Double> percentUpdateConsumer) {
        this(GutilPercent.DEFAULT_STEP_DOUBLE, percentUpdateConsumer);
    }

    @Override

    public void accept(Long allCount, Long currentCount) {
        double updatePercentDouble = GutilPercent.getUpdatePercentDouble(allCount, currentCount, step, lastPercent);
        if (updatePercentDouble != -1) {
            percentUpdateConsumer.accept(updatePercentDouble);
        }

    }

}
