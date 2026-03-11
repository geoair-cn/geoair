package cn.geoair.map.dynamic.dbservice.core.typehander;

import cn.geoair.base.Gir;
import cn.geoair.map.dynamic.dbservice.core.typehander.dm.DmGeomTypeHandler;
import cn.geoair.map.dynamic.dbservice.core.typehander.oracle.OracleBlobTypeHandler;
import cn.geoair.map.dynamic.dbservice.core.typehander.oracle.OracleGeomTypeHandler;
import cn.geoair.map.dynamic.dbservice.core.typehander.pg.PgGeomTypeHandler;
import cn.geoair.map.dynamic.dbservice.core.typehander.pg.PostGisGeomTypeHandler;
import cn.hutool.core.lang.Singleton;
import cn.hutool.db.meta.JdbcType;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author ：张俊
 * @date ：Created in 2024/4/18 17:54 @description： TODO
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
            // PostgreSQL/PostGIS相关类型处理器注册
            TypeHandlerRegistry.register(
                    net.postgis.jdbc.PGgeometry.class, Singleton.get(PgGeomTypeHandler.class));
            TypeHandlerRegistry.register(
                    net.postgis.jdbc.geometry.Geometry.class,
                    Singleton.get(PostGisGeomTypeHandler.class));
            TypeHandlerRegistry.register(1111, Singleton.get(PgGeomTypeHandler.class));
            Gir.log.info("PostGIS类型处理器注册成功");
        } catch (Throwable e) {
            System.err.println("PostGIS类型处理器注册失败: " + e.getMessage());
            e.printStackTrace(); // 保留堆栈信息便于调试
        }
    }

    static {
        try {
            // 达梦数据库相关类型处理器注册
            TypeHandlerRegistry.register(
                    dm.jdbc.driver.DmdbStruct.class, Singleton.get(DmGeomTypeHandler.class));
            Gir.log.info("达梦数据库类型处理器注册成功");
        } catch (Throwable e) {
            System.err.println("达梦数据库类型处理器注册失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    static {
        try {
            // Oracle BLOB/CLOB类型处理器注册
            TypeHandlerRegistry.register(
                    oracle.sql.BLOB.class, Singleton.get(OracleBlobTypeHandler.class));
            TypeHandlerRegistry.register(
                    oracle.jdbc.OracleBlob.class, Singleton.get(OracleBlobTypeHandler.class));
            TypeHandlerRegistry.register(
                    oracle.sql.CLOB.class, Singleton.get(CLOBTypeHandler.class));
            TypeHandlerRegistry.register(
                    oracle.jdbc.OracleClob.class, Singleton.get(CLOBTypeHandler.class));
            Gir.log.info("Oracle BLOB/CLOB类型处理器注册成功");
        } catch (Throwable e) {
            System.err.println("Oracle BLOB/CLOB类型处理器注册失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    static {
        try {
            // Oracle空间类型处理器注册
            TypeHandlerRegistry.register(
                    oracle.sql.STRUCT.class, Singleton.get(OracleGeomTypeHandler.class));
            Gir.log.info("Oracle空间类型处理器注册成功");
        } catch (Throwable e) {
            System.err.println("Oracle空间类型处理器注册失败: " + e.getMessage());
            e.printStackTrace();
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
