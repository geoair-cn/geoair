package cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * V3 GeoJSON 图层输入配置。
 *
 * <p>路径交给 Spark/Hadoop 文件系统解析，可使用 {@code file:///}、{@code hdfs://}、
 * {@code s3a://} 以及文件通配符。集群运行时，本地路径必须对所有 executor 可见；对象存储
 * 凭证应通过 Hadoop/Spark 配置提供，不应写入路径。</p>
 *
 * @author 张逢吉
 */
@Data
@Accessors(chain = true)
public class V3GeoJsonInputConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 一个或多个 GeoJSON 文件、目录或通配路径。 */
    private List<String> paths = new ArrayList<>();

    /** 文件组织方式。 */
    private V3GeoJsonMode mode = V3GeoJsonMode.AUTO;

    /** Spark 读取阶段期望的最小分区数。 */
    private Integer minPartitionNum = 20;

    /** 是否跳过格式错误、缺少几何或无法解析的单条 Feature。 */
    private boolean skipInvalidFeature;

    /** 使用单个路径创建配置。 */
    public static V3GeoJsonInputConfig of(String path) {
        return new V3GeoJsonInputConfig().setPaths(new ArrayList<>(Arrays.asList(path)));
    }

    /** 使用多个路径创建配置。 */
    public static V3GeoJsonInputConfig of(String... paths) {
        return new V3GeoJsonInputConfig().setPaths(
                paths == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(paths)));
    }
}
