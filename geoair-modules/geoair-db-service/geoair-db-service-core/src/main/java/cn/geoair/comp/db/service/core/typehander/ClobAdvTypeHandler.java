package cn.geoair.comp.db.service.core.typehander;

import cn.geoair.map.dynamic.adv.query.typehandler.AdvBaseTypeHandler;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandlerContext;
import java.sql.Clob;

/**
 * CLOB 类型处理器：读取 {@link Clob} 内容为 String。
 * <p>
 * 注册到 ds-service 的 executor 上，在 bSelectListStream 读取阶段完成转换。
 *
 * @author zhangjun
 */
public class ClobAdvTypeHandler extends AdvBaseTypeHandler<String> {

    @Override
    public boolean supports(Class<?> javaType, Object value) {
        return value instanceof Clob;
    }

    @Override
    protected String convertNonNullForRead(Object value, Class<?> javaType, AdvTypeHandlerContext context) {
        try {
            Clob clob = (Clob) value;
            return clob.getSubString(1, (int) clob.length());
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }
}
