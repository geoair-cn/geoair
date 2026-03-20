package cn.geoair.comp.message.converter.jts.jackson.config;

import cn.geoair.comp.message.converter.jts.jackson.utils.GirJacksonUtils;
import cn.hutool.extra.spring.SpringUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.util.Map;


public class GirJtsJacksonAutoConfig implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(GirJtsJacksonAutoConfig.class);


    @Override
    public void afterPropertiesSet() {
        Map<String, ObjectMapper> beansOfType = SpringUtil.getBeansOfType(ObjectMapper.class);
        if (beansOfType != null) {
            beansOfType.forEach((name, objectMapper) -> {
                GirJacksonUtils.registerModule(objectMapper);
            });
        }


    }


}
