package cn.geoair.map.dynamic.file.core.transfer;

import cn.geoair.map.dynamic.file.core.tran.model.TranProgress;

/**
 * 传输进度回调。
 *
 * @author 张逢吉
 */
@FunctionalInterface
public interface GeoTransferProgressListener {

    /** 接收一次批次完成后的进度快照。 */
    void onProgress(TranProgress progress);
}
