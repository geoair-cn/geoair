package cn.geoair.map.tile.forge.fuser.provider;

import cn.geoair.map.tile.forge.core.bygwc.grid.GridSubset;
import cn.geoair.map.tile.forge.core.bygwc.grid.BoundingBox;
import cn.geoair.map.tile.forge.core.bygwc.grid.SRS;
import cn.geoair.map.tile.forge.core.bygwc.io.Resource;
import cn.geoair.map.tile.forge.fuser.fuser.TileFusionResult;
import cn.geoair.map.tile.forge.fuser.utils.GridInitUtils;

/** 当前内置网格的 XYZ 请求直读入口；内部 Getter 一律使用 TMS 行号。 */
public final class SameGridTileReader {
    private SameGridTileReader() { }

    public static boolean equivalentSrid(Integer source, int output) {
        return source != null && normalizeSrid(source) == normalizeSrid(output);
    }

    /** 4326/4490 共用本库经纬度矩阵；此规则不能推广到其他自定义瓦片矩阵。 */
    public static int normalizeSrid(int srid) {
        if (srid == 900913) return 3857;
        return srid == 4490 ? 4326 : srid;
    }

    public static int tmsY(GridSubset grid, int sourceSrid, int z, int x, int y) {
        if (grid == null || !SRS.getSRS(normalizeSrid(sourceSrid)).equals(grid.getSRS()))
            throw new IllegalArgumentException("源读取器网格与配置坐标系不一致");
        if (z < grid.getZoomStart() || z > grid.getZoomStop())
            throw new IllegalArgumentException("超出源网格级别范围");
        long rows = grid.getGridSet().getGrid(z).getNumTilesHigh();
        if (y < 0 || y >= rows) throw new IllegalArgumentException("瓦片Y坐标超出源网格范围");
        int row = Math.toIntExact(rows - y - 1);
        if (!grid.covers(new long[]{x, row, z})) throw new IllegalArgumentException("瓦片坐标超出源图层覆盖范围");
        return row;
    }

    public static TileFusionResult read(LayerTileGetter getter, int sourceSrid, int outputSrid,
                                        int z, int x, int xyzY, MissingTileMeaning missingMeaning) throws Exception {
        if (!equivalentSrid(sourceSrid, outputSrid)) throw new IllegalArgumentException("网格不同，不能直接读取");
        if (getter == null) throw new IllegalArgumentException("源读取器不能为空");
        GridSubset expected;
        if (normalizeSrid(outputSrid) == 3857) expected = GridInitUtils.getWorldGrid3857();
        else if (normalizeSrid(outputSrid) == 4326) expected = GridInitUtils.getTdtGrid4490();
        else throw new IllegalArgumentException("直接读取只支持当前内置的墨卡托和经纬度矩阵");
        GridSubset actual = getter.getSrcGridSubset();
        int row = tmsY(actual, sourceSrid, z, x, xyzY);
        int expectedRow = tmsY(expected, outputSrid, z, x, xyzY);
        BoundingBox nativeBox = actual.boundsFromIndex(new long[]{x, row, z});
        BoundingBox requestedBox = expected.boundsFromIndex(new long[]{x, expectedRow, z});
        double tolerance = Math.max(1, Math.max(requestedBox.getWidth(), requestedBox.getHeight())) * 1e-9;
        if (actual.getTileWidth() != expected.getTileWidth() || actual.getTileHeight() != expected.getTileHeight()
                || Math.abs(nativeBox.getMinX() - requestedBox.getMinX()) > tolerance
                || Math.abs(nativeBox.getMinY() - requestedBox.getMinY()) > tolerance
                || Math.abs(nativeBox.getMaxX() - requestedBox.getMaxX()) > tolerance
                || Math.abs(nativeBox.getMaxY() - requestedBox.getMaxY()) > tolerance)
            throw new IllegalArgumentException("源瓦片矩阵与目标内置矩阵不同，不能仅凭坐标系相同直接读取");
        CheckedLayerTileGetter checked = new CheckedLayerTileGetter(getter, missingMeaning, TileReadFailurePolicy.FAIL_FAST);
        Resource resource = checked.getTileResource(z, x, row);
        return checked.result(resource == null ? null : resource.getByteData(), null);
    }
}
