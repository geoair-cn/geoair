package cn.geoair.comp.jdbc.url;

import cn.geoair.comp.jdbc.url.impl.DefaultJdbcUrlCodec;
/**
 * JDBC URL 编解码器工厂。
 *
 * @author 张逢吉
 */
public final class GirJdbcUrlCodecs {
    private static final JdbcUrlCodec DEFAULT = new DefaultJdbcUrlCodec();

    private GirJdbcUrlCodecs() {
    }

    public static JdbcUrlCodec defaultCodec() {
        return DEFAULT;
    }
}
