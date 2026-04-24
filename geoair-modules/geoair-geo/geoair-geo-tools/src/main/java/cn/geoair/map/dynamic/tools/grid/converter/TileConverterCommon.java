package cn.geoair.map.dynamic.tools.grid.converter;

 
import cn.geoair.map.dynamic.tools.GirAdvToolsGlobalConfig;
import cn.geoair.map.dynamic.tools.GirGeoTools;
import cn.geoair.map.dynamic.tools.convert.GirGeoFormatOpt;
import cn.geoair.map.dynamic.tools.coordinate.GirCoordinateUtils;
import cn.geoair.map.dynamic.tools.grid.GirTileConverterOpt;
import cn.geoair.map.dynamic.tools.grid.dto.RangeApo;
import cn.geoair.map.dynamic.tools.grid.dto.TileZxyApo;
import cn.geoair.map.dynamic.tools.srid.GirSridConvertOpt;
import cn.hutool.core.util.StrUtil;

import java.util.*;
import java.util.stream.Collectors;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

public abstract class TileConverterCommon implements GirTileConverterOpt {

    protected static final double POINT_OFFSET = 0.0001; // 点几何缓冲偏移

    protected GirSridConvertOpt sridConvertOpt = GirGeoTools.me().getSridOpt();
    protected GirGeoFormatOpt formatOpt = GirGeoTools.me().getFormatOpt();

    GirAdvToolsGlobalConfig advToolsConfig;

    public TileConverterCommon(GirAdvToolsGlobalConfig advToolsConfig) {
        this.advToolsConfig = advToolsConfig;
        sridConvertOpt = GirGeoTools.getInstance(advToolsConfig).getSridOpt();
        formatOpt = GirGeoTools.getInstance(advToolsConfig).getFormatOpt();
    }


    protected abstract Geometry transform(Geometry geometry, int srcSrid);

    public abstract RangeApo tileRangeByBox(int z, Envelope tileBox);

    @Override
    public String xyzToWkt(int z, int x, int y, int targetSrid) {
        validateXyz(z, x, y);
        ReferencedEnvelope envelope = xyzToTileBox(z, x, y, targetSrid);
        Geometry geometry =
                sridConvertOpt
                        .convertToGeom(
                                envelope,
                                envelope.getCoordinateReferenceSystem()
                                        .getCoordinateSystem()
                                        .getDimension(),
                                targetSrid);
        return formatOpt.jtsGeometryToWktString(geometry, false);
    }

    /**
     * 校验XYZ参数合法性
     *
     * @param z 缩放级别（0~22）
     * @param x 瓦片X索引（非负）
     * @param y 瓦片Y索引（非负）
     */
    protected void validateXyz(int z, int x, int y) {
        if (z < 0 || z > 22) {
            throw new IllegalArgumentException("缩放级别必须在0~22之间");
        }
        if (x < 0) {
            throw new IllegalArgumentException("瓦片X索引不能为负数");
        }
        if (y < 0) {
            throw new IllegalArgumentException("瓦片Y索引不能为负数");
        }
    }

    /**
     * 等轴场景的时候可以直接翻转
     *
     * @param y Y索引
     * @param z 缩放级别
     * @return
     */
    public int reverseY(int y, int z) {
        validateXyz(z, 0, y);
        int maxYIndex = (1 << z) - 1;
        return maxYIndex - y;
    }

    public RangeApo tileRangeByGeom(int z, Geometry geometry) {
        if (geometry == null) {
            return null;
        }
        double xmin = 0.0;
        double ymin = 0.0;
        double xmax = 0.0;
        double ymax = 0.0;

        // 处理点要素（添加极小偏移量）
        if (geometry instanceof Point) {
            Point point = (Point) geometry;
            double x = point.getX();
            double y = point.getY();
            // 定义极小偏移量，确保点能落在一个瓦片内
            xmin = x - POINT_OFFSET;
            ymin = y - POINT_OFFSET;
            xmax = x + POINT_OFFSET;
            ymax = y + POINT_OFFSET;
        } else {
            // 处理线、面等其他几何要素
            Envelope envelope = geometry.getEnvelopeInternal();
            xmin = envelope.getMinX();
            ymin = envelope.getMinY();
            xmax = envelope.getMaxX();
            ymax = envelope.getMaxY();
        }

        Envelope envelope = new Envelope(xmin, xmax, ymin, ymax);

        // 转换为瓦片坐标
        return tileRangeByBox(z, envelope);
    }

    @Override
    public Set<TileZxyApo> zxyListByGeom(Geometry geometry, int srcSrid, List<Integer> targetZs) {
        if (targetZs == null || targetZs.isEmpty()) {
            throw new IllegalArgumentException("目标Z级别列表不能为空");
        }
        Set<TileZxyApo> zxySet = new LinkedHashSet<>();
        for (Integer z : targetZs) {
            zxySet.addAll(zxyListByGeom(geometry, srcSrid, z));
        }
        return zxySet;
    }

    @Override
    public Set<TileZxyApo> zxyListByGeom(Geometry geometry, int srcSrid, int minZ, int maxZ) {
        if (minZ < 0 || maxZ < 0 || minZ > maxZ) {
            throw new IllegalArgumentException("层级范围不合法：minZ=" + minZ + ", maxZ=" + maxZ);
        }
        List<Integer> targetZs = new ArrayList<>();
        for (int z = minZ; z <= maxZ; z++) {
            targetZs.add(z);
        }
        return zxyListByGeom(geometry, srcSrid, targetZs);
    }

    @Override
    public Set<TileZxyApo> zxyListByGeom(Geometry geometry, int srcSrid, int targetZ) {
        // 1. 参数校验
        if (geometry == null) {
            throw new IllegalArgumentException("Geometry对象不能为空");
        }
        validateXyz(targetZ, 0, 0);

        // 2. 坐标系转换：
        Geometry geom = transform(geometry, srcSrid);
        if (geom == null) {
            return Collections.emptySet();
        }

        // 3. 几何对象转瓦片索引范围
        RangeApo rangeApo = tileRangeByGeom(targetZ, geom);
        if (rangeApo == null) {
            return Collections.emptySet();
        }

        // 4. 遍历瓦片索引范围，生成TileZxyApo
        Set<String> zxySet = new LinkedHashSet<>();
        int startX = (int) Math.floor(rangeApo.getMinX());
        int endX = (int) Math.ceil(rangeApo.getMaxX());
        int startY = (int) Math.floor(rangeApo.getMinY());
        int endY = (int) Math.ceil(rangeApo.getMaxY());

        // 5. 边界过滤 & 生成ZXY对象
        int maxTileIndex = (1 << targetZ) - 1;
        for (int x = startX; x <= endX; x++) {
            for (int y = startY; y <= endY; y++) {
                if (x >= 0 && x <= maxTileIndex && y >= 0 && y <= maxTileIndex) {
                    zxySet.add(targetZ + "|" + x + "|" + y);
                }
            }
        }
        // 这里如果是set直接保存对象，那么就没办法去重
        return zxySet.stream()
                .distinct()
                .map(
                        s -> {
                            List<String> split = StrUtil.split(s, "|");
                            return new TileZxyApo(
                                    Integer.parseInt(split.get(0)),
                                    Integer.parseInt(split.get(1)),
                                    Integer.parseInt(split.get(2)));
                        })
                .collect(Collectors.toSet());
    }

    @Override
    public Set<TileZxyApo> zxyListByBox(Envelope envelope, int srcSrid, List<Integer> targetZs) {
        if (targetZs == null || targetZs.isEmpty()) {
            throw new IllegalArgumentException("目标Z级别列表不能为空");
        }
        Set<TileZxyApo> zxySet = new LinkedHashSet<>();
        for (int z : targetZs) {
            zxySet.addAll(zxyListByBox(envelope, srcSrid, z));
        }
        return zxySet;
    }

    @Override
    public Set<TileZxyApo> zxyListByBox(Envelope envelope, int srcSrid, int minZ, int maxZ) {
        if (minZ < 0 || maxZ < 0 || minZ > maxZ) {
            throw new IllegalArgumentException("层级范围不合法：minZ=" + minZ + ", maxZ=" + maxZ);
        }
        List<Integer> targetZs = new ArrayList<>();
        for (int z = minZ; z <= maxZ; z++) {
            targetZs.add(z);
        }
        return zxyListByBox(envelope, srcSrid, targetZs);
    }

    @Override
    public Set<TileZxyApo> zxyListByBox(Envelope envelope, int srcSrid, int targetZ) {

        Geometry geometry = sridConvertOpt.convertToGeom(envelope);

        return zxyListByGeom(geometry, srcSrid, targetZ);
    }
}
