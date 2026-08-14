package cn.geoair.comp.db.service.core.typehander;

import cn.geoair.base.Gir;
import cn.geoair.comp.db.service.core.typehander.oracle.OracleBlobTypeHandler;
import cn.hutool.core.lang.Singleton;
import cn.hutool.db.meta.JdbcType;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 类型处理器注册表，用于按 Java 类型或 JDBC 类型码查找对应的 {@link TypeHandler}。
 * <p>
 * 主要用于 {@code SafeSqlExecutor.tranOneRow()} 中对基础类型（String、Boolean、Date、Clob、Blob、byte[]）
 * 的二次格式化。几何类型的处理已移至 adv-query 的 {@code AdvTypeHandlerRegistry}。
 *
 * @author zhangjun
 */
public class TypeHandlerRegistry {

    static Map<Integer, TypeHandler<?>> jdbcTypeTypeHandlerHashMap = new HashMap<>();
    static Map<Class, TypeHandler<?>> classTypeHandlerHashMap = new HashMap<>();

    static TypeHandler<Object> objectType = new ObjectTypeHandler();

    static {
        classTypeHandlerHashMap.put(String.class, Singleton.get(StringTypeHandler.class));
        classTypeHandlerHashMap.put(Boolean.class, Singleton.get(BooleanTypeHandler.class));
        classTypeHandlerHashMap.put(Date.class, Singleton.get(DateTypeHandler.class));
        classTypeHandlerHashMap.put(Timestamp.class, Singleton.get(DateTypeHandler.class));
        classTypeHandlerHashMap.put(java.sql.Clob.class, Singleton.get(CLOBTypeHandler.class));
        classTypeHandlerHashMap.put(java.sql.Blob.class, Singleton.get(BlobTypeHandler.class));

        classTypeHandlerHashMap.put(byte[].class, Singleton.get(ByteTypeHandler.class));
        classTypeHandlerHashMap.put(Byte[].class, Singleton.get(ByteTypeHandler.class));

        jdbcTypeTypeHandlerHashMap.put(
                JdbcType.BOOLEAN.typeCode, Singleton.get(BooleanTypeHandler.class));
        jdbcTypeTypeHandlerHashMap.put(
                JdbcType.VARCHAR.typeCode, Singleton.get(StringTypeHandler.class));
        jdbcTypeTypeHandlerHashMap.put(
                JdbcType.NCHAR.typeCode, Singleton.get(StringTypeHandler.class));
        jdbcTypeTypeHandlerHashMap.put(
                JdbcType.CHAR.typeCode, Singleton.get(StringTypeHandler.class));
        jdbcTypeTypeHandlerHashMap.put(
                JdbcType.LONGVARCHAR.typeCode, Singleton.get(StringTypeHandler.class));
        jdbcTypeTypeHandlerHashMap.put(
                JdbcType.DATE.typeCode, Singleton.get(DateTypeHandler.class));
        jdbcTypeTypeHandlerHashMap.put(
                JdbcType.TIME.typeCode, Singleton.get(DateTypeHandler.class));
        jdbcTypeTypeHandlerHashMap.put(
                JdbcType.TIMESTAMP.typeCode, Singleton.get(DateTypeHandler.class));
    }

    static {
        try {
            // Oracle BLOB/CLOB 类型处理器注册
            TypeHandlerRegistry.register(
                    oracle.sql.BLOB.class, Singleton.get(OracleBlobTypeHandler.class));
            TypeHandlerRegistry.register(
                    oracle.jdbc.OracleBlob.class, Singleton.get(OracleBlobTypeHandler.class));
            TypeHandlerRegistry.register(
                    oracle.sql.CLOB.class, Singleton.get(CLOBTypeHandler.class));
            TypeHandlerRegistry.register(
                    oracle.jdbc.OracleClob.class, Singleton.get(CLOBTypeHandler.class));
            Gir.log.info("Oracle BLOB/CLOB 类型处理器注册成功");
        } catch (Throwable e) {
            // Oracle 驱动不在 classpath 时静默忽略
        }
    }

    public static TypeHandler getTypeHandlerByJavaType(Object c) {
        if (c == null) {
            return objectType;
        }
        TypeHandler<?> typeHandler = getTypeHandlerByClass(c.getClass());
        if (typeHandler == null) {
            return objectType;
        }
        return typeHandler;
    }

    private static TypeHandler getTypeHandlerByClass(Class<?> aClass) {
        if (aClass == null) {
            return null;
        }
        TypeHandler<?> typeHandler = classTypeHandlerHashMap.get(aClass);
        if (typeHandler == null) {
            Class<?> superclass = aClass.getSuperclass();
            if (superclass != null) {
                return getTypeHandlerByClass(superclass);
            }
        }
        return typeHandler;
    }

    public static TypeHandler getTypeHandlerByJDBCType(Integer typeCode) {
        TypeHandler<?> typeHandler = jdbcTypeTypeHandlerHashMap.get(typeCode);
        if (typeHandler == null) {
            return objectType;
        }
        return typeHandler;
    }

    public static void register(Class c, TypeHandler<?> typeHandler) {
        classTypeHandlerHashMap.put(c, typeHandler);
    }

    public static void register(Integer c, TypeHandler<?> typeHandler) {
        jdbcTypeTypeHandlerHashMap.put(c, typeHandler);
    }
}
