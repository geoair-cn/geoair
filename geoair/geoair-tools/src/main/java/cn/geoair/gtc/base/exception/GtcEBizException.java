package cn.geoair.gtc.base.exception;


import cn.geoair.gtc.base.util.GutilStr;

/**
 * @author ：张俊
 * @date ：Created in 2024/11/27 16:52
 * @description： 业务异常
 */
public class GtcEBizException extends GtcException {


    public GtcEBizException() {
        this("业务异常");
    }

    public GtcEBizException(String msg) {
        super(msg);
    }

    public GtcEBizException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public GtcEBizException(Throwable e) {
        super(e);
    }

    public GtcEBizException(String messageTemplate, Object... params) {
        super(GutilStr.format(messageTemplate, params));
    }

    public GtcEBizException(Throwable throwable, String messageTemplate, Object... params) {
        super(GutilStr.format(messageTemplate, params), throwable);
    }
}
