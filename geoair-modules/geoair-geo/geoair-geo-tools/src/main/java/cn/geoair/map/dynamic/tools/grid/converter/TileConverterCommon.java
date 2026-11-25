package cn.geoair.map.dynamic.tools.grid.converter;

import cn.geoair.map.dynamic.tools.GirGeoTools;
import cn.geoair.map.dynamic.tools.ToolsConfig;
import cn.geoair.map.dynamic.tools.convert.GirGeoFormatOpt;
import cn.geoair.map.dynamic.tools.grid.GirTileConverterOpt;
import cn.geoair.map.dynamic.tools.grid.dto.BoxReferencedEnvelope;
import cn.geoair.map.dynamic.tools.grid.dto.RangeApo;
import cn.geoair.map.dynamic.tools.grid.dto.TileRange;
import cn.geoair.map.dynamic.tools.grid.dto.TileYAxis;
import cn.geoair.map.dynamic.tools.grid.dto.TileZxyApo;
import cn.geoair.map.dynamic.tools.srid.GirSridConvertOpt;
import cn.hutool.core.util.StrUtil;
import java.util.*;
import java.util.stream.Collectors;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

/**
 * 瓦片转换器的公共基础实现。
 *
 * <p>负责通用的几何范围处理、SRID 转换、闭区间 {@link RangeApo} 遍历及 XYZ/TMS 适配。 子类只需实现本坐标系的范围与坐标计算。所有不带 {@link
 * TileYAxis} 参数的方法均采用 Google/XYZ 顶部原点。
 *
 * @author 张逢吉
 */
public abstract class TileConverterCommon implements GirTileConverterOpt {

    /** 将点或零面积范围扩展为可计算范围时使用的坐标偏移量。 */
    protected static final double POINT_OFFSET = 0.0001;

    /** 坐标参考系转换工具。 */
    protected GirSridConvertOpt sridConvertOpt;
    /** 空间格式转换工具。 */
    protected GirGeoFormatOpt formatOpt;

    /** 当前转换器使用的工具配置。 */
    protected final ToolsConfig advToolsConfig;

    /**
     * 使用指定配置创建瓦片转换器基础实例。
     *
     * @param advToolsConfig 工具配置；为 {@code null} 时创建默认配置
     */
    public TileConverterCommon(ToolsConfig advToolsConfig) {
        this.advToolsConfig = advToolsConfig == null ? new ToolsConfig() : advToolsConfig;
        GirGeoTools geoTools = GirGeoTools.getInstance(this.advToolsConfig);
        sridConvertOpt = geoTools.getSridOpt();
        formatOpt = geoTools.getFormatOpt();
    }

    protected abstract Geometry transform(Geometry geometry, int srcSrid);

    public abstract RangeApo tileRangeByBox(int z, Envelope tileBox);

    @Override
    public String xyzToWkt(int z, int x, int y, int targetSrid) {
        validateXyz(z, x, y);
        BoxReferencedEnvelope envelope = xyzToTileBox(z, x, y, targetSrid);
        return envelope.getWktString(targetSrid);
    }

    /**
     * 校验瓦片计算的基础参数。
     *
     * @param z 缩放级别（0~22）
     * @param x 瓦片X索引（非负）
     * @param y 瓦片Y索引（非负）
     * @throws IllegalArgumentException 层级超出支持范围或行列号为负时抛出
     *     <p>本方法有意不校验 {@code x/y < 2^z}：边界计算会使用 {@code x + 1} 或 {@code y + 1}
     *     表示瓦片的右/下边界。需要验证实际瓦片行列号时，应由调用方按当前 网格的列数和 {@link #getTileRowCount(int)} 校验。
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
        // RangeApo 采用闭区间，尾部索引本身就是最后一个需要处理的瓦片。
        // 不再对最大值作 ceil/减一等二次解释，保持历史瓦片列表输出不变。
        int startX = rangeApo.getMinX();
        int endX = rangeApo.getMaxX();
        int startY = rangeApo.getMinY();
        int endY = rangeApo.getMaxY();

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
    public Set<TileZxyApo> zxyListByBox(
            Envelope envelope, int srcSrid, List<Integer> targetZs, TileYAxis yAxis) {
        if (targetZs == null || targetZs.isEmpty()) {
            throw new IllegalArgumentException("目标Z级别列表不能为空");
        }
        Set<TileZxyApo> zxySet = new LinkedHashSet<>();
        for (int z : targetZs) {
            zxySet.addAll(zxyListByBox(envelope, srcSrid, z, yAxis));
        }
        return zxySet;
    }

    @Override
    public Set<TileZxyApo> zxyListByBox(
            Envelope envelope, int srcSrid, int minZ, int maxZ, TileYAxis yAxis) {
        if (minZ < 0 || maxZ < 0 || minZ > maxZ) {
            throw new IllegalArgumentException("层级范围不合法：minZ=" + minZ + ", maxZ=" + maxZ);
        }
        List<Integer> targetZs = new ArrayList<>();
        for (int z = minZ; z <= maxZ; z++) {
            targetZs.add(z);
        }
        return zxyListByBox(envelope, srcSrid, targetZs, yAxis);
    }

    @Override
    public BoxReferencedEnvelope boundsFromTileZxyApos(
            Set<TileZxyApo> zxyList, TileYAxis yAxis, int targetSrid) {
        if (zxyList == null || zxyList.isEmpty()) {
            throw new IllegalArgumentException("zxyList 不能为空");
        }
        zxyList = toXyzTiles(zxyList, yAxis);

        // 检查并获取统一的 zoom
        int zoom = -1;
        long minX = Long.MAX_VALUE;
        long maxX = Long.MIN_VALUE;
        long minY = Long.MAX_VALUE;
        long maxY = Long.MIN_VALUE;

        for (TileZxyApo tile : zxyList) {
            if (zoom == -1) {
                zoom = tile.getZ();
            } else if (zoom != tile.getZ()) {
                throw new IllegalArgumentException("所有瓦片必须在同一层级");
            }

            minX = Math.min(minX, tile.getX());
            maxX = Math.max(maxX, tile.getX());
            minY = Math.min(minY, tile.getY());
            maxY = Math.max(maxY, tile.getY());
        }

        return boundsFromTileRange(
                TileRange.closed(
                        zoom, (int) minX, (int) maxX, (int) minY, (int) maxY, TileYAxis.XYZ),
                targetSrid);
    }

    @Override
    public BoxReferencedEnvelope boundsFromRangeApo(RangeApo rangeApo, int targetSrid) {
        if (rangeApo == null) {
            throw new IllegalArgumentException("RangeApo 不能为空");
        }

        int zoom = rangeApo.getZ();
        if (zoom < 0) {
            throw new IllegalArgumentException("RangeApo 中未设置 zoom");
        }

        long minTileX = rangeApo.getMinX();
        long maxTileX = rangeApo.getMaxX();
        long minTileY = rangeApo.getMinY();
        long maxTileY = rangeApo.getMaxY();

        return boundsFromTileRange(minTileX, maxTileX, minTileY, maxTileY, zoom, targetSrid);
    }

    public abstract BoxReferencedEnvelope boundsFromTileRange(
            long minTileX, long maxTileX, long minTileY, long maxTileY, int zoom, int targetSrid);

    @Override
    public Set<TileZxyApo> zxyListByBox(
            Envelope envelope, int srcSrid, int targetZ, TileYAxis yAxis) {

        Geometry geometry = sridConvertOpt.convertToGeom(envelope);

        return fromXyzTiles(zxyListByGeom(geometry, srcSrid, targetZ), yAxis);
    }
}
