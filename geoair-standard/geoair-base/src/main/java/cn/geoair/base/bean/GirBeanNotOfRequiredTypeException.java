package cn.geoair.base.bean;

import cn.geoair.base.util.GutilStr;

/**
 * Bean类型不匹配的异常
 *
 * @author Ray
 */
public class GirBeanNotOfRequiredTypeException extends GirBeanException {

    /** */
    private static final long serialVersionUID = 1345140759210704903L;

    public GirBeanNotOfRequiredTypeException() {
        this("系统异常");
    }

    public GirBeanNotOfRequiredTypeException(String msg) {
        super(msg);
    }

    public GirBeanNotOfRequiredTypeException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public GirBeanNotOfRequiredTypeException(Throwable e) {
        super(e);
    }

    public GirBeanNotOfRequiredTypeException(String messageTemplate, Object... params) {
        super(messageTemplate, params);
    }

    public GirBeanNotOfRequiredTypeException(
            Throwable throwable, String messageTemplate, Object... params) {
        this(GutilStr.format(messageTemplate, params), throwable);
    }
}
