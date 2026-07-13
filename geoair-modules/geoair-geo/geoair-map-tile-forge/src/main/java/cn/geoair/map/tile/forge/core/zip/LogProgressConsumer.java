package cn.geoair.map.tile.forge.core.zip;


import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.base.percent.GiPercentConsumer;
import cn.geoair.base.util.GutilPercent;

/**
 * @author ：张俊
 * @date ：Created in 2026/4/23 14:10
 * @description： TODO
 */
public class LogProgressConsumer implements ProgressConsumer {
    GiLogger log = GirLoggerFactory.getLogger();

    GiPercentConsumer percentConsumerInt = GutilPercent.getPercentConsumerInt(percent -> log.info("当前进度: {}%  {}", percent, GutilPercent.getProgressDisplay(percent)));

    @Override
    public void accept(Long allCount, Long currentCount) {
        percentConsumerInt.accept(allCount, currentCount);
    }
}
