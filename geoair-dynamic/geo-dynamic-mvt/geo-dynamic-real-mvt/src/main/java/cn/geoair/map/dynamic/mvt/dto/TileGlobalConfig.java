package cn.geoair.map.dynamic.mvt.dto;

import cn.geoair.map.dynamic.mvt.tools.param.TileExecParams;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@Accessors(chain = true)
public class TileGlobalConfig {

    TileRequestParams tileRequestParams;

    TileExecParams tileExecParams;

    TileExecutorConfig tileExecConfig;

    String layerName;

    Integer version = 0;


    JSONObject customVariable;


}
