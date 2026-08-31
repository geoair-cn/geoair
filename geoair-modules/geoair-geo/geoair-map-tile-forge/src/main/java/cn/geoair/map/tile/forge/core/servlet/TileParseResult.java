package cn.geoair.map.tile.forge.core.servlet;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TileParseResult {
    private String fileId;
    private String fileName;
    private String serviceName;
    private String z;
    private String x;
    private String y;
    private String format;
    private String fullPath; // 可选：完整路径
    private String requestURI; // 原始请求URI   不带ip和端口的真实url
    private String requestHost; // 原始请求 的ip和端口。http开头
    private String contentAfterPrefix; // prefixName后的内容

    public static TileParseResult of() {
        return new TileParseResult();
    }

    // 构造函数
    public TileParseResult() {}

    // 辅助方法：判断是否为瓦片
    public boolean isTile() {
        return z != null && x != null && y != null;
    }

    public boolean isValid() {
        return fileId != null && fileName != null && serviceName != null;
    }

    @Override
    public String toString() {
        return String.format(
                "TileParseResult{fileId='%s', fileName='%s', serviceName='%s', z='%s', x='%s', y='%s', format='%s'}",
                fileId, fileName, serviceName, z, x, y, format);
    }
}
