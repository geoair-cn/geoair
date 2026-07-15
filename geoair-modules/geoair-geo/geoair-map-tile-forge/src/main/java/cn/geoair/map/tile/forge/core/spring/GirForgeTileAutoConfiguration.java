package cn.geoair.map.tile.forge.core.spring;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.tile.forge.core.GirLayerConfigContextHelper;
import cn.geoair.map.tile.forge.core.config.SpringProviderConfig;
import cn.geoair.map.tile.forge.core.config.TileTempPathConfig;
import cn.geoair.map.tile.forge.core.service.GirMapTileService;
import cn.geoair.map.tile.forge.core.support.TileStorageSupportAdapter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@EnableConfigurationProperties({SpringProviderConfig.class, TileTempPathConfig.class})
public class GirForgeTileAutoConfiguration {

    private static final GiLogger log = GirLoggerFactory.getLogger(GirForgeTileAutoConfiguration.class);


    private final ObjectProvider<GirLayerConfigContextHelper> contextHelperProvider;

    public GirForgeTileAutoConfiguration(
            ObjectProvider<GirLayerConfigContextHelper> contextHelperProvider) {
        this.contextHelperProvider = contextHelperProvider;
    }

    @Bean
    @ConditionalOnMissingBean
    public TileStorageSupportAdapter tileStorageSupportAdapter() {
        GirLayerConfigContextHelper contextHelper = contextHelperProvider.getIfAvailable();

        if (contextHelper == null) {
            String errorMsg = String.join("\n",
                    "========================================",
                    "【GirForge 启动失败】",
                    "依赖 Bean GirLayerConfigContextHelper 未找到！",
                    "========================================",
                    "解决方案：",
                    "1. 客户端手动实现 GirLayerConfigContextHelper 该接口",
                    "========================================"
            );
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        return new TileStorageSupportAdapter(contextHelper);
    }

    @Bean
    @ConditionalOnMissingBean
    public GirMapTileService girMapTileService(TileStorageSupportAdapter adapter) {
        GirLayerConfigContextHelper contextHelper = contextHelperProvider.getIfAvailable();

        if (contextHelper == null) {
            throw new IllegalStateException(
                    "GirLayerConfigContextHelper Bean 不存在，无法创建 GirMapTileService"
            );
        }


        return new GirMapTileService(contextHelper, adapter);
    }
}
