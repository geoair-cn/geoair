/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package cn.geoair.map.dynamic.mvt.tools;


import java.awt.*;
import java.awt.geom.AffineTransform;

import cn.geoair.map.dynamic.tools.GirGeoTools;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.operation.transform.ConcatenatedTransform;
import org.geotools.referencing.operation.transform.ProjectiveTransform;
import org.geotools.renderer.lite.RendererUtilities;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.MathTransform2D;
import org.geotools.api.referencing.operation.TransformException;

/** 从geoserver里面抄过来的 ,空间坐标转屏幕坐标 */
public class PipelineBuilder {

    // The base simplification tolerance for screen coordinates.
    private static final double PIXEL_BASE_SAMPLE_SIZE = 0.25;

    static class Context {

        // MathTransform sourceToTargetCrs;

        MathTransform targetToScreen;

        // MathTransform sourceToScreen;

        ReferencedEnvelope renderingArea; // WMS request; bounding box - in final map
        // (target) CRS (BBOX from

        // WMS)

        Rectangle paintArea; // WMS request; rectangle of the image (width and height from
        // WMS)

        public CoordinateReferenceSystem sourceCrs; // data's CRS

        public AffineTransform worldToScreen;

        public double screenSimplificationDistance;

        public double pixelSizeInTargetCRS; // approximate size of a pixel in the Target
        // CRS

        public int queryBuffer;
    }

    Context context;

    CoordinateReferenceSystem sourceCrs;

    public static PipelineBuilder newBuilder(Envelope extent, int srid) throws FactoryException {
        Double xmin = extent.getMinX();
        Double xmax = extent.getMaxX();
        Double ymin = extent.getMinY();
        Double ymax = extent.getMaxY();
        CoordinateReferenceSystem sourceCrs = GirGeoTools.defaultInstance().getSridOpt().getCRS(srid);
        Rectangle paintArea = new Rectangle(0, 0, 4096, 4096);
        ReferencedEnvelope mapArea = new ReferencedEnvelope(xmin, xmax, ymin, ymax, sourceCrs);
        Context context = new Context();
        context.renderingArea = mapArea;
        context.paintArea = paintArea;
        context.sourceCrs = sourceCrs;
        context.worldToScreen =
                RendererUtilities.worldToScreenTransform(mapArea, paintArea); // 边界框与画布转换的算子
        context.screenSimplificationDistance = PIXEL_BASE_SAMPLE_SIZE / 2.0;
        context.queryBuffer = 96;

        context.targetToScreen = ProjectiveTransform.create(context.worldToScreen); // 边界框与屏幕坐标转换的算子
        // context.sourceToScreen =
        // ConcatenatedTransform.create(context.sourceToTargetCrs,
        // context.targetToScreen); // 源往屏幕坐标转换

        PipelineBuilder pipelineBuilder = new PipelineBuilder();
        pipelineBuilder.context = context;
        return pipelineBuilder;
    }

    public Geometry transform(Geometry geom) {
        final MathTransform sourceToScreen = context.targetToScreen;
        final MathTransform tx = sourceToScreen;
        try {
            Geometry transformed = JTS.transform(geom, tx);
            return transformed;
        } catch (TransformException e) {

        }
        return geom;
    }

    public Geometry simplify(Geometry geom) {
        double screenSimplificationDistance = context.screenSimplificationDistance;
        switch (geom.getDimension()) {
            case 2:
                return TopologyPreservingSimplifier.simplify(geom, screenSimplificationDistance);
            case 1:
                return DouglasPeuckerSimplifier.simplify(geom, screenSimplificationDistance);
            default:
                return geom;
        }
    }

    public static MathTransform buildTransform(
            CoordinateReferenceSystem sourceCRS, CoordinateReferenceSystem destCRS)
            throws FactoryException {

        MathTransform transform = null;
        if (sourceCRS.getCoordinateSystem().getDimension() >= 3) {
            // We are going to transform over to DefaultGeographic.WGS84 on the fly
            // so we will set up our math transform to take it from there
            MathTransform toWgs84_3d =
                    CRS.findMathTransform(sourceCRS, DefaultGeographicCRS.WGS84_3D);
            MathTransform toWgs84_2d =
                    CRS.findMathTransform(
                            DefaultGeographicCRS.WGS84_3D, DefaultGeographicCRS.WGS84);
            transform = ConcatenatedTransform.create(toWgs84_3d, toWgs84_2d);
            sourceCRS = DefaultGeographicCRS.WGS84;
        }

        // the basic crs transformation, if any
        MathTransform2D sourceToTarget =
                (MathTransform2D) CRS.findMathTransform(sourceCRS, destCRS, true);

        if (transform == null) {
            return sourceToTarget;
        }
        if (sourceToTarget.isIdentity()) {
            return transform;
        }
        return ConcatenatedTransform.create(transform, sourceToTarget);
    }
}
