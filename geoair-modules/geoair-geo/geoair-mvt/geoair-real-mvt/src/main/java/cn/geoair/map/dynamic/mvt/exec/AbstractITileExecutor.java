package cn.geoair.map.dynamic.mvt.exec;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.dynamic.adv.GirAdvQuery;
import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.mvt.GirRealMvtHelper;
import cn.geoair.map.dynamic.mvt.consumer.VectorTileBuilderConsumer;
import cn.geoair.map.dynamic.mvt.dto.TileExecutorConfig;
import cn.geoair.map.dynamic.mvt.dto.TileRequestParams;
import cn.geoair.map.dynamic.mvt.exec.dto.TileRequest;
import cn.geoair.map.dynamic.mvt.tools.AdvMvtTileUtils;
import cn.geoair.map.dynamic.mvt.tools.model.PbfInfo;
import cn.geoair.map.dynamic.mvt.tools.param.TileExecParams;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import lombok.Getter;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/12/19 11:29 @description： TODO
 */
public abstract class AbstractITileExecutor implements ITileExecutor {
    public static GiLogger log = GirLoggerFactory.getLogger();

    abstract void getRecordByStream(VectorTileBuilderConsumer vectorTileBuilder);

    abstract void featuresTransform(GirAdvOneRow oneRow);

    @Override
    public TileExecutorConfig getTileExecutorConfig() {
        return tileExecutorConfig;
    }

    JSONObject customVariable;

    @Override
    public ITileExecutor setCustomVariable(JSONObject customVariable) {
        this.customVariable = customVariable;
        return this;
    }

    /** 当前矢量瓦片请求是否成功 */
    @Getter protected boolean successIs = false;

    protected TileExecutorConfig tileExecutorConfig = new TileExecutorConfig();

    // 核心配置参数：直接持有TileRequestParams对象
    protected TileRequestParams requestParams;

    protected TileExecParams tileExecParams;

    /** 网格的坐标系 */
    int gridSrid;

    /** 数据的坐标系 */
    int sourceDataSrid;

    protected int zoom;

    protected int x;

    protected int y;

    // 执行器（固定依赖，单独维护）
    protected IAdvExecutor iAdvExecutor;

    /** 图层名称 */
    String layerName;

    public AbstractITileExecutor(TileRequestParams requestParams, String layerName) {
        this.requestParams = requestParams;
        this.layerName = layerName;
        // 初始化执行器（从Params获取数据源ID和Schema）
        gridSrid = requestParams.isGeoIs() ? 4326 : 3857;
        String srid = requestParams.getSrid();
        if (StrUtil.isEmpty(srid) || srid.equals("0")) {
            if (StrUtil.isNotBlank(requestParams.getTransform())) {
                sourceDataSrid = Integer.parseInt(requestParams.getTransform());
            } else {
                sourceDataSrid = 3857;
            }
        } else {
            sourceDataSrid = Integer.parseInt(srid);
        }
        this.iAdvExecutor =
                GirAdvQuery.getIAdvExecutor(
                        Objects.isNull(requestParams.getDsId()) ? "-1" : requestParams.getDsId(),
                        requestParams.getSchemaName());
    }

    @Override
    public TileExecParams getTileExecParams(int zoom, int x, int y) {
        tileExecParams =
                AdvMvtTileUtils.getTileExecParamsNotHasSql(zoom, x, y, gridSrid, sourceDataSrid);
        String execSql = getExecSql(tileExecParams);
        tileExecParams.setExecSql(execSql);
        return tileExecParams.copy();
    }

    public void setTileExecutorConfig(TileExecutorConfig tileExecutorConfig) {
        this.tileExecutorConfig = tileExecutorConfig;
    }

    public TileRequest getTileData(int zoom, int x, int y) {
        this.zoom = zoom;
        this.x = x;
        this.y = y;
        TileRequest tileRequest = new TileRequest();
        byte[] pbfData = new byte[0];
        try {
            this.tileExecParams = getTileExecParams(zoom, x, y);
            boolean ignoreMinZoom = tileExecutorConfig.isIgnoreMinZoom();
            if (zoom >= requestParams.getMinZoom() || ignoreMinZoom) {
                VectorTileBuilderConsumer vectorTileBuilder =
                        GirRealMvtHelper.getInstance()
                                .getVectorTileBuilderConsumer(
                                        this.tileExecParams.getGridExtent(),
                                        layerName,
                                        gridSrid,
                                        getTileGlobalConfig());
                getRecordByStream(vectorTileBuilder);
                pbfData = vectorTileBuilder.build();
                successIs = true;
            }
        } catch (Exception e) {
            log.error("获取瓦片数据异常，当前参数：{}", requestParams.toString());
            log.error("获取瓦片数据异常", e);
            successIs = false;
            pbfData = e.getMessage().getBytes(StandardCharsets.UTF_8);
        }

        tileRequest.setPbfInfo(
                new PbfInfo()
                        .setData(pbfData)
                        .setZoom(zoom)
                        .setGridSrid(tileExecParams.getGridSrid()));
        tileRequest.setSuccessIs(successIs);
        return tileRequest;
    }
}
