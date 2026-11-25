package cn.geoair.comp.message.converter.jts.jackson.serializer.oracle.module;

import cn.geoair.comp.message.converter.jts.jackson.serializer.oracle.StdOracleJsonObjectSerializer;

import com.fasterxml.jackson.databind.module.SimpleModule;

import oracle.sql.json.OracleJsonObject;

/**
 * @author ：张俊
 * @date ：Created in 2026/4/21 09:31
 * @description： TODO
 */
public class OracleJsonObjectModule extends SimpleModule {
    public OracleJsonObjectModule() {
        addSerializer(OracleJsonObject.class, new StdOracleJsonObjectSerializer());
    }
}
