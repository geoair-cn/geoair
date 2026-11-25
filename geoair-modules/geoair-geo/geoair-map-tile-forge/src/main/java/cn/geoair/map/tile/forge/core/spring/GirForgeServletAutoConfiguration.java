package cn.geoair.map.tile.forge.core.spring;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.tile.forge.core.service.GirMapTileService;
import cn.geoair.map.tile.forge.core.servlet.D3TerrainServlet;
import cn.geoair.map.tile.forge.core.servlet.D3TilesServlet;
import cn.geoair.map.tile.forge.core.servlet.MvtTilesServlet;
import cn.geoair.map.tile.forge.core.servlet.XYZServlet;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.function.Function;

@Configuration
@ConditionalOnWebApplication
public class GirForgeServletAutoConfiguration {

    private static final GiLogger log =
            GirLoggerFactory.getLogger(GirForgeServletAutoConfiguration.class);

    private GirMapTileService getRequiredService(
            ObjectProvider<GirMapTileService> provider, String servletName) {
        GirMapTileService service = provider.getIfAvailable();
        if (service == null) {
            String errorMsg =
                    String.format(
                            "【启动失败】GirMapTileService Bean 不存在！%n"
                                    + "无法创建 %s。%n"
                                    + "请确保 GirForgeTileAutoConfiguration 已正确加载并初始化。",
                            servletName);
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        return service;
    }

    private <T> T createServlet(
            ObjectProvider<GirMapTileService> provider,
            String servletName,
            Function<GirMapTileService, T> creator) {
        GirMapTileService service = getRequiredService(provider, servletName);
        log.info("{} 初始化成功", servletName);
        return creator.apply(service);
    }

    @Bean
    @Primary
    public D3TilesServlet d3TilesServlet(ObjectProvider<GirMapTileService> provider) {
        return createServlet(provider, "D3TilesServlet", D3TilesServlet::new);
    }

    @Bean
    @Primary
    public D3TerrainServlet d3TerrainServlet(ObjectProvider<GirMapTileService> provider) {
        return createServlet(provider, "D3TerrainServlet", D3TerrainServlet::new);
    }

    @Bean
    @Primary
    public XYZServlet xyzServlet(ObjectProvider<GirMapTileService> provider) {
        return createServlet(provider, "XYZServlet", XYZServlet::new);
    }

    @Bean
    @Primary
    public MvtTilesServlet mvtTilesServlet(ObjectProvider<GirMapTileService> provider) {
        return createServlet(provider, "MvtTilesServlet", MvtTilesServlet::new);
    }
}
