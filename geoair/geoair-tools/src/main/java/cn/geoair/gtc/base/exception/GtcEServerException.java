package cn.geoair.gtc.base.exception;

import cn.geoair.gtc.base.util.GutilStr;

/**
 * @author ：张俊
 * @date ：Created in 2024/11/27 16:52
 * @description： 服务器异常
 */
public class GtcEServerException extends GtcException {
    public GtcEServerException() {
        this("服务异常");
    }

    public GtcEServerException(String msg) {
        super(msg);
    }

    public GtcEServerException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public GtcEServerException(Throwable e) {
        super(e);
    }

    public GtcEServerException(String messageTemplate, Object... params) {
        super(GutilStr.format(messageTemplate, params));
    }

    public GtcEServerException(Throwable throwable, String messageTemplate, Object... params) {
        super(GutilStr.format(messageTemplate, params), throwable);
    }
}
