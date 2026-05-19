package cn.geoair.map.dynamic.mvt.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author ：张俊
 * @date ：Created in 2025/9/3 10:04
 * @description： 事件返回值
 */
@Data
@Accessors(chain = true)
public class ParamCheckResult {

    private boolean success;
    private String message;

    public static ParamCheckResult of(final boolean success) {
        ParamCheckResult publishEventResult = new ParamCheckResult();
        publishEventResult.setSuccess(success);
        return publishEventResult;
    }
}
