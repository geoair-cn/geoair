package cn.geoair.map.tile.forge.fuser.encoding;

import lombok.Getter;

/** 缓存编码与服务响应格式独立维护。 */
@Getter
public enum TileEncodingMode {
    LEGACY("沿用现有方式"),
    SOURCE("保留源格式"),
    JPEG("JPEG压缩"),
    PNG("PNG无损");
    private final String description;

    TileEncodingMode(String description) {
        this.description = description;
    }

    public static TileEncodingMode parse(String value) {
        if (value == null || value.trim().isEmpty()) return LEGACY;
        for (TileEncodingMode mode : values()) if (mode.name().equalsIgnoreCase(value)) return mode;
        throw new IllegalArgumentException("缓存编码策略无效");
    }
}
