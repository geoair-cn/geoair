package cn.geoair.map.dynamic.mvt.tools.model;


import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import org.locationtech.jts.geom.Coordinate;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/12/29 10:53
 * @description： FeatureWithGrid
 */
public class FeatureWithGrid {
    private GirAdvOneRow feature;
    private String gridId;
    private Coordinate center;

    public FeatureWithGrid(GirAdvOneRow feature, String gridId, Coordinate center) {
        this.feature = feature;
        this.gridId = gridId;
        this.center = center;
    }

    public GirAdvOneRow getFeature() {
        return feature;
    }

    public String getGridId() {
        return gridId;
    }

    public Coordinate getCenter() {
        return center;
    }
}
