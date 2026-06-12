package cn.geoair.comp.dynamic.ds.tx;

import cn.geoair.base.exception.GirException;

public class GirDsJdbcTxException extends GirException {
    public GirDsJdbcTxException(String msg) {
        super(msg);
    }

    public GirDsJdbcTxException(Throwable e) {
        super(e);
    }

    public GirDsJdbcTxException(String message, Throwable cause) {
        super(message, cause);
    }
}
