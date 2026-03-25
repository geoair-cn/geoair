package cn.geoair.map.dynamic.mvt.tools.param;

import cn.hutool.core.bean.BeanUtil;
import lombok.Data;
import lombok.experimental.Accessors;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

/**
 * @author ：张逢吉
 * @date ：Created in 2022/2/3 11:37 @description： 瓦片执行的时候一部分配置信息
 */
@Data
@Accessors(chain = true)
public class TileExecParams {

    // 执行的SQL
    String execSql;

    // 【网格源】瓦片范围
    protected Envelope gridExtent;

    // 【数据源】瓦片范围
    protected Envelope dataExtent;

    // 【数据源】瓦片范围包围盒
    protected Envelope dataExtentBox;

    // 【网格源】 瓦片范围包围盒
    protected Envelope gridExtentBox;

    // 带缓冲区的【网格源】 范围几何体，用于空间查询时避免边界问题
    protected Geometry gridExtentBufferBoxGeom;

    // 带缓冲区的 【网格源】范围 包围盒
    protected Envelope gridExtentBufferEnvelope;

    // 带缓冲区的 【数据源】范围 几何体
    protected Geometry dataExtentBufferBoxGeom;

    // 带缓冲区的 【数据源】范围 包围盒
    protected Envelope dataExtentBufferEnvelope;

    /** 网格的坐标系 */
    int gridSrid;

    /** 数据的坐标系 */
    int sourceDataSrid;

    protected int zoom;

    protected int x;

    protected int y;

    public TileExecParams copy() {
        TileExecParams params = new TileExecParams();
        BeanUtil.copyProperties(this, params);
        return params;
    }
}
