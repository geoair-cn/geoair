package cn.geoair.map.tile.forge.fuser;

import cn.geoair.map.dynamic.tools.GirAdvTools;

import cn.geoair.map.dynamic.tools.grid.dto.BoxReferencedEnvelope;
import cn.geoair.map.dynamic.tools.simple.GirServletUtil;
import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;
import cn.geoair.map.tile.forge.core.bygwc.grid.BoundingBox;
import cn.geoair.web.util.GirHttpServletHelper;
import cn.geoair.map.tile.forge.fuser.fuser.FuserExec;
import cn.geoair.map.tile.forge.fuser.fuser.GirFuserExecFactory;
import lombok.extern.slf4j.Slf4j;


/**
 * @author ：张俊
 * @date ：Created in 2023/12/4 15:47
 * @description： xyz的图层叠加处理
 */
@Slf4j
public class TileServiceTran {

    /**
     * 谷歌的服务，用4326的网格请求
     *
     * @param layerName 图层名称
     * @param z         zoom等级
     * @param x         x坐标
     * @param y         y坐标
     */
    public void googleServiceTo4326Request(String layerName, Integer z, Integer x, Integer y) {
        BoxReferencedEnvelope box = GirAdvTools.getTileGrid4326Opt().xyzToTileBox(z, x, y, 3857);
        BoundingBox bounds = new BoundingBox(box.getMinX(), box.getMinY(), box.getMaxX(), box.getMaxY());
        processTileRequest(layerName, z, x, y, bounds);
    }

    /**
     * grid4490服务转3857请求
     *
     * @param layerName 图层名称
     * @param z         zoom等级
     * @param x         x坐标
     * @param y         y坐标
     */
    public void grid4490ServiceTo3857Request(String layerName, Integer z, Integer x, Integer y) {
        BoxReferencedEnvelope box = GirAdvTools.getTileGrid3857Opt().xyzToTileBox(z, x, y, 4326);
        BoundingBox bounds = new BoundingBox(box.getMinX(), box.getMinY(), box.getMaxX(), box.getMaxY());
        processTileRequest(layerName, z, x, y, bounds);
    }

    /**
     * 处理瓦片请求的核心方法
     *
     * @param layerName 图层名称
     * @param z         zoom等级
     * @param x         x坐标
     * @param y         y坐标
     * @param bounds    边界框
     */
    private void processTileRequest(String layerName, Integer z, Integer x, Integer y, BoundingBox bounds) {
        try {
            FuserExec cacheTileFuser = GirFuserExecFactory.createCachedFuser(
                    layerName, z, x, y, bounds, 256, 256, ImageMime.png);
            byte[] imageBytes = cacheTileFuser.toImageBytes();

            // 返回响应
            if (imageBytes != null && imageBytes.length > 0) {
                GirServletUtil.toResponse(GirHttpServletHelper.getResponse(), imageBytes, "image/png");
            } else {
                GirServletUtil.toResponse(GirHttpServletHelper.getResponse(), "获取瓦片失败！".getBytes(), "application/text");
            }

        } catch (Exception e) {
            log.error("生成瓦片失败: layerName={}, z={}, x={}, y={}", layerName, z, x, y, e);
            GirServletUtil.toResponse(GirHttpServletHelper.getResponse(), "服务异常！".getBytes(), "application/text");
        }
    }
}
