package cn.geoair.map.tile.forge.fuser.encoding;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 一次任务或图层写入的编码参数，透明保护始终开启。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TileEncodingPolicy {
    private String mode = TileEncodingMode.LEGACY.name();
    private int quality = 75;

    public void validate() {
        mode = TileEncodingMode.parse(mode).name();
        if (quality < 1 || quality > 100) throw new IllegalArgumentException("JPEG质量须在1～100之间");
    }
}
