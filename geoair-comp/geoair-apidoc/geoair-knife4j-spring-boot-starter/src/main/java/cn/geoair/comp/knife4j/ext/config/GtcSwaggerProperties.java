package cn.geoair.comp.knife4j.ext.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

;

@Component
@ConfigurationProperties(prefix = "gtc.gtc-apidoc")
public class GtcSwaggerProperties {
    /**
     *  是否启用swagger注解
     */
    private boolean enable = false;


    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }
}
