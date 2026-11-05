package cn.geoair.map.tile.forge.fuser.upstream;

import cn.geoair.base.data.model.annotation.GaModel;
import cn.geoair.base.data.model.annotation.GaModelField;

import lombok.Data;
import lombok.ToString;

import java.util.LinkedHashMap;
import java.util.Map;

/** 每图层的上游参数。业务方负责安全存储，不应直接返回到公开元数据。 */
@Data
@GaModel(text = "图层上游请求参数")
public class UpstreamRequestOptions {
    @GaModelField(text = "来源页面")
    private String referer;

    @GaModelField(text = "页面域名")
    private String origin;

    @GaModelField(text = "自定义请求头")
    @ToString.Exclude
    private Map<String, String> headers = new LinkedHashMap<>();

    @GaModelField(text = "自定义查询参数")
    @ToString.Exclude
    private Map<String, String> queryParameters = new LinkedHashMap<>();

    @GaModelField(text = "是否跟随同来源重定向")
    private boolean followRedirects = true;

    @GaModelField(text = "最大请求次数")
    private int maxAttempts = 3;
}
