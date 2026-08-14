package cn.geoair.base.percent;

import java.io.Serializable;

/**
 * 进度条的消费者
 */
@FunctionalInterface
public interface GiPercentConsumer extends Serializable {

    void accept(Long allCount, Long currentCount);
}
