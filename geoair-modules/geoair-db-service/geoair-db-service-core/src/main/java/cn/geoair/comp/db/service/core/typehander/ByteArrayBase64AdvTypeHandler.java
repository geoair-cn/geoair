package cn.geoair.comp.db.service.core.typehander;

import cn.geoair.map.dynamic.adv.query.typehandler.AdvBaseTypeHandler;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandlerContext;
import cn.hutool.core.convert.Convert;
import java.util.Base64;

/**
 * 字节数组类型处理器：byte[] / Byte[] → Base64 String。
 *
 * <p>注册到 ds-service 的 executor 上，在 bSelectListStream 读取阶段完成转换。
 *
 * @author zhangjun
 */
public class ByteArrayBase64AdvTypeHandler extends AdvBaseTypeHandler<Object> {

    @Override
    public boolean supports(Class<?> javaType, Object value) {
        return value instanceof byte[] || value instanceof Byte[];
    }

    @Override
    protected Object convertNonNullForRead(
            Object value, Class<?> javaType, AdvTypeHandlerContext context) {
        return Base64.getEncoder().encodeToString(Convert.toPrimitiveByteArray(value));
    }

    @Override
    protected Object convertNonNullForWrite(
            Object value, Class<?> javaType, AdvTypeHandlerContext context) {
        return Base64.getEncoder().encodeToString(Convert.toPrimitiveByteArray(value));
    }
}
