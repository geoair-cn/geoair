package cn.geoair.map.dynamic.mvt.tools.test;

import cn.geoair.map.dynamic.mvt.tools.PipelineBuilder;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

/** PipelineBuilder 示例 */
public class PipelineBuilderExample {

    public static void main(String[] args) throws Exception {
        Envelope extent = new Envelope(116.35, 116.55, 39.85, 40.05);
        PipelineBuilder builder = PipelineBuilder.newBuilder(extent, 4326);

        GeometryFactory factory = new GeometryFactory();
        Geometry line =
                factory.createLineString(
                        new Coordinate[] {
                            new Coordinate(116.40, 39.90),
                            new Coordinate(116.45, 39.93),
                            new Coordinate(116.50, 39.97)
                        });

        Geometry transformed = builder.transform(line);
        Geometry simplified = builder.simplify(transformed);

        System.out.println("source = " + line);
        System.out.println("transformed = " + transformed);
        System.out.println("simplified = " + simplified);
    }
}
