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
public class GirIntPercentConsumer implements GiPercentConsumer {

    GiLogger log = GirLoggerFactory.getLogger();

    /**
     * 上次更新的进度
     */
    int[] lastPercent = {0};

    //    默认步长
    int step = 10;


    Consumer<Integer> percentUpdateConsumer;

    public GirIntPercentConsumer(Integer step, Consumer<Integer> percentUpdateConsumer) {
        this.step = step;
        this.percentUpdateConsumer = percentUpdateConsumer;
    }

    public GirIntPercentConsumer(Consumer<Integer> percentUpdateConsumer) {
        this(GutilPercent.DEFAULT_STEP, percentUpdateConsumer);
    }

    @Override

    public void accept(Long allCount, Long currentCount) {
        int updatePercentInt = GutilPercent.getUpdatePercentInt(allCount, currentCount, step, lastPercent);
        if (updatePercentInt != -1) {
            percentUpdateConsumer.accept(updatePercentInt);
        }

    }

}
