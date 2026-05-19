package cn.geoair.map.dynamic.file.shp;

import cn.geoair.map.dynamic.file.core.link.LinkInfo;
import java.io.File;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/** Shapefile 文件链接信息 注意：shp 必须配套 .shx/.dbf 等文件，统一放在同一目录 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
public class ShpLinkInfo extends LinkInfo {

    /** shp 文件路径（.shp 文件） */
    private String shpFilePath;

    /** 文件编码（GBK/UTF-8，shp默认GBK） */
    private String charset = "GBK";

    /** 空间坐标系 SRID */
    private int srid = 4326;

    /** 校验 shp 文件合法性 */
    @Override
    public void checkLinkInfo() {
        if (shpFilePath == null || shpFilePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Shapefile 文件路径不能为空");
        }

        File shpFile = new File(shpFilePath);
        // 读取场景：文件必须存在
        if (shpFile.exists()) {
            if (!shpFile.isFile()) {
                throw new IllegalArgumentException("指定路径不是文件：" + shpFilePath);
            }
            if (!shpFile.getName().toLowerCase().endsWith(".shp")) {
                throw new IllegalArgumentException("必须是 .shp 文件：" + shpFilePath);
            }
        } else {
            // 写入场景：创建父目录
            File parentDir = shpFile.getParentFile();
            if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                throw new IllegalArgumentException("无法创建 shp 父目录：" + parentDir.getAbsolutePath());
            }
        }
    }
}
