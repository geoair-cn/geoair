package cn.geoair.map.dynamic.mvt.tools.model;

import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import java.io.Serializable;
import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author ：张逢吉
 * @date ：Created in 2022/1/4 10:50 @description： pbf的信息
 */
@Data
@Accessors(chain = true)
public class PbfInfo implements Serializable {

    /** 主图层的pbf数据信息 */
    byte[] data = new byte[0];

    /** 边界的pbf数据信息（如果有） */
    byte[] dataBoundary = new byte[0];

    /** 标签的pbf数据信息 （如果有） */
    byte[] dataLabel = new byte[0];

    /** 当前pbf经过简化优化只有的要要素信息 ，用于统计，当然，如果不统计的话，就不要保存进来，降低内存 */
    List<GirAdvOneRow> thisPbfFeatureList;

    /** 当前的缩放级别 */
    int zoom;

    /** 当前pbf的网格的坐标系 */
    int gridSrid;
}
