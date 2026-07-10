package cn.geoair.map.dynamic.mvt.dto;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.dynamic.mvt.tools.param.TileExecParams;
import com.alibaba.fastjson2.JSONObject;
import lombok.Data;
import lombok.experimental.Accessors;
 

 
@Data
@Accessors(chain = true)
public class TileGlobalConfig {
    public static GiLogger log = GirLoggerFactory.getLogger();
    TileRequestParams tileRequestParams;

    TileExecParams tileExecParams;

    TileExecutorConfig tileExecConfig;

    String layerName;

    Integer version = 0;

    JSONObject customVariable;
}
