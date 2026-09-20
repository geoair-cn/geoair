package cn.geoair.map.dynamic.statics.mvt.v4.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * V4 相对 V3 新增的能力开关与单机运行时参数。
 *
 * <p>这里只放 <b>V4 真正实现了</b> 的项。没实现的参数不留字段 —— 留了空壳会让
 * "V4 到底具备 tippecanoe 的哪些能力"这件事变得无法判定。各参数的实现位置见
 * {@code V4开发计划.md} 第三节。</p>
 *
 * @author 张逢吉
 */
@Data
@NoArgsConstructor
public class V4Options implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 对应 tippecanoe {@code --preserve-input-order}：保持要素在输入里的先后顺序。
     * <p>V4 读取时给每行打一个全局递增序号，编码前按该序号稳定排序。
     * 关闭时不做这层排序（与 V3 一致，瓦片内要素顺序由聚合过程决定）。</p>
     */
    private boolean preserveInputOrder = false;

    /**
     * 对应 tippecanoe {@code --reorder}：编码前按要素在瓦片内的空间关系重排。
     * <p>让空间上相邻的要素在 PBF 里也相邻，几何增量编码更省字节。</p>
     */
    private boolean reorder = false;

    /**
     * 对应 tippecanoe {@code --hilbert}：按 Hilbert 曲线顺序排列要素。
     * <p>与 {@link #reorder} 同为空间排序，只是序函数换成 Hilbert 曲线索引；
     * 两者同时开启时以 {@code hilbert} 为准。</p>
     */
    private boolean hilbert = false;

    /**
     * 对应 tippecanoe {@code --attribute-type}：强制指定属性类型。
     * <p>键为字段名，值为 {@code int} / {@code long} / {@code double} / {@code boolean} / {@code string}。
     * 在读取阶段完成转换，转换失败时保留原值并累计告警计数，不中断任务。</p>
     */
    private Map<String, String> attributeTypes = new LinkedHashMap<>();

    /**
     * 对应 tippecanoe {@code -T/--no-tile-stats} 的<b>反向</b>开关：是否统计瓦片要素信息。
     *
     * <p>开启时（默认）统计每个图层的要素数、几何类型、字段类型与取值分布，写进任务元数据的
     * {@code tilestats} 段（结构与 V1/V2 写库那套一致，见 {@code TileStatRoot}）。
     * 关闭时既不统计也不写出，同时省掉去重位图与字段计数这两块运行期开销。</p>
     */
    private boolean tileStats = true;

    /**
     * V4 自有参数（非 tippecanoe 参数）：内存中累计多少行后溢写到磁盘。
     * <p>V4 不依赖 Spark 的分区与 spill，靠这个阈值把内存占用钉在可控范围：
     * 达到阈值就把当前缓冲按瓦片键排序落成一个 run 文件并清空内存，
     * 全部读完后对所有 run 做归并，逐瓦片编码。</p>
     */
    private int spillRowThreshold = 1500000;

    /**
     * V4 自有参数：溢写文件目录。留空时使用 JVM 临时目录下的 {@code v4-spill-<随机>}。
     */
    private String spillDirectory;

    /** 深拷贝，避免调用方共享可变状态。 */
    public V4Options copy() {
        V4Options copy = new V4Options();
        copy.preserveInputOrder = this.preserveInputOrder;
        copy.reorder = this.reorder;
        copy.hilbert = this.hilbert;
        copy.attributeTypes = new LinkedHashMap<>(this.attributeTypes);
        copy.tileStats = this.tileStats;
        copy.spillRowThreshold = this.spillRowThreshold;
        copy.spillDirectory = this.spillDirectory;
        return copy;
    }
}
