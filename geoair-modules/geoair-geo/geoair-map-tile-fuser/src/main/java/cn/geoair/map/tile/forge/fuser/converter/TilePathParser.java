package cn.geoair.map.tile.forge.fuser.converter;

import java.io.File;

/** 瓦片文件路径解析器 从文件路径中解析出 z, x, y 坐标 */
@FunctionalInterface
public interface TilePathParser {
    /**
     * 解析瓦片文件路径
     *
     * @param file 瓦片文件
     * @param relativePath 相对于源根目录的路径（例如：5/12/34.png）
     * @param config 转换配置
     * @return TileInfo 对象，如果解析失败返回 null
     */
    MbtilesFromLocalFileConverter.TileInfo parse(
            File file, String relativePath, MbtilesFromLocalFileConverter.ConvertConfig config);

    /** 默认的瓦片路径解析器 支持格式：{z}/{y}/{x}.png 或 {z}/{x}/{y}.png 从路径中自动提取数字 */
    static final TilePathParser DEFAULT_ZYX_PARSER =
            (file, relativePath, config) -> {
                try {
                    // 按分隔符分割路径
                    String[] parts = relativePath.split("[/\\\\]");
                    if (parts.length < 3) {
                        return null;
                    }

                    MbtilesFromLocalFileConverter.TileInfo tile =
                            new MbtilesFromLocalFileConverter.TileInfo();
                    tile.path = file.toPath();

                    // 从文件名提取 x 或 y
                    String fileName = parts[parts.length - 1];
                    int dotIndex = fileName.lastIndexOf('.');
                    String nameWithoutExt =
                            dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;

                    // 尝试从文件名和父目录提取坐标
                    // 默认格式：z/y/x.png
                    try {
                        tile.z = Integer.parseInt(parts[0]);
                        tile.y = Integer.parseInt(parts[parts.length - 2]);
                        tile.x = Integer.parseInt(nameWithoutExt);
                        return tile;
                    } catch (NumberFormatException e) {
                        // 尝试其他格式：z/x/y.png
                        try {
                            tile.z = Integer.parseInt(parts[0]);
                            tile.x = Integer.parseInt(parts[parts.length - 2]);
                            tile.y = Integer.parseInt(nameWithoutExt);
                            return tile;
                        } catch (NumberFormatException e2) {
                            return null;
                        }
                    }
                } catch (Exception e) {
                    return null;
                }
            };

    /** 标准 TMS 格式解析器：{z}/{y}/{x}.png（Y轴需要翻转） */
    static final TilePathParser TMS_ZYX_PARSER =
            (file, relativePath, config) -> {
                try {
                    String[] parts = relativePath.split("[/\\\\]");
                    if (parts.length < 3) {
                        return null;
                    }

                    MbtilesFromLocalFileConverter.TileInfo tile =
                            new MbtilesFromLocalFileConverter.TileInfo();
                    tile.path = file.toPath();

                    String fileName = parts[parts.length - 1];
                    int dotIndex = fileName.lastIndexOf('.');
                    String nameWithoutExt =
                            dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;

                    tile.z = Integer.parseInt(parts[0]);
                    tile.y = Integer.parseInt(parts[parts.length - 2]);
                    tile.x = Integer.parseInt(nameWithoutExt);
                    return tile;
                } catch (Exception e) {
                    return null;
                }
            };

    /** Google 标准格式解析器：{z}/{x}/{y}.png（Y轴不需要翻转） */
    static final TilePathParser GOOGLE_ZXY_PARSER =
            (file, relativePath, config) -> {
                try {
                    String[] parts = relativePath.split("[/\\\\]");
                    if (parts.length < 3) {
                        return null;
                    }

                    MbtilesFromLocalFileConverter.TileInfo tile =
                            new MbtilesFromLocalFileConverter.TileInfo();
                    tile.path = file.toPath();

                    String fileName = parts[parts.length - 1];
                    int dotIndex = fileName.lastIndexOf('.');
                    String nameWithoutExt =
                            dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;

                    tile.z = Integer.parseInt(parts[0]);
                    tile.x = Integer.parseInt(parts[parts.length - 2]);
                    tile.y = Integer.parseInt(nameWithoutExt);
                    return tile;
                } catch (Exception e) {
                    return null;
                }
            };
}
