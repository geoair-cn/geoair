package cn.geoair.map.tile.forge.core.zip.model;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author ：张俊
 * @date ：Created in 2026/7/3 15:47
 * @description： 根位置的文件模型对象
 */
@Data
@Accessors(chain = true)
public class RootPathInfo {

    public static RootPathInfo of() {
        return new RootPathInfo();
    }

    String rootPath;

    String rootFileName;

    String rootFilePath;

    String rootFileExtension;


}
