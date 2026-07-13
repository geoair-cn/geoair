package cn.geoair.base.percent;

/**
 * 进度更新的消费者
 */

public interface GiPercentUpdateConsumer {

    void start(Number allCount);

    void update(Number updatePercent);


}
