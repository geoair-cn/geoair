package cn.geoair.map.tile.forge.core.zip;


import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.base.util.GutilPercent;

/**
 * @author ：张俊
 * @date ：Created in 2026/4/23 14:10
 * @description： TODO
 */
public class LogProgressConsumer implements ProgressConsumer {
    GiLogger log = GirLoggerFactory.getLogger();
    int[] lastPercent = {0};

    @Override
    public void accept(Long allCount, Long currentCount) {
        int percent = GutilPercent.getUpdatePercent(currentCount, allCount, 1, lastPercent);
        if (percent != -1) {
            log.info("当前进度: {}%  {}",
                    percent, GutilPercent.getProgressDisplay(percent));

        }
    }
}
