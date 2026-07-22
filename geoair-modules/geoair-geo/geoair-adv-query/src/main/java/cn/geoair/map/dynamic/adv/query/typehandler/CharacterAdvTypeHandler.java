package cn.geoair.map.dynamic.adv.query.typehandler;

/**
 * @author ：张逢吉
 * @date ：Created in 2026/7/22
 * @description： 字符类型处理器
 */
public class CharacterAdvTypeHandler extends AdvBaseTypeHandler<Character> {

    @Override
    public boolean supports(Class<?> javaType, Object value) {
        return javaType == Character.class || javaType == char.class;
    }

    @Override
    protected Character convertNonNullForRead(
            Object value, Class<?> javaType, AdvTypeHandlerContext context) {
        if (value instanceof Character) {
            return (Character) value;
        }
        String text = String.valueOf(value);
        if (text.isEmpty()) {
            return null;
        }
        return text.charAt(0);
    }

    @Override
    protected Object convertNonNullForWrite(
            Character value, Class<?> javaType, AdvTypeHandlerContext context) {
        return String.valueOf(value);
    }
}
