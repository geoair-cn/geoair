package cn.geoair.comp.message.converter.jts.jackson.serializer.oracle.module;

import cn.geoair.comp.message.converter.jts.jackson.serializer.oracle.*;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;

public class OracleStdTypesFullModule extends SimpleModule {
    public OracleStdTypesFullModule() {
        // 大字段类型
        addSerializer(NClob.class, new StdNClobSerializer());
        addDeserializer(NClob.class, new StdNClobDeserializer());

        addSerializer(Clob.class, new StdClobSerializer());


        addSerializer(Blob.class, new StdBlobSerializer());

        // RAW 二进制类型
        addSerializer(byte[].class, new StdRawSerializer());
        addDeserializer(byte[].class, new StdRawDeserializer());



    }
}
