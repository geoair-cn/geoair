package cn.geoair.comp.message.converter.jts.jackson.config;

import cn.geoair.comp.message.converter.jts.jackson.utils.GirJacksonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class GirJtsJacksonAutoConfig implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(GirJtsJacksonAutoConfig.class);

    @Autowired(required = false)
    private List<ObjectMapper> objectMappers;

    @Override
    public void afterPropertiesSet() {
        if (objectMappers != null) {
            for (ObjectMapper objectMapper : objectMappers) {
                GirJacksonUtils.registerModule(objectMapper);
            }
        }
    }


}
