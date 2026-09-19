package cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3;

import cn.hutool.core.bean.BeanUtil;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * V3 多图层任务中的单个 MVT 内部图层配置。
 * <p>
 * 该对象只供 V3 使用，不会修改或复用 V1/V2 的 {@code TileSliceParameter} 参数语义。
 * 同一任务中的每个图层都可以来自不同数据源、采用不同查询和读取策略。
 *
 * @author 张逢吉
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class MvtLayerSliceParameter implements Serializable {

    private static final long serialVersionUID = 1L;

    /** MVT PBF 内部图层名称，必须在一个任务内唯一。 */
    private String layerName;

    /** 本图层的数据输入配置，可选择 JDBC 或 GeoJSON。 */
    private V3LayerInputConfig inputConfig;

    /** 几何字段名称。 */
    private String geomFieldName = "geometry";

    /** 唯一标识字段名称。 */
    private String idFieldName;

    /**
     * 源数据 SRID。未设置时 JDBC 默认使用 3857，GeoJSON 按 RFC 7946 默认使用 4326。
     */
    private Integer sourceDataSrid;

    /** 最小切片级别；为空时继承任务级别。 */
    private Integer minZoom;

    /** 最大切片级别；为空时继承任务级别。 */
    private Integer maxZoom;

    /** 输出属性字段；为空时保留除几何字段外的全部属性。 */
    private List<String> includeFields = new ArrayList<>();

    /**
     * 需要排除的输出属性字段，优先级高于 {@link #includeFields}。
     * <p>对应 tippecanoe 的 {@code -x/--exclude}。</p>
     */
    private List<String> excludeFields = new ArrayList<>();

    /** 始终保留的系统字段。 */
    private Set<String> sysIncludeFields = new HashSet<>();

    /** 是否开启单瓦片的本图层要素数量限制。 */
    private boolean featureLimitEnabled = false;

    /** 单瓦片内本图层最多保留的要素数。 */
    private Integer featureLimit;

    /**
     * 本图层在 Spark 聚合阶段的强制安全上限，默认 8000。
     * <p>设为 null 或小于等于 0 可关闭该兜底；正常情况下不建议关闭，以免低层级高密度瓦片造成 executor OOM。</p>
     */
    private Integer hardFeatureLimit = 8000;

    /** 超限时是否优先合并高密度区域的要素。 */
    private boolean coalesceDensestAsNeeded = true;

    /** 超限时是否丢弃高密度区域的要素。 */
    private boolean dropDensestAsNeeded = true;

    /** 几何简化等级，0 或 null 表示不额外简化。对应 tippecanoe 的 {@code -D/--simplification}。 */
    private Integer simplificationLevel;

    /**
     * 是否对线状几何做简化。
     * <p>对应 tippecanoe 的 {@code -ps/--no-line-simplification}（本字段为其反向开关）。</p>
     */
    private boolean simplifyLines = true;

    /**
     * 简化时是否保留与其它要素共享的节点。
     * <p>开启时使用拓扑保持简化，关闭时逐要素独立简化（更快但可能产生缝隙）。
     * 对应 tippecanoe 的 {@code -pS/--no-simplification-of-shared-nodes}（本字段为其反向开关）。</p>
     */
    private boolean preserveSharedNodes = true;

    /**
     * 空间合并距离（屏幕单位，瓦片边长按 4096 计）。
     * <p>大于 0 时，本图层的点要素若相互距离小于该值，会在编码前合并为一个多点要素。
     * 对应 tippecanoe 的 {@code -g/--cluster-distance}。</p>
     */
    private Integer coalesceDistance;

    /** PBF 超限时的保留优先级，数值越大越优先保留。 */
    private int priority = 0;

    /** 写入 PBF 时采用的几何表达方式。 */
    private MvtLayerGeometryMode geometryMode = MvtLayerGeometryMode.ORIGINAL;

    /**
     * 单个 PBF 因总大小超限需要裁剪要素时，是否优先丢弃屏幕占用最小的要素。
     * <p>关闭时按数据读取顺序裁剪。对应 tippecanoe 的 {@code --drop-smallest-as-needed}。</p>
     */
    private boolean dropSmallestAsNeeded = false;

    /**
     * 是否为要素生成 MVT 特征 id。
     * <p>开启后，本图层的每个要素都会被写入一个瓦片内唯一的递增 id，
     * 供客户端做要素点击、高亮等操作。对应 tippecanoe 的 {@code -ai/--generate-ids}。</p>
     */
    private boolean generateIds = false;

    /** 返回当前输入类型对应的有效源数据 SRID。 */
    public int resolveSourceDataSrid() {
        if (sourceDataSrid != null) {
            return sourceDataSrid;
        }
        return inputConfig != null && inputConfig.getInputType() == V3TileInputType.GEOJSON ? 4326 : 3857;
    }



    /**
     * 深拷贝当前参数对象。
     */
    public MvtLayerSliceParameter copy() {
        MvtLayerSliceParameter copy = new MvtLayerSliceParameter();

        BeanUtil.copyProperties(this, copy);

        return copy;
    }
}
