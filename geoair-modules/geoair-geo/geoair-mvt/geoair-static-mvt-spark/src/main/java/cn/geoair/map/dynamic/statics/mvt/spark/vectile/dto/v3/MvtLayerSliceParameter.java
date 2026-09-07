package cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3;

import cn.geoair.map.dynamic.statics.mvt.spark.vectile.ReadStrategy;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.DataSourceConfig;
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

    /** 本图层的数据输入源。 */
    private DataSourceConfig inputSource;

    /** 几何字段名称。 */
    private String geomFieldName;

    /** 唯一标识字段名称。 */
    private String idFieldName;

    /** 查询语句。 */
    private String queryStatement;

    /** 源数据的 SRID，默认 Web Mercator。 */
    private int sourceDataSrid = 3857;

    /** 数据读取策略。 */
    private ReadStrategy readStrategy = ReadStrategy.ID_PAGE;

    /** 最大读取分区数；为空时使用 V3 的默认值。 */
    private Integer maxPartionNum = 20;

    /** 最小切片级别；为空时继承任务级别。 */
    private Integer minZoom;

    /** 最大切片级别；为空时继承任务级别。 */
    private Integer maxZoom;

    /** 输出属性字段；为空时保留除几何字段外的全部属性。 */
    private List<String> includeFields = new ArrayList<>();

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

    /** 几何简化等级，0 或 null 表示不额外简化。 */
    private Integer simplificationLevel;

    /** 空间合并距离（像素），供后续密度策略扩展使用。 */
    private Integer coalesceDistance;

    /** PBF 超限时的保留优先级，数值越大越优先保留。 */
    private int priority = 0;

    /** 写入 PBF 时采用的几何表达方式。 */
    private MvtLayerGeometryMode geometryMode = MvtLayerGeometryMode.ORIGINAL;



    /**
     * 深拷贝当前参数对象。
     */
    public MvtLayerSliceParameter copy() {
        MvtLayerSliceParameter copy = new MvtLayerSliceParameter();

        BeanUtil.copyProperties(this, copy);

        return copy;
    }
}
