package cn.geoair.map.tile.forge.core.zip;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.base.percent.GiPercentUpdateConsumer;
import cn.geoair.base.percent.GirPercentConsumer;
import cn.geoair.base.util.GutilPercent;

/**
 * @author ：张俊
 * @date ：Created in 2026/4/23 14:10
 * @description： TODO
 */
public class LogProgressConsumer implements ProgressConsumer {
    GiLogger log = GirLoggerFactory.getLogger();

    GirPercentConsumer percentConsumerInt =
            GutilPercent.getPercentConsumerInt(
                    new GiPercentUpdateConsumer() {
                        @Override
                        public void start(Number allCount) {}

                        @Override
                        public void update(Number updatePercent) {
                            log.info(
                                    "当前进度: {}%  {}",
                                    updatePercent,
                                    GutilPercent.getProgressDisplay(updatePercent.doubleValue()));
                        }
                    },
                    1);

    @Override
    public void accept(Long allCount, Long currentCount) {
        percentConsumerInt.accept(allCount, currentCount);
    }
}
