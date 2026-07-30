package cn.geoair.map.tile.forge.core.servlet;

public class TileParseResult {
    private String fileId;
    private String fileName;
    private String serviceName;
    private String z;
    private String x;
    private String y;
    private String format;
    private String fullPath; // 可选：完整路径

    // 构造函数
    public TileParseResult() {}

    public TileParseResult(String fileId, String fileName, String serviceName,
                          String z, String x, String y, String format) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.serviceName = serviceName;
        this.z = z;
        this.x = x;
        this.y = y;
        this.format = format;
    }

    // Getter和Setter方法
    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getZ() { return z; }
    public void setZ(String z) { this.z = z; }

    public String getX() { return x; }
    public void setX(String x) { this.x = x; }

    public String getY() { return y; }
    public void setY(String y) { this.y = y; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public String getFullPath() { return fullPath; }
    public void setFullPath(String fullPath) { this.fullPath = fullPath; }

    // 辅助方法：判断是否为瓦片
    public boolean isTile() {
        return z != null && x != null && y != null;
    }


    @Override
    public String toString() {
        return String.format("TileParseResult{fileId='%s', fileName='%s', serviceName='%s', z='%s', x='%s', y='%s', format='%s'}",
                           fileId, fileName, serviceName, z, x, y, format);
    }
}
