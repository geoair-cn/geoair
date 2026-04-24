package cn.geoair.map.dynamic.statics.mvt.spark.vectile.utils;


import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;

import cn.geoair.map.dynamic.tools.GirGeoTools;
import org.locationtech.jts.geom.Geometry;
import scala.Tuple2;
import scala.Tuple4;

import java.io.Serializable;
import java.util.*;


/**
 * @author ：张俊
 * @date ：Created in 2026/4/1 12:03
 * @description： 瓦片迭代器：懒加载，逐个瓦片返回，避免一次性创建所有对象
 */
public class TileIterator
        implements Iterator<Tuple2<String, List<GirAdvOneRow>>> , Serializable {

    private final GirAdvOneRow feature;
    private final Geometry geom;
    private final int minZoom;
    private final int maxZoom;
    private final int outGridSrid;

    // 迭代状态
    private int currentZoom;
    private int currentX;
    private int currentY;
    private int xMin;
    private int xMax;
    private int yMin;
    private int yMax;
    private boolean hasNext;
    private boolean initialized;

    public TileIterator(GirAdvOneRow feature, Geometry geom,
                        int minZoom, int maxZoom, int outGridSrid) {
        this.feature = feature;
        this.geom = geom;
        this.minZoom = minZoom;
        this.maxZoom = maxZoom;
        this.outGridSrid = outGridSrid;
        this.currentZoom = minZoom;
        this.hasNext = minZoom <= maxZoom;
        this.initialized = false;
    }

    /**
     * 初始化当前 zoom 级别的瓦片范围
     */
    private void initCurrentZoom() {
        if (initialized) {
            return;
        }

        Tuple4<Integer, Integer, Integer, Integer> tileRange =
                TileUtils.rangeToIndex(currentZoom, geom, outGridSrid);

        this.xMin = tileRange._1();
        this.xMax = tileRange._2();
        this.yMin = tileRange._3();
        this.yMax = tileRange._4();
        this.currentX = xMin;
        this.currentY = yMin;
        this.initialized = true;

        // 检查当前 zoom 是否有有效瓦片
        if (xMin > xMax || yMin > yMax) {
            moveToNextZoom();
        }
    }

    /**
     * 移动到下一个 zoom 级别
     */
    private void moveToNextZoom() {
        currentZoom++;
        initialized = false;

        if (currentZoom > maxZoom) {
            hasNext = false;
        }
    }

    /**
     * 移动到下一个瓦片位置
     */
    private void moveToNextTile() {
        currentX++;

        if (currentX > xMax) {
            currentX = xMin;
            currentY++;

            if (currentY > yMax) {
                // 当前 zoom 的所有瓦片遍历完毕，移动到下一个 zoom
                moveToNextZoom();
            }
        }
    }

    @Override
    public boolean hasNext() {
        // 如果已经确定没有下一个，直接返回
        if (!hasNext) {
            return false;
        }

        // 如果未初始化，先初始化当前 zoom
        if (!initialized) {
            initCurrentZoom();
        }

        // 检查当前位置是否有效
        while (hasNext) {
            if (currentZoom > maxZoom) {
                hasNext = false;
                return false;
            }

            if (currentX >= xMin && currentX <= xMax &&
                    currentY >= yMin && currentY <= yMax) {
                return true;
            }

            // 当前位置无效，移动到下一个有效位置
            moveToNextTile();
        }

        return false;
    }

    @Override
    public Tuple2<String, List<GirAdvOneRow>> next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        // 生成当前瓦片的 quadKey
        String quadKey = GirGeoTools.me().getTileGridBingMapOpt()
                .xyzToQuadKey(currentX, currentY, currentZoom);

        // 使用 Collections.singletonList 避免创建新的 ArrayList
        // 注意：这个 List 是不可变的，如果下游需要修改，需要改为 new ArrayList<>(1)
        List<GirAdvOneRow> singleList =  Collections.singletonList(feature);

        // 创建结果 Tuple2
        Tuple2<String, List<GirAdvOneRow>> result =
                new Tuple2<>(quadKey, singleList);

        // 移动到下一个瓦片
        moveToNextTile();

        return result;
    }
}
