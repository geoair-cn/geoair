package cn.geoair.map.dynamic.mvt.controller;

import cn.geoair.base.api.annotation.GaApi;
import cn.geoair.map.dynamic.mvt.dto.TileRequestParams;

import cn.geoair.map.dynamic.tools.GirGeoTools;
import cn.geoair.map.dynamic.tools.grid.dto.BoxReferencedEnvelope;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Geometry;
import org.springframework.web.bind.annotation.*;

@Slf4j
// @Controller
@RequiredArgsConstructor
@CrossOrigin
@GaApi(
    text = "矢量瓦片服务",
    tags = {"矢量瓦片服务"}
)
@RequestMapping("/vectorTileService")
public class VectorTileV2Controller extends TileCommon {

    @RequestMapping(value = "/v2/real/{layerName}/{z}/{x}/{y}.pbf", method = RequestMethod.GET)
    public void realVectorTileV2(
            @RequestParam("paramTile") String paramTile,
            @PathVariable("layerName") String layerName,
            @PathVariable Integer x,
            @PathVariable Integer y,
            @PathVariable Integer z,
            @RequestParam(required = false) Integer minZoom,
            @RequestParam(required = false) Boolean isGeo, // 新增接收端动态修改网格
            HttpServletResponse response,
            HttpServletRequest request)
            throws Exception {

        if (ObjectUtil.isEmpty(paramTile)) {
            toResponse(
                    response,
                    "参数错误001".getBytes(StandardCharsets.UTF_8),
                    "text/plain; charset=utf-8");
            return;
        }
        TileRequestParams params = TileRequestParams.fromBase32(paramTile);
        if (minZoom != null) {
            params.setMinZoom(minZoom);
        }
        if (isGeo != null) {
            params.setGeo(isGeo);
        }
        doMvt(layerName, params, z, x, y, response, request, 2);
    }

    @RequestMapping(value = "/v2/debug/{layerName}/{z}/{x}/{y}.pbf", method = RequestMethod.GET)
    public void realVectorTileDebug(
            @RequestParam("paramTile") String paramTile,
            @PathVariable("layerName") String layerName,
            @PathVariable Integer x,
            @PathVariable Integer y,
            @PathVariable Integer z,
            @RequestParam(required = false) Integer minZoom,
            HttpServletResponse response,
            HttpServletRequest request)
            throws Exception {
        HttpSession session = request.getSession();
        if (ObjectUtil.isEmpty(paramTile)) {
            toResponse(
                    response,
                    "参数错误001".getBytes(StandardCharsets.UTF_8),
                    "text/plain; charset=utf-8");
        }
        TileRequestParams params = TileRequestParams.fromBase32(paramTile);

        JSONObject re = new JSONObject();

        re.put("params", params);
        re.put("z", z);
        re.put("x", x);
        re.put("y", y);
        try {
            BoxReferencedEnvelope boxReferencedEnvelope = null;
            int gridSrid = params.isGeo() ? 4326 : 3857;
            if (!params.isGeo()) {
                boxReferencedEnvelope =
                        GirGeoTools.me().getTileGrid3857Opt().xyzToTileBox(z, x, y, 3857);
            } else {
                boxReferencedEnvelope =
                        GirGeoTools.me().getTileGrid4326Opt().xyzToTileBox(z, x, y, 4326);
            }
            Geometry geometry = GirGeoTools.me().getSridOpt().convertToGeom(boxReferencedEnvelope);
            re.put("bbox", geometry.toText());
            Geometry convert = GirGeoTools.me().getSridOpt().convert(geometry, gridSrid, 4326);
            re.put("bbox4326", convert.toText());
        } catch (Exception e) {
        }
        String jsonString = JSON.toJSONString(re, JSONWriter.Feature.PrettyFormat);
        toResponse(
                response,
                jsonString.getBytes(StandardCharsets.UTF_8),
                "application/json; charset=utf-8");
    }
}
