package cn.geoair.base.percent;

/**
 * 进度条的消费者
 */
@FunctionalInterface
public interface GiPercentConsumer {

    void accept(Long allCount, Long currentCount);
}
