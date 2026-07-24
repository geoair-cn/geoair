package cn.geoair.map.tile.forge.fuser.provider.impl.grid4490;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.dynamic.tools.GirAdvTools;
import cn.geoair.map.tile.forge.core.bygwc.io.ByteArrayResource;
import cn.geoair.map.tile.forge.core.bygwc.io.Resource;
import cn.geoair.map.tile.forge.fuser.entity.PxyLayerInfo;
import cn.geoair.map.tile.forge.fuser.enums.OriginType;
import cn.geoair.map.tile.forge.fuser.provider.BaseTileGetter;
import cn.geoair.web.mime.GiMimeType;
import cn.hutool.core.io.FileUtil;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;

import javax.imageio.ImageIO;

/**
 * 本地文件瓦片获取器（仅负责文件读取，不包含缓存逻辑）
 *
 * @author 张俊
 * @date Created in 2026/5/9 14:10
 */
public class Grid4490LocalFileTileGetter extends BaseTileGetter {
    private static GiLogger log = GirLoggerFactory.getLogger();
    private final String filePathTemplate;

    public Grid4490LocalFileTileGetter(PxyLayerInfo layerInfo) {
        super(layerInfo);
        this.filePathTemplate = layerInfo.getPath();
    }

    @Override
    public Resource getTileResource(int z, int x, int y) {

        OriginType originType = OriginType.fromMode(super.getLayerInfo().getOriginType());
        if (originType.isGoogle()) {
            y = GirAdvTools.getTileGrid4326SeparateOpt().reverseY(y, z);
        }
        String filePath =
                filePathTemplate
                        .replace("{z}", String.valueOf(z))
                        .replace("{x}", String.valueOf(x))
                        .replace("{y}", String.valueOf(y));

        File file = FileUtil.file(filePath);
        log.debug("尝试获取瓦片文件: {}", file.getAbsolutePath());

        if (!FileUtil.exist(file)) {
            log.debug("本地瓦片文件不存在: {}", file.getAbsolutePath());
            return null;
        }

        try {
            BufferedImage read = ImageIO.read(file);
            byte[] imageBytes;

            if (read != null) {
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    GiMimeType srcFormat = getSrcFormat();
                    String internalName = srcFormat.getInternalName();
                    ImageIO.write(read, internalName, baos);
                    imageBytes = baos.toByteArray();
                }
                log.info("从本地文件读取瓦片成功（转换为PNG）: {}", file.getAbsolutePath());
            } else {
                imageBytes = FileUtil.readBytes(file);
                log.debug("从本地文件读取瓦片成功（原始格式）: {}", file.getAbsolutePath());
            }

            return new ByteArrayResource(imageBytes);
        } catch (Exception e) {
            log.error("读取本地瓦片文件失败: {}", file.getAbsolutePath(), e);
            return null;
        }
    }
}
