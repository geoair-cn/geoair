package cn.geoair.comp.message.converter.jts.jackson.utils;

import cn.geoair.comp.message.converter.jts.jackson.serializer.jts.JtsExtModule;
import cn.geoair.comp.message.converter.jts.jackson.serializer.oracle.module.OracleStdTypesFullModule;
import cn.geoair.comp.message.converter.jts.jackson.serializer.oracle.module.OracleTypesModule;
import cn.geoair.comp.message.converter.jts.jackson.serializer.pggeom.net.NetPGGeometryModule;
import cn.geoair.comp.message.converter.jts.jackson.serializer.pggeom.org.OrgPGGeometryModule;
import cn.geoair.map.dynamic.tools.convert.GirOracleTran;
import cn.geoair.map.dynamic.tools.convert.GirPostGisTran;
import com.bedatadriven.jackson.datatype.jts.JtsModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

/**
 * @author ：张俊
 * @date ：Created in 2026/3/19 18:47 @description： TODO
 */
@Slf4j
public class GirJacksonUtils {

    public static void registerModule(ObjectMapper objectMapper) {
        objectMapper.registerModule(new JtsModule());
        log.debug("JtsModule注册");
        objectMapper.registerModule(new JtsExtModule());
        log.debug("JtsExtModule注册");
        if (GirPostGisTran.isPostGisAvailable() && GirPostGisTran.isNetConvert()) {
            objectMapper.registerModule(new NetPGGeometryModule());
            log.debug("NetPGGeometryModule 注册");
        }
        if (GirPostGisTran.isPostGisAvailable() && GirPostGisTran.isOrgConvert()) {
            objectMapper.registerModule(new OrgPGGeometryModule());
            log.debug("OrgPGGeometryModule 注册");
        }
        if (GirOracleTran.isOracleSpatialAvailable()) {
            objectMapper.registerModule(new OracleStdTypesFullModule());
            objectMapper.registerModule(new OracleTypesModule());
            log.debug("OracleStdTypesFullModule ,OracleTypesModule注册");
        }
    }
}
