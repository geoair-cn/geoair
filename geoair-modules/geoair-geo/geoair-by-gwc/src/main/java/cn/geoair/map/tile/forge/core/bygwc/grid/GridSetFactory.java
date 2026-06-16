/**
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * <p>You should have received a copy of the GNU Lesser General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 *
 * @author Arne Kepp, OpenGeo, Copyright 2009
 */
package cn.geoair.map.tile.forge.core.bygwc.grid;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.hutool.core.lang.Assert;


public class GridSetFactory {
    private static GiLogger log = GirLoggerFactory.getLogger(GridSetFactory.class);

    /**
     * Default pixel size in meters, producing a default of 90.7 DPI
     *

     */
    public static final double DEFAULT_PIXEL_SIZE_METER = 0.00028;

    public static int DEFAULT_LEVELS = 22;

    public static final double EPSG4326_TO_METERS = 6378137.0 * 2.0 * Math.PI / 360.0;

    public static final double EPSG3857_TO_METERS = 1;

    private static GridSet baseGridSet(String name, SRS srs, int tileWidth, int tileHeight) {
        GridSet gridSet = new GridSet();

        gridSet.setName(name);
        gridSet.setSrs(srs);

        gridSet.setTileWidth(tileWidth);
        gridSet.setTileHeight(tileHeight);

        return gridSet;
    }

    /**
     * Note that you should provide EITHER resolutions or scales. Providing both will cause a
     * precondition violation exception.
     */
    public static GridSet createGridSet(
            final String name,
            final SRS srs,
            final BoundingBox extent,
            boolean alignTopLeft,
            double[] resolutions,
            double[] scaleDenoms,
            Double metersPerUnit,
            double pixelSize,
            String[] scaleNames,
            int tileWidth,
            int tileHeight,
            boolean yCoordinateFirst) {

        Assert.notNull(name, "名称为空");
        Assert.notNull(srs, "srs is null");
        Assert.notNull(extent, "范围为空");
        Assert.isTrue(!extent.isNull() && extent.isSane(), "范围无效: " + extent);
        Assert.isTrue(resolutions != null || scaleDenoms != null, "网格集定义必须具有分辨率或比例分母");
        Assert.isTrue(resolutions == null || scaleDenoms == null, "只应提供分辨率或 scaleDenom 之一，而不是两者");

        for (int i = 1; resolutions != null && i < resolutions.length; i++) {
            if (resolutions[i] >= resolutions[i - 1]) {
                throw new IllegalArgumentException(
                        "每个分辨率都应该低于它的前一个。资源["
                                + i
                                + "] == "
                                + resolutions[i]
                                + ", Res["
                                + (i - 1)
                                + "] == "
                                + resolutions[i - 1]
                                + ".");
            }
        }

        for (int i = 1; scaleDenoms != null && i < scaleDenoms.length; i++) {
            if (scaleDenoms[i] >= scaleDenoms[i - 1]) {
                throw new IllegalArgumentException(
                        "每个比例分母都应该低于它的前一个。规模["
                                + i
                                + "] == "
                                + scaleDenoms[i]
                                + ", Scale["
                                + (i - 1)
                                + "] == "
                                + scaleDenoms[i - 1]
                                + ".");
            }
        }

        GridSet gridSet = baseGridSet(name, srs, tileWidth, tileHeight);

        gridSet.setResolutionsPreserved(resolutions != null);

        gridSet.setPixelSize(pixelSize);

        gridSet.setOriginalExtent(extent);
        gridSet.yBaseToggle = alignTopLeft;

        gridSet.setyCoordinateFirst(yCoordinateFirst);

        if (metersPerUnit == null) {
            if (srs.equals(SRS.getEPSG4326())) {
                gridSet.setMetersPerUnit(EPSG4326_TO_METERS);
            } else if (srs.equals(SRS.getEPSG3857())) {
                gridSet.setMetersPerUnit(EPSG3857_TO_METERS);
            } else {
                if (resolutions == null) {
                    log.warn(
                            "GridSet "
                                    + name
                                    + " was defined without metersPerUnit, assuming 1m/unit."
                                    + " All scales will be off if this is incorrect.");
                } else {
                    log.warn(
                            "GridSet "
                                    + name
                                    + " was defined without metersPerUnit. "
                                    + "Assuming 1m per SRS unit for WMTS scale output.");

                    gridSet.setScaleWarning(true);
                }
                gridSet.setMetersPerUnit(1.0);
            }
        } else {
            gridSet.setMetersPerUnit(metersPerUnit);
        }

        if (resolutions == null) {
            gridSet.setGridLevels(new Grid[scaleDenoms.length]);
        } else {
            gridSet.setGridLevels(new Grid[resolutions.length]);
        }

        for (int i = 0; i < gridSet.getNumLevels(); i++) {
            Grid curGrid = new Grid();

            if (scaleDenoms != null) {
                curGrid.setScaleDenominator(scaleDenoms[i]);
                curGrid.setResolution(pixelSize * (scaleDenoms[i] / gridSet.getMetersPerUnit()));
            } else {
                curGrid.setResolution(resolutions[i]);
                curGrid.setScaleDenominator(
                        (resolutions[i] * gridSet.getMetersPerUnit()) / DEFAULT_PIXEL_SIZE_METER);
            }

            final double mapUnitWidth = tileWidth * curGrid.getResolution();
            final double mapUnitHeight = tileHeight * curGrid.getResolution();

            final long tilesWide =
                    (long) Math.ceil((extent.getWidth() - mapUnitWidth * 0.01) / mapUnitWidth);
            final long tilesHigh =
                    (long) Math.ceil((extent.getHeight() - mapUnitHeight * 0.01) / mapUnitHeight);

            curGrid.setNumTilesWide(tilesWide);
            curGrid.setNumTilesHigh(tilesHigh);

            if (scaleNames == null || scaleNames[i] == null) {
                curGrid.setName(gridSet.getName() + ":" + i);
            } else {
                curGrid.setName(scaleNames[i]);
            }

            gridSet.setGrid(i, curGrid);
        }

        return gridSet;
    }

    public static GridSet createGridSet(
            final String name,
            final SRS srs,
            final BoundingBox extent,
            final boolean alignTopLeft,
            final int levels,
            final Double metersPerUnit,
            final double pixelSize,
            final int tileWidth,
            final int tileHeight,
            final boolean yCoordinateFirst) {

        final double extentWidth = extent.getWidth();
        final double extentHeight = extent.getHeight();

        double resX = extentWidth / tileWidth;
        double resY = extentHeight / tileHeight;

        final int tilesWide, tilesHigh;
        if (resX <= resY) {
            // use one tile wide by N tiles high
            tilesWide = 1;
            tilesHigh = (int) Math.round(resY / resX);
            // previous resY was assuming 1 tile high, recompute with the actual number of tiles
            // high
            resY = resY / tilesHigh;
        } else {
            // use one tile high by N tiles wide
            tilesHigh = 1;
            tilesWide = (int) Math.round(resX / resY);
            // previous resX was assuming 1 tile wide, recompute with the actual number of tiles
            // wide
            resX = resX / tilesWide;
        }

        // the maximum of resX and resY is the one that adjusts better
        final double res = Math.max(resX, resY);

        final double adjustedExtentWidth = tilesWide * tileWidth * res;
        final double adjustedExtentHeight = tilesHigh * tileHeight * res;

        BoundingBox adjExtent = new BoundingBox(extent);
        adjExtent.setMaxX(adjExtent.getMinX() + adjustedExtentWidth);
        // Do we keep the top or the bottom fixed?
        if (alignTopLeft) {
            adjExtent.setMinY(adjExtent.getMaxY() - adjustedExtentHeight);
        } else {
            adjExtent.setMaxY(adjExtent.getMinY() + adjustedExtentHeight);
        }

        double[] resolutions = new double[levels];
        resolutions[0] = res;

        for (int i = 1; i < levels; i++) {
            resolutions[i] = resolutions[i - 1] / 2;
        }

        return createGridSet(
                name,
                srs,
                adjExtent,
                alignTopLeft,
                resolutions,
                null,
                metersPerUnit,
                pixelSize,
                null,
                tileWidth,
                tileHeight,
                yCoordinateFirst);
    }
}
