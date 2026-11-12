package cn.geoair.map.tile.forge.core.support.arcgis;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.tile.forge.core.GirLayerConfigContextHelper;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.zip.ProgressConsumer;
import cn.geoair.map.tile.forge.core.zip.cache.ZipArchiveIndexBuilder;
import cn.geoair.map.tile.forge.core.zip.cache.ZipDirectoryGetter;
import cn.geoair.map.tile.forge.core.zip.ZipResourceReader;
import java.util.List;
/**
 * @author ：张俊
 * &#064;date ：Created in 2025/11/17 10:16
 * &#064;description：本地配置XML获取器抽象类，用于从本地文件系统读取ArcGIS图层配置文件
 */
 
public abstract class AbstractArcgisZipDirectoryGetter extends AbstractArcgisSupport implements ZipDirectoryGetter {
    public static GiLogger log = GirLoggerFactory.getLogger();
    protected final ZipResourceReader zipResources;
    public AbstractArcgisZipDirectoryGetter(GirLayerConfigContextHelper contextHelper) {
        super(contextHelper);
        this.zipResources = new ZipResourceReader(this);
    }

    @Override
    public GirLayerConfigContextHelper getContextHelper() {
        return contextHelper;
    }

    public void preCacheCentralDir(GirLayerConfigContext layerConfigContext, List<ProgressConsumer> progressConsumers) {
        ZipArchiveIndexBuilder.build(layerConfigContext, this, progressConsumers);
    }


}
