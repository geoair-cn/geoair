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

    /** 移除名称的路径 */
    String rootPath;

    /** 不带路径的文件名称 */
    String rootFileName;

    /** 根文件的带路径的全路径 */
    String rootFilePath;

    /** 根文件的标准名称 */
    String rootFileStandardName;
}
