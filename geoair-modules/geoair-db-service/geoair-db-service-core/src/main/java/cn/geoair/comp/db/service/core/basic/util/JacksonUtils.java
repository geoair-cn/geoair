package cn.geoair.comp.db.service.core.basic.util;

import cn.geoair.comp.message.converter.jts.jackson.utils.GirJacksonUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * @author ：张俊
 * @date ：Created in 2026/3/19 18:44
 * @description： TODO
 */
public class JacksonUtils {
    static ObjectMapper objectMapper = null;

    public static ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
            GirJacksonUtils.registerModule(objectMapper);
            // 全局配置：保留 null 值（一次性配置，无需每次设置）
            objectMapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, true);
            objectMapper.setDefaultPropertyInclusion(JsonInclude.Include.ALWAYS);
            // 可选：其他常用配置（和 FastJSON 对齐）
            objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false); // 空对象不报错
            objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true); // 日期转时间戳
        }
        return objectMapper;
    }

    public static String toJSONString(Object obj) {
        try {
            return getObjectMapper().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Jackson 序列化 JSON 失败", e);
        }
    }
}
