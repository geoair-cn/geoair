package cn.geoair.comp.db.service.core.typehander;

import cn.geoair.map.dynamic.adv.query.typehandler.AdvBaseTypeHandler;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandlerContext;
import java.sql.Blob;

/**
 * BLOB 类型处理器：读取 {@link Blob} 为占位符字符串。
 * <p>
 * 注册到 ds-service 的类型转换注册表中使用。
 *
 * @author zhangjun
 */
public class BlobAdvTypeHandler extends AdvBaseTypeHandler<String> {

    private final String placeholder;

    public BlobAdvTypeHandler() {
        this("(Blob)");
    }

    public BlobAdvTypeHandler(String placeholder) {
        this.placeholder = placeholder;
    }

    @Override
    public boolean supports(Class<?> javaType, Object value) {
        return value instanceof Blob;
    }

    @Override
    protected String convertNonNullForRead(Object value, Class<?> javaType, AdvTypeHandlerContext context) {
        return placeholder;
    }
}
