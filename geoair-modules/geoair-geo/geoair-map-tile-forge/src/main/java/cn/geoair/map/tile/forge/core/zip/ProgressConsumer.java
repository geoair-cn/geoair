package cn.geoair.map.tile.forge.core.zip;

import cn.geoair.base.percent.GiPercentConsumer;

/**
 * 进度条的消费者

 */
@FunctionalInterface
public interface ProgressConsumer  extends GiPercentConsumer {

    void accept(Long allCount, Long currentCount);
}
