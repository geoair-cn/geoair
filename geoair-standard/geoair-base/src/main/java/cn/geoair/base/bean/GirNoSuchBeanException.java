package cn.geoair.base.bean;

import cn.geoair.base.util.GutilStr;

/**
 * 获取不到Bean的异常
 *
 * @author Ray
 */
public class GirNoSuchBeanException extends GirBeanException {

    /** */
    private static final long serialVersionUID = -341043413233724962L;

    public GirNoSuchBeanException() {
        this("系统异常");
    }

    public GirNoSuchBeanException(String msg) {
        super(msg);
    }

    public GirNoSuchBeanException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public GirNoSuchBeanException(Throwable e) {
        super(e);
    }

    public GirNoSuchBeanException(String messageTemplate, Object... params) {
        super(messageTemplate, params);
    }

    public GirNoSuchBeanException(Throwable throwable, String messageTemplate, Object... params) {
        this(GutilStr.format(messageTemplate, params), throwable);
    }
}
