package cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3;

import cn.geoair.base.util.GutilObject;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.DataSourceConfig;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.TileSliceParameter;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.codec.Base32;
import cn.hutool.core.io.unit.DataSizeUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.URLUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * V3 多图层静态 MVT 切片任务参数。
 * <p>
 * 一条输出记录代表一个瓦片集合（{@link #tileSetName}）中的一个 XYZ 瓦片，
 * 其 {@code tile_data} 是包含多个 MVT 内部 layer 的单个 PBF。该参数模型与
 * V1/V2 的单图层参数完全隔离。
 *
 * @author 张逢吉
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class MultiLayerTileSliceParameter implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 写入瓦片缓存表的数据源。
     */
    private DataSourceConfig outputSource;

    /**
     * 输出记录的图层集合名称，同时写入 layer_name 字段。
     */
    private String tileSetName;

    /**
     * 输出版本号。
     */
    private String edition = "";

    /**
     * 输出瓦片网格 SRID，目前支持 3857 / 4326 等既有网格。
     */
    private int outGridSrid = 3857;

    /**
     * 任务默认最小切片级别。
     */
    private int minZoom = 4;

    /**
     * 任务默认最大切片级别。
     */
    private int maxZoom = 15;

    /**
     * 单个最终 PBF 的总大小限制是否启用。
     */
    private boolean tileSizeLimitEnabled = true;

    /**
     * 单个最终 PBF 的总大小限制，例如 2MB。
     */
    private String tileSizeLimit = "2MB";

    /**
     * Spark reduce 分区数。
     */
    private int reducePartitionNum = 3000;

    /**
     * 当前任务追踪号。
     */
    private String trackId = IdUtil.fastSimpleUUID();

    /**
     * 要写入同一个 PBF 的内部图层配置。
     */
    private List<MvtLayerSliceParameter> layers = new ArrayList<>();

    /**
     * 将人类可读大小转换为字节数。
     *
     * @return 未配置限制时返回 null
     */
    public Long getTileSizeLimitByte() {
        return tileSizeLimit == null ? null : DataSizeUtil.parse(tileSizeLimit);
    }


    public void clearLayers() {
        layers.clear();
    }

    public void addLayers(MvtLayerSliceParameter mvtLayerSliceParameter) {
        for (MvtLayerSliceParameter layer : layers) {
            String layerName = layer.getLayerName();
            if (layerName.equals(mvtLayerSliceParameter.getLayerName())) {
                return;
            }
        }
        layers.add(mvtLayerSliceParameter);
    }

    /**
     * 将任务参数编码为适合命令行或任务传递的 Base32 字符串。
     */
    public String toBase32() {
        String json = JSON.toJSONString(this);
        JSONObject object = JSON.parseObject(json);
        object.entrySet().removeIf(entry -> ObjectUtil.isEmpty(entry.getValue()));
        return Base32.encode(object.toString());
    }

    /**
     * 从 {@link #toBase32()} 的结果还原 V3 参数。
     */
    public static MultiLayerTileSliceParameter fromBase32(String baseString) {
        try {
            String json = Base32.decodeStr(URLUtil.decode(baseString));
            return JSON.parseObject(json, MultiLayerTileSliceParameter.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("无法解析 MultiLayerTileSliceParameter Base32 参数", e);
        }
    }

    /**
     * 深拷贝当前参数对象。
     */
    public MultiLayerTileSliceParameter copy() {
        MultiLayerTileSliceParameter copy = new MultiLayerTileSliceParameter();
        BeanUtil.copyProperties(this, copy);
        copy.clearLayers();
        List<MvtLayerSliceParameter> originLayers = getLayers();
        if (GutilObject.isNotEmpty(originLayers)) {
            for (MvtLayerSliceParameter originLayer : originLayers) {
                copy.addLayers(originLayer.copy());
            }
        }
        return copy;
    }

}
