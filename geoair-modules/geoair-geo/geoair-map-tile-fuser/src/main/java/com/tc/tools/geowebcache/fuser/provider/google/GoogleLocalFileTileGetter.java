package com.tc.tools.geowebcache.fuser.provider.google;

import cn.geoair.map.dynamic.tools.GirAdvTools;
import cn.geoair.map.tile.forge.core.bygwc.io.ByteArrayResource;
import cn.geoair.map.tile.forge.core.bygwc.io.Resource;
import cn.hutool.core.io.FileUtil;

import com.tc.tools.geowebcache.fuser.entity.PxyLayerInfo;
import com.tc.tools.geowebcache.fuser.enums.OriginType;

import com.tc.tools.geowebcache.fuser.provider.BaseTileGetter;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;

/**
 * 本地文件瓦片获取器（仅负责文件读取，不包含缓存逻辑）
 *
 * @author 张俊
 * @date Created in 2026/5/9 14:10
 */
@Slf4j
public class GoogleLocalFileTileGetter extends BaseTileGetter {

    private final String filePathTemplate;




    public GoogleLocalFileTileGetter(PxyLayerInfo config) {
        super(config);
        this.filePathTemplate = config.getPath();

    }

    @Override
    public Resource getTileResource(int z, int x, int y) {

        OriginType originType = OriginType.fromMode(super.getLayerInfo().getOriginType());
        if (originType.isGoogle()) {
            y= GirAdvTools.getTileGrid3857Opt().reverseY(y, z);
        }
        String filePath = filePathTemplate.replace("{z}", String.valueOf(z))
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
                    ImageIO.write(read, "png", baos);
                    imageBytes = baos.toByteArray();
                }
                log.debug("从本地文件读取瓦片成功（转换为PNG）: {}", file.getAbsolutePath());
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
