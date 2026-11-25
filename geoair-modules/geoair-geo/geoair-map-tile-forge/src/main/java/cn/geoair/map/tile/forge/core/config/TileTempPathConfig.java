// 文件路径: src/main/java/cn/geoair/arcgis/tile/config/TileTempPathConfig.java

package cn.geoair.map.tile.forge.core.config;

import cn.geoair.base.Gir;
import cn.geoair.map.tile.forge.core.enums.GirStorageType;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.hutool.core.io.FileUtil;
import java.io.File;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** 瓦片服务临时路径配置类 用于管理瓦片操作过程中的本地临时文件存储路径 */
@Data
@ConfigurationProperties(prefix = "geoair.file.temp")
public class TileTempPathConfig {

    public TileTempPathConfig() {
        instance = this;
    }

    static TileTempPathConfig instance;

    public static TileTempPathConfig getInstance() {
        if (instance == null) {
            try {
                instance = Gir.beans.getBean(TileTempPathConfig.class);
            } catch (Exception e) {
                Gir.log.error("无法在beans中获取到TileTempPathConfig对象，使用默认对象");
                return new TileTempPathConfig();
            }
        }
        return instance;
    }

    /** 临时文件存储根路径 默认值: I:\arcgisTest\temp 可通过 application.yml 配置 tile.temp.path 修改 */
    private String tempPath = FileUtil.getTmpDirPath();

    /**
     * 设置临时路径 (允许程序运行时动态修改)
     *
     * @param path 新的临时路径
     */
    public void setTempPath(String path) {
        this.tempPath = path;
    }

    /**
     * 获取临时路径
     *
     * @return 当前配置的临时路径
     */
    public String getTempPath() {
        return tempPath;
    }

    /**
     * 构建本地临时目录路径
     *
     * @param layerConfigContext 图层配置
     * @return 临时目录的绝对路径
     */
    public String buildLocalTempDirPath(GirLayerConfigContext layerConfigContext) {
        String layerName = layerConfigContext.getLayerName();
        GirStorageType girStorageType = layerConfigContext.getStorageType();
        String separator = getTempPath().endsWith(File.separator) ? "" : File.separator;
        File tempDir =
                FileUtil.file(
                        tempPath
                                + separator
                                + layerName
                                + File.separator
                                + girStorageType.getValue());
        String tempDirAbsolutePath = tempDir.getAbsolutePath();

        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        return tempDirAbsolutePath;
    }
}
