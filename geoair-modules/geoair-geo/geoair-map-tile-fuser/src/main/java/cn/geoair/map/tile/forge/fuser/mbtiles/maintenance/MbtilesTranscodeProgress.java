package cn.geoair.map.tile.forge.fuser.mbtiles.maintenance;

import cn.geoair.base.data.model.annotation.GaModelField;

import lombok.Data;

import java.util.Map;
import java.util.TreeMap;

/** 真实累计量与阶段结果；总数未知时保持 null。 */
@Data
public class MbtilesTranscodeProgress {
    private String sourceIdentity;
    private String optionsFingerprint;
    private boolean complete;

    @GaModelField(text = "已确认总数")
    private Long total;

    @GaModelField(text = "已处理数量")
    private long processed;

    @GaModelField(text = "已检查数量")
    private long checked;

    @GaModelField(text = "已转换数量")
    private long converted;

    @GaModelField(text = "跳过数量")
    private long skipped;

    @GaModelField(text = "透明保护数量")
    private long transparent;

    @GaModelField(text = "格式匹配数量")
    private long matching;

    @GaModelField(text = "无收益数量")
    private long noSaving;

    @GaModelField(text = "并发冲突数量")
    private long conflicts;

    @GaModelField(text = "失败数量")
    private long failed;

    @GaModelField(text = "处理前瓦片字节")
    private long inputBytes;

    @GaModelField(text = "处理后瓦片字节")
    private long outputBytes;

    @GaModelField(text = "恢复级别")
    private int cursorZoom = -1;

    @GaModelField(text = "恢复列号")
    private int cursorX = -1;

    @GaModelField(text = "恢复库内行号")
    private int cursorRow = -1;

    @GaModelField(text = "截止级别")
    private Integer endZoom;

    @GaModelField(text = "截止列号")
    private Integer endX;

    @GaModelField(text = "截止库内行号")
    private Integer endRow;

    @GaModelField(text = "处理说明")
    private String detail;

    @GaModelField(text = "最后进展时间戳")
    private long lastProgressAt;

    @GaModelField(text = "各级处理结果")
    private Map<Integer, MbtilesTranscodeLevel> levels = new TreeMap<>();
}
