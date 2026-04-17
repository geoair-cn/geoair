package cn.geoair.comp.message.converter.jts.jackson.serializer.oracle.module;

import cn.geoair.comp.message.converter.jts.jackson.serializer.oracle.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import oracle.sql.BLOB;
import oracle.sql.CLOB;
import oracle.sql.NCLOB;

public class OracleTypesModule extends SimpleModule {

    public OracleTypesModule() {

        addSerializer(NCLOB.class, new OracleNClobSerializer());
        addDeserializer(NCLOB.class, new OracleNClobDeserializer());

        addSerializer(CLOB.class, new OracleClobSerializer());
        addDeserializer(CLOB.class, new OracleClobDeserializer());

        addSerializer(BLOB.class, new OracleBlobSerializer());
        addDeserializer(BLOB.class, new OracleBlobDeserializer());
    }
}
