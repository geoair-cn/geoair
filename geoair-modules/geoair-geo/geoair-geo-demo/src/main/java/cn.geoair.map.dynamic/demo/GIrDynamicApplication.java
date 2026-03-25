package cn.geoair.map.dynamic.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
// @ComponentScan(value = "com.gir")
public class GIrDynamicApplication {

    public static void main(String[] args) {
        System.setProperty("GEOSERVER_DATA_DIR", "E:\\测试数据\\geoserver");
        System.setProperty("GEOWEBCACHE_CACHE_DIR", "E:\\测试数据\\geoserver\\gwc");
        SpringApplication.run(GIrDynamicApplication.class, args);
    }
}
