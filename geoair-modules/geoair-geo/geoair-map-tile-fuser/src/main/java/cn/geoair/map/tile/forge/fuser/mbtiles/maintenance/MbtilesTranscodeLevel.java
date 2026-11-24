package cn.geoair.map.tile.forge.fuser.mbtiles.maintenance;

import cn.geoair.base.data.model.annotation.GaModelField;
import lombok.Data;

/** 各级转码累计量，与总任务一并在缓存库提交。 */
@Data
public class MbtilesTranscodeLevel {
    @GaModelField(text = "级别")
    private int zoom;

    @GaModelField(text = "已处理数量")
    private long processed;

    @GaModelField(text = "已转换数量")
    private long converted;

    @GaModelField(text = "跳过数量")
    private long skipped;

    @GaModelField(text = "失败数量")
    private long failed;

    @GaModelField(text = "处理前瓦片字节")
    private long inputBytes;

    @GaModelField(text = "处理后瓦片字节")
    private long outputBytes;
}
