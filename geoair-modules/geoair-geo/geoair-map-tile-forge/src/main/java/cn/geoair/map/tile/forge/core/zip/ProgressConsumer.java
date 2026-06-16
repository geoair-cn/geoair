package cn.geoair.map.tile.forge.core.zip;

/**
 * 进度条的消费者

 */
@FunctionalInterface
public interface ProgressConsumer  {

    void accept(Long allCount, Long currentCount);
}
