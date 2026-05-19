package cn.geoair.comp.message.converter.jts.jackson.serializer.oracle.module;

import cn.geoair.comp.message.converter.jts.jackson.serializer.oracle.OracleSdoGeometrySerializer;

import com.fasterxml.jackson.databind.module.SimpleModule;

import oracle.sql.STRUCT;

/**
 * @author ：张俊
 * @date ：Created in 2026/4/18 10:03
 * @description： TODO
 */
public class SdoGeometryModule extends SimpleModule {

    public SdoGeometryModule() {
        addSerializer(STRUCT.class, new OracleSdoGeometrySerializer());
    }
}
