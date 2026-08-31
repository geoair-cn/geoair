package cn.geoair.base.percent;

/**
 * 进度更新监听器。
 *
 * <p>进度开始（{@link #onStart}）与进度更新（{@link #onUpdate}）的生命周期回调， 通常由 {@link GirProgressReporter}
 * 按步长节流后触发，调用方在此实现日志、进度条等展示逻辑。
 */
public interface GiProgressListener {

    /**
     * 进度开始
     *
     * @param total 任务总量
     */
    void onStart(Number total);

    /**
     * 进度更新
     *
     * @param percent 当前进度（0 ~ 100）
     */
    void onUpdate(Number percent);
}
