package cn.geoair.map.tile.forge.core.zip;

import cn.geoair.base.percent.GiProgressReporter;

/**
 * 进度条的消费者

 */
@FunctionalInterface
public interface ProgressConsumer  extends GiProgressReporter {

    void report(Long allCount, Long currentCount);
}
